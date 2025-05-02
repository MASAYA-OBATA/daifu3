package com.example.daifu3.data

import androidx.compose.ui.graphics.Color

enum class Suit(val symbol: String, val color: Color) {
    SPADES("♠", Color.Black),
    HEARTS("♥", Color.Red),
    DIAMONDS("♦", Color.Red),
    CLUBS("♣", Color.Black),
    JOKER("J", Color.Blue)  // Jokerは専用スート扱いにすることで一貫性を保つ
}
