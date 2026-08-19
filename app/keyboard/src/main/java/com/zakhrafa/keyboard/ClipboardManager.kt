package com.zakhrafa.keyboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.os.Build

/** Length-prefixed storage keeps multiline and Unicode clipboard entries intact. */
internal object ClipboardHistoryCodec {
    fun encode(values: List<String>): String = buildString {
        values.forEach { value ->
            append(value.length)
            append(':')
            append(value)
        }
    }

    fun decode(encoded: String): List<String> {
        if (encoded.isEmpty()) return emptyList()
        val result = mutableListOf<String>()
        var cursor = 0
        while (cursor < encoded.length) {
            val separator = encoded.indexOf(':', cursor)
            if (separator <= cursor) return emptyList()
            val length = encoded.substring(cursor, separator).toIntOrNull() ?: return emptyList()
            val start = separator + 1
            val end = start + length
            if (length < 0 || end > encoded.length) return emptyList()
            result += encoded.substring(start, end)
            cursor = end
        }
        return result
    }
}

internal object ClipboardLinkDetector {
    private val webLink = Regex("(?i)^(?:(?:https?://)|(?:www\\.))[^\\s]+$")
    fun isLink(text: String): Boolean = webLink.matches(text.trim())
}

internal class ClipboardManager(private val svc: ZakhrafaKeyboardService) {
    private val prefs = svc.getSharedPreferences("clipboard_history", Context.MODE_PRIVATE)
    private val systemClipboard = svc.getSystemService(Context.CLIPBOARD_SERVICE) as?
        android.content.ClipboardManager
    private val maxItems = 100
    private var listening = false
    private var pendingLink: String? = null
    private var pendingSignature: String? = null
    private var observedSignature: String? = null

    private val clipListener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
        refreshPrimaryClip(fromChange = true)
    }

    init {
        // Old releases polluted this value with partially typed decorated words and
        // used a delimiter that corrupted multiline clips. Start the repaired store clean.
        if (!prefs.getBoolean("history_v2_initialized", false)) {
            prefs.edit()
                .remove("history")
                .putString("history_v2", "")
                .putBoolean("history_v2_initialized", true)
                .apply()
        }
    }

    fun start() {
        if (svc.isSensitiveEditor()) return
        if (!listening) {
            runCatching { systemClipboard?.addPrimaryClipChangedListener(clipListener) }
            listening = true
        }
        refreshPrimaryClip(fromChange = false)
    }

    fun stop() {
        if (!listening) return
        runCatching { systemClipboard?.removePrimaryClipChangedListener(clipListener) }
        listening = false
    }

    fun pendingLinkSuggestion(): String? = pendingLink

    fun preview(text: String, limit: Int = 46): String {
        val compact = text.replace(Regex("\\s+"), " ").trim()
        return if (compact.length <= limit) compact else compact.take(limit - 1) + "…"
    }

    fun pastePendingLink() {
        val text = pendingLink ?: return
        if (svc.inputHandler.commitExternalText(text)) {
            addEntry(text)
            consumePendingLink()
        }
    }

    fun dismissPendingLink() {
        consumePendingLink()
    }

    private fun consumePendingLink() {
        pendingSignature?.let {
            prefs.edit().putString("consumed_clip_signature", it).apply()
        }
        pendingLink = null
        pendingSignature = null
        svc.updateSuggestions()
    }

    fun getHistory(): List<String> = ClipboardHistoryCodec.decode(
        prefs.getString("history_v2", "").orEmpty()
    )

    fun addEntry(text: String) {
        if (text.isBlank()) return
        val safeText = text.take(10_000)
        val history = getHistory().toMutableList()
        history.remove(safeText)
        history.add(0, safeText)
        prefs.edit().putString(
            "history_v2",
            ClipboardHistoryCodec.encode(history.take(maxItems))
        ).apply()
    }

    fun removeEntry(text: String) {
        val next = getHistory().filterNot { it == text }
        prefs.edit().putString("history_v2", ClipboardHistoryCodec.encode(next)).apply()
    }

    fun clearHistory() {
        prefs.edit().putString("history_v2", "").apply()
    }

    fun latestText(): String? = readPrimaryClip()?.text

    fun copyToClipboard(text: String) {
        if (text.isBlank()) return
        runCatching { systemClipboard?.setPrimaryClip(ClipData.newPlainText("zakhrafa", text)) }
        addEntry(text)
    }

    fun pasteFromHistory(text: String): Boolean {
        val pasted = svc.inputHandler.commitExternalText(text)
        if (pasted) {
            addEntry(text)
            if (text == pendingLink) consumePendingLink()
        }
        return pasted
    }

    private fun refreshPrimaryClip(fromChange: Boolean) {
        if (svc.isSensitiveEditor()) return
        val snapshot = readPrimaryClip()
        if (snapshot == null) {
            pendingLink = null
            pendingSignature = null
            if (fromChange) svc.updateSuggestions()
            return
        }
        if (snapshot.signature == observedSignature) return
        observedSignature = snapshot.signature
        // Store real system clipboard changes only. Typed keyboard words never enter history.
        addEntry(snapshot.text)
        val consumed = prefs.getString("consumed_clip_signature", "").orEmpty()
        if (ClipboardLinkDetector.isLink(snapshot.text) && snapshot.signature != consumed) {
            pendingLink = snapshot.text
            pendingSignature = snapshot.signature
        } else {
            pendingLink = null
            pendingSignature = null
        }
        svc.updateSuggestions()
    }

    private fun readPrimaryClip(): ClipSnapshot? {
        if (svc.isSensitiveEditor()) return null
        return runCatching {
            val description = systemClipboard?.primaryClipDescription ?: return@runCatching null
            if (isSensitive(description)) return@runCatching null
            val clip = systemClipboard.primaryClip ?: return@runCatching null
            if (clip.itemCount == 0) return@runCatching null
            val item = clip.getItemAt(0)
            val text = (item.text?.toString() ?: item.uri?.toString()).orEmpty().trim()
            if (text.isBlank() || text.length > 10_000) return@runCatching null
            val timestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                description.timestamp
            } else {
                0L
            }
            ClipSnapshot(text, "${text.hashCode()}:${text.length}:$timestamp")
        }.getOrNull()
    }

    private fun isSensitive(description: ClipDescription): Boolean {
        val extras = description.extras ?: return false
        val legacy = extras.getBoolean("android.content.extra.IS_SENSITIVE", false)
        return legacy || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            extras.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false))
    }

    private data class ClipSnapshot(val text: String, val signature: String)
}
