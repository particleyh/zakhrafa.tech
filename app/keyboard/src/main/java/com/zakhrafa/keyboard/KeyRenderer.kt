package com.zakhrafa.keyboard

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

internal class KeyRenderer(private val svc: ZakhrafaKeyboardService) {

    private data class VariantPopup(
        val window: PopupWindow,
        val choices: List<Pair<TextView, String>>
    )

    private var activeVariantPopup: VariantPopup? = null
    private var activeKeyPreview: PopupWindow? = null

    fun keyView(key: KeyboardLayouts.KeySpec): View {
        return FrameLayout(svc).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            contentDescription = keyContentDescription(key)
            background = keyBackgroundDrawable(key)
            layoutParams = LinearLayout.LayoutParams(0, svc.keySizePx(), key.weight).apply {
                setMargins(svc.dp(2), svc.dp(2), svc.dp(2), svc.dp(2))
            }
            isClickable = true
            isFocusable = true
            elevation = svc.dp(2).toFloat()

            addView(TextView(svc).apply {
                text = key.label
                val baseTextSize = when {
                    key.label == "مسافة" || key.label == "space" -> 13f
                    key.label.length > 2 -> 12f
                    key.label.length == 1 && key.label[0].isDigit() -> 15f
                    else -> 23f
                }
                textSize = baseTextSize * (svc.keyboardPrefs.labelScale / 100f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                includeFontPadding = false
                setTextColor(if (key.special) svc.currentTheme.toolbarTextColor else svc.currentTheme.keyTextColor)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            })

            svc.layouts.longPressHint(key)?.let { hint ->
                addView(TextView(svc).apply {
                    text = hint
                    textSize = 9f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    includeFontPadding = false
                    setTextColor(faintKeyTextColor())
                    translationY = svc.dp(4).toFloat()
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                })
            }

            if (key.output == " " && key.action == null) {
                attachSpacebarGesture(this)
            } else {
                setOnClickListener {
                    svc.tapFeedback(this)
                    key.action?.invoke() ?: svc.commitKey(key.output)
                }
            }

            if (key.longPress.isNotEmpty()) {
                isLongClickable = true
                setOnLongClickListener {
                    dismissKeyPreview()
                    svc.tapFeedback(this)
                    if (key.longPress.size == 1) {
                        // A visible one-key shortcut should type immediately. Requiring
                        // a second tap made the shortcut look broken to users.
                        svc.commitKey(key.longPress.first())
                    } else {
                        activeVariantPopup?.window?.dismiss()
                        activeVariantPopup = showVariantPopup(this, key.longPress)
                    }
                    true
                }
            }

            val shouldPreviewPress = !key.special && key.output != " "
            if (shouldPreviewPress || key.longPress.size > 1) {
                setOnTouchListener { view, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            if (shouldPreviewPress) showKeyPreview(view, key.label)
                            false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val active = activeVariantPopup ?: return@setOnTouchListener false
                            val selected = choiceAt(active, event.rawX, event.rawY)
                            active.choices.forEach { (choice, value) ->
                                choice.alpha = if (value == selected) 0.65f else 1f
                            }
                            false
                        }
                        MotionEvent.ACTION_UP -> {
                            dismissKeyPreview()
                            val active = activeVariantPopup ?: return@setOnTouchListener false
                            val selected = choiceAt(active, event.rawX, event.rawY)
                            active.window.dismiss()
                            activeVariantPopup = null
                            if (selected != null) svc.commitKey(selected)
                            // A long press must never also emit the base key.
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            dismissKeyPreview()
                            activeVariantPopup?.window?.dismiss()
                            activeVariantPopup = null
                            false
                        }
                        else -> false
                    }
                }
            }

            if (key.label == "⌫") {
                svc.attachRepeatingDelete(this)
            }
        }
    }

    /** Tap for a space; drag horizontally to move the cursor, like mainstream IMEs. */
    private fun attachSpacebarGesture(view: View) {
        var startX = 0f
        var lastStep = 0
        var moved = false
        view.setOnClickListener { svc.commitKey(" ") }
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    lastStep = 0
                    moved = false
                    svc.tapFeedback(view)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val step = ((event.x - startX) / svc.dp(18)).toInt()
                    if (step != lastStep) {
                        val distance = step - lastStep
                        svc.moveCursor(distance)
                        lastStep = step
                        moved = true
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    /** Standard IME-style key preview, deliberately non-touchable so it never eats a key-up. */
    private fun showKeyPreview(anchor: View, label: String) {
        if (!anchor.isAttachedToWindow) return
        dismissKeyPreview()
        val bubble = TextView(svc).apply {
            text = label
            textSize = if (label.length > 1) 22f else 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            includeFontPadding = false
            setTextColor(svc.currentTheme.keyTextColor)
            background = circular(svc.currentTheme.keyBackground, svc.currentTheme.primaryColor)
            layoutParams = ViewGroup.LayoutParams(svc.dp(64), svc.dp(64))
        }
        val preview = PopupWindow(
            bubble,
            svc.dp(64),
            svc.dp(64),
            false
        ).apply {
            isTouchable = false
            isOutsideTouchable = false
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = svc.dp(10).toFloat()
        }
        preview.setOnDismissListener {
            if (activeKeyPreview === preview) activeKeyPreview = null
        }
        activeKeyPreview = preview
        try {
            preview.showAsDropDown(
                anchor,
                anchor.width / 2 - svc.dp(32),
                -anchor.height - svc.dp(64) - svc.dp(8),
                Gravity.NO_GRAVITY
            )
        } catch (_: Exception) {
            activeKeyPreview = null
        }
    }

    private fun dismissKeyPreview() {
        val preview = activeKeyPreview
        activeKeyPreview = null
        try { preview?.dismiss() } catch (_: Exception) {}
    }

    fun controlButton(
        label: String,
        widthDp: Int? = null,
        weight: Float = 0f,
        onClick: () -> Unit
    ): TextView {
        return TextView(svc).apply {
            text = label
            textSize = if (label == "⚙" || label == "🖼" || label == "📋" || label == "😊") 19f else 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG
            setSingleLine(true)
            minWidth = 0
            includeFontPadding = false
            isClickable = true
            isFocusable = true
            contentDescription = when (label) {
                "⚙" -> "إعدادات الكيبورد"
                "🖼" -> "تغيير خلفية الكيبورد"
                "📋" -> "الحافظة"
                else -> label
            }
            setTextColor(svc.currentTheme.toolbarTextColor)
            background = StateListDrawable().apply {
                addState(
                    intArrayOf(android.R.attr.state_pressed),
                    rounded(svc.currentTheme.primaryColor, svc.dp(9).toFloat(), svc.currentTheme.primaryColor)
                )
                addState(
                    intArrayOf(),
                    rounded(svc.currentTheme.surface, svc.dp(9).toFloat(), svc.currentTheme.borderColor)
                )
            }
            setPadding(svc.dp(10), 0, svc.dp(10), 0)
            layoutParams = if (widthDp != null) {
                LinearLayout.LayoutParams(svc.dp(widthDp), svc.dp(38))
            } else {
                LinearLayout.LayoutParams(0, svc.dp(38), weight)
            }.apply { setMargins(svc.dp(2), 0, svc.dp(2), 0) }
            setOnClickListener {
                svc.tapFeedback(this)
                onClick()
            }
        }
    }

    fun suggestionChip(
        label: String,
        primary: Boolean = false,
        passive: Boolean = false,
        onClick: () -> Unit = {}
    ): TextView {
        return TextView(svc).apply {
            text = label
            textSize = if (primary) 15f else 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG
            setSingleLine(true)
            isClickable = !passive
            isFocusable = !passive
            setTextColor(if (primary) svc.currentTheme.background else svc.currentTheme.toolbarTextColor)
            background = rounded(
                if (primary) svc.currentTheme.primaryColor else svc.currentTheme.surface,
                svc.dp(16).toFloat(),
                if (primary) svc.currentTheme.primaryColor else svc.currentTheme.borderColor
            )
            setPadding(svc.dp(14), svc.dp(2), svc.dp(14), svc.dp(2))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                svc.dp(38)
            ).apply { setMargins(svc.dp(3), 0, svc.dp(3), 0) }
            if (!passive) {
                setOnClickListener {
                    svc.tapFeedback(this)
                    onClick()
                }
            }
        }
    }

    fun sheetItem(label: String, selected: Boolean = false, onClick: () -> Unit): TextView {
        return TextView(svc).apply {
            text = label
            textSize = 16f
            setSingleLine(true)
            isClickable = true
            isFocusable = true
            setTextColor(if (selected) svc.currentTheme.background else svc.currentTheme.keyTextColor)
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            background = StateListDrawable().apply {
                addState(
                    intArrayOf(android.R.attr.state_pressed),
                    rounded(svc.currentTheme.specialKeyBackground, svc.dp(10).toFloat(), svc.currentTheme.primaryColor)
                )
                addState(
                    intArrayOf(),
                    rounded(
                        if (selected) svc.currentTheme.primaryColor else svc.currentTheme.surface,
                        svc.dp(10).toFloat(),
                        if (selected) svc.currentTheme.primaryColor else svc.currentTheme.borderColor
                    )
                )
            }
            setPadding(svc.dp(14), 0, svc.dp(14), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                svc.dp(48)
            ).apply { setMargins(0, svc.dp(4), 0, svc.dp(4)) }
            setOnClickListener {
                svc.tapFeedback(this)
                onClick()
            }
        }
    }

    private fun showVariantPopup(anchor: View, variants: List<String>): VariantPopup? {
        if (!anchor.isAttachedToWindow) return null
        val isDiacriticPalette = variants.isNotEmpty() &&
            variants.all { it in ARABIC_DIACRITIC_VARIANTS }
        val content = LinearLayout(svc).apply {
            orientation = if (isDiacriticPalette) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER
            setPadding(svc.dp(6), svc.dp(6), svc.dp(6), svc.dp(6))
            background = rounded(svc.currentTheme.surface, svc.dp(18).toFloat(), svc.currentTheme.borderColor)
        }
        val popup = PopupWindow(
            content,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            isTouchable = true
            isOutsideTouchable = true
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = svc.dp(10).toFloat()
            setOnDismissListener {
                if (activeVariantPopup?.window === this) activeVariantPopup = null
            }
        }

        val choices = mutableListOf<Pair<TextView, String>>()
        fun choiceFor(value: String): TextView {
            val choice = TextView(svc).apply {
                // Combining marks are much easier to recognise on a dotted circle.
                text = if (isDiacriticPalette) "\u25CC$value" else value
                textSize = when {
                    isDiacriticPalette -> 28f
                    value.length > 1 -> 18f
                    else -> 23f
                }
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                includeFontPadding = false
                isClickable = true
                setTextColor(svc.currentTheme.keyTextColor)
                background = circular(svc.currentTheme.keyBackground, svc.currentTheme.borderColor)
                val size = if (isDiacriticPalette) svc.dp(60) else svc.dp(48)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(svc.dp(2), svc.dp(2), svc.dp(2), svc.dp(2))
                }
                setOnClickListener {
                    try { popup.dismiss() } catch (_: Exception) {}
                    activeVariantPopup = null
                    svc.commitKey(value)
                }
            }
            choices += choice to value
            return choice
        }
        if (isDiacriticPalette) {
            variants.chunked(3).forEach { group ->
                content.addView(LinearLayout(svc).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    group.forEach { addView(choiceFor(it)) }
                })
            }
        } else {
            variants.forEach { content.addView(choiceFor(it)) }
        }

        return try {
            content.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            // Anchor-relative coordinates are reliable inside an IME window; absolute
            // screen coordinates can place a popup below the keyboard on some OEMs.
            val xOffset = anchor.width / 2 - content.measuredWidth / 2
            val yOffset = -anchor.height - content.measuredHeight - svc.dp(8)
            popup.showAsDropDown(anchor, xOffset, yOffset, Gravity.NO_GRAVITY)
            VariantPopup(popup, choices)
        } catch (_: Exception) {
            null
        }
    }

    private fun choiceAt(popup: VariantPopup, rawX: Float, rawY: Float): String? {
        val location = IntArray(2)
        return popup.choices.firstOrNull { (view, _) ->
            view.getLocationOnScreen(location)
            rawX >= location[0] && rawX < location[0] + view.width &&
                rawY >= location[1] && rawY < location[1] + view.height
        }?.second
    }

    private fun keyContentDescription(key: KeyboardLayouts.KeySpec): String {
        return when (key.label) {
            "🌐" -> "تغيير اللغة"
            "😊" -> "رموز ووجوه"
            "📋" -> "الحافظة"
            "⌫" -> "حذف"
            "⏎" -> "إدخال"
            "➤" -> "إرسال"
            "⌕" -> "بحث"
            "✓" -> "تم"
            "→" -> "التالي"
            "ّ" -> "شدة"
            "َ" -> "فتحة"
            "ُ" -> "ضمة"
            "ِ" -> "كسرة"
            "ْ" -> "سكون"
            else -> key.label
        }
    }

    private fun faintKeyTextColor(): Int {
        return Color.argb(
            145,
            Color.red(svc.currentTheme.keyTextColor),
            Color.green(svc.currentTheme.keyTextColor),
            Color.blue(svc.currentTheme.keyTextColor)
        )
    }

    private fun keyBackgroundDrawable(key: KeyboardLayouts.KeySpec): StateListDrawable {
        val normal = rounded(
            if (key.special) svc.currentTheme.specialKeyBackground else svc.currentTheme.keyBackground,
            svc.dp(12).toFloat(),
            svc.currentTheme.borderColor
        )
        val pressed = rounded(
            if (key.special) svc.currentTheme.primaryColor else svc.currentTheme.surface,
            svc.dp(12).toFloat(),
            svc.currentTheme.primaryColor
        )
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressed)
            addState(intArrayOf(), normal)
        }
    }

    internal fun rounded(color: Int, radius: Float, stroke: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            setStroke(svc.dp(1), stroke)
        }
    }

    private fun circular(color: Int, stroke: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(svc.dp(1), stroke)
        }
    }
}
