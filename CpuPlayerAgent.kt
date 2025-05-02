package com.example.daifu3.ai

import com.example.daifu3.data.Card
import com.example.daifu3.data.PlayerRank
import com.example.daifu3.game.PlayGenerator
import com.example.daifu3.game.RuleEvaluator

object CpuPlayerAgent {

    fun choosePlay(hand: List<Card>, isRevolution: Boolean): List<Card> {
        val possiblePlays = PlayGenerator.generatePossiblePlays(hand, isRevolution)
        if (possiblePlays.isEmpty()) return emptyList()

        // 手札の枚数によって戦略を変える
        return when {
            hand.size <= 3 -> strategicPlayForEndgame(possiblePlays, hand, isRevolution)
            hand.size <= 8 -> strategicPlayMidgame(possiblePlays, hand, isRevolution)
            else -> strategicPlayEarlyGame(possiblePlays, hand, isRevolution)
        }
    }

    private fun strategicPlayEarlyGame(plays: List<List<Card>>, hand: List<Card>, isRevolution: Boolean): List<Card> {
        // 序盤：弱いカードからプレイ
        // ただし、革命のチャンスがあれば優先
        val revolutionPlays = plays.filter {
            RuleEvaluator.checkRevolution(it, isRevolution)
        }

        if (revolutionPlays.isNotEmpty()) {
            return revolutionPlays.first()
        }

        // 弱いカードから出す
        return plays.minByOrNull { play ->
            play.maxOf { RuleEvaluator.getCardStrength(it, isRevolution) }
        } ?: emptyList()
    }

    private fun strategicPlayMidgame(plays: List<List<Card>>, hand: List<Card>, isRevolution: Boolean): List<Card> {
        // 中盤：バランス型（最も枚数を減らしやすい戦略）
        // 複数枚出せる場合は優先
        val multiCardPlays = plays.filter { it.size > 1 }
        if (multiCardPlays.isNotEmpty()) {
            // より多くのカードを出せるものを優先
            return multiCardPlays.maxByOrNull { it.size } ?: multiCardPlays.first()
        }

        // それ以外は中程度の強さのカードを出す（最強・最弱は避ける）
        val sortedPlays = plays.sortedBy { play ->
            play.maxOf { RuleEvaluator.getCardStrength(it, isRevolution) }
        }

        return if (sortedPlays.size > 2) {
            sortedPlays[sortedPlays.size / 2]  // 中間の強さのプレイ
        } else {
            sortedPlays.first()  // 最弱のプレイ
        }
    }

    private fun strategicPlayForEndgame(plays: List<List<Card>>, hand: List<Card>, isRevolution: Boolean): List<Card> {
        // 終盤：上がりに向けて最適化（できるだけ多くのカードを出す）

        // ジョーカーを持っていたら早めに出す（上がれないため）
        val jokerPlays = plays.filter { play -> play.any { it.isJoker } }
        if (jokerPlays.isNotEmpty() && hand.size > 1) {
            return jokerPlays.first()
        }

        // 枚数の多いプレイを優先
        val sortedBySize = plays.sortedByDescending { it.size }
        if (sortedBySize.first().size > 1) {
            return sortedBySize.first()
        }

        // 単体の場合は強いカードを優先
        return plays.maxByOrNull { play ->
            play.maxOf { RuleEvaluator.getCardStrength(it, isRevolution) }
        } ?: emptyList()
    }
}