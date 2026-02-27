package com.smartautocorrect.keyboard.domain.usecase

import com.smartautocorrect.keyboard.domain.repository.DictionaryRepository
import javax.inject.Inject

/**
 * Use case: Record that the user typed [word] to improve future suggestions.
 */
class LearnWordUseCase @Inject constructor(
    private val repository: DictionaryRepository
) {
    suspend operator fun invoke(word: String) {
        if (word.length >= 2) {
            repository.learnWord(word.lowercase())
        }
    }
}
