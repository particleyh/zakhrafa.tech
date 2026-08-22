package com.zakhrafa.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InputLimitTest {
    @Test
    fun shortInputIsUnchanged() {
        val input = "زخرفة Hello ✦"
        assertEquals(input, limitInput(input))
    }

    @Test
    fun longInputIsLimitedByCodePointWithoutSplittingEmoji() {
        val input = "💚".repeat(MAX_INPUT_LENGTH + 10)
        val limited = limitInput(input)

        assertEquals(MAX_INPUT_LENGTH, limited.codePointCount(0, limited.length))
        assertTrue(limited.all { !it.isSurrogate() } || limited.length == MAX_INPUT_LENGTH * 2)
    }
}
