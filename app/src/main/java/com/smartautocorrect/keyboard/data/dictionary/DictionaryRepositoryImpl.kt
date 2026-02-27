package com.smartautocorrect.keyboard.data.dictionary

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartautocorrect.keyboard.data.database.UserWordDao
import com.smartautocorrect.keyboard.data.database.UserWordEntity
import com.smartautocorrect.keyboard.domain.model.WordSuggestion
import com.smartautocorrect.keyboard.domain.repository.DictionaryRepository
import com.smartautocorrect.keyboard.ml.PredictionEngine
import com.smartautocorrect.keyboard.utils.LevenshteinUtils
import com.smartautocorrect.keyboard.utils.Trie
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [DictionaryRepository].
 *
 * - Loads word dictionaries from JSON assets files
 * - Applies Levenshtein-based autocorrect with frequency ranking
 * - Delegates bigram predictions to [PredictionEngine]
 * - Persists user word frequency in Room via [UserWordDao]
 */
@Singleton
class DictionaryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userWordDao: UserWordDao,
    private val predictionEngine: PredictionEngine,
    private val gson: Gson
) : DictionaryRepository {

    // Per-language dictionaries: word -> frequency
    private val dictionaries: MutableMap<String, Map<String, Int>> = mutableMapOf()

    // Per-language Trie for fast prefix-based word completion
    private val tries: MutableMap<String, Trie> = mutableMapOf()

    init {
        // Pre-load English dictionary on construction
        loadDictionary("en")
    }

    /**
     * Load the JSON dictionary for the given [language] from assets.
     * Asset file naming: dictionary_en.json, dictionary_hi.json, etc.
     */
    private fun loadDictionary(language: String) {
        if (dictionaries.containsKey(language)) return
        try {
            val filename = "dictionary_$language.json"
            val json = context.assets.open(filename).bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, Int>>() {}.type
            val dict: Map<String, Int> = gson.fromJson(json, type) ?: emptyMap()
            dictionaries[language] = dict
            // Build Trie from the loaded dictionary for fast prefix lookups
            val trie = Trie()
            dict.forEach { (word, freq) -> trie.insert(word, freq) }
            tries[language] = trie
        } catch (e: Exception) {
            // Fall back to empty map/trie if asset not found
            dictionaries[language] = emptyMap()
            tries[language] = Trie()
        }
    }

    override fun isValidWord(word: String): Boolean {
        val lower = word.lowercase()
        return tries.values.any { it.contains(lower) }
    }

    override suspend fun getSuggestions(
        word: String,
        previousWord: String?,
        language: String
    ): List<WordSuggestion> = withContext(Dispatchers.Default) {
        loadDictionary(language)
        val lower = word.lowercase()
        val dict = dictionaries[language] ?: emptyMap()
        val trie = tries.getValue(language)
        val maxDist = LevenshteinUtils.maxAllowedDistance(lower.length)

        val suggestions = mutableListOf<WordSuggestion>()

        // 1. Exact match - highest priority
        if (dict.containsKey(lower)) {
            suggestions.add(
                WordSuggestion(lower, score = 100f + (dict[lower] ?: 0), isAutocorrect = false)
            )
        }

        // 2. Check user's personal word list (higher weight for frequency boosting)
        val userWord = userWordDao.getWord(lower)
        if (userWord != null) {
            suggestions.add(
                WordSuggestion(userWord.word, score = 80f + userWord.frequency, isAutocorrect = false)
            )
        }

        // 3. Prefix-based completions from Trie (efficient O(k) prefix lookup)
        if (lower.length >= 2) {
            trie.wordsWithPrefix(lower, limit = 5)
                .filter { (w, _) -> w != lower }
                .take(3)
                .forEach { (w, freq) ->
                    if (suggestions.none { it.word == w }) {
                        suggestions.add(WordSuggestion(w, score = PREFIX_COMPLETION_BASE_SCORE + freq * FREQUENCY_WEIGHT, isAutocorrect = false))
                    }
                }
        }

        // 4. Levenshtein-based corrections from dictionary (calculate distance once per candidate)
        if (maxDist > 0) {
            data class Candidate(val word: String, val freq: Int, val dist: Int, val score: Float)

            dict.entries
                .mapNotNull { (dictWord, freq) ->
                    val lenDiff = kotlin.math.abs(lower.length - dictWord.length)
                    if (lenDiff > maxDist) return@mapNotNull null
                    val dist = LevenshteinUtils.distance(lower, dictWord)
                    if (dist !in 1..maxDist) return@mapNotNull null
                    val score = (maxDist - dist + 1).toFloat() * LEVENSHTEIN_SCORE_MULTIPLIER + freq * FREQUENCY_WEIGHT
                    Candidate(dictWord, freq, dist, score)
                }
                .sortedByDescending { it.score }
                .take(3)
                .forEach { (dictWord, _, dist, score) ->
                    suggestions.add(
                        WordSuggestion(dictWord, score, isAutocorrect = dist == 1 && lower.length > 3)
                    )
                }
        }

        // 5. Bigram predictions (add if not already in list)
        if (!previousWord.isNullOrEmpty()) {
            val bigramSuggestions = predictionEngine.predict(previousWord)
            bigramSuggestions.forEach { sug ->
                if (suggestions.none { it.word == sug.word }) {
                    suggestions.add(sug.copy(score = sug.score * 30f))
                }
            }
        }

        // Return top 3 unique suggestions sorted by score
        suggestions
            .distinctBy { it.word }
            .sortedByDescending { it.score }
            .take(3)
    }

    override suspend fun learnWord(word: String) {
        val existing = userWordDao.getWord(word)
        if (existing == null) {
            userWordDao.insertWord(UserWordEntity(word = word))
        } else {
            userWordDao.incrementFrequency(word)
        }
    }

    override suspend fun getBigramSuggestions(
        previousWord: String,
        language: String
    ): List<WordSuggestion> = withContext(Dispatchers.Default) {
        predictionEngine.predict(previousWord)
    }

    companion object {
        /** Base score for prefix-based completions (words that start with the typed prefix). */
        private const val PREFIX_COMPLETION_BASE_SCORE = 70f
        /** Score multiplier per edit-distance unit for Levenshtein corrections. */
        private const val LEVENSHTEIN_SCORE_MULTIPLIER = 10f
        /** Weight applied to word frequency when computing suggestion scores. */
        private const val FREQUENCY_WEIGHT = 0.001f
    }
}
