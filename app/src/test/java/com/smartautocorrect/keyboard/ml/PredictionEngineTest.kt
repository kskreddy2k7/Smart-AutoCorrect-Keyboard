package com.smartautocorrect.keyboard.ml

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Unit tests for [PredictionEngine].
 */
class PredictionEngineTest {

    private lateinit var tfliteHelper: TFLiteHelper
    private lateinit var predictionEngine: PredictionEngine

    @Before
    fun setUp() {
        tfliteHelper = mock(TFLiteHelper::class.java)
        `when`(tfliteHelper.isAvailable()).thenReturn(false)
        predictionEngine = PredictionEngine(tfliteHelper)
    }

    @Test
    fun `predict - returns empty list when no bigrams recorded`() {
        val result = predictionEngine.predict("hello")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `recordBigram and predict - returns recorded next word`() {
        predictionEngine.recordBigram("hello", "world")
        val result = predictionEngine.predict("hello")
        assertEquals(1, result.size)
        assertEquals("world", result[0].word)
    }

    @Test
    fun `predict - returns top predictions sorted by frequency`() {
        predictionEngine.recordBigram("the", "cat")
        predictionEngine.recordBigram("the", "dog")
        predictionEngine.recordBigram("the", "dog")
        predictionEngine.recordBigram("the", "cat")
        predictionEngine.recordBigram("the", "dog")

        val result = predictionEngine.predict("the", limit = 2)
        assertEquals(2, result.size)
        assertEquals("dog", result[0].word) // highest frequency
        assertEquals("cat", result[1].word)
    }

    @Test
    fun `predict - respects limit parameter`() {
        repeat(5) { i -> predictionEngine.recordBigram("test", "word$i") }
        val result = predictionEngine.predict("test", limit = 3)
        assertEquals(3, result.size)
    }

    @Test
    fun `loadBigrams - loads external bigram data`() {
        val data = mapOf("hello" to mapOf("world" to 10, "there" to 5))
        predictionEngine.loadBigrams(data)
        val result = predictionEngine.predict("hello")
        assertEquals("world", result[0].word)
    }
}
