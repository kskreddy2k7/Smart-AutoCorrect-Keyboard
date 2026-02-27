package com.smartautocorrect.keyboard.utils

/**
 * Trie (prefix tree) data structure for efficient word prefix lookups.
 *
 * Supports fast insertion, exact-match lookups, and prefix-based word retrieval.
 * Used by the dictionary to find all words starting with a given prefix in O(k + m)
 * time where k is the prefix length and m is the number of matching words.
 */
class Trie {

    private val root = TrieNode()

    /**
     * Inserts [word] into the trie with its associated [frequency].
     * If the word already exists, the higher frequency is retained.
     */
    fun insert(word: String, frequency: Int = 1) {
        var node = root
        for (char in word) {
            node = node.children.getOrPut(char) { TrieNode() }
        }
        node.isEndOfWord = true
        if (frequency > node.frequency) {
            node.frequency = frequency
        }
    }

    /** Returns true if [word] is present as a complete word in the trie. */
    fun contains(word: String): Boolean {
        val node = findNode(word) ?: return false
        return node.isEndOfWord
    }

    /**
     * Returns the frequency of [word], or 0 if the word is not in the trie.
     */
    fun getFrequency(word: String): Int {
        val node = findNode(word) ?: return 0
        return if (node.isEndOfWord) node.frequency else 0
    }

    /**
     * Returns all complete words that start with [prefix], sorted by frequency descending.
     * Returns at most [limit] results. Returns an empty list if no prefix match is found.
     */
    fun wordsWithPrefix(prefix: String, limit: Int = 10): List<Pair<String, Int>> {
        val prefixNode = findNode(prefix) ?: return emptyList()
        val results = mutableListOf<Pair<String, Int>>()
        collectWords(prefixNode, StringBuilder(prefix), results)
        return results.sortedByDescending { it.second }.take(limit)
    }

    /** Traverses the trie from root following characters in [str]; returns the final node or null. */
    private fun findNode(str: String): TrieNode? {
        var node = root
        for (char in str) {
            node = node.children[char] ?: return null
        }
        return node
    }

    /**
     * Depth-first traversal from [node], accumulating complete words into [results].
     * [currentWord] tracks the string built up so far on the current path.
     */
    private fun collectWords(
        node: TrieNode,
        currentWord: StringBuilder,
        results: MutableList<Pair<String, Int>>
    ) {
        if (node.isEndOfWord) {
            results.add(currentWord.toString() to node.frequency)
        }
        for ((char, child) in node.children) {
            currentWord.append(char)
            collectWords(child, currentWord, results)
            currentWord.deleteCharAt(currentWord.length - 1)
        }
    }

    private class TrieNode {
        val children: MutableMap<Char, TrieNode> = mutableMapOf()
        var isEndOfWord: Boolean = false
        var frequency: Int = 0
    }
}
