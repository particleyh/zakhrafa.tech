package com.zakhrafa.keyboard.core

import android.content.Context
import com.zakhrafa.keyboard.theme.KeyboardTheme
import com.zakhrafa.keyboard.theme.Themes

data class KeyboardSettings(
    val themeName: String = Themes.pearl.name,
    val keyHeightDp: Int = 46,
    val haptic: Boolean = true,
    val sound: Boolean = false,
    val numberRow: Boolean = true,
    val suggestionCount: Int = 18,
    val labelScale: Int = 100,
    val wideSpacebar: Boolean = true,
    val backgroundUri: String = "",
    val defaultLanguage: String = "ar",
    val showStyleSuggestions: Boolean = true,
    val reduceMotion: Boolean = false
) {
    fun theme(): KeyboardTheme = Themes.all.firstOrNull { it.name == themeName } ?: Themes.pearl
}

/** Single persistence boundary for the keyboard. Old v29 keys are intentionally supported. */
class KeyboardSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): KeyboardSettings {
        return KeyboardSettings(
            themeName = prefs.getString("theme", Themes.pearl.name) ?: Themes.pearl.name,
            keyHeightDp = prefs.getInt("key_height", 46).coerceIn(38, 76),
            haptic = prefs.getBoolean("haptic", true),
            sound = prefs.getBoolean("sound", false),
            numberRow = prefs.getBoolean("number_row", true),
            suggestionCount = prefs.getInt("suggestion_count", 18).coerceIn(4, 24),
            labelScale = prefs.getInt("label_scale", 100).coerceIn(75, 115),
            wideSpacebar = prefs.getBoolean("wide_spacebar", true),
            backgroundUri = prefs.getString("background_uri", "") ?: "",
            defaultLanguage = prefs.getString("default_language", "ar") ?: "ar",
            showStyleSuggestions = prefs.getBoolean("show_style_suggestions", true),
            reduceMotion = prefs.getBoolean("reduce_motion", false)
        )
    }

    fun write(settings: KeyboardSettings) {
        prefs.edit()
            .putString("theme", settings.themeName)
            .putInt("key_height", settings.keyHeightDp)
            .putBoolean("haptic", settings.haptic)
            .putBoolean("sound", settings.sound)
            .putBoolean("number_row", settings.numberRow)
            .putInt("suggestion_count", settings.suggestionCount)
            .putInt("label_scale", settings.labelScale)
            .putBoolean("wide_spacebar", settings.wideSpacebar)
            .putString("background_uri", settings.backgroundUri)
            .putString("default_language", settings.defaultLanguage)
            .putBoolean("show_style_suggestions", settings.showStyleSuggestions)
            .putBoolean("reduce_motion", settings.reduceMotion)
            .apply()
    }

    fun update(transform: (KeyboardSettings) -> KeyboardSettings): KeyboardSettings {
        val next = transform(read())
        write(next)
        return next
    }

    companion object {
        const val PREFS = "keyboard_settings"
    }
}
