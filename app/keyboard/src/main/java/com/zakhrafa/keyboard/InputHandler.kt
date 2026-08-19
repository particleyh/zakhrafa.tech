package com.zakhrafa.keyboard

import android.os.Build
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import com.zakhrafa.keyboard.core.InputSession
import com.zakhrafa.keyboard.core.KeyboardMode

internal class InputHandler(private val svc: ZakhrafaKeyboardService) {
    private val session = InputSession()
    private val snapshotRequest = ExtractedTextRequest().apply {
        token = 5301
        hintMaxChars = 2048
        hintMaxLines = 10
    }

    fun isWordCharacter(c: Char): Boolean {
        val code = c.code
        return c.isLetterOrDigit() ||
            code == 0x0640 ||
            code in 0x0610..0x061A ||
            code in 0x064B..0x065F ||
            code == 0x0670 ||
            code in 0x06D6..0x06ED
    }

    fun commitKey(raw: String) {
        val before = session.currentWord
        val endsWord = raw.any { !isWordCharacter(it) }
        val style = svc.activeStyle.takeIf { svc.supportsSuggestions() }
        val committed = runCatching {
            session.commit(
                svc.currentInputConnection,
                raw,
                when (svc.mode) {
                    ZakhrafaKeyboardService.LayoutMode.ENGLISH -> KeyboardMode.ENGLISH
                    ZakhrafaKeyboardService.LayoutMode.ARABIC -> KeyboardMode.ARABIC
                    else -> KeyboardMode.SYMBOLS
                },
                style
            )
        }.isSuccess
        if (!committed) {
            resetCurrentWord()
            svc.clearSuggestions()
            return
        }
        svc.currentWord = session.currentWord
        if (svc.isSensitiveEditor()) {
            // Passwords must never reach candidates, decoration state, or history.
            resetCurrentWord()
            svc.clearSuggestions()
            return
        }
        if (endsWord) {
            resetCurrentWord()
            svc.clearSuggestions()
        } else {
            svc.scheduleSuggestions()
        }
        if (session.currentWord != before && style != null && session.currentWord.length >= 3) {
            svc.wordStartedWithStyle = true
        }
    }

    fun deleteOne() {
        val fallbackLength = svc.currentOutputLengths.lastOrNull() ?: 1
        if (runCatching { session.delete(svc.currentInputConnection, fallbackLength) }.isFailure) {
            resetCurrentWord()
            svc.clearSuggestions()
            return
        }
        svc.currentWord = session.currentWord
        svc.currentCommittedLength = session.currentWord.length
        svc.scheduleSuggestions()
    }

    fun enter() {
        svc.resetCurrentWord()
        svc.clearSuggestions()
        val connection = svc.currentInputConnection ?: return
        val action = svc.currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_NONE
        val handledByEditor = action != EditorInfo.IME_ACTION_NONE &&
            action != EditorInfo.IME_ACTION_UNSPECIFIED &&
            runCatching { connection.performEditorAction(action) }.getOrDefault(false)
        if (!handledByEditor) {
            runCatching {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
        }
    }

    fun replaceCurrentWord(text: String) {
        if (runCatching { session.replaceWord(svc.currentInputConnection, text, svc.currentCommittedLength) }.isFailure) {
            resetCurrentWord()
            svc.clearSuggestions()
            return
        }
        svc.resetCurrentWord()
        svc.clearSuggestions()
    }

    fun commitExternalText(text: String): Boolean {
        if (text.isEmpty() || svc.isSensitiveEditor()) return false
        val committed = runCatching {
            svc.currentInputConnection?.commitText(text, 1) ?: return false
        }.isSuccess
        if (committed) {
            resetCurrentWord()
            svc.clearSuggestions()
        }
        return committed
    }

    fun resetCurrentWord() {
        session.reset()
        svc.currentWord = ""
        svc.currentCommittedLength = 0
        svc.currentOutputLengths.clear()
        svc.wordStartedWithStyle = false
    }

    /** Chat apps commonly clear their compose field after Send without a key event. */
    fun clearIfEditorWasCleared() {
        if (svc.currentWord.isBlank()) return
        val connection = svc.currentInputConnection ?: return
        val extractedText = runCatching {
            connection.getExtractedText(snapshotRequest, 0)?.text
        }.getOrNull()
        if (extractedText != null && extractedText.all(::isVisuallyEmptyCharacter)) {
            resetCurrentWord()
            svc.clearSuggestions()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val surroundingText = runCatching {
                connection.getSurroundingText(256, 256, 0)?.text
            }.getOrNull()
            if (surroundingText != null && surroundingText.all(::isVisuallyEmptyCharacter)) {
                resetCurrentWord()
                svc.clearSuggestions()
                return
            }
        }
        val beforeCursor = runCatching { connection.getTextBeforeCursor(256, 0)?.toString() }.getOrNull()
        val afterCursor = runCatching { connection.getTextAfterCursor(256, 0)?.toString() }.getOrNull()
        if (beforeCursor == null && afterCursor == null) return
        val visibleText = beforeCursor.orEmpty() + afterCursor.orEmpty()
        if (visibleText.all(::isVisuallyEmptyCharacter)) {
            resetCurrentWord()
            svc.clearSuggestions()
        }
    }

    /** Receives monitored editor snapshots when a host app clears its composer. */
    fun onEditorTextChanged(text: CharSequence?) {
        if (svc.currentWord.isBlank() || text == null) return
        if (text.all(::isVisuallyEmptyCharacter)) {
            resetCurrentWord()
            svc.clearSuggestions()
        }
    }

    /** A send/reset normally returns the composing cursor to the first position. */
    fun onEditorSelectionChanged(selectionStart: Int, selectionEnd: Int) {
        if (svc.currentWord.isNotBlank() && selectionStart == 0 && selectionEnd == 0) {
            resetCurrentWord()
            svc.clearSuggestions()
        } else {
            clearIfEditorWasCleared()
        }
    }

    private fun isVisuallyEmptyCharacter(char: Char): Boolean =
        char.isWhitespace() || char == '\u200E' || char == '\u200F' ||
            char in '\u202A'..'\u202E' || char in '\u2066'..'\u2069' || char == '\uFEFF'
}
