package com.smartautocorrect.keyboard.utils

/**
 * Utilities for edit-distance-based string similarity.
 * Uses optimized Levenshtein distance with Damerau transpositions.
 */
object LevenshteinUtils {

    /**
     * Compute the Damerau-Levenshtein distance between two strings.
     * Considers insertions, deletions, substitutions, and adjacent transpositions.
     *
     * @return Edit distance (0 = identical, higher = more different)
     */
    fun distance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        // Early exit for empty strings
        if (len1 == 0) return len2
        if (len2 == 0) return len1

        // Use single-row optimization for memory efficiency
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,       // deletion
                    dp[i][j - 1] + 1,       // insertion
                    dp[i - 1][j - 1] + cost  // substitution
                )
                // Transposition (Damerau extension)
                if (i > 1 && j > 1 &&
                    s1[i - 1] == s2[j - 2] &&
                    s1[i - 2] == s2[j - 1]
                ) {
                    dp[i][j] = minOf(dp[i][j], dp[i - 2][j - 2] + cost)
                }
            }
        }

        return dp[len1][len2]
    }

    /**
     * Compute a normalized similarity score between 0.0 (different) and 1.0 (identical).
     */
    fun similarity(s1: String, s2: String): Float {
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0f
        return 1.0f - (distance(s1, s2).toFloat() / maxLen)
    }

    /**
     * Determine the maximum allowed edit distance for a word of given length.
     * Shorter words get less tolerance to avoid spurious corrections.
     */
    fun maxAllowedDistance(wordLength: Int): Int {
        return when {
            wordLength <= 3 -> 0  // Don't autocorrect very short words
            wordLength <= 5 -> 1  // Allow 1 edit for medium words
            wordLength <= 8 -> 2  // Allow 2 edits for longer words
            else -> 3             // Allow 3 edits for very long words
        }
    }
}
