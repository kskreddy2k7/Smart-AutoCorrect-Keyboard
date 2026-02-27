package com.smartautocorrect.keyboard.domain.repository

import com.smartautocorrect.keyboard.domain.model.WordSuggestion

/**
 * Repository interface for dictionary operations.
 * Abstracts the data source from domain logic.
 */
interface DictionaryRepository {
    /**
     * Check if a word exists in the dictionary.
     */
    fun isValidWord(word: String): Boolean

    /**
     * Get autocorrect and predictive text suggestions for a given word prefix/typo.
     *
     * @param word The typed word (may contain typos)
     * @param previousWord The previous word for bigram prediction (nullable)
     * @param language Language code (e.g. "en", "hi")
     * @return List of up to 3 suggestions sorted by relevance
     */
    suspend fun getSuggestions(
        word: String,
        previousWord: String?,
        language: String
    ): List<WordSuggestion>

    /**
     * Record that the user typed a word, increasing its frequency.
     */
    suspend fun learnWord(word: String)

    /**
     * Get bigram prediction: what word likely follows [previousWord].
     */
    suspend fun getBigramSuggestions(previousWord: String, language: String): List<WordSuggestion>
}
