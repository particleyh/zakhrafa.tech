package com.zakhrafa.keyboard

import android.content.Intent
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.zakhrafa.engine.ZakhrafaEngine
import com.zakhrafa.keyboard.core.DecorationCatalog
import com.zakhrafa.keyboard.core.KeyboardMode

internal class StylePicker(
    private val svc: ZakhrafaKeyboardService,
    private val renderer: KeyRenderer
) {

    private val catalog by lazy { DecorationCatalog(svc) }

    fun showStylePicker() {
        if (svc.isSensitiveEditor()) {
            Toast.makeText(svc, "الزخارف مخفية في حقول كلمات المرور", Toast.LENGTH_SHORT).show()
            return
        }
        val effectiveMode = if (svc.mode == ZakhrafaKeyboardService.LayoutMode.ARABIC ||
            svc.mode == ZakhrafaKeyboardService.LayoutMode.ENGLISH) svc.mode else svc.lastTextMode
        val allStyles = catalog.stylesFor(
            if (effectiveMode == ZakhrafaKeyboardService.LayoutMode.ENGLISH) KeyboardMode.ENGLISH
            else KeyboardMode.ARABIC
        )

        svc.keyboardContainer.removeAllViews()
        val sheet = LinearLayout(svc).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(svc.currentTheme.background)
            setPadding(svc.dp(10), svc.dp(8), svc.dp(10), svc.dp(10))
        }

        sheet.addView(LinearLayout(svc).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            addView(TextView(svc).apply {
                text = "اختيار زخرفة"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(svc.currentTheme.keyTextColor)
                layoutParams = LinearLayout.LayoutParams(0, svc.dp(44), 1f)
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
            })
            addView(renderer.controlButton("إغلاق", widthDp = 72) { closeSheet() })
        })

        val searchBar = EditText(svc).apply {
            hint = "بحث عن زخرفة..."
            textSize = 15f
            setTextColor(svc.currentTheme.keyTextColor)
            setHintTextColor(svc.currentTheme.toolbarTextColor)
            setPadding(svc.dp(14), svc.dp(10), svc.dp(14), svc.dp(10))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            background = renderer.rounded(svc.currentTheme.surface, svc.dp(10).toFloat(), svc.currentTheme.borderColor)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                svc.dp(44)
            ).apply { setMargins(0, 0, 0, svc.dp(6)) }
        }
        sheet.addView(searchBar)

        val categoryBar = HorizontalScrollView(svc).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, svc.dp(42)
            ).apply { setMargins(0, 0, 0, svc.dp(4)) }
        }
        val categoryRow = LinearLayout(svc).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
        }
        categoryBar.addView(categoryRow)
        sheet.addView(categoryBar)

        val list = LinearLayout(svc).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val scrollView = ScrollView(svc).apply {
            addView(list)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                svc.dp(240)
            )
        }
        sheet.addView(scrollView)
        svc.keyboardContainer.addView(sheet)

        var selectedCategory = "all"
        fun renderStyles(filter: String) {
            list.removeAllViews()
            list.addView(renderer.sheetItem("بدون زخرفة") {
                svc.setActiveDecorationStyle(null)
                closeSheet()
            })
            val searched = if (filter.isBlank()) allStyles else allStyles.filter {
                it.label.contains(filter, ignoreCase = true) ||
                    it.categoryLabel.contains(filter, ignoreCase = true)
            }
            val filtered = searched.filter { entry ->
                when (selectedCategory) {
                    "premium" -> entry.isPremium
                    "arabic" -> entry.categoryLabel.startsWith("ar")
                    "english" -> entry.categoryLabel.startsWith("en")
                    "frames" -> entry.categoryLabel == "frame"
                    else -> true
                }
            }
            filtered.forEach { entry ->
                val style = entry.style
                val unlocked = !entry.isPremium || AdManager.isPremiumStyleUnlocked(svc, style.name)
                val sample = if (entry.categoryLabel.startsWith("en")) "Zakhrafa" else "زخرفة"
                val preview = if (entry.categoryLabel == "frame") {
                    style.map[com.zakhrafa.keyboard.core.DecorationCatalog.FRAME_LEFT].orEmpty() +
                        sample +
                        style.map[com.zakhrafa.keyboard.core.DecorationCatalog.FRAME_RIGHT].orEmpty()
                } else if (entry.categoryLabel.startsWith("en")) {
                    ZakhrafaEngine.applyEnglishStyle(sample, style)
                } else {
                    ZakhrafaEngine.mapChars(sample, style.map)
                }
                val favorite = if (catalog.isFavorite(entry.id)) "★ " else ""
                val prefix = if (!unlocked) "🔒 " else favorite
                list.addView(renderer.sheetItem("$prefix${entry.label}  $preview") {
                    if (unlocked) {
                        svc.setActiveDecorationStyle(style)
                        catalog.markRecent(entry.id)
                        closeSheet()
                    } else {
                        Toast.makeText(svc, "افتح هذه الزخرفة من إعدادات الكيبورد", Toast.LENGTH_SHORT).show()
                        val intent = Intent(svc, SettingsActivity::class.java).apply {
                            action = SettingsActivity.ACTION_UNLOCK_STYLE
                            putExtra("style_name", style.name)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        svc.startActivity(intent)
                    }
                })
            }
            if (filtered.isEmpty()) {
                list.addView(TextView(svc).apply {
                    text = "لا توجد نتائج"
                    textSize = 15f
                    setTextColor(svc.currentTheme.toolbarTextColor)
                    gravity = Gravity.CENTER
                    setPadding(0, svc.dp(16), 0, svc.dp(16))
                })
            }
        }

        listOf(
            "مميزة" to "premium", "عربي" to "arabic", "English" to "english",
            "إطارات" to "frames", "الكل" to "all"
        ).forEach { (label, category) ->
            categoryRow.addView(renderer.controlButton(label, widthDp = if (label == "English") 78 else 66) {
                selectedCategory = category
                renderStyles(searchBar.text?.toString().orEmpty())
            })
        }
        renderStyles("")
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                renderStyles(s?.toString().orEmpty())
            }
        })
    }

    private fun closeSheet() {
        svc.rebuildKeyboard()
    }
}
