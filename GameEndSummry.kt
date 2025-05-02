package com.example.daifu3.data

data class GameEndSummary(
    val winnerName: String,
    val scoreChanges: Map<String, Int>,
    val finalScores: Map<String, Int>,
    val finalRanks: Map<String, PlayerRank>
)
