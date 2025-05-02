package com.example.daifu3.data

enum class Rank(val symbol: String, val defaultStrength: Int) {
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("10", 10),
    JACK("J", 11),
    QUEEN("Q", 12),
    KING("K", 13),
    ACE("A", 14),
    TWO("2", 15),
    JOKER("JOKER", 99)  // Jokerだけ特別扱い
}
