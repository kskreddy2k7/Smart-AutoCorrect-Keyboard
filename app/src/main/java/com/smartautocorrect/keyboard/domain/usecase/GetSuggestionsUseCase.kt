package com.smartautocorrect.keyboard.domain.usecase

import com.smartautocorrect.keyboard.domain.model.WordSuggestion
import com.smartautocorrect.keyboard.domain.repository.DictionaryRepository
import javax.inject.Inject

/**
 * Use case: Retrieve word suggestions for the current typed word.
 * Combines autocorrect suggestions and bigram predictions.
 */
class GetSuggestionsUseCase @Inject constructor(
    private val repository: DictionaryRepository
) {
    /**
     * Get suggestions for [currentWord], optionally using [previousWord] for bigram prediction.
     */
    suspend operator fun invoke(
        currentWord: String,
        previousWord: String?,
        language: String = "en"
    ): List<WordSuggestion> {
        if (currentWord.isEmpty()) {
            // No current word - use bigram prediction if previous word exists
            return if (!previousWord.isNullOrEmpty()) {
                repository.getBigramSuggestions(previousWord, language)
            } else {
                emptyList()
            }
        }
        return repository.getSuggestions(currentWord, previousWord, language)
    }
}
