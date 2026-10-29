package com.zakhrafa.keyboard

import android.view.View

// Every row is declared in physical screen order from left to right. Arabic
// glyph direction must never be used to reorder the key Views themselves.
internal const val PHYSICAL_KEY_ROW_DIRECTION = View.LAYOUT_DIRECTION_LTR
internal const val ARABIC_TOP_ROW_LEFT_TO_RIGHT = "ضصثقفغعهخحج"
internal const val ARABIC_HOME_ROW_LEFT_TO_RIGHT = "شسيبلاتنمكط"
internal const val ARABIC_NUMBER_ROW_LEFT_TO_RIGHT = "١٢٣٤٥٦٧٨٩٠"
internal const val ENGLISH_NUMBER_ROW_LEFT_TO_RIGHT = "1234567890"
internal val ARABIC_DIACRITIC_VARIANTS = listOf("َ", "ُ", "ِ", "ّ", "ْ", "ً", "ٌ", "ٍ", "ٰ")

// Samsung-style symbol pages: page 1 carries the everyday signs, page 2 the
// extended set. Each string is one physical row, left to right. The toggle
// key and delete sit outside the strings so their positions stay fixed.
internal const val SYMBOLS_PAGE1_ROW2 = "@#\$%&-+()/"
internal const val SYMBOLS_PAGE1_ROW3 = "=*\"':؛!؟;?"
internal const val SYMBOLS_PAGE1_ROW4 = "_€£¥©®°¶§"
internal const val SYMBOLS_PAGE2_ROW1 = "~`^<>[]{}"
internal const val SYMBOLS_PAGE2_ROW2 = "\\|…–—•«»¡¿"
internal const val SYMBOLS_PAGE2_ROW3 = "±×÷≠≈√∞π∑µ"
internal const val SYMBOLS_PAGE2_ROW4 = "™¢₹₩℅∆‰℃℉"

internal class KeyboardLayouts(private val svc: ZakhrafaKeyboardService) {

    data class KeySpec(
        val label: String,
        val output: String = label,
        val weight: Float = 1f,
        val special: Boolean = false,
        val action: (() -> Unit)? = null,
        val longPress: List<String> = emptyList()
    )

    fun keyRows(mode: ZakhrafaKeyboardService.LayoutMode): List<List<KeySpec>> {
        return when (mode) {
            ZakhrafaKeyboardService.LayoutMode.ARABIC -> arabicRows()
            ZakhrafaKeyboardService.LayoutMode.ENGLISH -> englishRows()
            ZakhrafaKeyboardService.LayoutMode.SYMBOLS -> symbolRows()
            ZakhrafaKeyboardService.LayoutMode.EMOJI -> emptyList()
        }
    }

    private fun arabicRows(): List<List<KeySpec>> {
        val rows = mutableListOf<List<KeySpec>>()
        if (svc.keyboardPrefs.numberRow || svc.isSensitiveEditor()) rows.add(numberRow())
        rows.add(ARABIC_TOP_ROW_LEFT_TO_RIGHT.map { arabicKey(it) })
        rows.add(ARABIC_HOME_ROW_LEFT_TO_RIGHT.map { arabicKey(it) })
        rows.add(listOf(
            arabicKey('ذ'), arabicKey('ء'), arabicKey('ؤ'), arabicKey('ر'),
            arabicKey('ى'), arabicKey('ة'), arabicKey('و'), arabicKey('ز'), arabicKey('ظ'), arabicKey('د'),
            KeySpec("⌫", special = true, weight = 1.5f, action = { svc.deleteOne() })
        ))
        rows.add(bottomRow("مسافة", "،"))
        return rows
    }

    private fun arabicKey(char: Char): KeySpec {
        val label = char.toString()
        return KeySpec(label, longPress = arabicVariants(label))
    }

    private fun arabicVariants(label: String): List<String> {
        val letterVariants = when (label) {
            "ا" -> listOf("أ", "إ", "آ")
            "ى" -> listOf("ئ", "ي")
            "ي" -> listOf("ى", "ئ", "ے")
            "ء" -> listOf("أ", "إ", "آ", "ؤ", "ئ")
            "ؤ" -> listOf("و", "ء", "ؤ")
            "و" -> listOf("ؤ", "و")
            "ة" -> listOf("ه", "ة")
            "ه" -> listOf("ة", "ه")
            "ك" -> listOf("گ", "ڪ", "ك")
            "ق" -> listOf("ڨ", "ق")
            "ف" -> listOf("ڤ", "ف")
            else -> emptyList()
        }
        return letterVariants.distinct().filter { it != label }
    }

    private fun englishRows(): List<List<KeySpec>> {
        val rows = mutableListOf<List<KeySpec>>()
        if (svc.keyboardPrefs.numberRow || svc.isSensitiveEditor()) rows.add(numberRow())
        fun letters(value: String) = if (svc.englishShifted) value.uppercase() else value
        rows.add(letters("qwertyuiop").map { KeySpec(it.toString()) })
        rows.add(listOf(
            KeySpec(letters("a")), KeySpec(letters("s"), longPress = listOf(";")), KeySpec(letters("d")),
            KeySpec(letters("f")), KeySpec(letters("g")), KeySpec(letters("h")),
            KeySpec(letters("j")), KeySpec(letters("k")), KeySpec(letters("l"), longPress = listOf(":", "(", ")")),
        ))
        rows.add(listOf(
            KeySpec("⇧", special = true, weight = 1.3f, action = { svc.toggleEnglishShift() }),
            *letters("zxcvbnm").map { KeySpec(it.toString()) }.toTypedArray(),
            KeySpec("⌫", special = true, weight = 1.55f, action = { svc.deleteOne() })
        ))
        rows.add(bottomRow("space", ","))
        return rows
    }

    private fun bottomRow(spaceLabel: String, comma: String): List<KeySpec> {
        val commaLongPress = when (comma) {
            "،" -> listOf("؟", "!", "؛")
            else -> listOf(".", ";")
        }
        val periodLongPress = when (comma) {
            "،" -> ARABIC_DIACRITIC_VARIANTS
            else -> listOf(",", "?")
        }
        return if (svc.keyboardPrefs.wideSpacebar) {
            listOf(
                KeySpec("123", special = true, weight = 1.18f, action = { svc.switchMode(ZakhrafaKeyboardService.LayoutMode.SYMBOLS) }),
                KeySpec("🌐", special = true, weight = 1f, action = { svc.toggleLanguage() }),
                KeySpec(comma, weight = .78f, longPress = commaLongPress),
                KeySpec(spaceLabel, " ", special = true, weight = 4.9f),
                KeySpec("😊", special = true, weight = 1f, action = { svc.switchMode(ZakhrafaKeyboardService.LayoutMode.EMOJI) }),
                KeySpec(".", weight = .78f, longPress = periodLongPress),
                KeySpec(svc.enterKeyLabel(), special = true, weight = 1.42f, action = { svc.enter() })
            )
        } else {
            listOf(
                KeySpec("123", special = true, weight = 1.14f, action = { svc.switchMode(ZakhrafaKeyboardService.LayoutMode.SYMBOLS) }),
                KeySpec("🌐", special = true, weight = 1f, action = { svc.toggleLanguage() }),
                KeySpec(comma, weight = .78f, longPress = commaLongPress),
                KeySpec(spaceLabel, " ", special = true, weight = 4.45f),
                KeySpec("😊", special = true, weight = 1f, action = { svc.switchMode(ZakhrafaKeyboardService.LayoutMode.EMOJI) }),
                KeySpec(".", weight = .78f, longPress = periodLongPress),
                KeySpec(svc.enterKeyLabel(), special = true, weight = 1.3f, action = { svc.enter() })
            )
        }
    }

    private fun symbolRows(): List<List<KeySpec>> {
        // The bottom row mirrors the letter layouts so permanent keys (emoji,
        // space, period, enter) keep the same position when switching modes.
        // Only the 123/ABC label differs. Delete stays at the right edge of the
        // content row directly above the bottom row; the page toggle sits at
        // the left edge of that same row so the bottom row never moves.
        val english = svc.lastTextMode == ZakhrafaKeyboardService.LayoutMode.ENGLISH
        // On numbers/emoji pages, the key that returns to letters reads "أبجد"
        // when the keyboard will land on Arabic and "ABC" when on English.
        val lettersLabel = if (english) "ABC" else "أبجد"
        val bottom = bottomRow(
            if (english) "space" else "مسافة",
            if (english) "," else "،"
        ).map { key ->
            if (key.label == "123") {
                key.copy(label = lettersLabel, output = lettersLabel, action = { svc.switchMode(svc.lastTextMode) })
            } else key
        }
        val pageOneLongPress = mapOf(
            "%" to listOf("٪"),
            "\"" to listOf("“", "”"),
            "'" to listOf("‘", "’")
        )
        fun row(value: String) = value.map { char ->
            val label = char.toString()
            KeySpec(label, longPress = pageOneLongPress[label] ?: emptyList())
        }
        val toggle = KeySpec(
            label = if (svc.symbolPage == 0) "1/2" else "2/2",
            special = true,
            weight = 1.18f,
            action = { svc.toggleSymbolPage() }
        )
        val delete = KeySpec("⌫", special = true, weight = 1.5f, action = { svc.deleteOne() })
        return if (svc.symbolPage == 0) listOf(
            numberRow(),
            row(SYMBOLS_PAGE1_ROW2),
            row(SYMBOLS_PAGE1_ROW3),
            listOf(toggle) + row(SYMBOLS_PAGE1_ROW4) + delete,
            bottom
        ) else listOf(
            row(SYMBOLS_PAGE2_ROW1),
            row(SYMBOLS_PAGE2_ROW2),
            row(SYMBOLS_PAGE2_ROW3),
            listOf(toggle) + row(SYMBOLS_PAGE2_ROW4) + delete,
            bottom
        )
    }

    private fun emojiRows(): List<List<KeySpec>> {
        val skinTones = listOf("\uD83C\uDFFB", "\uD83C\uDFFC", "\uD83C\uDFFD", "\uD83C\uDFFE", "\uD83C\uDFFF")
        fun skinToneVariants(base: String): List<String> = skinTones.map { base + it }
        val english = svc.lastTextMode == ZakhrafaKeyboardService.LayoutMode.ENGLISH
        val lettersLabel = if (english) "ABC" else "أبجد"

        return listOf(
            listOf("😀", "😁", "😂", "🤣", "😍", "🥰", "😘", "😎", "😉", "😊").map { KeySpec(it) },
            listOf("😅", "😭", "🥹", "😡", "😤", "🤔", "🙄", "😴", "🤲", "🙏").map { KeySpec(it) },
            listOf(
                KeySpec("👍", longPress = skinToneVariants("👍")),
                KeySpec("👎", longPress = skinToneVariants("👎")),
                KeySpec("👏", longPress = skinToneVariants("👏")),
                KeySpec("💪", longPress = skinToneVariants("💪")),
                KeySpec("🤝", longPress = skinToneVariants("🤝")),
                KeySpec("👌", longPress = skinToneVariants("👌")),
                KeySpec("✌", longPress = skinToneVariants("✌")),
                KeySpec("🤍"),
                KeySpec("❤️"),
                KeySpec("💔")
            ),
            listOf("🔥", "✨", "⭐", "🌹", "🎉", "✅", "💯", "👑", "💎", "🎮").map { KeySpec(it) },
            listOf("🇸🇦", "🇦🇪", "🇰🇼", "🇶🇦", "🇧🇭", "🇴🇲", "🇾🇪", "🇮🇶", "🇪🇬", "🇯🇴").map { KeySpec(it) },
            listOf(
                KeySpec(lettersLabel, special = true, weight = 1.3f, action = { svc.switchMode(svc.lastTextMode) }),
                KeySpec("123", special = true, weight = 1.3f, action = { svc.switchMode(ZakhrafaKeyboardService.LayoutMode.SYMBOLS) }),
                KeySpec("مسافة", " ", special = true, weight = 5.0f),
                KeySpec("⌫", special = true, weight = 1.34f, action = { svc.deleteOne() }),
                KeySpec("⏎", special = true, weight = 1.34f, action = { svc.enter() })
            )
        )
    }

    fun numberRow(): List<KeySpec> {
        return ENGLISH_NUMBER_ROW_LEFT_TO_RIGHT.mapIndexed { index, digit ->
            // Western digits are the visible default in both language modes. Arabic
            // digits stay one long press away, which satisfies both number systems
            // without stealing a full row from everyday typing.
            KeySpec(
                label = digit.toString(),
                longPress = listOf(ARABIC_NUMBER_ROW_LEFT_TO_RIGHT[index].toString())
            )
        }
    }

    fun longPressHint(key: KeySpec): String? {
        if (key.special || key.longPress.isEmpty()) return null
        if (key.longPress.size == 1) return key.longPress.first()
        return key.longPress.firstOrNull()
    }
}
