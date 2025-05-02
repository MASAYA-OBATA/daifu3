package com.example.daifu3.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daifu3.ai.CpuPlayerAgent
import com.example.daifu3.data.*
import com.example.daifu3.game.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _selectedCards = MutableStateFlow<List<Card>>(emptyList())
    val selectedCards: StateFlow<List<Card>> = _selectedCards.asStateFlow()

    private val _cardsToExchange = MutableStateFlow<List<Card>>(emptyList())
    val cardsToExchange: StateFlow<List<Card>> = _cardsToExchange.asStateFlow()

    private val _autoConfirmCountdown = MutableStateFlow(-1)
    val autoConfirmCountdown: StateFlow<Int> = _autoConfirmCountdown.asStateFlow()

    init {
        startNewGame(listOf("あなた", "CPU 1", "CPU 2", "CPU 3"))
    }

    private fun startNewGame(playerNames: List<String>) {
        viewModelScope.launch {
            val players = playerNames.mapIndexed { index, name ->
                Player(
                    id = "player_${index}",
                    name = name,
                    hand = emptyList(),
                    isHuman = index == 0
                )
            }
            val deck = DaifugoRuleEngine.createShuffledDeck()
            val (dealtPlayers, _) = DaifugoRuleEngine.dealCards(players, deck)

            // 手札を弱い順にソート
            val sortedPlayers = dealtPlayers.map { player ->
                player.copy(
                    hand = player.hand.sortedBy { RuleEvaluator.getCardStrength(it, false) }
                )
            }

            _gameState.value = GameState(
                players = sortedPlayers,
                currentPlayerIndex = 0,
                gamePhase = GamePhase.PLAYING,
                gameMessage = "ゲームスタート！ ${sortedPlayers[0].name}のターンです"
            )
        }
    }

    fun startNewGame() {
        viewModelScope.launch {
            val playerNames = listOf("あなた", "CPU 1", "CPU 2", "CPU 3")
            val players = playerNames.mapIndexed { index, name ->
                Player(
                    id = "player_${index}",
                    name = name,
                    hand = emptyList(),
                    isHuman = index == 0,
                    score = 0,  // スコアをリセット
                    rank = PlayerRank.HEIMIN  // ランクをリセット
                )
            }
            val deck = DaifugoRuleEngine.createShuffledDeck()
            val (dealtPlayers, _) = DaifugoRuleEngine.dealCards(players, deck)

            // 手札を弱い順にソート
            val sortedPlayers = dealtPlayers.map { player ->
                player.copy(
                    hand = player.hand.sortedBy { RuleEvaluator.getCardStrength(it, false) }
                )
            }

            _gameState.value = GameState(
                players = sortedPlayers,
                currentPlayerIndex = 0,
                gamePhase = GamePhase.PLAYING,
                gameMessage = "新しいゲームを開始しました！ ${sortedPlayers[0].name}のターンです"
            )
            _selectedCards.value = emptyList()
            _cardsToExchange.value = emptyList()
            _autoConfirmCountdown.value = -1
        }
    }

    fun toggleCardSelection(card: Card) {
        // 即座に状態を更新するために、viewModelScopeを使わない
        val current = _selectedCards.value.toMutableList()
        if (current.contains(card)) {
            current.remove(card)
        } else {
            current.add(card)
        }
        _selectedCards.value = current
    }

    fun isSelected(card: Card): Boolean {
        return _selectedCards.value.contains(card)
    }

    fun toggleExchangeCardSelection(card: Card) {
        // 即座に状態を更新するために、viewModelScopeを使わない
        val current = _cardsToExchange.value.toMutableList()
        val state = _gameState.value
        val humanPlayer = state.players.find { it.isHuman } ?: return
        val exchangeCount = abs(state.cardsToExchangeInfo[humanPlayer.id] ?: 0)
        val rank = humanPlayer.rank

        if (current.contains(card)) {
            current.remove(card)
        } else {
            // 大貧民・貧民の場合、最強ランクのカードだけ選択可能
            if (rank == PlayerRank.DAIHINMIN || rank == PlayerRank.HINMIN) {
                // 最強ランクのカードを特定
                val sortedCards = humanPlayer.hand.sortedByDescending {
                    RuleEvaluator.getCardStrength(it, state.isRevolution)
                }
                val strongestRank = sortedCards.firstOrNull()?.rank

                // 選択しようとしているカードが最強ランクでない場合は拒否
                if (card.rank != strongestRank) {
                    _gameState.value = state.copy(
                        gameMessage = "大貧民・貧民は最強ランクのカードしか選択できません"
                    )
                    return
                }
            }

            // 交換枚数の制限を確認
            if (current.size < exchangeCount) {
                current.add(card)
            }
        }
        _cardsToExchange.value = current
    }

    fun isSelectedForExchange(card: Card): Boolean {
        return _cardsToExchange.value.contains(card)
    }

    fun startCardExchangeSelection() {
        viewModelScope.launch {
            val state = _gameState.value
            val humanPlayer = state.players.find { it.isHuman } ?: return@launch
            val exchangeCount = abs(state.cardsToExchangeInfo[humanPlayer.id] ?: 0)

            if (exchangeCount <= 0) return@launch

            val rank = humanPlayer.rank

            if (rank == PlayerRank.DAIHINMIN || rank == PlayerRank.HINMIN) {
                // 大貧民・貧民の場合

                // 同位札を含めた最強カードを見つける
                val sortedCards = humanPlayer.hand.sortedByDescending {
                    RuleEvaluator.getCardStrength(it, state.isRevolution)
                }

                // 最強カードのランク
                val strongestRank = sortedCards.firstOrNull()?.rank

                // 同じランクのカードを集める
                val sameRankCards = if (strongestRank != null) {
                    sortedCards.filter { it.rank == strongestRank }
                } else {
                    emptyList()
                }

                if (sameRankCards.size > exchangeCount) {
                    // 同位札があり選択が必要

                    // 5秒のカウントダウンを開始
                    _autoConfirmCountdown.value = 5

                    // カウントダウンを進める
                    for (i in 4 downTo 0) {
                        delay(1000)
                        _autoConfirmCountdown.value = i

                        // ユーザーが既に選択を完了していたら中断
                        if (_cardsToExchange.value.size == exchangeCount) {
                            break
                        }
                    }

                    // カウントダウン終了後も選択が完了していなければ自動選択
                    if (_cardsToExchange.value.size != exchangeCount) {
                        val autoSelected = sameRankCards.take(exchangeCount)
                        _cardsToExchange.value = autoSelected

                        // 0.5秒待ってから自動確定
                        delay(500)
                        confirmCardExchange()
                    } else {
                        // 選択完了していたら確認メッセージ
                        _gameState.value = state.copy(
                            gameMessage = "カードが選択されました。交換を確定してください。"
                        )
                        _autoConfirmCountdown.value = -1
                    }
                } else {
                    // 同位札がなく選択の余地がない場合は自動選択
                    val cardsToGive = sameRankCards.take(exchangeCount)
                    _cardsToExchange.value = cardsToGive

                    // 自動選択されたことを表示するメッセージ
                    _gameState.value = state.copy(
                        gameMessage = "最強カード${exchangeCount}枚が自動的に選択されました。交換を確定してください。"
                    )
                }
            }
        }
    }

    fun playSelectedCards() {
        viewModelScope.launch {
            val currentState = _gameState.value
            val selected = _selectedCards.value
            val currentPlayer = currentState.currentPlayer

            if (selected.isEmpty()) {
                _gameState.value = currentState.copy(
                    gameMessage = "カードを選んでください"
                )
                return@launch
            }

            // 場のカードが前回自分が出したものかチェック（全員パスして戻ってきた場合）
            val isOwnField = currentState.lastPlayerToPlayIndex == currentState.currentPlayerIndex &&
                    currentState.fieldCards.isNotEmpty()

            // 自分のフィールドなら自由に出せる、そうでなければ通常のルールチェック
            val isValidPlay = if (isOwnField) {
                // 自分のフィールドの場合は基本的なチェックのみ
                RuleEvaluator.canPlayCards(selected, currentState.isRevolution)
            } else {
                // 通常のルールチェック
                RuleEvaluator.isValidPlay(selected, currentState.fieldCards, currentState)
            }

            if (!isValidPlay) {
                _gameState.value = currentState.copy(
                    gameMessage = "無効なプレイです"
                )
                return@launch
            }

            // 残りがジョーカーのみになる場合はプレイ禁止
            if (currentPlayer != null) {
                val remainingCards = currentPlayer.hand.filterNot { selected.contains(it) }
                if (remainingCards.isNotEmpty() && remainingCards.all { it.isJoker }) {
                    _gameState.value = currentState.copy(
                        gameMessage = "残りがジョーカーのみになるようなプレイはできません"
                    )
                    return@launch
                }
            }

            // GameStateUpdaterを使って状態更新
            val newState = GameStateUpdater.applyPlay(currentState, selected)
            _gameState.value = newState
            _selectedCards.value = emptyList()

            // ゲームフェーズによって次の処理を決定
            when (newState.gamePhase) {
                GamePhase.PLAYING -> processCpuTurns()
                GamePhase.CARD_EXCHANGE -> handleCardExchange()
                GamePhase.ROUND_OVER -> prepareNextRound()
                else -> {} // その他のフェーズは処理しない
            }
        }
    }

    fun passTurn() {
        viewModelScope.launch {
            val currentState = _gameState.value

            // 場に何もないときはパスできない（最初のプレイヤー）
            if (currentState.fieldCards.isEmpty()) {
                _gameState.value = currentState.copy(
                    gameMessage = "最初のプレイヤーはパスできません。カードを出してください。"
                )
                return@launch
            }

            // 自分が出したカードに対して自分はパスできない（全員パスした後に自分の番に戻った場合）
            if (currentState.lastPlayerToPlayIndex == currentState.currentPlayerIndex) {
                _gameState.value = currentState.copy(
                    gameMessage = "自分が出したカードに対しては自分はパスできません。カードを出してください。"
                )
                return@launch
            }

            val newState = GameStateUpdater.applyPass(currentState)
            _gameState.value = newState

            // CPU処理
            if (newState.gamePhase == GamePhase.PLAYING) {
                processCpuTurns()
            }
        }
    }

    fun confirmCardExchange() {
        viewModelScope.launch {
            val state = _gameState.value
            val humanPlayer = state.players.find { it.isHuman }
            if (humanPlayer == null) {
                handleCardExchange() // 人間プレイヤーがいない場合は自動処理
                return@launch
            }

            val exchangeCount = abs(state.cardsToExchangeInfo[humanPlayer.id] ?: 0)
            val humanRank = humanPlayer.rank

            // 交換を実行
            val exchanges = mutableMapOf<String, List<Card>>()

            // 人間プレイヤーの処理
            if (exchangeCount > 0) {
                // 人間が大富豪または富豪の場合は選択したカードを使用
                if (humanRank == PlayerRank.DAIFUGO || humanRank == PlayerRank.FUGO) {
                    if (_cardsToExchange.value.size != exchangeCount) {
                        // 選択枚数が足りない
                        _gameState.value = state.copy(
                            gameMessage = "${exchangeCount}枚のカードを選択してください"
                        )
                        return@launch
                    }
                    exchanges[humanPlayer.id] = _cardsToExchange.value
                } else {
                    // それ以外のランクでは交換なし
                    exchanges[humanPlayer.id] = emptyList()
                }
            } else if (exchangeCount < 0) {
                // 人間が大貧民または貧民の場合は強制選択または既に選択されたカードを使用
                if (humanRank == PlayerRank.DAIHINMIN || humanRank == PlayerRank.HINMIN) {
                    if (_cardsToExchange.value.size != -exchangeCount) {
                        // 選択枚数が足りない場合は自動選択
                        val cardsToGive = ExchangeHelper.getStrongestCardsForExchange(
                            humanPlayer.hand,
                            -exchangeCount,
                            state.isRevolution
                        )
                        exchanges[humanPlayer.id] = cardsToGive
                    } else {
                        // 既に選択されているカードを使用
                        exchanges[humanPlayer.id] = _cardsToExchange.value
                    }
                } else {
                    // それ以外のランクでは交換なし
                    exchanges[humanPlayer.id] = emptyList()
                }
            }

            // CPU自動交換
            for (player in state.players.filter { !it.isHuman }) {
                val count = state.cardsToExchangeInfo[player.id] ?: 0
                val rank = player.rank

                if (count > 0) {
                    // もらう側（大富豪または富豪）は任意のカードを渡す
                    // CPUの場合は最弱カードを選択
                    if (rank == PlayerRank.DAIFUGO || rank == PlayerRank.FUGO) {
                        exchanges[player.id] = ExchangeHelper.getWeakestCardsForExchange(
                            player.hand,
                            count,
                            state.isRevolution
                        )
                    }
                } else if (count < 0) {
                    // 渡す側（大貧民または貧民）は最強カードを強制的に渡す
                    if (rank == PlayerRank.DAIHINMIN || rank == PlayerRank.HINMIN) {
                        exchanges[player.id] = ExchangeHelper.getStrongestCardsForExchange(
                            player.hand,
                            -count,
                            state.isRevolution
                        )
                    }
                }
            }

            _gameState.value = GameStateUpdater.applyCardExchange(state, exchanges)
            _cardsToExchange.value = emptyList()
            _autoConfirmCountdown.value = -1

            // 次のラウンド開始
            processCpuTurns()
        }
    }

    private suspend fun processCpuTurns() {
        // 人間プレイヤーのターンになるまでCPUの処理を続ける
        while (_gameState.value.currentPlayer?.isHuman == false &&
            _gameState.value.gamePhase == GamePhase.PLAYING) {

            // 少し遅延を入れて、CPUの思考時間を演出
            delay(1000)

            val state = _gameState.value
            val cpuPlayer = state.currentPlayer!!

            // 場のカードが前回CPUが出したものかチェック（全員パスして戻ってきた場合）
            val isOwnField = state.lastPlayerToPlayIndex == state.currentPlayerIndex &&
                    state.fieldCards.isNotEmpty()

            // 場に何もない場合またはCPU自身が最後にカードを出した場合はパスできない
            if (state.fieldCards.isEmpty() || isOwnField) {
                // 有効なプレイを全て生成
                val possiblePlays = PlayGenerator.generatePossiblePlays(
                    cpuPlayer.hand,
                    state.isRevolution
                ).filter { play ->
                    // 残りがジョーカーのみになるプレイをフィルタリング
                    val remainingCards = cpuPlayer.hand.filterNot { card -> play.contains(card) }
                    !(remainingCards.isNotEmpty() && remainingCards.all { it.isJoker }) &&
                            if (isOwnField) {
                                // 自分のフィールドの場合は基本的なチェックのみ
                                RuleEvaluator.canPlayCards(play, state.isRevolution)
                            } else {
                                // 通常のルールチェック
                                RuleEvaluator.isValidPlay(play, state.fieldCards, state)
                            }
                }

                if (possiblePlays.isEmpty()) {
                    // 出せるカードがないが、パスもできない場合（通常ありえない）
                    _gameState.value = state.copy(
                        gameMessage = "CPU: 出せるカードがありません（エラー状態）"
                    )
                    break
                } else {
                    // AIに選択させる
                    val chosenPlay = CpuPlayerAgent.choosePlay(
                        cpuPlayer.hand,
                        state.isRevolution
                    )

                    // 選択されたプレイが有効か、残りがジョーカーのみにならないかチェック
                    val remainingCards = cpuPlayer.hand.filterNot { chosenPlay.contains(it) }
                    val isValidPlay = chosenPlay.isNotEmpty() &&
                            (if (isOwnField)
                                RuleEvaluator.canPlayCards(chosenPlay, state.isRevolution)
                            else
                                RuleEvaluator.isValidPlay(chosenPlay, state.fieldCards, state)) &&
                            !(remainingCards.isNotEmpty() && remainingCards.all { it.isJoker })

                    if (!isValidPlay) {
                        // AIが選んだプレイが無効な場合、可能なプレイから選び直す
                        val validPlay = possiblePlays.firstOrNull() ?: emptyList()
                        _gameState.value = GameStateUpdater.applyPlay(state, validPlay)
                    } else {
                        // 選択されたカードを出す
                        _gameState.value = GameStateUpdater.applyPlay(state, chosenPlay)
                    }
                }
            } else {
                // 通常の場合はパス可能
                // 有効なプレイを全て生成
                val possiblePlays = PlayGenerator.generatePossiblePlays(
                    cpuPlayer.hand,
                    state.isRevolution
                ).filter { play ->
                    // 残りがジョーカーのみになるプレイをフィルタリング
                    val remainingCards = cpuPlayer.hand.filterNot { card -> play.contains(card) }
                    !(remainingCards.isNotEmpty() && remainingCards.all { it.isJoker }) &&
                            RuleEvaluator.isValidPlay(play, state.fieldCards, state)
                }

                if (possiblePlays.isEmpty()) {
                    // 出せるカードがないのでパス
                    _gameState.value = GameStateUpdater.applyPass(state)
                } else {
                    // AIに選択させる
                    val chosenPlay = CpuPlayerAgent.choosePlay(
                        cpuPlayer.hand,
                        state.isRevolution
                    )

                    // 選択されたプレイが有効か、残りがジョーカーのみにならないかチェック
                    val remainingCards = cpuPlayer.hand.filterNot { chosenPlay.contains(it) }
                    val isValidPlay = chosenPlay.isNotEmpty() &&
                            RuleEvaluator.isValidPlay(chosenPlay, state.fieldCards, state) &&
                            !(remainingCards.isNotEmpty() && remainingCards.all { it.isJoker })

                    if (!isValidPlay) {
                        // AIが選んだプレイが無効な場合はパス
                        _gameState.value = GameStateUpdater.applyPass(state)
                    } else {
                        // 選択されたカードを出す
                        _gameState.value = GameStateUpdater.applyPlay(state, chosenPlay)
                    }
                }
            }

            // ゲームフェーズが変わったらCPUの処理を中断
            if (_gameState.value.gamePhase != GamePhase.PLAYING) {
                break
            }
        }
    }

    private fun handleCardExchange() {
        viewModelScope.launch {
            // 自動的にカード交換を行う（人間プレイヤーがいない場合）
            val state = _gameState.value
            val humanPlayer = state.players.find { it.isHuman }

            if (humanPlayer == null || state.cardsToExchangeInfo[humanPlayer.id] == 0) {
                // 人間プレイヤーがいないか、交換不要なら自動処理
                val exchanges = mutableMapOf<String, List<Card>>()

                // CPU自動交換
                for (player in state.players.filter { !it.isHuman }) {
                    val exchangeCount = state.cardsToExchangeInfo[player.id] ?: 0
                    val rank = player.rank

                    if (exchangeCount > 0) {
                        // もらう側（大富豪または富豪）は任意のカードを渡す
                        // CPUの場合は最弱カードを選択
                        if (rank == PlayerRank.DAIFUGO || rank == PlayerRank.FUGO) {
                            exchanges[player.id] = ExchangeHelper.getWeakestCardsForExchange(
                                player.hand,
                                exchangeCount,
                                state.isRevolution
                            )
                        }
                    } else if (exchangeCount < 0) {
                        // 渡す側（大貧民または貧民）は最強カードを強制的に渡す
                        if (rank == PlayerRank.DAIHINMIN || rank == PlayerRank.HINMIN) {
                            exchanges[player.id] = ExchangeHelper.getStrongestCardsForExchange(
                                player.hand,
                                -exchangeCount,
                                state.isRevolution
                            )
                        }
                    }
                }

                _gameState.value = GameStateUpdater.applyCardExchange(state, exchanges)

                // 次のラウンド開始
                processCpuTurns()
            } else if (humanPlayer.rank == PlayerRank.DAIHINMIN || humanPlayer.rank == PlayerRank.HINMIN) {
                // 人間が大貧民または貧民の場合は、選択プロセスを開始
                startCardExchangeSelection()
            }
            // 大富豪または富豪の場合は、確認ボタンが押されるまで待機
        }
    }

    private fun prepareNextRound() {
        viewModelScope.launch {
            // ラウンド終了の表示を少し待ってから次へ
            delay(3000)

            // 新しいラウンドのための準備
            val currentState = _gameState.value

            // ラウンド数が上限に達した場合はゲーム終了
            // （例：5ラウンドまでならゲーム終了）
            if (currentState.turnNumber >= 5) {
                _gameState.value = currentState.copy(
                    gamePhase = GamePhase.GAME_OVER,
                    gameMessage = "ゲーム終了！ 最終結果を確認してください。"
                )
            } else {
                // 新しいデッキでカードを配る
                val deck = DaifugoRuleEngine.createShuffledDeck()
                val newPlayers = currentState.players.map {
                    it.copy(
                        hand = emptyList(),
                        passCountInRound = 0,
                        finishedTurnOrder = 0,
                        rankThisRound = null
                    )
                }
                val (dealtPlayers, _) = DaifugoRuleEngine.dealCards(newPlayers, deck)

                // 手札を弱い順にソート
                val sortedPlayers = dealtPlayers.map { player ->
                    player.copy(
                        hand = player.hand.sortedBy { RuleEvaluator.getCardStrength(it, false) }
                    )
                }

                _gameState.value = currentState.copy(
                    players = sortedPlayers,
                    gamePhase = GamePhase.CARD_EXCHANGE,
                    gameMessage = "次のラウンドを開始します。カード交換フェーズです。",
                    fieldCards = emptyList(),
                    isRevolution = false,
                    passCountSinceLastPlay = 0,
                    currentTurnPlays = emptyList()
                )

                // カード交換処理へ
                handleCardExchange()
            }
        }
    }
}