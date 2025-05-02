package com.example.daifu3.game

import com.example.daifu3.data.Card
import com.example.daifu3.data.Player
import com.example.daifu3.data.PlayerRank

object ScoreCalculator {
    fun calculateScore(players: List<Player>, winnerId: String?): Map<String, Int> {
        val scoreChanges = mutableMapOf<String, Int>()
        var bonus = 0

        // 上がれなかった人のマイナス点計算
        players.forEach { player ->
            if (player.finishedTurnOrder == 0 && player.hand.isNotEmpty()) {
                // ジョーカー残りの倍率計算
                val jokerCount = player.hand.count { it.isJoker }
                val basePoints = player.hand.filterNot { it.isJoker }.sumOf { getCardPoints(it) }

                val penalty = when (jokerCount) {
                    0 -> basePoints
                    1 -> basePoints * 10  // ジョーカー1枚残りで10倍
                    else -> basePoints * 100  // ジョーカー2枚残りで100倍
                }

                scoreChanges[player.id] = -penalty
                if (winnerId != null) bonus += penalty
            } else {
                scoreChanges[player.id] = 0
            }
        }

        // 勝者にボーナス点を付与
        if (winnerId != null && bonus > 0) {
            scoreChanges[winnerId] = (scoreChanges[winnerId] ?: 0) + bonus
        }

        return scoreChanges
    }

    fun determineRanks(players: List<Player>): Map<String, PlayerRank> {
        // 既存の実装をそのまま使用
        val sortedPlayers = players.sortedWith(
            compareBy<Player>(
                { if (it.finishedTurnOrder == 0) Int.MAX_VALUE else it.finishedTurnOrder },
                { p -> if (p.finishedTurnOrder == 0) p.hand.sumOf { getCardPoints(it) } else 0 },
                { -it.score }
            )
        )

        val rankMap = mutableMapOf<String, PlayerRank>()
        val playerCount = sortedPlayers.size

        sortedPlayers.forEachIndexed { index, player ->
            rankMap[player.id] = when (playerCount) {
                3 -> when (index) {
                    0 -> PlayerRank.DAIFUGO
                    1 -> PlayerRank.HEIMIN
                    else -> PlayerRank.DAIHINMIN
                }
                4 -> when (index) {
                    0 -> PlayerRank.DAIFUGO
                    1 -> PlayerRank.FUGO
                    2 -> PlayerRank.HINMIN
                    else -> PlayerRank.DAIHINMIN
                }
                else -> when (index) {
                    0 -> PlayerRank.DAIFUGO
                    1 -> PlayerRank.FUGO
                    playerCount - 2 -> PlayerRank.HINMIN
                    playerCount - 1 -> PlayerRank.DAIHINMIN
                    else -> PlayerRank.HEIMIN
                }
            }
        }

        return rankMap
    }

    private fun getCardPoints(card: Card): Int {
        // ジョーカーは0点に修正
        return if (card.isJoker) 0 else card.points
    }
}