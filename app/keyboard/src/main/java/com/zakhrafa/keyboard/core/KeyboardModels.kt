package com.zakhrafa.keyboard.core

import com.zakhrafa.engine.models.StyleMap

enum class KeyboardMode { ARABIC, ENGLISH, SYMBOLS, EMOJI }

data class KeyboardUiState(
    val mode: KeyboardMode = KeyboardMode.ARABIC,
    val lastTextMode: KeyboardMode = KeyboardMode.ARABIC,
    val activeStyle: StyleMap? = null,
    val currentWord: String = "",
    val suggestionsVisible: Boolean = false,
    val utilityPanel: UtilityPanel = UtilityPanel.None
)

sealed interface UtilityPanel {
    data object None : UtilityPanel
    data object Clipboard : UtilityPanel
    data object Emoji : UtilityPanel
    data object Symbols : UtilityPanel
}

data class DecorationStyle(
    val id: String,
    val style: StyleMap,
    val isPremium: Boolean,
    val categoryLabel: String,
    val label: String
)
