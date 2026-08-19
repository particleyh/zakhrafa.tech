package com.zakhrafa.keyboard.core

import android.content.Context
import com.zakhrafa.engine.ZakhrafaEngine
import com.zakhrafa.engine.styles.ArabicStyles
import com.zakhrafa.engine.styles.CoolnamesStyles
import com.zakhrafa.engine.styles.Decorations
import com.zakhrafa.engine.styles.EnglishStyles
import com.zakhrafa.engine.styles.LegacyStyles
import com.zakhrafa.engine.styles.ProfessionalStyles
import com.zakhrafa.engine.styles.TotalStyles
import com.zakhrafa.engine.models.StyleMap

class DecorationCatalog(context: Context) {
    private val prefs = context.getSharedPreferences("decoration_catalog", Context.MODE_PRIVATE)

    fun stylesFor(mode: KeyboardMode): List<DecorationStyle> {
        val source = if (mode == KeyboardMode.ENGLISH) {
            EnglishStyles.all + LegacyStyles.english + ProfessionalStyles.english + TotalStyles.english + complexStyles() + frameStyles()
        } else {
            ArabicStyles.all + LegacyStyles.arabic + ProfessionalStyles.arabic + TotalStyles.arabic + coolNamesStyles() + frameStyles()
        }
        return source
            // Some imported collections contain placeholder identity maps. Never show a
            // style unless it visibly transforms a representative word.
            .filter { isVisibleDecoration(it, mode) }
            .distinctBy { stableId(it.name, it.category, it.map.toString(), it.range.toString(), it.special.orEmpty()) }
            .map {
                DecorationStyle(
                    id = stableId(it.name, it.category, it.map.toString(), it.range.toString(), it.special.orEmpty()),
                    style = it,
                    isPremium = it.category.startsWith("ar-pro") || it.category.startsWith("en-pro"),
                    categoryLabel = it.category,
                    label = friendlyName(it.name)
                )
            }
            // Keep true letter transformations first. Frame/side decorations wrap a
            // word rather than changing its letters, so they belong at the end.
            // Kotlin's stable sort preserves the curated engine order inside each group.
            .sortedWith(
                compareBy<DecorationStyle> { if (it.categoryLabel == "frame") 1 else 0 }
                    .thenByDescending { isFavorite(it.id) }
                    .thenByDescending { it.isPremium }
            )
    }

    fun isFavorite(id: String): Boolean = prefs.getBoolean("favorite_$id", false)

    fun toggleFavorite(id: String): Boolean {
        val next = !isFavorite(id)
        prefs.edit().putBoolean("favorite_$id", next).apply()
        return next
    }

    fun recentIds(): List<String> = (prefs.getString("recent", "") ?: "")
        .split(',').filter { it.isNotBlank() }

    fun markRecent(id: String) {
        val values = recentIds().toMutableList().apply {
            remove(id)
            add(0, id)
        }
        prefs.edit().putString("recent", values.distinct().take(12).joinToString(",")).apply()
    }

    private fun isVisibleDecoration(style: com.zakhrafa.engine.models.StyleMap, mode: KeyboardMode): Boolean {
        val sample = if (mode == KeyboardMode.ENGLISH) "Zakhrafa" else "زخرفة"
        if (style.category == "frame") {
            return style.map[FRAME_LEFT].orEmpty().isNotBlank() || style.map[FRAME_RIGHT].orEmpty().isNotBlank()
        }
        val output = if (mode == KeyboardMode.ENGLISH) {
            ZakhrafaEngine.applyEnglishStyle(sample, style)
        } else {
            ZakhrafaEngine.mapChars(sample, style.map)
        }
        return output.isNotBlank() && output != sample
    }

    private fun friendlyName(name: String): String = when {
        name.startsWith("Ar Style ") -> "زخرفة عربية ${name.removePrefix("Ar Style ")}" 
        name.startsWith("En Style ") -> "زخرفة إنجليزية ${name.removePrefix("En Style ")}" 
        else -> name
    }

    /** Makes the engine's extra Arabic collections usable as live keyboard styles. */
    private fun coolNamesStyles(): List<StyleMap> {
        val letters = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي"
        return CoolnamesStyles.styleNames.mapIndexed { index, name ->
            StyleMap(
                name = name,
                category = "ar-coolnames",
                map = letters.associate { letter ->
                    letter.toString() to CoolnamesStyles.apply(letter.toString(), index)
                }
            )
        }
    }

    /** Complex engine decorations are also character maps, so they work while typing. */
    private fun complexStyles(): List<StyleMap> {
        val letters = "abcdefghijklmnopqrstuvwxyz"
        return Decorations.complex.mapIndexed { index, decoration ->
            val glyphs = decoration.map.codePoints().toArray().map { String(Character.toChars(it)) }
            StyleMap(
                name = "زخرفة نادرة ${index + 1}",
                category = "en-complex",
                map = letters.mapIndexedNotNull { position, letter ->
                    glyphs.getOrNull(position)?.let { letter.toString() to it }
                }.toMap()
            )
        }
    }

    /** Side decorations are word wrappers, handled by InputSession as live frames. */
    private fun frameStyles(): List<StyleMap> = Decorations.side.map { decoration ->
        StyleMap(
            name = decoration.label,
            category = "frame",
            map = mapOf(FRAME_LEFT to decoration.left, FRAME_RIGHT to decoration.right)
        )
    }

    companion object {
        const val FRAME_LEFT = "__frame_left"
        const val FRAME_RIGHT = "__frame_right"
    }

    private fun stableId(vararg parts: String): String = parts.joinToString("|").hashCode().toUInt().toString(16)
}
