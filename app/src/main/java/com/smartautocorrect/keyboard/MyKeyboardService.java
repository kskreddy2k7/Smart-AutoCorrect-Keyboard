package com.smartautocorrect.keyboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;

public class MyKeyboardService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {
    
    private KeyboardView keyboardView;
    private Keyboard keyboard;
    private StringBuilder wordBuffer;
    private SpellChecker spellChecker;

    @Override
    public void onCreate() {
        super.onCreate();
        wordBuffer = new StringBuilder();
        spellChecker = new SpellChecker();
    }

    @Override
    public View onCreateInputView() {
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);
        keyboard = new Keyboard(this, R.xml.keyboard_layout);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);
        return keyboardView;
    }

    @Override
    public void onPress(int primaryCode) {
    }

    @Override
    public void onRelease(int primaryCode) {
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                CharSequence selectedText = ic.getSelectedText(0);
                if (selectedText != null && selectedText.length() > 0) {
                    ic.commitText("", 1);
                    wordBuffer.setLength(0);
                } else {
                    ic.deleteSurroundingText(1, 0);
                    if (wordBuffer.length() > 0) {
                        wordBuffer.deleteCharAt(wordBuffer.length() - 1);
                    }
                }
                break;
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                wordBuffer.setLength(0);
                break;
            case 32: // Space key
                if (wordBuffer.length() > 0) {
                    String currentWord = wordBuffer.toString();
                    String correctedWord = spellChecker.correctWord(currentWord);
                    
                    if (!currentWord.equals(correctedWord)) {
                        ic.deleteSurroundingText(currentWord.length(), 0);
                        ic.commitText(correctedWord, 1);
                    }
                    
                    wordBuffer.setLength(0);
                }
                ic.commitText(" ", 1);
                break;
            default:
                char code = (char) primaryCode;
                wordBuffer.append(code);
                ic.commitText(String.valueOf(code), 1);
                break;
        }
    }

    @Override
    public void onText(CharSequence text) {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeUp() {
    }
}
