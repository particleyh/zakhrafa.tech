package com.zakhrafa.keyboard

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import com.zakhrafa.engine.ZakhrafaEngine
import com.zakhrafa.engine.models.DecorationResult
import com.zakhrafa.engine.models.StyleMap
import java.util.concurrent.Executors

internal class SuggestionManager(
    private val svc: ZakhrafaKeyboardService,
    private val renderer: KeyRenderer
) {
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "zakhrafa-suggestions").apply { isDaemon = true }
    }
    private val cacheLock = Any()
    private var pendingRunnable: Runnable? = null
    private var editorWatch: Runnable? = null
    private var requestGeneration = 0
    @Volatile private var closed = false

    private var lastKey: SuggestionKey? = null
    private var lastResults: List<DecorationResult> = emptyList()

    private data class SuggestionKey(
        val word: String,
        val mode: ZakhrafaKeyboardService.LayoutMode,
        val styleKey: String,
        val limit: Int
    )

    private data class SuggestionRequest(val key: SuggestionKey, val style: StyleMap?)

    fun updateSuggestions(suggestionStrip: LinearLayout) {
        suggestionStrip.removeAllViews()

        // A newly copied link has temporary priority over decoration predictions.
        // It stays in this same rail until the user pastes or dismisses it.
        val pendingLink = if (svc.supportsSuggestions()) {
            svc.clipboardManager.pendingLinkSuggestion()
        } else {
            null
        }
        if (pendingLink != null) {
            svc.idleControls.visibility = View.GONE
            svc.suggestionsScroll.visibility = View.VISIBLE
            suggestionStrip.addView(renderer.suggestionChip("🔗  ${svc.clipboardManager.preview(pendingLink)}", primary = true) {
                svc.clipboardManager.pastePendingLink()
            })
            suggestionStrip.addView(renderer.suggestionChip("×") {
                svc.clipboardManager.dismissPendingLink()
            })
            stopEditorWatch()
            return
        }

        // This is deliberately one strip: predictions replace the idle actions
        // while typing, then the decoration controls return when the word ends.
        val hasWord = svc.currentWord.isNotBlank() && svc.supportsSuggestions()
        svc.idleControls.visibility = if (hasWord) View.GONE else View.VISIBLE
        svc.suggestionsScroll.visibility = if (hasWord) View.VISIBLE else View.GONE
        if (!hasWord) {
            stopEditorWatch()
            return
        }

        startEditorWatch()
        val request = currentRequest() ?: return
        val results = synchronized(cacheLock) {
            if (lastKey == request.key) lastResults else null
        }
        if (results == null) {
            scheduleSuggestions()
            return
        }
        results.forEachIndexed { index, result ->
            suggestionStrip.addView(renderer.suggestionChip(result.text, primary = index == 0) {
                svc.inputHandler.replaceCurrentWord(result.text)
            })
        }
    }

    fun scheduleSuggestions() {
        if (closed) return
        pendingRunnable?.let { handler.removeCallbacks(it) }
        val generation = ++requestGeneration
        pendingRunnable = Runnable {
            val request = currentRequest()
            if (request == null) {
                svc.updateSuggestions()
                return@Runnable
            }
            runCatching {
                worker.execute {
                    if (closed) return@execute
                    val results = computeDecorationSuggestions(request)
                    synchronized(cacheLock) {
                        lastKey = request.key
                        lastResults = results
                    }
                    if (closed) return@execute
                    handler.post {
                        if (!closed && generation == requestGeneration && currentRequest()?.key == request.key) {
                            svc.updateSuggestions()
                        }
                    }
                }
            }
        }
        pendingRunnable?.let { handler.postDelayed(it, 30) }
    }

    fun cancelAndClear() {
        pendingRunnable?.let { handler.removeCallbacks(it) }
        pendingRunnable = null
        requestGeneration++
        stopEditorWatch()
        synchronized(cacheLock) {
            lastKey = null
            lastResults = emptyList()
        }
    }

    fun close() {
        closed = true
        cancelAndClear()
        handler.removeCallbacksAndMessages(null)
        worker.shutdownNow()
    }

    /** Some chat apps clear their composer after Send without notifying the IME. */
    private fun startEditorWatch() {
        if (editorWatch != null) return
        editorWatch = object : Runnable {
            override fun run() {
                svc.inputHandler.clearIfEditorWasCleared()
                if (svc.currentWord.isBlank()) {
                    editorWatch = null
                } else {
                    handler.postDelayed(this, 450)
                }
            }
        }
        editorWatch?.let { handler.postDelayed(it, 450) }
    }

    private fun stopEditorWatch() {
        editorWatch?.let { handler.removeCallbacks(it) }
        editorWatch = null
    }

    private fun currentRequest(): SuggestionRequest? {
        val word = svc.currentWord
        if (word.isBlank() || !svc.supportsSuggestions()) return null
        val style = svc.activeStyle
        return SuggestionRequest(
            SuggestionKey(
                word = word,
                mode = svc.mode,
                styleKey = style?.let { "${it.category}|${it.name}" }.orEmpty(),
                limit = svc.keyboardPrefs.suggestionCount.coerceIn(4, 24)
            ),
            style
        )
    }

    private fun computeDecorationSuggestions(request: SuggestionRequest): List<DecorationResult> {
        return runCatching {
            val results = mutableListOf<DecorationResult>()
            val word = request.key.word

            request.style?.let { style ->
                val text = if (style.category == "frame") {
                    style.map[com.zakhrafa.keyboard.core.DecorationCatalog.FRAME_LEFT].orEmpty() + word +
                        style.map[com.zakhrafa.keyboard.core.DecorationCatalog.FRAME_RIGHT].orEmpty()
                } else if (request.key.mode == ZakhrafaKeyboardService.LayoutMode.ENGLISH) {
                    ZakhrafaEngine.applyEnglishStyle(word, style)
                } else {
                    ZakhrafaEngine.mapChars(word, style.map)
                }
                if (text != word) results.add(DecorationResult(text, "النمط المختار", "active"))
            }

            val baseFilter = if (request.key.mode == ZakhrafaKeyboardService.LayoutMode.ENGLISH) "english" else "arabic"
            results += ZakhrafaEngine.generateAll(word, baseFilter).take(18)
            results += ZakhrafaEngine.generateAll(word, "symbols").take(12)

            val final = results.distinctBy { it.text }
                .take(request.key.limit)

            final
        }.getOrDefault(emptyList())
    }

}
