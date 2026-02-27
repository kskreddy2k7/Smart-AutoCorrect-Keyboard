package com.smartautocorrect.keyboard.ml

import com.smartautocorrect.keyboard.domain.model.WordSuggestion
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bigram-based next-word prediction engine.
 *
 * Maintains in-memory bigram frequency counts trained from user typing.
 * Optionally delegates to a TFLite model for enhanced predictions.
 */
@Singleton
class PredictionEngine @Inject constructor(
    private val tfliteHelper: TFLiteHelper
) {
    // Bigram map: previous_word -> (next_word -> count)
    private val bigramCounts: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()

    /**
     * Record that [nextWord] followed [prevWord] in user input.
     * Updates the in-memory bigram model.
     */
    fun recordBigram(prevWord: String, nextWord: String) {
        val prev = prevWord.lowercase()
        val next = nextWord.lowercase()
        bigramCounts.getOrPut(prev) { mutableMapOf() }
            .merge(next, 1, Int::plus)
    }

    /**
     * Get top [limit] next-word predictions given [previousWord].
     * Falls back to rule-based bigram model if TFLite is unavailable.
     *
     * @return List of [WordSuggestion] sorted by probability descending
     */
    fun predict(previousWord: String, limit: Int = 3): List<WordSuggestion> {
        // Try TFLite first
        if (tfliteHelper.isAvailable()) {
            val mlPredictions = tfliteHelper.predict(previousWord, limit)
            if (mlPredictions.isNotEmpty()) return mlPredictions
        }

        // Fallback: rule-based bigram model
        val prev = previousWord.lowercase()
        val nextCounts = bigramCounts[prev] ?: return emptyList()
        val total = nextCounts.values.sum().toFloat()

        return nextCounts.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (word, count) ->
                WordSuggestion(
                    word = word,
                    score = count / total,
                    isAutocorrect = false
                )
            }
    }

    /**
     * Load bigram data from a serialized map (e.g. loaded from Room or assets).
     */
    fun loadBigrams(data: Map<String, Map<String, Int>>) {
        bigramCounts.clear()
        data.forEach { (prev, nexts) ->
            bigramCounts[prev] = nexts.toMutableMap()
        }
    }
}
