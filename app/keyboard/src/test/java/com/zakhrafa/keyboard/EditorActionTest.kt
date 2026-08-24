package com.zakhrafa.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorActionTest {
    @Test
    fun multilineSearchFieldInsertsNewline() {
        assertTrue(shouldInsertNewlineForEditor(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            EditorInfo.IME_ACTION_SEARCH
        ))
    }

    @Test
    fun singleLineSearchFieldKeepsSearchAction() {
        assertFalse(shouldInsertNewlineForEditor(
            InputType.TYPE_CLASS_TEXT,
            EditorInfo.IME_ACTION_SEARCH
        ))
    }

    @Test
    fun multilineSendFieldKeepsSendAction() {
        assertFalse(shouldInsertNewlineForEditor(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            EditorInfo.IME_ACTION_SEND
        ))
    }

    @Test
    fun noEnterActionAlwaysInsertsNewline() {
        assertTrue(shouldInsertNewlineForEditor(
            InputType.TYPE_CLASS_TEXT,
            EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_ENTER_ACTION
        ))
    }
}
