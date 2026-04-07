package com.example.screentimetracker

object LoadingMessages {
    val messages = listOf(
        "Counting your scrolls...",
        "Analysing app usage...",
        "Calculating screen time...",
        "Checking your streaks...",
        "Crunching the numbers...",
        "Reviewing your habits..."
    )

    fun random() = messages.random()
}