package com.smartautocorrect.keyboard.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [LevenshteinUtils].
 */
class LevenshteinUtilsTest {

    @Test
    fun `distance - identical strings returns 0`() {
        assertEquals(0, LevenshteinUtils.distance("hello", "hello"))
    }

    @Test
    fun `distance - empty strings returns 0`() {
        assertEquals(0, LevenshteinUtils.distance("", ""))
    }

    @Test
    fun `distance - one empty string returns other length`() {
        assertEquals(5, LevenshteinUtils.distance("hello", ""))
        assertEquals(5, LevenshteinUtils.distance("", "hello"))
    }

    @Test
    fun `distance - single substitution`() {
        assertEquals(1, LevenshteinUtils.distance("helo", "hello"))
    }

    @Test
    fun `distance - transposition counts as 1`() {
        // Damerau extension: adjacent transposition = 1 edit
        assertEquals(1, LevenshteinUtils.distance("teh", "the"))
    }

    @Test
    fun `distance - completely different strings`() {
        val dist = LevenshteinUtils.distance("abc", "xyz")
        assertEquals(3, dist)
    }

    @Test
    fun `similarity - identical strings returns 1`() {
        assertEquals(1.0f, LevenshteinUtils.similarity("test", "test"), 0.001f)
    }

    @Test
    fun `similarity - completely different strings returns near 0`() {
        val sim = LevenshteinUtils.similarity("abc", "xyz")
        assertTrue(sim < 0.1f)
    }

    @Test
    fun `maxAllowedDistance - short words get 0 tolerance`() {
        assertEquals(0, LevenshteinUtils.maxAllowedDistance(3))
    }

    @Test
    fun `maxAllowedDistance - medium words get 1 tolerance`() {
        assertEquals(1, LevenshteinUtils.maxAllowedDistance(5))
    }

    @Test
    fun `maxAllowedDistance - longer words get higher tolerance`() {
        assertEquals(2, LevenshteinUtils.maxAllowedDistance(7))
        assertEquals(3, LevenshteinUtils.maxAllowedDistance(10))
    }
}
