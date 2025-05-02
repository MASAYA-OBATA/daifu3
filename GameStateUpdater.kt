package com.example.daifu3.game

import com.example.daifu3.data.Card
import com.example.daifu3.data.GameState
import com.example.daifu3.data.Rank
import com.example.daifu3.data.Player
import com.example.daifu3.data.PlayerRank
import com.example.daifu3.data.GamePhase
import com.example.daifu3.data.GameEndSummary
import kotlin.math.abs

object GameStateUpdater {
    private const val SPADE_3_ID = "♥3"

    fun applyPass(gameState: GameState): GameState {
        val currentPlayerIndex = gameState.currentPlayerIndex
        val players = gameState.players
        if (players.isEmpty()) return gameState

        val updatedPlayers = players.mapIndexed { i, p ->
            if (i == currentPlayerIndex) p.copy(passCountInRound = p.passCountInRound + 1) else p
        }

        val nextPassCount = gameState.passCountSinceLastPlay + 1
        val activePlayers = updatedPlayers.filter { it.hand.isNotEmpty() && it.finishedTurnOrder == 0 }
        val shouldClear = gameState.fieldCards.isNotEmpty() && gameState.lastPlayerToPlayIndex != null && nextPassCount >= activePlayers.size

        val nextTurnNumber = gameState.turnNumber + 1
        var message = "${updatedPlayers.getOrNull(currentPlayerIndex)?.name ?: "Player"} passed."

        return if (shouldClear) {
            val nextPlayerIndex = gameState.lastPlayerToPlayIndex!!
            message += " Field cleared. ${updatedPlayers.getOrNull(nextPlayerIndex)?.name ?: "Next player"}'s turn."
            gameState.copy(
                players = updatedPlayers.map { it.copy(passCountInRound = 0) },
                fieldCards = emptyList(),
                currentPlayerIndex = nextPlayerIndex,
                lastPlayerToPlayIndex = null,
                passCountSinceLastPlay = 0,
                currentTurnPlays = emptyList(),
                gameMessage = message,
                turnNumber = nextTurnNumber
            )
        } else {
            val nextPlayerIndex = findNextActivePlayerIndex(updatedPlayers, currentPlayerIndex)
            gameState.copy(
                players = updatedPlayers,
                currentPlayerIndex = nextPlayerIndex,
                lastPlayerToPlayIndex = gameState.lastPlayerToPlayIndex,
                passCountSinceLastPlay = nextPassCount,
                gameMessage = message,
                turnNumber = nextTurnNumber
            )
        }
    }

    fun applyPlay(gameState: GameState, playedCards: List<Card>): GameState {
        val currentPlayerIndex = gameState.currentPlayerIndex
        val currentPlayer = gameState.players.getOrNull(currentPlayerIndex) ?: return gameState

        val isEightCutActive = gameState.isEightCutActive
        val isRevolution = gameState.isRevolution

        var message = "${currentPlayer.name} played ${playedCards.joinToString(", ") { it.toString() }}."

        val newHand = currentPlayer.hand.filterNot { card -> playedCards.any { it.id == card.id } }
        var updatedPlayers = gameState.players.mapIndexed { i, p ->
            if (i == currentPlayerIndex) p.copy(hand = newHand, passCountInRound = 0) else p
        }

        val revolutionTriggered = RuleEvaluator.checkRevolution(playedCards, isRevolution)
        val nextRevolution = if (revolutionTriggered) !isRevolution else isRevolution
        if (revolutionTriggered) message += if (nextRevolution) " (Revolution!)" else " (Counter-Revolution!)"

        val isEightCut = isEightCutActive && playedCards.any { it.rank == Rank.EIGHT }
        val isSpade3Counter = gameState.fieldCards.size == 1 && gameState.fieldCards.first().isJoker &&
                playedCards.size == 1 && playedCards.first().id == SPADE_3_ID
        val clearField = isEightCut || isSpade3Counter
        if (isEightCut) message += " (8-Cut!)"
        if (isSpade3Counter) message += " (Spade 3 Counter!)"

        val playerWon = newHand.isEmpty()
        var gamePhase = gameState.gamePhase
        var roundWinnerId = gameState.roundWinnerId
        var gameEndSummary: GameEndSummary? = null
        var ranksForNextRound = gameState.ranksLastRound
        var exchangeInfo = gameState.cardsToExchangeInfo

        if (playerWon) {
            val alreadyFinished = updatedPlayers.count { it.finishedTurnOrder > 0 }
            val finishedOrder = alreadyFinished + 1
            message += " ${currentPlayer.name} finished ($finishedOrder place)!"
            if (roundWinnerId == null) roundWinnerId = currentPlayer.id
            updatedPlayers = updatedPlayers.mapIndexed { i, p ->
                if (i == currentPlayerIndex) p.copy(finishedTurnOrder = finishedOrder) else p
            }
            val activePlayersAfterPlay = updatedPlayers.count { it.hand.isNotEmpty() && it.finishedTurnOrder == 0 }
            if (activePlayersAfterPlay <= 1) {
                if (gamePhase != GamePhase.ROUND_OVER) message += " Round Over!"
                gamePhase = GamePhase.ROUND_OVER
            }
        }

        val nextPlayerIndex: Int
        val nextFieldCards: List<Card>
        val nextLastPlayerIndex: Int?
        val nextTurnNumber = gameState.turnNumber + 1

        if (gamePhase == GamePhase.ROUND_OVER) {
            nextFieldCards = playedCards
            nextPlayerIndex = 0
            nextLastPlayerIndex = currentPlayerIndex

            val lastPlayer = updatedPlayers.find { it.hand.isNotEmpty() && it.finishedTurnOrder == 0 }
            if (lastPlayer != null) {
                val lastOrder = gameState.players.size
                updatedPlayers = updatedPlayers.map {
                    if (it.id == lastPlayer.id) it.copy(finishedTurnOrder = lastOrder) else it
                }
            }

            val finalRanksMap = ScoreCalculator.determineRanks(updatedPlayers)
            updatedPlayers = updatedPlayers.map {
                it.copy(rankThisRound = finalRanksMap[it.id])
            }
            val scoreChanges = ScoreCalculator.calculateScore(updatedPlayers, roundWinnerId)
            val finalScores = updatedPlayers.associate {
                it.id to it.score + (scoreChanges[it.id] ?: 0)
            }
            updatedPlayers = updatedPlayers.map {
                it.copy(score = finalScores[it.id] ?: it.score)
            }

            gameEndSummary = GameEndSummary(
                winnerName = updatedPlayers.find { it.id == roundWinnerId }?.name ?: "N/A",
                scoreChanges = scoreChanges,
                finalScores = finalScores,
                finalRanks = finalRanksMap
            )
            ranksForNextRound = updatedPlayers.mapNotNull { it.rankThisRound }
            exchangeInfo = ExchangeHelper.determineExchangeInfo(updatedPlayers, finalRanksMap)
            gamePhase = GamePhase.CARD_EXCHANGE
        } else if (clearField) {
            nextFieldCards = emptyList()
            nextPlayerIndex = currentPlayerIndex
            nextLastPlayerIndex = null
            updatedPlayers = updatedPlayers.map { it.copy(passCountInRound = 0) }
        } else {
            nextFieldCards = playedCards
            nextPlayerIndex = findNextActivePlayerIndex(updatedPlayers, currentPlayerIndex)
            nextLastPlayerIndex = currentPlayerIndex
        }

        return gameState.copy(
            players = updatedPlayers,
            fieldCards = nextFieldCards,
            currentPlayerIndex = nextPlayerIndex,
            isRevolution = if (gamePhase == GamePhase.CARD_EXCHANGE) false else nextRevolution,
            gamePhase = gamePhase,
            lastPlayerToPlayIndex = nextLastPlayerIndex,
            passCountSinceLastPlay = 0,
            currentTurnPlays = if (clearField || gamePhase == GamePhase.ROUND_OVER) emptyList() else gameState.currentTurnPlays + listOf(playedCards),
            gameMessage = message,
            roundWinnerId = roundWinnerId,
            turnNumber = nextTurnNumber,
            ranksLastRound = ranksForNextRound,
            cardsToExchangeInfo = exchangeInfo,
            gameEndSummary = gameEndSummary
        )
    }

    fun applyCardExchange(gameState: GameState, exchanges: Map<String, List<Card>>): GameState {
        if (gameState.gamePhase != GamePhase.CARD_EXCHANGE) return gameState

        val players = gameState.players
        val ranks = players.associate { it.id to (it.rankThisRound ?: PlayerRank.HEIMIN) }
        val hands = players.associate { it.id to it.hand.toMutableList() }.toMutableMap()

        val daifugoId = players.find { ranks[it.id] == PlayerRank.DAIFUGO }?.id
        val daihinminId = players.find { ranks[it.id] == PlayerRank.DAIHINMIN }?.id
        if (daifugoId != null && daihinminId != null) {
            val cardsToGiveByDaihinmin = exchanges[daihinminId]
                ?: ExchangeHelper.getStrongestCardsForExchange(hands[daihinminId] ?: emptyList(), 2, gameState.isRevolution)
            val cardsToGiveByDaifugo = exchanges[daifugoId]
                ?: ExchangeHelper.getWeakestCardsForExchange(hands[daifugoId] ?: emptyList(), 2, gameState.isRevolution)

            hands[daifugoId]?.removeAll(cardsToGiveByDaifugo.toSet())
            hands[daifugoId]?.addAll(cardsToGiveByDaihinmin)
            hands[daihinminId]?.removeAll(cardsToGiveByDaihinmin.toSet())
            hands[daihinminId]?.addAll(cardsToGiveByDaifugo)
        }

        val fugoId = players.find { ranks[it.id] == PlayerRank.FUGO }?.id
        val hinminId = players.find { ranks[it.id] == PlayerRank.HINMIN }?.id
        if (fugoId != null && hinminId != null) {
            val cardToGiveByHinmin = exchanges[hinminId]?.firstOrNull()
                ?: ExchangeHelper.getStrongestCardsForExchange(hands[hinminId] ?: emptyList(), 1, gameState.isRevolution).firstOrNull()
            val cardToGiveByFugo = exchanges[fugoId]?.firstOrNull()
                ?: ExchangeHelper.getWeakestCardsForExchange(hands[fugoId] ?: emptyList(), 1, gameState.isRevolution).firstOrNull()

            if (cardToGiveByFugo != null) hands[fugoId]?.remove(cardToGiveByFugo)
            if (cardToGiveByHinmin != null) hands[fugoId]?.add(cardToGiveByHinmin)

            if (cardToGiveByHinmin != null) hands[hinminId]?.remove(cardToGiveByHinmin)
            if (cardToGiveByFugo != null) hands[hinminId]?.add(cardToGiveByFugo)
        }

        val updatedPlayers = players.map { player ->
            val newHand = hands[player.id]?.sortedBy { RuleEvaluator.getCardStrength(it, false) } ?: player.hand
            player.copy(
                hand = newHand,
                rank = player.rankThisRound ?: player.rank,
                passCountInRound = 0,
                finishedTurnOrder = 0,
                rankThisRound = null
            )
        }
        val nextPlayerIndex = updatedPlayers.indexOfFirst { it.rank == PlayerRank.DAIFUGO }.takeIf { it != -1 } ?: 0

        return gameState.copy(
            players = updatedPlayers,
            fieldCards = emptyList(),
            currentPlayerIndex = nextPlayerIndex,
            lastPlayerToPlayIndex = null,
            passCountSinceLastPlay = 0,
            isRevolution = false,
            gameMessage = "Card exchange complete. ${updatedPlayers.getOrNull(nextPlayerIndex)?.name ?: "Player"}'s turn.",
            gamePhase = GamePhase.PLAYING,
            cardsToExchangeInfo = emptyMap(),
            gameEndSummary = null,
            roundWinnerId = null,
            turnNumber = 0
        )
    }

    private fun findNextActivePlayerIndex(players: List<Player>, currentIndex: Int): Int {
        var nextIndex = currentIndex
        var checkedCount = 0
        while (checkedCount < players.size * 2) {
            nextIndex = (nextIndex + 1) % players.size
            val nextPlayer = players.getOrNull(nextIndex)
            if (nextPlayer != null && nextPlayer.hand.isNotEmpty() && nextPlayer.finishedTurnOrder == 0) {
                return nextIndex
            }
            checkedCount++
            if (checkedCount >= players.size && nextIndex == currentIndex) {
                return currentIndex
            }
        }
        return currentIndex
    }
}
