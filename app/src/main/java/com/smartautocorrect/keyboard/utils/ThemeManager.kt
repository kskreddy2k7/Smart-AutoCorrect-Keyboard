package com.smartautocorrect.keyboard.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages keyboard theme (light/dark) and color settings.
 * Persists user preferences via SharedPreferences.
 */
class ThemeManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Current theme mode. */
    var theme: Theme
        get() = Theme.fromValue(prefs.getString(KEY_THEME, Theme.LIGHT.value) ?: Theme.LIGHT.value)
        set(value) = prefs.edit().putString(KEY_THEME, value.value).apply()

    /** Keyboard background color (ARGB hex string). */
    var keyboardBackground: String
        get() = prefs.getString(KEY_BG_COLOR, DEFAULT_LIGHT_BG) ?: DEFAULT_LIGHT_BG
        set(value) = prefs.edit().putString(KEY_BG_COLOR, value).apply()

    /** Key background color (ARGB hex string). */
    var keyColor: String
        get() = prefs.getString(KEY_COLOR, DEFAULT_LIGHT_KEY) ?: DEFAULT_LIGHT_KEY
        set(value) = prefs.edit().putString(KEY_COLOR, value).apply()

    /** Keyboard language. */
    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    /** Whether suggestions are enabled. */
    var suggestionsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SUGGESTIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_SUGGESTIONS, value).apply()

    /** Whether autocorrect is enabled. */
    var autocorrectEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTOCORRECT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTOCORRECT, value).apply()

    enum class Theme(val value: String) {
        LIGHT("light"),
        DARK("dark");

        companion object {
            fun fromValue(value: String): Theme =
                values().firstOrNull { it.value == value } ?: LIGHT
        }
    }

    companion object {
        private const val PREFS_NAME = "keyboard_theme_prefs"
        private const val KEY_THEME = "theme"
        private const val KEY_BG_COLOR = "bg_color"
        private const val KEY_COLOR = "key_color"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_SUGGESTIONS = "suggestions_enabled"
        private const val KEY_AUTOCORRECT = "autocorrect_enabled"
        private const val DEFAULT_LIGHT_BG = "#FFECEFF1"
        private const val DEFAULT_LIGHT_KEY = "#FFFFFFFF"
    }
}
