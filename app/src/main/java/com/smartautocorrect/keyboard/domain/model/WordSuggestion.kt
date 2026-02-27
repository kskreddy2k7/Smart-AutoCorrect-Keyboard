package com.smartautocorrect.keyboard.domain.model

/**
 * Represents a word suggestion shown in the suggestion bar.
 *
 * @param word The suggested word text
 * @param score Relevance score (higher = more relevant)
 * @param isAutocorrect True if this is an autocorrect replacement (shown highlighted)
 */
data class WordSuggestion(
    val word: String,
    val score: Float,
    val isAutocorrect: Boolean = false
)
