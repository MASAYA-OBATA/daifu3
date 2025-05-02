package com.example.daifu3.data

data class Player(
    val id: String,
    val name: String,
    val hand: List<Card>,
    val score: Int = 0,
    val rank: PlayerRank = PlayerRank.HEIMIN,
    val isHuman: Boolean = false,
    val passCountInRound: Int = 0,
    val rankThisRound: PlayerRank? = null,
    val finishedTurnOrder: Int = 0
)
