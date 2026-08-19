package com.zakhrafa.engine.styles

import com.zakhrafa.engine.models.StyleMap

object EnglishStyles {
    private fun buildEnMap(fn: (Int) -> String?): Map<String, String> {
        val letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val m = mutableMapOf<String, String>()
        for (c in letters) {
            val r = fn(c.code)
            if (r != null) m[c.toString()] = r
        }
        return m
    }

    val all = listOf(
        StyleMap("بولد", "en-math", range = Triple(119834, 119808, 120782)),
        StyleMap("مائل", "en-math", range = Triple(120354, 120328, null)),
        StyleMap("بولد مائل", "en-math", range = Triple(120406, 120380, 120782)),
        StyleMap("خط يد عريض", "en-math", range = Triple(120042, 120016, null)),
        StyleMap("خط يد", "en-math", special = "script"),
        StyleMap("فراكتور", "en-math", special = "fraktur"),
        StyleMap("فراكتور عريض", "en-math", special = "boldFraktur"),
        StyleMap("مزدوج", "en-math", special = "doubleStruck"),
        StyleMap("أحادي المسافة", "en-math", range = Triple(120458, 120432, 120822)),
        StyleMap("سانس سيرف", "en-math", range = Triple(120250, 120224, 120802)),
        StyleMap("سانس عريض", "en-math", range = Triple(120302, 120276, 120812)),
        StyleMap("سانس مائل", "en-math", range = Triple(120354, 120328, null)),
        StyleMap("دائري", "en-fancy", buildEnMap { cp ->
            when {
                cp in 97..122 -> String(Character.toChars(9424 + cp - 97))
                cp in 65..90 -> String(Character.toChars(9398 + cp - 65))
                else -> null
            }
        }),
        StyleMap("حروف صغيرة", "en-fancy", mapOf(
            "a" to "ᴀ", "b" to "ʙ", "c" to "ᴄ", "d" to "ᴅ", "e" to "ᴇ", "f" to "ꜰ", "g" to "ɢ", "h" to "ʜ", "i" to "ɪ", "j" to "ᴊ", "k" to "ᴋ", "l" to "ʟ", "m" to "ᴍ", "n" to "ɴ", "o" to "ᴏ", "p" to "ᴘ", "q" to "ǫ", "r" to "ʀ", "s" to "ꜱ", "t" to "ᴛ", "u" to "ᴜ", "v" to "ᴠ", "w" to "ᴡ", "x" to "x", "y" to "ʏ", "z" to "ᴢ"
        )),
        StyleMap("مربع أسود", "en-fancy", buildEnMap { cp ->
            val off = if (cp <= 90) 127280 else 127246
            val base = if (cp <= 90) 65 else 97
            String(Character.toChars(off + cp - base))
        }),
        StyleMap("يوناني", "en-decorated", mapOf(
            "a" to "α", "b" to "в", "c" to "¢", "d" to "∂", "e" to "є", "f" to "ƒ", "g" to "g", "h" to "н", "i" to "ι", "j" to "נ", "k" to "к", "l" to "ℓ", "m" to "м", "n" to "η", "o" to "σ", "p" to "ρ", "q" to "q", "r" to "я", "s" to "ѕ", "t" to "т", "u" to "υ", "v" to "ν", "w" to "ω", "x" to "χ", "y" to "у", "z" to "z"
        )),
        StyleMap("هاكر", "en-decorated", mapOf(
            "a" to "@", "b" to "ß", "c" to "¢", "d" to "Ð", "e" to "€", "f" to "ƒ", "g" to "9", "h" to "#", "i" to "!", "j" to "ʝ", "k" to "Ҡ", "l" to "£", "m" to "₥", "n" to "₪", "o" to "Ø", "p" to "¶", "r" to "®", "s" to "§", "t" to "†", "u" to "µ", "v" to "✓", "w" to "ω", "x" to "×", "y" to "¥", "z" to "ž"
        )),
        StyleMap("سيبر", "en-decorated", mapOf(
            "a" to "4", "b" to "8", "c" to "[", "d" to "|)", "e" to "3", "f" to "ƒ", "g" to "6", "h" to "#", "i" to "1", "j" to "_|", "k" to "|<", "l" to "1", "m" to "^^", "n" to "|\\|", "o" to "0", "p" to "|*", "r" to "|2", "s" to "5", "t" to "7", "u" to "(_)", "v" to "\\/", "w" to "\\/\\/", "x" to "><", "y" to "`/", "z" to "2"
        )),
        StyleMap("ناري", "en-decorated", mapOf(
            "a" to "Δ", "b" to "β", "c" to "¢", "d" to "Đ", "e" to "Σ", "f" to "Ғ", "g" to "Ǥ", "h" to "Ħ", "i" to "į", "j" to "ʆ", "k" to "Ҡ", "l" to "Ł", "m" to "Μ", "n" to "И", "o" to "Θ", "p" to "ρ", "r" to "Я", "s" to "Ϟ", "t" to "Ŧ", "u" to "Ʊ", "v" to "V", "w" to "Ш", "x" to "Ж", "y" to "Ұ", "z" to "乙"
        )),
        StyleMap("كريستال", "en-decorated", mapOf(
            "a" to "ä", "b" to "þ", "c" to "č", "d" to "ď", "e" to "ë", "f" to "ƒ", "g" to "ğ", "h" to "ħ", "i" to "ï", "j" to "ĵ", "k" to "ķ", "l" to "ľ", "m" to "ṁ", "n" to "ň", "o" to "ö", "p" to "ρ", "r" to "ř", "s" to "š", "t" to "ť", "u" to "ü", "v" to "v", "w" to "ŵ", "x" to "ẋ", "y" to "ÿ", "z" to "ž"
        )),
        StyleMap("حاد", "en-decorated", mapOf(
            "a" to "ᗩ", "b" to "ᗷ", "c" to "ᑕ", "d" to "ᗪ", "e" to "ᗴ", "f" to "ᖴ", "g" to "Ǥ", "h" to "ᕼ", "i" to "I", "j" to "ᒍ", "k" to "ᖽ", "l" to "ᒪ", "m" to "ᗰ", "n" to "ᑎ", "o" to "O", "p" to "ᑭ", "r" to "ᖇ", "s" to "ᔕ", "t" to "T", "u" to "ᑌ", "v" to "ᐯ", "w" to "ᗯ", "x" to "᙭", "y" to "Y", "z" to "ᘔ"
        )),
        StyleMap("روني", "en-decorated", mapOf(
            "a" to "ᚨ", "b" to "ᛒ", "c" to "ᚲ", "d" to "ᛞ", "e" to "ᛖ", "f" to "ᚠ", "g" to "ᚷ", "h" to "ᚺ", "i" to "ᛁ", "j" to "ᛃ", "k" to "ᚲ", "l" to "ᛚ", "m" to "ᛗ", "n" to "ᚾ", "o" to "ᛟ", "p" to "ᛈ", "r" to "ᚱ", "s" to "ᛋ", "t" to "ᛏ", "u" to "ᚢ", "v" to "ᚡ", "w" to "ᚹ", "x" to "ᛪ", "y" to "ᚤ", "z" to "ᛉ"
        )),
        StyleMap("سايبر بنك", "en-decorated", mapOf(
            "a" to "Δ", "b" to "β", "c" to "ᑕ", "d" to "∂", "e" to "є", "f" to "ƒ", "g" to "ﻭ", "h" to "н", "i" to "ι", "j" to "נ", "k" to "κ", "l" to "ℓ", "m" to "м", "n" to "Π", "o" to "σ", "p" to "ρ", "q" to "ợ", "r" to "尺", "s" to "ř", "t" to "ţ", "u" to "ย", "v" to "v", "w" to "ฬ", "x" to "Ж", "y" to "ץ", "z" to "ž"
        )),
        StyleMap("جليتش 1", "en-effect", buildEnMap { cp -> "${cp.toChar()}̸" }),
        StyleMap("خط تحت", "en-effect", buildEnMap { cp -> "${cp.toChar()}̲" }),
        StyleMap("شطب", "en-effect", buildEnMap { cp -> "${cp.toChar()}̶" }),
        StyleMap("علمي 🇦-🇿", "en-trending", buildEnMap { cp ->
            if (cp in 97..122) String(Character.toChars(0x1F1E6 + cp - 97)) else null
        })
    )
}
