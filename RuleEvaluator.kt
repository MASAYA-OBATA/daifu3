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

        // スペード3のジョーカー対抗
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

        val allSameRank = cards.all { it.rank == cards.first().rank }
        if (allSameRank) return true // 同ランクならOK（ペア、トリプル、フォーカード）

        return isSequence(cards, isRevolution) // そうでなければ階段か？
    }

    // --- カードの代表強さを取得（比較用）---
    fun getRepresentativeStrength(cards: List<Card>, isRevolution: Boolean): Int {
        if (cards.isEmpty()) return -1
        return if (isRevolution) {
            cards.minOf { getCardStrength(it, isRevolution) }
        } else {
            cards.maxOf { getCardStrength(it, isRevolution) }
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

        val suits = cards.map { it.suit }.toSet()
        if (suits.size != 1) return false // 同じスートじゃないとダメ

        val sorted = cards.sortedBy { getCardStrength(it, isRevolution) }
        for (i in 0 until sorted.size - 1) {
            if (sorted[i + 1].rank.defaultStrength - sorted[i].rank.defaultStrength != 1) {
                return false
            }
        }
        return true
    }

    // --- 革命成立チェック（特殊ルールを追加） ---
    fun checkRevolution(playedCards: List<Card>, isRevolution: Boolean): Boolean {
        // 基本ルール：4枚以上同じランクで革命
        val groupedByRank = playedCards.groupBy { it.rank }

        // 通常の4枚以上での革命
        if (groupedByRank.values.any { it.size >= 4 }) {
            return true
        }

        // 特殊ルール：3枚の「3」で革命
        val threes = groupedByRank[Rank.THREE]
        if (threes != null && threes.size >= 3) {
            return true
        }

        // 特殊ルール：3枚の「2」で革命返し
        val twos = groupedByRank[Rank.TWO]
        if (twos != null && twos.size >= 3) {
            // 現在革命中なら革命返し、そうでなければ革命
            return true
        }

        return false
    }
}