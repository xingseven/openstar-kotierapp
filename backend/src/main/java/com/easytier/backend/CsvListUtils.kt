package com.easytier.backend

fun csvToMutableStringList(text: String): MutableList<String> {
    return text.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
}