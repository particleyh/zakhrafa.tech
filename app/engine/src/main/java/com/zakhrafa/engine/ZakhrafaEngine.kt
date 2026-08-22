package com.zakhrafa.engine

import com.zakhrafa.engine.models.*
import com.zakhrafa.engine.styles.*
import java.lang.StringBuilder
import kotlin.random.Random

object ZakhrafaEngine {

    fun mapChars(text: String, map: Map<String, String>): String {
        val sb = StringBuilder()
        for (char in text) {
            sb.append(map[char.toString()] ?: char.toString())
        }
        return sb.toString()
    }

    fun rangeMap(text: String, lo: Int, hi: Int?, digit: Int?): String {
        val sb = StringBuilder()
        for (char in text) {
            val cp = char.code
            when {
                cp in 97..122 -> sb.append(Character.toChars(lo + (cp - 97)))
                hi != null && cp in 65..90 -> sb.append(Character.toChars(hi + (cp - 65)))
                digit != null && cp in 48..57 -> sb.append(Character.toChars(digit + (cp - 48)))
                else -> sb.append(char)
            }
        }
        return sb.toString()
    }

    fun zalgo(text: String, intensity: Int = 3): String {
        val up = listOf(0x030d, 0x030e, 0x0304, 0x0305, 0x033f, 0x0311, 0x0306, 0x0310, 0x0352, 0x0357, 0x0351, 0x0307)
        val mid = listOf(0x0315, 0x031b, 0x0340, 0x0341, 0x0358, 0x0321, 0x0322, 0x0327)
        val down = listOf(0x0316, 0x0317, 0x0318, 0x0319, 0x031c, 0x031d, 0x031e, 0x031f, 0x0320, 0x0324, 0x0325, 0x0326)
        
        val sb = StringBuilder()
        for (char in text) {
            sb.append(char)
            if (char == ' ') continue
            repeat(intensity) { sb.append(Character.toChars(up[Random.nextInt(up.size)])) }
            repeat(intensity / 2) { sb.append(Character.toChars(mid[Random.nextInt(mid.size)])) }
            repeat(intensity) { sb.append(Character.toChars(down[Random.nextInt(down.size)])) }
        }
        return sb.toString()
    }

    fun applyEnglishStyle(text: String, style: StyleMap): String {
        return when {
            style.map.isNotEmpty() -> buildString {
                text.forEach { char ->
                    append(
                        style.map[char.toString()]
                            ?: style.map[char.lowercaseChar().toString()]
                            ?: char
                    )
                }
            }
            style.range != null -> rangeMap(text, style.range.first, style.range.second, style.range.third)
            style.special != null -> applySpecialEnStyle(text, style.special)
            else -> text
        }
    }

    private fun applySpecialEnStyle(text: String, type: String): String {
        val sb = StringBuilder()
        for (c in text) {
            val cp = c.code
            val res = when (type) {
                "script" -> {
                    val ex = mapOf(101 to 119890, 103 to 119892, 111 to 119900, 66 to 8492, 69 to 8496, 70 to 8497, 72 to 8459, 73 to 8464, 76 to 8466, 77 to 8499, 82 to 8475)
                    when {
                        ex.containsKey(cp) -> Character.toChars(ex[cp]!!)
                        cp in 97..122 -> Character.toChars(119990 + cp - 97)
                        cp in 65..90 -> Character.toChars(119964 + cp - 65)
                        else -> charArrayOf(c)
                    }
                }
                "fraktur", "boldFraktur" -> {
                    val bold = type == "boldFraktur"
                    val ex = mapOf(67 to 8450, 72 to 8461, 78 to 8469, 80 to 8473, 81 to 8474, 82 to 8477, 90 to 8484)
                    val exB = mapOf(67 to 8493, 72 to 8460, 73 to 8465, 82 to 8476, 90 to 8488)
                    when {
                        !bold && ex.containsKey(cp) -> Character.toChars(ex[cp]!!)
                        bold && exB.containsKey(cp) -> Character.toChars(exB[cp]!!)
                        cp in 97..122 -> Character.toChars((if (bold) 120198 else 120094) + cp - 97)
                        cp in 65..90 -> Character.toChars((if (bold) 120172 else 120068) + cp - 65)
                        else -> charArrayOf(c)
                    }
                }
                "doubleStruck" -> {
                    val ex = mapOf(67 to 8450, 72 to 8461, 78 to 8469, 80 to 8473, 81 to 8474, 82 to 8477, 90 to 8484)
                    when {
                        ex.containsKey(cp) -> Character.toChars(ex[cp]!!)
                        cp in 97..122 -> Character.toChars(120146 + cp - 97)
                        cp in 65..90 -> Character.toChars(120120 + cp - 65)
                        cp in 48..57 -> Character.toChars(120792 + cp - 48)
                        else -> charArrayOf(c)
                    }
                }
                else -> charArrayOf(c)
            }
            sb.append(res)
        }
        return sb.toString()
    }

    private fun complexDecorate(text: String, template: ComplexDecoration): String {
        val sb = StringBuilder()
        for (c in text.lowercase()) {
            val cp = c.code
            if (cp in 97..122) {
                val m = template.map.codePoints().toArray()
                if (cp - 97 < m.size) {
                    sb.append(Character.toChars(m[cp - 97]))
                } else {
                    sb.append(c)
                }
            } else {
                sb.append(c)
            }
        }
        return "${template.left}${sb}${template.right}"
    }

    fun generateAll(text: String, filter: String = "all", platform: String = "none"): List<DecorationResult> {
        val results = mutableListOf<DecorationResult>()
        val hasAr = text.any { it.code in 0x0600..0x06FF }
        val hasEn = text.any { it.lowercaseChar() in 'a'..'z' }
        val platforms = listOf("pubg", "freefire", "tiktok", "instagram", "facebook")
        val isPlat = filter in platforms

        fun add(txt: String, name: String, cat: String) {
            if (txt.isNotBlank() && txt != text) {
                results.add(DecorationResult(txt, name, cat))
            }
        }

        // Each filter stays focused. Platform filters add their own curated results only.
        val showAr = filter == "all" || filter == "arabic"
        val showEn = filter == "all" || filter == "english"
        val showComplex = filter == "all" || filter == "complex"
        val showSymbols = filter == "all" || filter == "symbols"

        fun addPlatformResults(platformId: String) {
            val pSyms = when (platformId) {
                "pubg" -> listOf("乂", "ツ", "メ", "气", "〆", "ジ", "乙", "刁", "丶", "۝")
                "freefire" -> listOf("乂", "么", "〆", "乡", "彡", "ツ", "シ", "メ", "⚡")
                "tiktok" -> listOf("♬", "♪", "♫", "✦", "♡", "ツ", "☁", "✧")
                "instagram" -> listOf("♡", "✧", "✦", "☾", "☼", "•", "⟡", "𓆩", "𓆪")
                "facebook" -> listOf("★", "•", "ღ", "♡", "✓", "✦", "❖")
                else -> listOf("★", "✦", "◆", "♡")
            }
            val frames = when (platformId) {
                "pubg" -> listOf("꧁" to "꧂", "乂" to "乂", "『" to "』", "༺" to "༻")
                "freefire" -> listOf("꧁" to "꧂", "么" to "么", "〆" to "〆", "༒" to "༒")
                "tiktok" -> listOf("♬" to "♬", "ꜱ" to "ツ", "✦" to "✦", "☁" to "☁")
                "instagram" -> listOf("𓆩" to "𓆪", "♡" to "♡", "☾" to "☽", "⟡" to "⟡")
                "facebook" -> listOf("★" to "★", "ღ" to "ღ", "❖" to "❖", "✓" to "✓")
                else -> listOf("꧁" to "꧂", "★" to "★")
            }

            frames.forEachIndexed { index, (left, right) ->
                add("$left$text$right", "$platformId إطار ${index + 1}", "platform")
            }
            pSyms.forEachIndexed { index, symbol ->
                add("$symbol $text $symbol", "$platformId رمز ${index + 1}", "platform")
                if (hasAr) add("$symbol ${mapChars(text, ArabicStyles.all[0].map)} $symbol", "$platformId عربي", "platform")
                if (hasEn) add("$symbol ${applyEnglishStyle(text, EnglishStyles.all.first())} $symbol", "$platformId English", "platform")
            }
        }

        // Platform categories must surface their unique names first; otherwise they look like
        // the generic Arabic/English category until the user scrolls.
        if (isPlat || platform != "none") {
            addPlatformResults(if (isPlat) filter else platform)
        }

        if (showAr) {
            ArabicStyles.all.forEach { s -> add(mapChars(text, s.map), s.name, s.category) }
            LegacyStyles.arabic.forEach { s -> add(mapChars(text, s.map), s.name, s.category) }
            ProfessionalStyles.arabic.forEach { s -> add(mapChars(text, s.map), s.name, s.category) }
            TotalStyles.arabic.forEach { s -> add(mapChars(text, s.map), s.name, s.category) }
            if (hasAr) {
                CoolnamesStyles.styleNames.forEachIndexed { i, name ->
                    add(CoolnamesStyles.apply(text, i), name, "ar-coolnames")
                }
            }
            if (hasAr) add(zalgo(text, 1), "عربي زالجو", "ar-effect")
        }

        if (showEn) {
            EnglishStyles.all.forEach { s -> add(applyEnglishStyle(text, s), s.name, s.category) }
            LegacyStyles.english.forEach { s -> add(applyEnglishStyle(text, s), s.name, s.category) }
            ProfessionalStyles.english.forEach { s -> add(applyEnglishStyle(text, s), s.name, s.category) }
            TotalStyles.english.forEach { s -> add(applyEnglishStyle(text, s), s.name, s.category) }
            
            if (hasEn) add(zalgo(text, 2), "English Zalgo", "en-effect")
        }

        if (showComplex) {
            Decorations.complex.forEachIndexed { i, template ->
                add(complexDecorate(text, template), "نادرة ${i + 1}", "complex")
            }
        }

        if (showSymbols) {
            Decorations.side.forEach { sd -> add("${sd.left}${text}${sd.right}", sd.label, "symbols") }
        }

        return results.distinctBy { it.text }
    }

    fun suggest(text: String): List<DecorationResult> {
        if (text.isBlank()) return emptyList()
        val all = generateAll(text).shuffled().take(15)
        return all.distinctBy { it.text }.take(10)
    }

    fun countStyles(): Int {
        return ArabicStyles.all.size + LegacyStyles.arabic.size + ProfessionalStyles.arabic.size + TotalStyles.arabic.size +
                EnglishStyles.all.size + LegacyStyles.english.size + ProfessionalStyles.english.size + TotalStyles.english.size +
                Decorations.complex.size + Decorations.side.size +
                CoolnamesStyles.styleNames.size
    }
}
