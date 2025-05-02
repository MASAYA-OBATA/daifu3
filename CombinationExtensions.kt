package com.example.daifu3.game

fun <T> List<T>.combinations(n: Int): List<List<T>> {
    if (n < 0 || n > this.size) return emptyList(); if (n == 0) return listOf(emptyList())
    if (n == 1) return this.map { listOf(it) }; if (n == this.size) return listOf(this.toList())
    val first = this.first(); val rest = this.drop(1)
    val combsWithoutFirst = rest.combinations(n); val combsWithFirst = rest.combinations(n - 1).map { listOf(first) + it }
    return combsWithoutFirst + combsWithFirst
}