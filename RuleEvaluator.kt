package com.example.daifu3.game

import com.example.daifu3.data.Card
import com.example.daifu3.data.GameState
import com.example.daifu3.data.Rank
import com.example.daifu3.data.Suit

object RuleEvaluator {

    // --- 出したカードが有効かを判定 ---
    fun isValidPlay(
        playedCards: List<Card>,
        fieldCards: List<Card>,
        gameState: GameState
    ): Boolean {
        if (playedCards.isEmpty()) return false

        val isRevolution = gameState.isRevolution
        val isEightCutActive = gameState.isEightCutActive

        // ジョーカーを含む上がりは禁止
        val isLastPlay = gameState.currentPlayer?.hand?.size == playedCards.size
        if (isLastPlay && playedCards.any { it.isJoker }) {
            return false
        }

        // 8切りの特例
        if (isEightCutActive && playedCards.any { it.rank == Rank.EIGHT }) {
            return canPlayCards(playedCards, isRevolution)
        }

        // スペード3のジョーカー対抗 - 修正：単体ジョーカーの場合のみ有効
        val isSpade3Counter = fieldCards.size == 1 && fieldCards.first().isJoker &&
                playedCards.size == 1 && playedCards.first().suit == Suit.SPADES &&
                playedCards.first().rank == Rank.THREE
        if (isSpade3Counter) {
            return true
        }

        // 場が空なら、出し方が正しければOK
        if (fieldCards.isEmpty()) {
            return canPlayCards(playedCards, isRevolution)
        }

        // 枚数違いは絶対にNG
        if (playedCards.size != fieldCards.size) return false

        // 出し方が成立しているか
        if (!canPlayCards(playedCards, isRevolution)) return false

        // 強さ比較
        val playedStrength = getRepresentativeStrength(playedCards, isRevolution)
        val fieldStrength = getRepresentativeStrength(fieldCards, isRevolution)

        return if (isRevolution) {
            playedStrength < fieldStrength // 小さいほど強い（革命中）
        } else {
            playedStrength > fieldStrength // 大きいほど強い（通常時）
        }
    }

    // --- 出し方が正しいかチェック（枚数揃い、階段チェックなど） ---
    fun canPlayCards(cards: List<Card>, isRevolution: Boolean): Boolean {
        if (cards.isEmpty()) return false
        if (cards.size == 1) return true // 1枚なら問題ない

        // ジョーカーを抜いたカード
        val nonJokerCards = cards.filterNot { it.isJoker }

        // すべてジョーカーなら有効
        if (nonJokerCards.isEmpty()) return true

        // 同じランクの通常カードとジョーカーの組み合わせなら有効
        if (nonJokerCards.all { it.rank == nonJokerCards.first().rank }) {
            return true // 同ランクならOK（ペア、トリプル、フォーカード）
        }

        // 階段の場合
        return isSequence(cards, isRevolution)
    }

    // --- カードの代表強さを取得（比較用）---
    fun getRepresentativeStrength(cards: List<Card>, isRevolution: Boolean): Int {
        if (cards.isEmpty()) return -1

        // ジョーカーを含む場合、ジョーカーを除いたカードの強さを考慮
        val nonJokerCards = cards.filterNot { it.isJoker }
        if (nonJokerCards.isEmpty()) {
            // 全てジョーカーの場合は最強
            return 100
        }

        return if (isRevolution) {
            nonJokerCards.minOf { getCardStrength(it, isRevolution) }
        } else {
            nonJokerCards.maxOf { getCardStrength(it, isRevolution) }
        }
    }

    // --- カード1枚の強さ数値 ---
    fun getCardStrength(card: Card, isRevolution: Boolean): Int {
        return when {
            card.isJoker -> 100 // ジョーカーは最強（または最弱）
            else -> card.rank.defaultStrength
        }
    }

    // --- 階段（ストレート）判定 ---
    fun isSequence(cards: List<Card>, isRevolution: Boolean): Boolean {
        if (cards.size < 3) return false // 最低3枚必要

        // ジョーカーを含む処理
        val jokers = cards.filter { it.isJoker }
        val nonJokers = cards.filterNot { it.isJoker }

        // 非ジョーカーが0枚または1枚なら階段にはならない
        if (nonJokers.size <= 1) return false

        // 全てのカードが同じスートかチェック（ジョーカー除く）
        val suits = nonJokers.map { it.suit }.toSet()
        if (suits.size != 1) return false // 同じスートじゃないとダメ

        // 非ジョーカーカードをソート
        val sorted = nonJokers.sortedBy { getCardStrength(it, isRevolution) }

        // 必要なジョーカーの数を計算
        var neededJokers = 0
        var lastRank = sorted.first().rank.defaultStrength

        for (i in 1 until sorted.size) {
            val currentRank = sorted[i].rank.defaultStrength
            val gap = currentRank - lastRank - 1
            if (gap > 0) {
                neededJokers += gap
            }
            lastRank = currentRank
        }

        // ジョーカーの数が必要数以上あれば階段として有効