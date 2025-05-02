package com.example.daifu3.game

import com.example.daifu3.data.*

object DaifugoRuleEngine {

    fun createShuffledDeck(): List<Card> {
        val suits = Suit.values()
        val ranks = Rank.values()

        val deck = mutableListOf<Card>()
        for (suit in suits) {
            for (rank in ranks) {
                deck.add(Card(suit, rank))
            }
        }

        // Joker 2枚追加（必要な場合）
        deck.add(Card(Suit.JOKER, Rank.JOKER))
        deck.add(Card(Suit.JOKER, Rank.JOKER))

        deck.shuffle()
        return deck
    }

    fun dealCards(players: List<Player>, deck: List<Card>): Pair<List<Player>, List<Card>> {
        val playerCount = players.size
        val hands = List(playerCount) { mutableListOf<Card>() }

        deck.forEachIndexed { index, card ->
            hands[index % playerCount].add(card)
        }

        val updatedPlayers = players.mapIndexed { i, player ->
            player.copy(hand = hands[i])
        }

        return Pair(updatedPlayers, emptyList())
    }

    fun isValidPlay(played: List<Card>, field: List<Card>): Boolean {
        if (played.isEmpty()) return false
        if (field.isEmpty()) return true
        if (played.size != field.size) return false

        val playedMax = played.maxOf { it.rank.ordinal }
        val fieldMax = field.maxOf { it.rank.ordinal }

        return playedMax > fieldMax
    }
}
