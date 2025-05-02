package com.example.daifu3.data

data class GameState(
    val players: List<Player> = emptyList(),
    val deck: List<Card> = emptyList(),
    val fieldCards: List<Card> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val isRevolution: Boolean = false,
    val gamePhase: GamePhase = GamePhase.DEALING,
    val lastPlayerToPlayIndex: Int? = null,
    val passCountSinceLastPlay: Int = 0,
    val currentTurnPlays: List<List<Card>> = emptyList(),
    val gameMessage: String? = null,
    val ranksLastRound: List<PlayerRank> = List(4) { PlayerRank.HEIMIN },
    val isEightCutActive: Boolean = true,
    val roundWinnerId: String? = null,
    val turnNumber: Int = 0,
    val cardsToExchangeInfo: Map<String, Int> = emptyMap(),
    val gameEndSummary: GameEndSummary? = null
) {
    val currentPlayer: Player? get() = players.getOrNull(currentPlayerIndex)
    val activePlayerCount: Int get() = players.count { it.hand.isNotEmpty() }
    val isCurrentPlayerActive: Boolean get() = currentPlayer?.hand?.isNotEmpty() == true
}
