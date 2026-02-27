package com.smartautocorrect.keyboard;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SpellChecker {
    private Set<String> dictionary;

    public SpellChecker() {
        dictionary = new HashSet<>(Arrays.asList(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
            "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
            "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
            "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
            "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
            "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
            "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
            "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
            "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
            "is", "was", "are", "been", "has", "had", "were", "said", "did", "having",
            "hello", "world", "test", "android", "keyboard", "auto", "correct", "spell",
            "check", "word", "text", "type", "typing", "input", "method", "service"
        ));
    }

    public boolean isValidWord(String word) {
        return dictionary.contains(word.toLowerCase());
    }

    public String correctWord(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }

        String lowerWord = word.toLowerCase();
        
        if (isValidWord(lowerWord)) {
            return word;
        }

        String bestMatch = word;
        int minDistance = Integer.MAX_VALUE;

        for (String dictWord : dictionary) {
            int distance = levenshteinDistance(lowerWord, dictWord);
            if (distance < minDistance && distance <= 2) {
                minDistance = distance;
                bestMatch = dictWord;
            }
        }

        return bestMatch;
    }

    private int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[len1][len2];
    }
}
