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
            dictionaries[language] = gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            // Fall back to empty map if asset not found
            dictionaries[language] = emptyMap()
        }
    }

    override fun isValidWord(word: String): Boolean {
        val lower = word.lowercase()
        return dictionaries.values.any { it.containsKey(lower) }
    }

    override suspend fun getSuggestions(
        word: String,
        previousWord: String?,
        language: String
    ): List<WordSuggestion> = withContext(Dispatchers.Default) {
        loadDictionary(language)
        val lower = word.lowercase()
        val dict = dictionaries[language] ?: emptyMap()
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

        // 3. Levenshtein-based corrections from dictionary
        if (maxDist > 0) {
            dict.entries
                .filter { (dictWord, _) ->
                    // Early-terminate: if lengths differ by more than maxDist, skip expensive edit distance
                    val lenDiff = Math.abs(lower.length - dictWord.length)
                    if (lenDiff > maxDist) return@filter false
                    val dist = LevenshteinUtils.distance(lower, dictWord)
                    dist in 1..maxDist
                }
                .sortedByDescending { (dictWord, freq) ->
                    // Rank by combination of similarity and word frequency
                    val sim = LevenshteinUtils.similarity(lower, dictWord)
                    sim * 50f + freq * 0.001f
                }
                .take(3)
                .forEach { (dictWord, freq) ->
                    val dist = LevenshteinUtils.distance(lower, dictWord)
                    val score = (maxDist - dist + 1).toFloat() * 10f + freq * 0.001f
                    suggestions.add(
                        WordSuggestion(dictWord, score, isAutocorrect = dist == 1 && lower.length > 3)
                    )
                }
        }

        // 4. Bigram predictions (add if not already in list)
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
}
