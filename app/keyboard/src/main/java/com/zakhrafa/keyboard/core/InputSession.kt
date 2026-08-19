package com.zakhrafa.keyboard.core

import android.view.inputmethod.InputConnection
import com.zakhrafa.engine.ZakhrafaEngine
import com.zakhrafa.engine.models.StyleMap

/**
 * Tracks logical input separately from the Unicode output emitted by a style.
 * This avoids deleting only one UTF-16 unit from a decorated character.
 */
class InputSession {
    private data class EmittedToken(val logical: String, val output: String)

    private val tokens = ArrayDeque<EmittedToken>()
    private var activeFrame: Pair<String, String>? = null
    var currentWord: String = ""
        private set

    fun reset() {
        tokens.clear()
        currentWord = ""
        activeFrame = null
    }

    fun commit(
        inputConnection: InputConnection?,
        raw: String,
        mode: KeyboardMode,
        style: StyleMap?
    ) {
        if (inputConnection == null) return
        if (raw == " ") {
            inputConnection.commitText(" ", 1)
            reset()
            return
        }

        val logicalCharacter = raw.length == 1 && raw[0].isLetterOrDigit()
        val frame = style?.takeIf { it.category == "frame" }?.let {
            it.map[DecorationCatalog.FRAME_LEFT].orEmpty() to it.map[DecorationCatalog.FRAME_RIGHT].orEmpty()
        }
        if (logicalCharacter && frame != null) {
            commitFramedCharacter(inputConnection, raw, frame)
            return
        }
        val output = if (logicalCharacter && style != null) {
            if (mode == KeyboardMode.ENGLISH) ZakhrafaEngine.applyEnglishStyle(raw, style)
            else ZakhrafaEngine.mapChars(raw, style.map)
        } else raw
        // Do not insert invisible bidi characters into live typing. Android already
        // applies Arabic directionality; bidi marks can remain in an apparently
        // empty chat composer and also interfere with normal Arabic joining.
        val committedOutput = output

        inputConnection.commitText(committedOutput, 1)
        if (logicalCharacter) {
            tokens.addLast(EmittedToken(raw, committedOutput))
            currentWord += raw
        } else {
            reset()
        }
    }

    fun delete(inputConnection: InputConnection?, fallbackLength: Int = 1) {
        if (inputConnection == null) return
        activeFrame?.let { frame ->
            val prior = tokens.lastOrNull()?.output.orEmpty()
            if (prior.isNotEmpty()) inputConnection.deleteSurroundingText(prior.length, 0)
            currentWord = currentWord.dropLast(1)
            tokens.clear()
            if (currentWord.isNotEmpty()) {
                val output = frame.first + currentWord + frame.second
                inputConnection.commitText(output, 1)
                tokens.addLast(EmittedToken(currentWord, output))
            } else {
                activeFrame = null
            }
            return
        }
        val token = tokens.removeLastOrNull()
        if (token != null) {
            inputConnection.deleteSurroundingText(token.output.length, 0)
            currentWord = currentWord.dropLast(token.logical.length)
        } else {
            inputConnection.deleteSurroundingText(fallbackLength.coerceAtLeast(1), 0)
        }
    }

    fun replaceWord(inputConnection: InputConnection?, replacement: String, fallbackLength: Int = 0) {
        if (inputConnection == null) return
        val length = tokens.sumOf { it.output.length }.takeIf { it > 0 } ?: fallbackLength
        if (length > 0) inputConnection.deleteSurroundingText(length, 0)
        inputConnection.commitText("$replacement ", 1)
        reset()
    }

    private fun commitFramedCharacter(
        inputConnection: InputConnection,
        raw: String,
        frame: Pair<String, String>
    ) {
        val prior = tokens.lastOrNull()?.output.orEmpty()
        if (prior.isNotEmpty()) inputConnection.deleteSurroundingText(prior.length, 0)
        currentWord += raw
        val output = frame.first + currentWord + frame.second
        inputConnection.commitText(output, 1)
        tokens.clear()
        tokens.addLast(EmittedToken(currentWord, output))
        activeFrame = frame
    }
}
