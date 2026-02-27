package com.smartautocorrect.keyboard.ui.keyboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputConnection
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartautocorrect.keyboard.R
import com.smartautocorrect.keyboard.domain.model.WordSuggestion
import com.smartautocorrect.keyboard.domain.usecase.GetSuggestionsUseCase
import com.smartautocorrect.keyboard.domain.usecase.LearnWordUseCase
import com.smartautocorrect.keyboard.ml.PredictionEngine
import com.smartautocorrect.keyboard.utils.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main keyboard InputMethodService.
 *
 * Handles key input, autocorrect, suggestion bar display, and learning.
 * Annotated with @AndroidEntryPoint for Hilt injection.
 */
@AndroidEntryPoint
class KeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    @Inject lateinit var getSuggestionsUseCase: GetSuggestionsUseCase
    @Inject lateinit var learnWordUseCase: LearnWordUseCase
    @Inject lateinit var predictionEngine: PredictionEngine
    @Inject lateinit var themeManager: ThemeManager

    private lateinit var keyboardView: KeyboardView
    private lateinit var keyboard: Keyboard
    private lateinit var suggestionAdapter: SuggestionAdapter

    // Current word being typed
    private val wordBuffer = StringBuilder()
    // Previous word (for bigram prediction)
    private var previousWord: String = ""

    // Coroutine scope for async operations; cancelled in onDestroy
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    // Debounce job for suggestion updates
    private var suggestionJob: Job? = null

    override fun onCreateInputView(): View {
        val rootView = layoutInflater.inflate(R.layout.keyboard_view, null)

        keyboardView = rootView.findViewById(R.id.keyboardView)
        keyboard = Keyboard(this, R.xml.keyboard_layout)
        keyboardView.keyboard = keyboard
        keyboardView.setOnKeyboardActionListener(this)

        // Set up the suggestion bar RecyclerView
        val rvSuggestions = rootView.findViewById<RecyclerView>(R.id.rvSuggestions)
        suggestionAdapter = SuggestionAdapter { suggestion ->
            onSuggestionSelected(suggestion)
        }
        rvSuggestions.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvSuggestions.adapter = suggestionAdapter

        return rootView
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic: InputConnection = getCurrentInputConnection() ?: return

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> handleDelete(ic)
            Keyboard.KEYCODE_DONE -> handleDone(ic)
            KEYCODE_SPACE -> handleSpace(ic)
            else -> handleCharacter(ic, primaryCode)
        }
    }

    private fun handleDelete(ic: InputConnection) {
        val selectedText = ic.getSelectedText(0)
        if (selectedText != null && selectedText.isNotEmpty()) {
            // Delete selected text
            ic.commitText("", 1)
            wordBuffer.clear()
        } else {
            ic.deleteSurroundingText(1, 0)
            if (wordBuffer.isNotEmpty()) {
                wordBuffer.deleteCharAt(wordBuffer.length - 1)
            }
        }
        scheduleSuggestionUpdate()
    }

    private fun handleDone(ic: InputConnection) {
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        commitCurrentWord(ic)
        wordBuffer.clear()
        suggestionAdapter.submitList(emptyList())
    }

    private fun handleSpace(ic: InputConnection) {
        if (wordBuffer.isNotEmpty()) {
            val currentWord = wordBuffer.toString()
            commitWordWithAutocorrect(ic, currentWord)
            previousWord = currentWord.lowercase()
            wordBuffer.clear()
        }
        ic.commitText(" ", 1)
        scheduleSuggestionUpdate()
    }

    private fun handleCharacter(ic: InputConnection, primaryCode: Int) {
        val char = primaryCode.toChar()
        wordBuffer.append(char)
        ic.commitText(char.toString(), 1)
        scheduleSuggestionUpdate()
    }

    /**
     * Apply autocorrect for the word and commit it.
     * Uses the best suggestion if one with isAutocorrect = true exists.
     */
    private fun commitWordWithAutocorrect(ic: InputConnection, currentWord: String) {
        if (!themeManager.autocorrectEnabled) {
            serviceScope.launch { learnWordUseCase(currentWord) }
            return
        }

        serviceScope.launch {
            val suggestions = getSuggestionsUseCase(
                currentWord, previousWord, themeManager.language
            )
            val autocorrect = suggestions.firstOrNull { it.isAutocorrect }
            if (autocorrect != null && autocorrect.word != currentWord.lowercase()) {
                // Replace the typed word with the autocorrected word
                ic.deleteSurroundingText(currentWord.length, 0)
                ic.commitText(autocorrect.word, 1)
            }
            learnWordUseCase(currentWord)
            predictionEngine.recordBigram(previousWord, currentWord.lowercase())
        }
    }

    private fun commitCurrentWord(ic: InputConnection) {
        if (wordBuffer.isNotEmpty()) {
            val word = wordBuffer.toString()
            serviceScope.launch { learnWordUseCase(word) }
        }
    }

    /**
     * Debounced suggestion update triggered on each key press.
     */
    private fun scheduleSuggestionUpdate() {
        if (!themeManager.suggestionsEnabled) return
        suggestionJob?.cancel()
        suggestionJob = serviceScope.launch {
            delay(SUGGESTION_DEBOUNCE_MS)
            updateSuggestions()
        }
    }

    private suspend fun updateSuggestions() {
        val current = wordBuffer.toString()
        val suggestions = getSuggestionsUseCase(current, previousWord, themeManager.language)
        suggestionAdapter.submitList(suggestions)
    }

    /**
     * Called when the user taps a suggestion in the suggestion bar.
     * Replaces the current typed word with the suggestion.
     */
    private fun onSuggestionSelected(suggestion: WordSuggestion) {
        val ic: InputConnection = getCurrentInputConnection() ?: return
        val typedLength = wordBuffer.length
        if (typedLength > 0) {
            ic.deleteSurroundingText(typedLength, 0)
        }
        ic.commitText(suggestion.word, 1)
        serviceScope.launch { learnWordUseCase(suggestion.word) }
        predictionEngine.recordBigram(previousWord, suggestion.word)
        previousWord = suggestion.word
        wordBuffer.clear()
        scheduleSuggestionUpdate()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    // Unused but required by KeyboardView.OnKeyboardActionListener
    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}

    companion object {
        private const val KEYCODE_SPACE = 32
        private const val SUGGESTION_DEBOUNCE_MS = 100L
    }
}
