package com.zakhrafa.keyboard

import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.inputmethodservice.InputMethodService
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.zakhrafa.engine.models.StyleMap
import com.zakhrafa.keyboard.core.KeyboardSettingsRepository
import com.zakhrafa.keyboard.core.DecorationCatalog
import com.zakhrafa.keyboard.core.KeyboardUiState
import com.zakhrafa.keyboard.core.KeyboardMode
import com.zakhrafa.keyboard.theme.KeyboardTheme
import com.zakhrafa.keyboard.theme.Themes

class ZakhrafaKeyboardService : InputMethodService() {

    enum class LayoutMode { ARABIC, ENGLISH, SYMBOLS, EMOJI }

    data class KeyboardPrefs(
        val keyHeightDp: Int = 46,
        val haptic: Boolean = true,
        val sound: Boolean = false,
        val numberRow: Boolean = true,
        val suggestionCount: Int = 18,
        val labelScale: Int = 100,
        val wideSpacebar: Boolean = true,
        val backgroundUri: String = "",
        val defaultLanguage: String = "ar"
    )

    var mode = LayoutMode.ARABIC
        internal set
    var lastTextMode = LayoutMode.ARABIC
        internal set
    var englishShifted = false
        internal set
    var currentWord = ""
        internal set
    var currentCommittedLength = 0
        internal set
    val currentOutputLengths = mutableListOf<Int>()
    var activeStyle: StyleMap? = null
        internal set
    var wordStartedWithStyle = false
        internal set
    var currentTheme: KeyboardTheme = Themes.pearl
        internal set
    var keyboardPrefs = KeyboardPrefs()
        internal set
    var uiState: KeyboardUiState = KeyboardUiState()
        private set

    internal lateinit var rootLayout: LinearLayout
    internal lateinit var suggestionStrip: LinearLayout
    internal lateinit var keyboardContainer: FrameLayout
    private lateinit var toolbarStrip: LinearLayout
    internal lateinit var idleControls: LinearLayout
    internal lateinit var suggestionsScroll: HorizontalScrollView

    internal lateinit var layouts: KeyboardLayouts
    internal lateinit var renderer: KeyRenderer
    internal lateinit var inputHandler: InputHandler
    internal lateinit var suggestions: SuggestionManager
    internal lateinit var stylePicker: StylePicker
    internal lateinit var clipboardManager: ClipboardManager
    internal lateinit var emojiPicker: EmojiPicker
    private lateinit var settingsRepository: KeyboardSettingsRepository
    private lateinit var decorationCatalog: DecorationCatalog

    private var lastBuiltThemeName = ""
    private var lastBuiltPrefsHash = 0
    private var lastBuiltMode: LayoutMode? = null

    private val repeatHandler = Handler(Looper.getMainLooper())
    private var deleteRepeater: Runnable? = null
    private var deleteStartX = 0f
    private var isSlideDeleting = false
    private var deleteRepeated = false
    private var editorForcedSymbols = false
    private val extractedTextRequest = ExtractedTextRequest().apply {
        token = 53
        hintMaxChars = 2048
        hintMaxLines = 10
    }

    override fun onCreateInputView(): View {
        try {
            AdManager.initialize(this)
        } catch (_: Exception) {}
        loadSettings(applyDefaultLanguage = true)
        settingsRepository = KeyboardSettingsRepository(this)
        uiState = uiState.copy(mode = mode.toCoreMode(), lastTextMode = lastTextMode.toCoreMode())
        initHelpers()
        restoreActiveStyleForMode()
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // The keyboard chooses direction per row. Never inherit a device locale
            // here, otherwise QWERTY and utility buttons reverse on RTL phones.
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        rebuildKeyboard()
        return rootLayout
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        loadSettings(applyDefaultLanguage = false)
        adaptModeToEditor(attribute)
        restoreActiveStyleForMode()
        if (::inputHandler.isInitialized) inputHandler.resetCurrentWord()
        maybeRebuild()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        loadSettings(applyDefaultLanguage = false)
        adaptModeToEditor(info)
        restoreActiveStyleForMode()
        if (::inputHandler.isInitialized) inputHandler.resetCurrentWord()
        clearSuggestions()
        if (::clipboardManager.isInitialized) {
            if (supportsSuggestions()) clipboardManager.start() else clipboardManager.stop()
        }
        // A style/clipboard sheet replaces keyboardContainer. Rebuild every time a
        // new editor becomes visible so an old utility panel cannot leak into it.
        if (::rootLayout.isInitialized && ::renderer.isInitialized) rebuildKeyboard()
        beginEditorMonitoring()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::rootLayout.isInitialized && ::renderer.isInitialized) rebuildKeyboard()
    }

    override fun onFinishInput() {
        if (::inputHandler.isInitialized) inputHandler.resetCurrentWord()
        clearSuggestions()
        super.onFinishInput()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (::inputHandler.isInitialized) inputHandler.onEditorSelectionChanged(newSelStart, newSelEnd)
    }

    override fun onUpdateExtractedText(token: Int, text: ExtractedText?) {
        super.onUpdateExtractedText(token, text)
        if (::inputHandler.isInitialized) inputHandler.onEditorTextChanged(text?.text)
    }

    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo) {
        super.onUpdateCursorAnchorInfo(cursorAnchorInfo)
        if (::inputHandler.isInitialized) {
            inputHandler.onEditorSelectionChanged(
                cursorAnchorInfo.selectionStart,
                cursorAnchorInfo.selectionEnd
            )
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        loadSettings(applyDefaultLanguage = false)
        if (::clipboardManager.isInitialized && supportsSuggestions()) {
            clipboardManager.start()
        }
        maybeRebuild()
        updateSuggestions()
        val prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE)
        val pickerRequestedAt = prefs.getLong("open_style_picker_requested_at", 0L)
        // Remove both the v52 boolean and the v53 timestamp synchronously. A delayed
        // SharedPreferences apply could otherwise reopen the picker in another app.
        prefs.edit()
            .remove("open_style_picker_once")
            .remove("open_style_picker_requested_at")
            .commit()
        val requestIsFresh = pickerRequestedAt > 0L &&
            System.currentTimeMillis() - pickerRequestedAt in 0L..120_000L
        val requestCameFromOurSetupField = currentInputEditorInfo?.packageName == packageName
        if (requestIsFresh && requestCameFromOurSetupField) {
            rootLayout.post {
                if (currentInputEditorInfo?.packageName == packageName &&
                    ::keyboardContainer.isInitialized &&
                    keyboardContainer.isAttachedToWindow
                ) {
                    stylePicker.showStylePicker()
                }
            }
        }
    }

    override fun onWindowHidden() {
        if (::clipboardManager.isInitialized) clipboardManager.stop()
        if (::inputHandler.isInitialized) inputHandler.resetCurrentWord()
        clearSuggestions()
        // Utility sheets are transient. Never preserve one across a keyboard hide /
        // show cycle, even when an OEM reuses the same input view instance.
        if (::rootLayout.isInitialized && ::renderer.isInitialized) rebuildKeyboard()
        super.onWindowHidden()
    }

    private fun beginEditorMonitoring() {
        val connection = currentInputConnection ?: return
        runCatching {
            val extracted = connection.getExtractedText(
                extractedTextRequest,
                InputConnection.GET_EXTRACTED_TEXT_MONITOR
            )
            inputHandler.onEditorTextChanged(extracted?.text)
        }
        runCatching {
            connection.requestCursorUpdates(
                InputConnection.CURSOR_UPDATE_IMMEDIATE or InputConnection.CURSOR_UPDATE_MONITOR
            )
        }
    }

    private fun initHelpers() {
        layouts = KeyboardLayouts(this)
        renderer = KeyRenderer(this)
        inputHandler = InputHandler(this)
        suggestions = SuggestionManager(this, renderer)
        stylePicker = StylePicker(this, renderer)
        clipboardManager = ClipboardManager(this)
        emojiPicker = EmojiPicker(this, renderer)
        decorationCatalog = DecorationCatalog(this)
    }

    private fun needsRebuild(): Boolean {
        if (!::rootLayout.isInitialized) return false
        if (!::renderer.isInitialized) return false
        val prefsHash = keyboardPrefs.hashCode()
        if (lastBuiltThemeName == currentTheme.name && lastBuiltPrefsHash == prefsHash && lastBuiltMode == mode) return false
        return true
    }

    private fun maybeRebuild() {
        if (needsRebuild()) rebuildKeyboard()
    }

    internal fun rebuildKeyboard() {
        rootLayout.removeAllViews()
        rootLayout.setBackgroundColor(currentTheme.background)

        toolbarStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), dp(4), dp(5), dp(4))
            setBackgroundColor(currentTheme.toolbarBackground)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        }

        idleControls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        if (isSensitiveEditor()) {
            idleControls.addView(renderer.controlButton("🔒  وضع خاص", widthDp = 128) {})
        } else {
            idleControls.addView(renderer.controlButton("📋", widthDp = 40) { openClipboard() })
            if (activeStyle != null) {
                idleControls.addView(renderer.controlButton(activeStyleDisplayName(), widthDp = 150) { stylePicker.showStylePicker() })
                idleControls.addView(renderer.controlButton("إلغاء", widthDp = 66) {
                    setActiveDecorationStyle(null)
                    rebuildKeyboard()
                })
            } else {
                idleControls.addView(renderer.controlButton("✦  اختر زخرفة", widthDp = 150) { stylePicker.showStylePicker() })
            }
        }

        suggestionStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        suggestionsScroll = HorizontalScrollView(this@ZakhrafaKeyboardService).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(suggestionStrip)
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
            visibility = View.GONE
        }

        // Keep decoration controls and prediction chips in one compact rail.
        // Suggestions share the row rather than pushing controls into a second strip.
        toolbarStrip.addView(idleControls)
        toolbarStrip.addView(suggestionsScroll)
        rootLayout.addView(toolbarStrip)

        keyboardContainer = FrameLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        rootLayout.addView(keyboardContainer)

        renderKeys()
        updateSuggestions()
        lastBuiltThemeName = currentTheme.name
        lastBuiltPrefsHash = keyboardPrefs.hashCode()
        lastBuiltMode = mode
    }

    private fun renderKeys() {
        keyboardContainer.removeAllViews()

        if (mode == LayoutMode.EMOJI) {
            emojiPicker.showEmojiPicker()
            return
        }

        val frame = CroppedBackgroundFrame(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setBackgroundColor(currentTheme.background)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        if (keyboardPrefs.backgroundUri.isNotBlank()) {
            try {
                val image = ImageView(this).apply {
                    setImageURI(Uri.parse(keyboardPrefs.backgroundUri))
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    alpha = 0.34f
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                if (image.drawable != null) {
                    frame.backgroundView = image
                    frame.addView(image)
                }
            } catch (_: Exception) {
                getSharedPreferences("keyboard_settings", MODE_PRIVATE).edit().remove("background_uri").apply()
                keyboardPrefs = keyboardPrefs.copy(backgroundUri = "")
            }
        }
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(dp(2), dp(3), dp(2), dp(5))
        }

        layouts.keyRows(mode).forEach { row ->
            shell.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                // Key arrays already describe their physical left-to-right screen
                // positions. RTL here would mirror the entire Arabic keyboard.
                layoutDirection = PHYSICAL_KEY_ROW_DIRECTION
                row.forEach { key -> addView(renderer.keyView(key)) }
            })
        }

        frame.addView(shell)
        keyboardContainer.addView(frame)
    }

    internal fun updateSuggestions() {
        if (!::suggestionStrip.isInitialized) return
        suggestions.updateSuggestions(suggestionStrip)
    }

    internal fun commitKey(raw: String) {
        inputHandler.commitKey(raw)
        if (mode == LayoutMode.ENGLISH && englishShifted && raw.length == 1 && raw[0].isLetter()) {
            englishShifted = false
            rebuildKeyboard()
        }
    }

    internal fun deleteOne() = inputHandler.deleteOne()

    internal fun enter() = inputHandler.enter()

    internal fun enterKeyLabel(): String {
        if (shouldInsertNewline()) return "⏎"
        return when (currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)) {
            EditorInfo.IME_ACTION_SEND -> "➤"
            EditorInfo.IME_ACTION_SEARCH -> "⌕"
            EditorInfo.IME_ACTION_DONE -> "✓"
            EditorInfo.IME_ACTION_NEXT, EditorInfo.IME_ACTION_GO -> "→"
            else -> "⏎"
        }
    }

    internal fun shouldInsertNewline(): Boolean {
        val info = currentInputEditorInfo ?: return false
        return shouldInsertNewlineForEditor(info.inputType, info.imeOptions)
    }

    internal fun moveCursor(steps: Int) {
        if (steps == 0) return
        val keyCode = if (steps < 0) android.view.KeyEvent.KEYCODE_DPAD_LEFT else android.view.KeyEvent.KEYCODE_DPAD_RIGHT
        repeat(kotlin.math.abs(steps).coerceAtMost(8)) {
            currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode))
            currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode))
        }
    }

    internal fun resetCurrentWord() = inputHandler.resetCurrentWord()

    internal fun scheduleSuggestions() = suggestions.scheduleSuggestions()

    internal fun clearSuggestions() {
        if (!::suggestions.isInitialized) return
        suggestions.cancelAndClear()
        if (!::suggestionStrip.isInitialized || !::idleControls.isInitialized || !::suggestionsScroll.isInitialized) return
        suggestionStrip.removeAllViews()
        if (::clipboardManager.isInitialized && supportsSuggestions() &&
            clipboardManager.pendingLinkSuggestion() != null
        ) {
            suggestions.updateSuggestions(suggestionStrip)
            return
        }
        idleControls.visibility = View.VISIBLE
        suggestionsScroll.visibility = View.GONE
    }

    internal fun toggleLanguage() {
        switchMode(when (lastTextMode) {
            LayoutMode.ARABIC -> LayoutMode.ENGLISH
            else -> LayoutMode.ARABIC
        })
    }

    internal fun toggleEnglishShift() {
        if (mode != LayoutMode.ENGLISH) return
        englishShifted = !englishShifted
        rebuildKeyboard()
    }

    internal fun switchMode(nextMode: LayoutMode) {
        mode = nextMode
        if (nextMode == LayoutMode.ARABIC || nextMode == LayoutMode.ENGLISH) lastTextMode = nextMode
        if (nextMode != LayoutMode.ENGLISH) englishShifted = false
        restoreActiveStyleForMode()
        uiState = uiState.copy(
            mode = nextMode.toCoreMode(),
            lastTextMode = lastTextMode.toCoreMode(),
            utilityPanel = com.zakhrafa.keyboard.core.UtilityPanel.None
        )
        rebuildKeyboard()
    }

    internal fun setActiveDecorationStyle(style: StyleMap?) {
        activeStyle = style
        val prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE)
        val key = activeStylePreferenceKey()
        if (style == null) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, "${style.category}|${style.name}").apply()
        }
        uiState = uiState.copy(activeStyle = style)
    }

    private fun activeStylePreferenceKey(): String =
        if (effectiveTextMode() == LayoutMode.ENGLISH) "active_style_en" else "active_style_ar"

    private fun effectiveTextMode(): LayoutMode = when (mode) {
        LayoutMode.ENGLISH -> LayoutMode.ENGLISH
        LayoutMode.ARABIC -> LayoutMode.ARABIC
        else -> lastTextMode
    }

    private fun restoreActiveStyleForMode() {
        if (!::decorationCatalog.isInitialized) return
        val stored = getSharedPreferences("keyboard_settings", MODE_PRIVATE)
            .getString(activeStylePreferenceKey(), null)
        if (stored.isNullOrBlank()) {
            activeStyle = null
            return
        }
        val coreMode = if (effectiveTextMode() == LayoutMode.ENGLISH) {
            KeyboardMode.ENGLISH
        } else {
            KeyboardMode.ARABIC
        }
        activeStyle = decorationCatalog.stylesFor(coreMode)
            .firstOrNull { entry ->
                "${entry.style.category}|${entry.style.name}" == stored &&
                    (!entry.isPremium || AdManager.isPremiumStyleUnlocked(this, entry.style.name))
            }
            ?.style
    }

    private fun activeStyleDisplayName(): String {
        val name = activeStyle?.name.orEmpty()
        return when {
            name.startsWith("Ar Style ") -> "زخرفة عربية ${name.removePrefix("Ar Style ")}"
            name.startsWith("En Style ") -> "زخرفة إنجليزية ${name.removePrefix("En Style ")}"
            else -> name
        }
    }

    private fun adaptModeToEditor(info: EditorInfo?) {
        val inputClass = info?.inputType?.and(InputType.TYPE_MASK_CLASS) ?: return
        val needsSymbols = inputClass == InputType.TYPE_CLASS_NUMBER ||
            inputClass == InputType.TYPE_CLASS_PHONE ||
            inputClass == InputType.TYPE_CLASS_DATETIME
        if (needsSymbols && !editorForcedSymbols) {
            if (mode == LayoutMode.ARABIC || mode == LayoutMode.ENGLISH) lastTextMode = mode
            mode = LayoutMode.SYMBOLS
            englishShifted = false
            editorForcedSymbols = true
        } else if (!needsSymbols && editorForcedSymbols) {
            mode = lastTextMode
            editorForcedSymbols = false
        }
    }

    internal fun isSensitiveEditor(): Boolean {
        val inputType = currentInputEditorInfo?.inputType ?: return false
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return (inputClass == InputType.TYPE_CLASS_TEXT && variation in setOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        )) || (inputClass == InputType.TYPE_CLASS_NUMBER &&
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
    }

    internal fun supportsSuggestions(): Boolean {
        val inputType = currentInputEditorInfo?.inputType ?: return true
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        return (inputClass == InputType.TYPE_CLASS_TEXT || inputClass == InputType.TYPE_NULL) &&
            !isSensitiveEditor()
    }

    private fun LayoutMode.toCoreMode(): KeyboardMode = when (this) {
        LayoutMode.ARABIC -> KeyboardMode.ARABIC
        LayoutMode.ENGLISH -> KeyboardMode.ENGLISH
        LayoutMode.SYMBOLS -> KeyboardMode.SYMBOLS
        LayoutMode.EMOJI -> KeyboardMode.EMOJI
    }

    internal fun tapFeedback(view: View) {
        if (keyboardPrefs.haptic) runCatching { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
        if (keyboardPrefs.sound) {
            (getSystemService(AUDIO_SERVICE) as? AudioManager)?.let { audio ->
                runCatching { audio.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f) }
            }
        }
    }

    internal fun attachRepeatingDelete(view: View) {
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    deleteStartX = event.rawX
                    isSlideDeleting = false
                    deleteRepeated = false
                    startDeleteRepeat(360)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isSlideDeleting && deleteStartX - event.rawX > dp(30)) {
                        isSlideDeleting = true
                        deleteRepeater?.let { repeatHandler.removeCallbacks(it) }
                        startDeleteRepeat(50)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val shouldClick = !deleteRepeated
                    stopDeleteRepeat()
                    if (shouldClick) view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    stopDeleteRepeat()
                    true
                }
                else -> false
            }
        }
    }

    private fun startDeleteRepeat(initialDelay: Long) {
        deleteRepeater = object : Runnable {
            override fun run() {
                deleteRepeated = true
                deleteOne()
                repeatHandler.postDelayed(this, 50)
            }
        }
        deleteRepeater?.let { repeatHandler.postDelayed(it, initialDelay) }
    }

    private fun stopDeleteRepeat() {
        deleteRepeater?.let { repeatHandler.removeCallbacks(it) }
        deleteRepeater = null
        isSlideDeleting = false
    }

    internal fun isWordCharacter(c: Char): Boolean = inputHandler.isWordCharacter(c)

    internal fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    fun keySizePx(): Int {
        val screenHeight = resources.displayMetrics.heightPixels
        val toolbar = dp(56)
        val rows = when (mode) {
            LayoutMode.ARABIC, LayoutMode.ENGLISH -> if (keyboardPrefs.numberRow) 5 else 4
            LayoutMode.SYMBOLS -> 5
            LayoutMode.EMOJI -> 4
        }
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val screenFraction = if (isLandscape) 0.36 else 0.43
        val adaptive = ((screenHeight * screenFraction).toInt() - toolbar) / rows
        val configured = dp(keyboardPrefs.keyHeightDp)
        val minimum = if (isLandscape) dp(32) else dp(40)
        return configured.coerceIn(minimum, adaptive.coerceAtLeast(minimum))
    }

    internal fun openClipboard() {
        if (isSensitiveEditor()) {
            android.widget.Toast.makeText(this, "الحافظة مخفية في حقول كلمات المرور", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        keyboardContainer.removeAllViews()
        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(currentTheme.background)
            setPadding(dp(10), dp(8), dp(10), dp(10))
        }

        sheet.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(renderer.controlButton("📋  الحافظة", weight = 1f) {})
            addView(renderer.controlButton("إغلاق", widthDp = 72) { rebuildKeyboard() })
        })
        sheet.addView(TextView(this).apply {
            text = "تُحفظ النسخ محلياً على جهازك فقط"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(currentTheme.toolbarTextColor)
            setPadding(0, 0, 0, dp(4))
        })
        sheet.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(renderer.controlButton("تحديد الكل", weight = 1f) {
                currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
            })
            addView(renderer.controlButton("نسخ", weight = 1f) {
                currentInputConnection?.performContextMenuAction(android.R.id.copy)
            })
            addView(renderer.controlButton("قص", weight = 1f) {
                currentInputConnection?.performContextMenuAction(android.R.id.cut)
            })
            addView(renderer.controlButton("لصق", weight = 1f) {
                clipboardManager.latestText()?.let { clipboardManager.pasteFromHistory(it) }
                rebuildKeyboard()
            })
        })

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val latestClipboard = clipboardManager.latestText()
        val history = clipboardManager.getHistory().filterNot { it == latestClipboard }

        latestClipboard?.let { latest ->
            list.addView(renderer.sheetItem("📌  آخر نسخة: ${clipboardManager.preview(latest, 34)}") {
                val text = clipboardManager.latestText()
                if (text != null) {
                clipboardManager.pasteFromHistory(text)
                rebuildKeyboard()
            }
            })
        }

        if (history.isNotEmpty()) {
            list.addView(renderer.sheetItem("🗑  مسح السجل") {
                clipboardManager.clearHistory()
                openClipboard()
            })
            history.take(30).forEach { entry ->
                list.addView(LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutDirection = View.LAYOUT_DIRECTION_LTR
                    val pasteItem = renderer.sheetItem(clipboardManager.preview(entry)) {}.apply {
                        layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                            setMargins(0, dp(2), dp(3), dp(2))
                        }
                    }
                    addView(pasteItem.apply {
                        setOnClickListener {
                            tapFeedback(this)
                            clipboardManager.pasteFromHistory(entry)
                            rebuildKeyboard()
                        }
                    })
                    addView(renderer.controlButton("×", widthDp = 44) {
                        clipboardManager.removeEntry(entry)
                        openClipboard()
                    })
                })
            }
        } else if (latestClipboard == null) {
            list.addView(TextView(this).apply {
                text = "لا يوجد نسخ محفوظة"
                textSize = 14f
                setTextColor(currentTheme.toolbarTextColor)
                gravity = Gravity.CENTER
                setPadding(0, dp(16), 0, dp(16))
            })
        }

        val scrollView = android.widget.ScrollView(this).apply {
            addView(list)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                minOf(dp(310), (resources.displayMetrics.heightPixels * 0.42f).toInt())
            )
        }
        sheet.addView(scrollView)
        keyboardContainer.addView(sheet)
    }

    private fun loadSettings(applyDefaultLanguage: Boolean) {
        val settings = if (::settingsRepository.isInitialized) settingsRepository.read()
        else KeyboardSettingsRepository(this).read()
        val requestedTheme = settings.theme()
        // Older builds exposed Royal without a reward gate. Do not silently keep a
        // rewarded theme selected after the five-theme catalogue migration.
        currentTheme = if (requestedTheme.isRewarded && !AdManager.isPremiumThemeUnlocked(this, requestedTheme.name)) {
            Themes.pearl
        } else {
            requestedTheme
        }
        keyboardPrefs = KeyboardPrefs(
            keyHeightDp = settings.keyHeightDp,
            haptic = settings.haptic,
            sound = settings.sound,
            numberRow = settings.numberRow,
            suggestionCount = settings.suggestionCount,
            labelScale = settings.labelScale,
            wideSpacebar = settings.wideSpacebar,
            backgroundUri = settings.backgroundUri,
            defaultLanguage = settings.defaultLanguage
        )
        if (applyDefaultLanguage) {
            mode = if (keyboardPrefs.defaultLanguage == "en") LayoutMode.ENGLISH else LayoutMode.ARABIC
            lastTextMode = mode
        }
    }

    override fun onDestroy() {
        stopDeleteRepeat()
        repeatHandler.removeCallbacksAndMessages(null)
        if (::suggestions.isInitialized) suggestions.close()
        if (::clipboardManager.isInitialized) clipboardManager.stop()
        super.onDestroy()
    }
}
