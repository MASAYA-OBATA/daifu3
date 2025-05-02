package com.example.daifu3.game

import com.example.daifu3.data.Card

object PlayGenerator {
    fun generatePossiblePlays(hand: List<Card>, isRevolution: Boolean): List<List<Card>> {
        val plays = mutableListOf<List<Card>>()
        if (hand.isEmpty()) return plays

        val jokers = hand.filter { it.isJoker }
        val nonJokers = hand.filterNot { it.isJoker }

        // 1. シングル（単独）
        nonJokers.forEach { plays.add(listOf(it)) }
        jokers.forEach { plays.add(listOf(it)) }

        // 2. ペア、トリプル、フォーカード（ジョーカー混ぜ対応）
        val groupedByRank = nonJokers.groupBy { it.rank }
        for ((_, cards) in groupedByRank) {
            // 素の組み合わせ
            if (cards.size >= 2) plays.add(cards.take(2))
            if (cards.size >= 3) plays.add(cards.take(3))
            if (cards.size >= 4) plays.add(cards.take(4))

            // ジョーカー混ぜ（1枚だけ）
            if (cards.size == 1 && jokers.size >= 1) plays.add(cards + jokers.take(1))
            if (cards.size == 2 && jokers.size >= 1) plays.add(cards + jokers.take(1)) // 2+1で3枚にする
            if (cards.size == 3 && jokers.size >= 1) plays.add(cards + jokers.take(1)) // 3+1で4枚にする
        }

        // ジョーカーだけでペア、トリプル
        if (jokers.size >= 2) plays.add(jokers.take(2))
        if (jokers.size >= 3) plays.add(jokers.take(3))
        if (jokers.size >= 4) plays.add(jokers.take(4))

        // 3. 階段（ストレート）
        val suitGroups = nonJokers.groupBy { it.suit }
        for ((_, suitedCards) in suitGroups) {
            val sorted = suitedCards.sortedBy { RuleEvaluator.getCardStrength(it, isRevolution) }

            for (window in sorted.windowed(size = 3, step = 1)) {
                if (RuleEvaluator.isSequence(window, isRevolution)) {
                    plays.add(window)
                }
            }
            if (sorted.size >= 4) {
                for (window in sorted.windowed(size = 4, step = 1)) {
                    if (RuleEvaluator.isSequence(window, isRevolution)) {
                        plays.add(window)
                    }
                }
            }
            if (sorted.size >= 5) {
                for (window in sorted.windowed(size = 5, step = 1)) {
                    if (RuleEvaluator.isSequence(window, isRevolution)) {
                        plays.add(window)
                    }
                }
            }
        }

        return plays.distinct()
    }
}
