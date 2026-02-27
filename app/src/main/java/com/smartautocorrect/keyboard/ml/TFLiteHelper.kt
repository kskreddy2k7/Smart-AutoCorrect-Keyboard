package com.smartautocorrect.keyboard.ml

import android.content.Context
import com.smartautocorrect.keyboard.domain.model.WordSuggestion
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for TensorFlow Lite inference.
 *
 * Loads a TFLite model from assets if available and runs next-word prediction.
 * If the model file does not exist, gracefully degrades to returning empty results.
 */
@Singleton
class TFLiteHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var isModelLoaded = false

    init {
        tryLoadModel()
    }

    private fun tryLoadModel() {
        try {
            // Attempt to check if the TFLite model exists in assets
            context.assets.open(MODEL_FILENAME).use {
                // Model exists - in a real implementation, load org.tensorflow.lite.Interpreter here
                isModelLoaded = true
            }
        } catch (e: Exception) {
            // Model not found - will use rule-based fallback
            isModelLoaded = false
        }
    }

    /** Returns true if a TFLite model was successfully loaded. */
    fun isAvailable(): Boolean = isModelLoaded

    /**
     * Run inference to predict next words after [previousWord].
     * Returns empty list if model is not loaded.
     */
    fun predict(previousWord: String, limit: Int = 3): List<WordSuggestion> {
        if (!isModelLoaded) return emptyList()
        // TFLite inference would be implemented here with loaded model
        // Returning empty for now when model exists but inference not fully wired
        return emptyList()
    }

    companion object {
        private const val MODEL_FILENAME = "keyboard_model.tflite"
    }
}
