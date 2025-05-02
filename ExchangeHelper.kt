package com.example.daifu3.game

import com.example.daifu3.data.Card
import com.example.daifu3.data.Player
import com.example.daifu3.data.PlayerRank

object ExchangeHelper {
    fun determineExchangeInfo(players: List<Player>, ranks: Map<String, PlayerRank>): Map<String, Int> {
        val info = mutableMapOf<String, Int>(); val daifugoId = players.find { ranks[it.id] == PlayerRank.DAIFUGO }?.id; val daihinminId = players.find { ranks[it.id] == PlayerRank.DAIHINMIN }?.id
        val fugoId = players.find { ranks[it.id] == PlayerRank.FUGO }?.id; val hinminId = players.find { ranks[it.id] == PlayerRank.HINMIN }?.id
        if (daifugoId != null && daihinminId != null) { info[daifugoId] = -2; info[daihinminId] = 2 }; if (fugoId != null && hinminId != null) { info[fugoId] = -1; info[hinminId] = 1 }; return info
    }
    fun getStrongestCardsForExchange(hand: List<Card>, count: Int, isRevolution: Boolean = false): List<Card> { return hand.sortedByDescending { RuleEvaluator.getCardStrength(it, isRevolution) }.take(count) }
    fun getWeakestCardsForExchange(hand: List<Card>, count: Int, isRevolution: Boolean = false): List<Card> { return hand.sortedBy { RuleEvaluator.getCardStrength(it, isRevolution) }.take(count) }
}