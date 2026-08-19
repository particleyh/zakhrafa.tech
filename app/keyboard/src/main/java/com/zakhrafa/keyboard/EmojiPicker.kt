package com.zakhrafa.keyboard

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

internal class EmojiPicker(
    private val svc: ZakhrafaKeyboardService,
    private val renderer: KeyRenderer
) {
    private var currentCategoryIndex = 0
    private var gridContainer: GridLayout? = null
    private var categoryButtons = mutableListOf<TextView>()
    private var searchInput: EditText? = null
    private var isUpdatingSearch = false
    private var emojiCellSizePx = 0

    fun showEmojiPicker() {
        svc.keyboardContainer.removeAllViews()
        categoryButtons.clear()
        emojiCellSizePx = ((svc.resources.displayMetrics.widthPixels - svc.dp(8)) / 8)
            .coerceIn(svc.dp(32), svc.dp(48))

        val root = LinearLayout(svc).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setBackgroundColor(svc.currentTheme.background)
            setPadding(svc.dp(4), svc.dp(4), svc.dp(4), svc.dp(0))
        }

        val header = LinearLayout(svc).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL
            setPadding(svc.dp(4), 0, svc.dp(4), svc.dp(4))
        }

        searchInput = EditText(svc).apply {
            hint = "🔍  بحث عن إيموجي..."
            textSize = 13f
            setTextColor(svc.currentTheme.keyTextColor)
            setHintTextColor(svc.currentTheme.toolbarTextColor)
            setPadding(svc.dp(10), svc.dp(6), svc.dp(10), svc.dp(6))
            background = GradientDrawable().apply {
                setColor(svc.currentTheme.surface)
                cornerRadius = svc.dp(16).toFloat()
                setStroke(svc.dp(1), svc.currentTheme.borderColor)
            }
            layoutParams = LinearLayout.LayoutParams(0, svc.dp(36), 1f)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (!isUpdatingSearch) filterEmojis(s?.toString().orEmpty())
                }
            })
        }
        header.addView(searchInput)

        header.addView(renderer.controlButton("ABC", widthDp = 52) {
            svc.switchMode(svc.lastTextMode)
        })
        root.addView(header)

        val categoryBar = LinearLayout(svc).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER
            setPadding(0, svc.dp(2), 0, svc.dp(2))
        }

        val categories = EmojiData.categories
        categories.forEachIndexed { index, cat ->
            val btn = TextView(svc).apply {
                text = cat.icon
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(svc.dp(8), svc.dp(4), svc.dp(8), svc.dp(4))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    currentCategoryIndex = index
                    updateCategorySelection()
                    showCategoryEmojis(index)
                }
            }
            categoryButtons.add(btn)
            categoryBar.addView(btn)
        }

        val catScroll = HorizontalScrollView(svc).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(categoryBar)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(catScroll)

        val recentBtn = TextView(svc).apply {
            text = "⏱️"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(svc.dp(12), svc.dp(6), svc.dp(12), svc.dp(6))
            setOnClickListener {
                currentCategoryIndex = -1
                updateCategorySelection()
                showRecentEmojis()
            }
        }
        categoryBar.addView(recentBtn, 0)

        gridContainer = GridLayout(svc).apply {
            columnCount = 8
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(svc.dp(2), svc.dp(2), svc.dp(2), svc.dp(2))
        }

        val gridScroll = android.widget.ScrollView(svc).apply {
            addView(gridContainer)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                svc.dp(184)
            )
            isVerticalScrollBarEnabled = false
        }
        root.addView(gridScroll)

        val bottomRow = LinearLayout(svc).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL
            setPadding(svc.dp(4), svc.dp(4), svc.dp(4), svc.dp(4))
            setBackgroundColor(svc.currentTheme.toolbarBackground)
        }

        bottomRow.addView(renderer.controlButton("⌫", widthDp = 48) { svc.deleteOne() })

        val spaceBtn = renderer.controlButton("مسافة", weight = 3f) {
            svc.commitKey(" ")
        }
        bottomRow.addView(spaceBtn)

        bottomRow.addView(renderer.controlButton(svc.enterKeyLabel(), widthDp = 48) { svc.enter() })
        root.addView(bottomRow)

        svc.keyboardContainer.addView(root)
        updateCategorySelection()
        showCategoryEmojis(0)
    }

    private fun updateCategorySelection() {
        categoryButtons.forEachIndexed { index, btn ->
            val isSelected = index == currentCategoryIndex
            btn.background = if (isSelected) {
                GradientDrawable().apply {
                    setColor(svc.currentTheme.primaryColor)
                    cornerRadius = svc.dp(8).toFloat()
                }
            } else {
                null
            }
            btn.setTextColor(if (isSelected) Color.WHITE else svc.currentTheme.toolbarTextColor)
        }
    }

    private fun showCategoryEmojis(index: Int) {
        clearSearchWithoutFiltering()
        EmojiData.categories.getOrNull(index)?.let { addEmojiGrid(it.emojis) }
    }

    private fun showRecentEmojis() {
        clearSearchWithoutFiltering()
        val recent = EmojiData.getRecent(svc)
        if (recent.isEmpty()) {
            val empty = TextView(svc).apply {
                text = "لا يوجد إيموجي مستخدم مؤخراً"
                textSize = 14f
                setTextColor(svc.currentTheme.toolbarTextColor)
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(0, 8)
                    setPadding(0, svc.dp(30), 0, 0)
                }
            }
            gridContainer?.addView(empty)
        } else {
            addEmojiGrid(recent)
        }
    }

    private fun filterEmojis(query: String) {
        if (query.isBlank()) {
            if (currentCategoryIndex == -1) renderRecentEmojis()
            else EmojiData.categories.getOrNull(currentCategoryIndex)?.let { addEmojiGrid(it.emojis) }
            return
        }
        val all = EmojiData.categories.flatMap { it.emojis }
        val filtered = all.filter { emoji ->
            emoji.contains(query) || EmojiNames.nameFor(emoji).contains(query, ignoreCase = true)
        }
        addEmojiGrid(filtered)
    }

    private fun clearSearchWithoutFiltering() {
        val input = searchInput ?: return
        if (input.text.isEmpty()) return
        isUpdatingSearch = true
        input.setText("")
        isUpdatingSearch = false
    }

    private fun renderRecentEmojis() {
        gridContainer?.removeAllViews()
        val recent = EmojiData.getRecent(svc)
        if (recent.isEmpty()) {
            val empty = TextView(svc).apply {
                text = "لا يوجد إيموجي مستخدم مؤخراً"
                textSize = 14f
                setTextColor(svc.currentTheme.toolbarTextColor)
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(0, 8)
                    setPadding(0, svc.dp(30), 0, 0)
                }
            }
            gridContainer?.addView(empty)
        } else {
            addEmojiGrid(recent)
        }
    }

    private fun addEmojiGrid(emojis: List<String>) {
        gridContainer?.removeAllViews()
        val size = emojiCellSizePx.takeIf { it > 0 } ?: svc.dp(42)
        emojis.forEach { emoji ->
            val btn = TextView(svc).apply {
                text = emoji
                textSize = 22f
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(0, 0, 0, 0)
                }
                setOnClickListener {
                    EmojiData.addRecent(svc, emoji)
                    svc.commitKey(emoji)
                }
                setOnLongClickListener {
                    val variants = EmojiData.skinToneVariants(emoji)
                    if (variants.size > 1 && !emoji.contains("\uD83C\uDFFB")) {
                        showSkinTonePopup(this, emoji)
                    }
                    true
                }
            }
            gridContainer?.addView(btn)
        }
    }

    private fun showSkinTonePopup(anchor: View, baseEmoji: String) {
        val row = LinearLayout(svc).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(svc.dp(6), svc.dp(6), svc.dp(6), svc.dp(6))
            background = GradientDrawable().apply {
                setColor(svc.currentTheme.surface)
                cornerRadius = svc.dp(12).toFloat()
                setStroke(svc.dp(1), svc.currentTheme.borderColor)
            }
        }

        val popup = android.widget.PopupWindow(
            row,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            elevation = svc.dp(10).toFloat()
        }

        listOf(baseEmoji) + EmojiData.skinToneVariants(baseEmoji).filter { it != baseEmoji }.distinct().take(5).let { list ->
            (listOf(baseEmoji) + list).distinct().take(6)
        }.forEach { variant ->
            row.addView(TextView(svc).apply {
                text = variant
                textSize = 24f
                gravity = Gravity.CENTER
                setPadding(svc.dp(8), svc.dp(4), svc.dp(8), svc.dp(4))
                setOnClickListener {
                    try { popup.dismiss() } catch (_: Exception) {}
                    EmojiData.addRecent(svc, variant)
                    svc.commitKey(variant)
                }
            })
        }

        try {
            if (anchor.isAttachedToWindow) {
                popup.showAsDropDown(anchor, 0, -anchor.height - svc.dp(50), Gravity.CENTER)
            }
        } catch (_: Exception) {}
    }
}

object EmojiNames {
    private val names = mapOf(
        "😀" to "grinning face", "😃" to "grinning face smiling eyes",
        "😄" to "grinning face sweat", "😁" to "beaming face",
        "😆" to "grinning squinting", "😅" to "grinning sweat",
        "🤣" to "rolling on floor", "😂" to "face tears of joy",
        "🙂" to "slightly smiling", "😊" to "smiling face heart eyes",
        "😇" to "smiling halo", "🥰" to "smiling hearts",
        "😍" to "heart eyes", "🤩" to "star struck",
        "😘" to "blowing kiss", "😗" to "kissing",
        "😚" to "kissing closed eyes", "😙" to "kissing smiling",
        "😋" to "face savoring", "😛" to "face with tongue",
        "😜" to "winking tongue", "🤪" to "zany face",
        "😝" to "squinting tongue", "🤑" to "money mouth",
        "🤗" to "hugging face", "🤭" to "face hand over mouth",
        "🤫" to "shushing face", "🤔" to "thinking face",
        "😐" to "neutral face", "😑" to "expressionless",
        "😶" to "face without mouth", "😏" to "smirking",
        "😒" to "unamused", "🙄" to "eye roll",
        "😬" to "grimacing", "🤥" to "lying face",
        "😌" to "relieved", "😔" to "pensive",
        "😪" to "sleepy", "🤤" to "drooling",
        "😴" to "sleeping", "😷" to "face with mask",
        "🤒" to "face thermometer", "🤕" to "face head bandage",
        "🤢" to "nauseated", "🤮" to "face vomiting",
        "🥵" to "hot face", "🥶" to "cold face",
        "🥴" to "woozy face", "😵" to "face with crossed eyes",
        "🤯" to "exploding head", "🤠" to "cowboy hat",
        "🥳" to "partying face", "😎" to "smiling sunglasses",
        "🤓" to "nerd face", "🧐" to "monocle face",
        "😕" to "confused", "😟" to "worried",
        "🙁" to "slightly frowning", "😮" to "face open mouth",
        "😯" to "hushed", "😲" to "astonished",
        "😳" to "flushed", "🥺" to "pleading face",
        "😦" to "frowning open mouth", "😧" to "anguished",
        "😨" to "fearful", "😰" to "anxious sweat",
        "😥" to "sad but relieved", "😢" to "crying face",
        "😭" to "loudly crying", "😱" to "face screaming",
        "😖" to "confounded", "😣" to "persevering",
        "😞" to "disappointed", "😓" to "downcast sweat",
        "😩" to "weary", "😫" to "tired face",
        "🥱" to "yawning face", "😤" to "face steam",
        "😡" to "pouting face", "😠" to "angry face",
        "🤬" to "face symbols", "😈" to "smiling devil",
        "👿" to "angry devil", "💀" to "skull",
        "💩" to "pile of poo", "🤡" to "clown face",
        "👹" to "ogre", "👻" to "ghost",
        "👽" to "alien", "👾" to "alien monster",
        "🤖" to "robot", "👍" to "thumbs up",
        "👎" to "thumbs down", "❤️" to "red heart",
        "🔥" to "fire", "✨" to "sparkles",
        "⭐" to "star", "🎉" to "party popper",
        "🌹" to "rose", "💯" to "hundred points",
        "👑" to "crown", "💎" to "gem stone"
    )

    fun nameFor(emoji: String): String {
        return names[emoji] ?: ""
    }
}
