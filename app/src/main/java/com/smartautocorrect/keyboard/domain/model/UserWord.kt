package com.smartautocorrect.keyboard.domain.model

/**
 * Domain model representing a word learned from user typing.
 *
 * @param word The word text
 * @param frequency How many times the user has typed this word
 * @param lastUsed Timestamp of last usage (epoch millis)
 */
data class UserWord(
    val word: String,
    val frequency: Int,
    val lastUsed: Long
)
