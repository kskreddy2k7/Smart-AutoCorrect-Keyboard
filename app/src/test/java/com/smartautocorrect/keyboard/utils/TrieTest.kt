package com.smartautocorrect.keyboard.utils

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [Trie].
 */
class TrieTest {

    private lateinit var trie: Trie

    @Before
    fun setUp() {
        trie = Trie()
    }

    @Test
    fun `contains - returns false for empty trie`() {
        assertFalse(trie.contains("hello"))
    }

    @Test
    fun `insert and contains - exact word is found`() {
        trie.insert("hello", 10)
        assertTrue(trie.contains("hello"))
    }

    @Test
    fun `contains - prefix alone is not a word`() {
        trie.insert("hello", 10)
        assertFalse(trie.contains("hell"))
    }

    @Test
    fun `insert - both prefix and full word can coexist`() {
        trie.insert("hell", 5)
        trie.insert("hello", 10)
        assertTrue(trie.contains("hell"))
        assertTrue(trie.contains("hello"))
    }

    @Test
    fun `getFrequency - returns 0 for unknown word`() {
        assertEquals(0, trie.getFrequency("unknown"))
    }

    @Test
    fun `getFrequency - returns correct frequency after insert`() {
        trie.insert("world", 42)
        assertEquals(42, trie.getFrequency("world"))
    }

    @Test
    fun `getFrequency - retains higher frequency on duplicate insert`() {
        trie.insert("hello", 5)
        trie.insert("hello", 20)
        assertEquals(20, trie.getFrequency("hello"))
    }

    @Test
    fun `getFrequency - does not return frequency for prefix-only node`() {
        trie.insert("hello", 10)
        assertEquals(0, trie.getFrequency("hell"))
    }

    @Test
    fun `wordsWithPrefix - returns empty list when prefix not found`() {
        trie.insert("hello", 5)
        val results = trie.wordsWithPrefix("xyz")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `wordsWithPrefix - returns matching words`() {
        trie.insert("hello", 10)
        trie.insert("help", 8)
        trie.insert("world", 5)
        val results = trie.wordsWithPrefix("hel")
        assertEquals(2, results.size)
        val words = results.map { it.first }
        assertTrue(words.contains("hello"))
        assertTrue(words.contains("help"))
        assertFalse(words.contains("world"))
    }

    @Test
    fun `wordsWithPrefix - results sorted by frequency descending`() {
        trie.insert("hello", 10)
        trie.insert("help", 30)
        trie.insert("helm", 20)
        val results = trie.wordsWithPrefix("hel")
        assertEquals("help", results[0].first)
        assertEquals("helm", results[1].first)
        assertEquals("hello", results[2].first)
    }

    @Test
    fun `wordsWithPrefix - respects limit parameter`() {
        trie.insert("aa", 1)
        trie.insert("ab", 2)
        trie.insert("ac", 3)
        trie.insert("ad", 4)
        trie.insert("ae", 5)
        val results = trie.wordsWithPrefix("a", limit = 3)
        assertEquals(3, results.size)
    }

    @Test
    fun `wordsWithPrefix - includes exact prefix word if it is end of word`() {
        trie.insert("he", 50)
        trie.insert("hello", 10)
        val results = trie.wordsWithPrefix("he")
        val words = results.map { it.first }
        assertTrue(words.contains("he"))
        assertTrue(words.contains("hello"))
    }

    @Test
    fun `wordsWithPrefix - empty prefix returns all words`() {
        trie.insert("apple", 3)
        trie.insert("banana", 7)
        val results = trie.wordsWithPrefix("")
        assertEquals(2, results.size)
    }

    @Test
    fun `insert - default frequency is 1`() {
        trie.insert("test")
        assertEquals(1, trie.getFrequency("test"))
    }
}
