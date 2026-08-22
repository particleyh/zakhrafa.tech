package com.zakhrafa.engine

import com.zakhrafa.engine.styles.EnglishStyles
import com.zakhrafa.engine.styles.LegacyStyles
import com.zakhrafa.engine.styles.TotalStyles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZakhrafaEngineTest {
    @Test
    fun generatedLegacyMapsTransformIndividualCharacters() {
        assertTrue(TotalStyles.arabic[1].map.containsKey("ض"))
        assertTrue(TotalStyles.english[1].map.containsKey("q"))
        assertNotEquals("ض", ZakhrafaEngine.mapChars("ض", TotalStyles.arabic[1].map))
        assertNotEquals("q", ZakhrafaEngine.mapChars("q", TotalStyles.english[1].map))
    }

    @Test
    fun mathematicalEnglishStylesPreserveLetterCase() {
        val result = ZakhrafaEngine.applyEnglishStyle("Aa", EnglishStyles.all.first())
        val codePoints = result.codePoints().toArray()

        assertEquals(2, codePoints.size)
        assertNotEquals(codePoints[0], codePoints[1])
    }

    @Test
    fun filtersOnlyReturnTheirRequestedFamilies() {
        val symbols = ZakhrafaEngine.generateAll("Hello", "symbols")
        val platform = ZakhrafaEngine.generateAll("Hello", "pubg")

        assertTrue(symbols.isNotEmpty())
        assertTrue(symbols.all { it.category == "symbols" })
        assertTrue(platform.isNotEmpty())
        assertTrue(platform.all { it.category == "platform" })
    }

    @Test
    fun generatorIncludesStylesSharedWithKeyboardCatalog() {
        val arabic = ZakhrafaEngine.generateAll("محمد", "arabic")
        val english = ZakhrafaEngine.generateAll("Zakhrafa", "english")

        assertTrue(arabic.any { it.category == "ar-legacy" })
        assertTrue(english.any { it.category == "en-legacy" })
        assertTrue(ZakhrafaEngine.countStyles() >= LegacyStyles.arabic.size + LegacyStyles.english.size)
    }

    @Test
    fun generatedResultsNeverRepeatOrReturnOriginalInput() {
        val input = "زخرفة Hello"
        val results = ZakhrafaEngine.generateAll(input)

        assertFalse(results.any { it.text == input })
        assertEquals(results.size, results.distinctBy { it.text }.size)
        assertTrue(results.size > 200)
    }
}
