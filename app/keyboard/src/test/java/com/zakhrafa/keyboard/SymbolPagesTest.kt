package com.zakhrafa.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolPagesTest {
    @Test
    fun pageOneCarriesEverydaySignsIncludingPercentAndLatinPunctuation() {
        val pageOne = SYMBOLS_PAGE1_ROW2 + SYMBOLS_PAGE1_ROW3 + SYMBOLS_PAGE1_ROW4
        listOf(
            "%", ";", "?", "=", "+", "-", "*", "/", "\"", "'", ":", "!",
            "&", "#", "$", "(", ")", "_", "@",
            "€", "£", "¥", "©", "®", "°", "¶", "§"
        ).forEach { sign -> assertTrue("page 1 missing $sign", pageOne.contains(sign)) }
    }

    @Test
    fun pageTwoCarriesExtendedSigns() {
        val pageTwo = SYMBOLS_PAGE2_ROW1 + SYMBOLS_PAGE2_ROW2 + SYMBOLS_PAGE2_ROW3 + SYMBOLS_PAGE2_ROW4
        listOf(
            "<", ">", "[", "]", "{", "}", "|", "\\", "~", "^", "`",
            "…", "–", "—", "•", "«", "»", "¡", "¿",
            "±", "×", "÷", "≠", "≈", "√", "∞", "π", "∑", "µ",
            "™", "¢", "₹", "₩", "℅", "∆", "‰", "℃", "℉"
        ).forEach { sign -> assertTrue("page 2 missing $sign", pageTwo.contains(sign)) }
    }

    @Test
    fun arabicPunctuationStaysOnPageOne() {
        assertTrue(SYMBOLS_PAGE1_ROW3.contains("؛"))
        assertTrue(SYMBOLS_PAGE1_ROW3.contains("؟"))
    }

    @Test
    fun rowsMatchTheFiveRowSymbolKeyboard() {
        assertEquals(10, SYMBOLS_PAGE1_ROW2.length)
        assertEquals(10, SYMBOLS_PAGE1_ROW3.length)
        assertEquals(9, SYMBOLS_PAGE1_ROW4.length)
        assertEquals(9, SYMBOLS_PAGE2_ROW1.length)
        assertEquals(10, SYMBOLS_PAGE2_ROW2.length)
        assertEquals(10, SYMBOLS_PAGE2_ROW3.length)
        assertEquals(9, SYMBOLS_PAGE2_ROW4.length)
    }

    @Test
    fun noSignIsDuplicatedAcrossPages() {
        val pageOne = (SYMBOLS_PAGE1_ROW2 + SYMBOLS_PAGE1_ROW3 + SYMBOLS_PAGE1_ROW4).toSet()
        val pageTwo = (SYMBOLS_PAGE2_ROW1 + SYMBOLS_PAGE2_ROW2 + SYMBOLS_PAGE2_ROW3 + SYMBOLS_PAGE2_ROW4).toSet()
        assertTrue(pageOne.intersect(pageTwo).isEmpty())
    }
}
