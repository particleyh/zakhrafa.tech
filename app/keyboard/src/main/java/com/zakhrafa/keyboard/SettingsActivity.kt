package com.zakhrafa.keyboard

import android.annotation.SuppressLint
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.zakhrafa.keyboard.theme.KeyboardTheme
import com.zakhrafa.keyboard.theme.Themes
import java.io.File

class SettingsActivity : ComponentActivity() {
    companion object {
        const val REQUEST_BACKGROUND_IMAGE = 4207
        const val ACTION_PICK_BACKGROUND = "com.keyboard.calligraphy.PICK_BACKGROUND"
        const val ACTION_UNLOCK_STYLE = "com.keyboard.calligraphy.UNLOCK_STYLE"
        const val ACTION_UNLOCK_THEME = "com.keyboard.calligraphy.UNLOCK_THEME"
        const val PLAY_PACKAGE_ID = "com.keyboard.calligraphy"
        const val PLAY_URL = "https://play.google.com/store/apps/details?id=$PLAY_PACKAGE_ID"
    }

    private val prefs by lazy { getSharedPreferences("keyboard_settings", MODE_PRIVATE) }
    private lateinit var content: LinearLayout
    private var rootLayout: LinearLayout? = null
    private var tutorialDialog: AlertDialog? = null
    private var settingsScroll: ScrollView? = null
    private var settingsScrollY = 0
    private var autoOpenedBackgroundPicker = false
    private val selectionHandler = Handler(Looper.getMainLooper())
    private var selectionCheck: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdManager.initialize(this)
        AdManager.loadRewarded(this)
        AdManager.loadInterstitial(this)
        when (intent?.action) {
            ACTION_UNLOCK_STYLE -> {
                showStyleUnlock(intent.getStringExtra("style_name").orEmpty())
                return
            }
            ACTION_UNLOCK_THEME -> {
                showThemeUnlock(intent.getStringExtra("theme_name").orEmpty())
                return
            }
        }
        render()
        if (intent?.action == ACTION_PICK_BACKGROUND && !autoOpenedBackgroundPicker) {
            autoOpenedBackgroundPicker = true
            content.post { pickBackgroundImage() }
        }
    }

    override fun onDestroy() {
        selectionCheck?.let { selectionHandler.removeCallbacks(it) }
        tutorialDialog?.dismiss()
        super.onDestroy()
    }

    private fun showStyleUnlock(styleName: String) {
        if (styleName.isBlank()) {
            render()
            return
        }
        showRewardUnlock(
            title = "فتح زخرفة مميزة",
            message = "شاهد إعلان مكافأة واحد لفتح هذه الزخرفة بشكل دائم.",
            button = "🎬  شاهد الإعلان وافتح الزخرفة",
            allowInterstitialFallback = true
        ) {
            AdManager.unlockPremiumStyle(this, styleName)
            Toast.makeText(this, "تم فتح الزخرفة", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showThemeUnlock(themeName: String) {
        val theme = Themes.all.firstOrNull { it.name == themeName }
        if (theme == null || !theme.isRewarded) {
            render()
            return
        }
        showRewardUnlock(
            title = "فتح ثيم ${theme.label}",
            message = "شاهد إعلان مكافأة واحد لفتح هذا الثيم بشكل دائم.",
            button = "🎬  شاهد الإعلان وافتح الثيم"
        ) {
            AdManager.unlockPremiumTheme(this, theme.name)
            prefs.edit().putString("theme", theme.name).apply()
            Toast.makeText(this, "تم تفعيل ثيم ${theme.label}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRewardUnlock(
        title: String,
        message: String,
        button: String,
        allowInterstitialFallback: Boolean = false,
        onReward: () -> Unit
    ) {
        val theme = selectedTheme()
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(40), dp(28), dp(40))
            setBackgroundColor(theme.background)
        }
        screen.addView(TextView(this).apply {
            text = "🔒"
            textSize = 48f
            gravity = Gravity.CENTER
        })
        screen.addView(TextView(this).apply {
            text = title
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(theme.keyTextColor)
            setPadding(0, dp(12), 0, dp(6))
        })
        screen.addView(TextView(this).apply {
            text = message
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(theme.toolbarTextColor)
            setPadding(0, 0, 0, dp(18))
        })
        screen.addView(choice(button, false, prominent = true) {
            val rewardedShown = AdManager.hasRewardedReady() && AdManager.showRewarded(this) {
                    onReward()
                    finish()
                }
            if (!rewardedShown) {
                if (allowInterstitialFallback && AdManager.showInterstitialIfReady(this)) {
                    Toast.makeText(
                        this,
                        "تم عرض إعلان عادي. هذه الزخرفة لا تُفتح إلا بإعلان مكافأة؛ عُد لاحقاً.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this, "لا يوجد إعلان متاح الآن. ارجع لاحقاً للمحاولة.", Toast.LENGTH_LONG).show()
                }
                AdManager.loadRewarded(this)
                AdManager.loadInterstitial(this)
            }
        })
        setContentView(screen)
    }

    override fun onResume() {
        super.onResume()
        if (intent?.action == ACTION_PICK_BACKGROUND) return
        if (intent?.action == ACTION_UNLOCK_STYLE ||
            intent?.action == ACTION_UNLOCK_THEME) {
            return
        }
        waitForKeyboardSelection()
        showSetupTutorialIfNeeded()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Uses the platform picker result for broad compatibility with this simple View activity.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_BACKGROUND_IMAGE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            runCatching { saveBackgroundImage(uri) }
                .onSuccess { storedUri ->
                    prefs.edit().putString("background_uri", storedUri).apply()
                    Toast.makeText(this, "تم حفظ خلفية الكيبورد", Toast.LENGTH_SHORT).show()
                    if (intent?.action == ACTION_PICK_BACKGROUND) {
                        finish()
                        return
                    }
                    render()
                }
                .onFailure {
                    Toast.makeText(this, "تعذر اختيار الصورة، جرّب صورة أخرى", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun render() {
        settingsScrollY = settingsScroll?.scrollY ?: settingsScrollY
        // Keep one activity root. Replacing the window with setContentView on every
        // preference click was the visible white flicker users were seeing.
        val root = rootLayout ?: LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF5F7FB.toInt())
            rootLayout = this
        }
        root.removeAllViews()
        val floatingPreview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(0xFFFFFFFF.toInt(), dp(16).toFloat(), 0xFFDDE3EA.toInt())
            elevation = dp(5).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(12), dp(12), dp(12), dp(4)) }
            addView(LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(title("معاينة مباشرة", 17f).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(34), 1f)
                })
                addView(TextView(this@SettingsActivity).apply {
                    text = "تتحدث فوراً"
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setTextColor(0xFF136F63.toInt())
                    background = rounded(0xFFE7F4F1.toInt(), dp(999).toFloat(), 0xFFB9DED5.toInt())
                    setPadding(dp(9), 0, dp(9), 0)
                })
            })
            addView(keyboardPreview())
        }
        root.addView(floatingPreview)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(24))
            setBackgroundColor(0xFFF5F7FB.toInt())
        }

        content.addView(title("إعدادات كيبورد مزخرف", 24f))
        content.addView(text("اضبط الشكل والكتابة هنا. الزخرفة المختارة تُحفظ تلقائياً، والحافظة الذكية تعرض الروابط للّصق السريع."))

        content.addView(primaryAction("1  تفعيل الكيبورد") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        content.addView(primaryAction("2  اختيار الكيبورد") {
            markWaitingForKeyboardSelection()
            showInputMethodPicker()
        })

        addThemeSection()
        addKeySizeSection()
        addLabelSizeSection()
        addBackgroundSection()
        addSuggestionSection()
        addToggleSection()
        addLanguageSection()
        addStoreSection()
        addPreview()

        val scroll = ScrollView(this).apply {
            addView(content)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        settingsScroll = scroll
        root.addView(scroll)
        if (root.parent == null) setContentView(root)
        scroll.post { scroll.scrollTo(0, settingsScrollY) }
    }

    private fun showSetupTutorialIfNeeded() {
        if (tutorialDialog?.isShowing == true) return
        if (prefs.getBoolean("awaiting_selection_success", false)) return
        when {
            !isKeyboardEnabled() -> showEnableDialog()
            !isKeyboardSelected() -> showPickerDialog()
        }
    }

    private fun showEnableDialog() {
        tutorialDialog = tutorialDialog(
            title = "تفعيل كيبورد مزخرف",
            hero = "👇",
            steps = listOf(
                "اضغط الزر بالأسفل",
                "اضغط على: <<<< كيبورد مزخرف - اضغط هنا >>>>",
                "شغّل الزر بجانبه ووافق",
                "ارجع للتطبيق لاختيار الكيبورد"
            ),
            action = "تفعيل الكيبورد"
        ) {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
    }

    private fun showPickerDialog() {
        tutorialDialog = tutorialDialog(
            title = "اختيار كيبورد مزخرف",
            hero = "☝",
            steps = listOf(
                "اضغط اختيار الكيبورد",
                "اختر كيبورد مزخرف من النافذة",
                "افتح خانة التجربة",
                "ستظهر الزخارف فوق زر اختيار زخرفة"
            ),
            action = "اختيار الكيبورد"
        ) {
                markWaitingForKeyboardSelection()
                showInputMethodPicker()
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun showKeyboardSelectedDialog() {
        tutorialDialog = tutorialDialog(
            title = "تم تفعيل كيبورد مزخرف",
            hero = "✓",
            steps = listOf(
                "مبروك، الكيبورد أصبح جاهزاً",
                "سنفتح لك اختيار الزخارف الآن",
                "اختر زخرفة واحدة وستطبق أثناء الكتابة"
            ),
            action = "اختار أول زخرفة"
        ) {
            prefs.edit()
                .remove("open_style_picker_once")
                .putLong("open_style_picker_requested_at", System.currentTimeMillis())
                .commit()
            openKeyboardForFirstStyle()
        }
    }

    /** Opens the selected IME itself, never Android's "choose input method" sheet. */
    private fun openKeyboardForFirstStyle() {
        val field = EditText(this).apply {
            hint = "اكتب هنا"
            textSize = 18f
            gravity = Gravity.END
            setSingleLine(true)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("اختار أول زخرفة")
            .setMessage("سيظهر كيبورد مزخرف الآن، ثم اختر الزخرفة التي تعجبك.")
            .setView(field)
            .setPositiveButton("تم", null)
            .create()
        dialog.setOnShowListener {
            field.requestFocus()
            dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            field.post {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(field, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        dialog.show()
    }

    private fun tutorialDialog(
        title: String,
        hero: String,
        steps: List<String>,
        action: String,
        onAction: () -> Unit
    ): AlertDialog {
        val pointer = TextView(this).apply {
            text = hero
            textSize = 36f
            gravity = Gravity.CENTER
        }
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(18))
            background = rounded(0xFFFFFFFF.toInt(), dp(18).toFloat(), 0xFFDDE3EA.toInt())
            addView(TextView(this@SettingsActivity).apply {
                text = title
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(0xFF101828.toInt())
                gravity = Gravity.END
            })
            addView(TextView(this@SettingsActivity).apply {
                text = "اتبع الخطوات بالترتيب، ولن تحتاج أي شرح آخر."
                textSize = 14f
                setTextColor(0xFF667085.toInt())
                gravity = Gravity.END
                setPadding(0, dp(6), 0, dp(14))
            })
            addView(pointer)
            steps.forEachIndexed { index, step ->
                addView(tutorialStep(index + 1, step))
            }
            addView(tutorialButton(action) {
                tutorialDialog?.dismiss()
                tutorialDialog = null
                Handler(Looper.getMainLooper()).postDelayed({ onAction() }, 180)
            })
        }
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()
        dialog.setOnShowListener {
            if (!prefs.getBoolean("reduce_motion", false)) {
                ObjectAnimator.ofFloat(pointer, View.TRANSLATION_Y, 0f, dp(10).toFloat(), 0f).apply {
                    duration = 950
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            }
        }
        dialog.show()
        return dialog
    }

    private fun tutorialStep(number: Int, textValue: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            setPadding(0, dp(5), 0, dp(5))
            addView(TextView(this@SettingsActivity).apply {
                text = textValue
                textSize = 15f
                setTextColor(0xFF101828.toInt())
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@SettingsActivity).apply {
                text = number.toString()
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(0xFFFFFFFF.toInt())
                gravity = Gravity.CENTER
                background = rounded(0xFF136F63.toInt(), dp(999).toFloat(), 0xFF136F63.toInt())
                layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply {
                    setMargins(dp(10), 0, 0, 0)
                }
            })
        }
    }

    private fun tutorialButton(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            background = rounded(0xFF136F63.toInt(), dp(12).toFloat(), 0xFF136F63.toInt())
            setPadding(0, dp(14), 0, dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(18), 0, 0) }
            setOnClickListener { onClick() }
        }
    }

    private fun isKeyboardEnabled(): Boolean {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == packageName }
    }

    private fun isKeyboardSelected(): Boolean {
        val selected = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return selected?.contains(packageName) == true
    }

    private fun showInputMethodPicker() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        waitForKeyboardSelection()
    }

    private fun markWaitingForKeyboardSelection() {
        prefs.edit()
            .putBoolean("awaiting_selection_success", true)
            .putLong("selection_requested_at", System.currentTimeMillis())
            .apply()
    }

    /** IME pickers are overlays on many devices and do not reliably call onResume. */
    private fun waitForKeyboardSelection() {
        if (!prefs.getBoolean("awaiting_selection_success", false)) return
        selectionCheck?.let { selectionHandler.removeCallbacks(it) }
        val startedAt = prefs.getLong("selection_requested_at", System.currentTimeMillis())
        selectionCheck = object : Runnable {
            override fun run() {
                if (!prefs.getBoolean("awaiting_selection_success", false)) return
                val elapsed = System.currentTimeMillis() - startedAt
                if (isKeyboardSelected() && elapsed >= 1_000L) {
                    prefs.edit().remove("awaiting_selection_success").apply()
                    showKeyboardSelectedDialog()
                } else if (elapsed < 20_000L) {
                    selectionHandler.postDelayed(this, 350L)
                } else {
                    prefs.edit().remove("awaiting_selection_success").apply()
                }
            }
        }
        selectionHandler.postDelayed(selectionCheck!!, 350L)
    }

    private fun addThemeSection() {
        content.addView(section("المظهر"))
        Themes.all.forEach { theme ->
            val unlocked = !theme.isRewarded || AdManager.isPremiumThemeUnlocked(this, theme.name)
            val current = prefs.getString("theme", Themes.pearl.name) == theme.name && unlocked
            content.addView(choice(
                value = if (unlocked) theme.label else "🔒 ${theme.label}  •  إعلان مكافأة",
                selected = current && unlocked,
                onClick = {
                    if (unlocked) {
                        updateString("theme", theme.name)
                    } else {
                        startActivity(Intent(this, SettingsActivity::class.java).apply {
                            action = ACTION_UNLOCK_THEME
                            putExtra("theme_name", theme.name)
                        })
                    }
                }
            ))
        }
    }

    private fun addKeySizeSection() {
        content.addView(section("حجم الأزرار"))
        content.addView(choice("مضغوط", prefs.getInt("key_height", 46) == 42) { updateInt("key_height", 42) })
        content.addView(choice("متوازن", prefs.getInt("key_height", 46) == 46) { updateInt("key_height", 46) })
        content.addView(choice("كبير", prefs.getInt("key_height", 46) == 52) { updateInt("key_height", 52) })
    }

    private fun addSuggestionSection() {
        content.addView(section("عدد الاقتراحات"))
        content.addView(choice("12 اقتراح", prefs.getInt("suggestion_count", 18) == 12) { updateInt("suggestion_count", 12) })
        content.addView(choice("18 اقتراح", prefs.getInt("suggestion_count", 18) == 18) { updateInt("suggestion_count", 18) })
        content.addView(choice("24 اقتراح", prefs.getInt("suggestion_count", 18) == 24) { updateInt("suggestion_count", 24) })
    }

    private fun addLabelSizeSection() {
        content.addView(section("حجم حروف الكيبورد"))
        content.addView(choice("صغير", prefs.getInt("label_scale", 100) == 90) { updateInt("label_scale", 90) })
        content.addView(choice("عادي", prefs.getInt("label_scale", 100) == 100) { updateInt("label_scale", 100) })
        content.addView(choice("كبير", prefs.getInt("label_scale", 100) == 115) { updateInt("label_scale", 115) })
    }

    private fun addBackgroundSection() {
        val hasImage = !prefs.getString("background_uri", "").isNullOrBlank()
        content.addView(section("خلفية الكيبورد"))
        content.addView(choice(
            value = if (hasImage) "تغيير صورة الخلفية" else "اختيار صورة من الجهاز",
            selected = hasImage,
            onClick = { pickBackgroundImage() }
        ))
        if (hasImage) {
            content.addView(choice("حذف صورة الخلفية", false) {
                clearBackgroundImage()
            })
        }
    }

    private fun pickBackgroundImage() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = "image/*"
            }
        } else {
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_BACKGROUND_IMAGE)
    }

    private fun saveBackgroundImage(source: Uri): String {
        val imageFile = File(filesDir, "keyboard_background_image.jpg")
        val bitmap = decodeScaledBitmap(source)
        imageFile.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
        }
        if (!bitmap.isRecycled) bitmap.recycle()
        return Uri.fromFile(imageFile).toString()
    }

    private fun decodeScaledBitmap(source: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("Invalid image")

        var sampleSize = 1
        val maxDimension = 1800
        while ((bounds.outWidth / sampleSize) > maxDimension || (bounds.outHeight / sampleSize) > maxDimension) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: error("Invalid image")
    }

    private fun clearBackgroundImage() {
        File(filesDir, "keyboard_background_image").delete()
        File(filesDir, "keyboard_background_image.jpg").delete()
        prefs.edit().remove("background_uri").apply()
        render()
    }

    private fun addToggleSection() {
        content.addView(section("الكتابة"))
        content.addView(choice(
            value = "صف الأرقام",
            selected = prefs.getBoolean("number_row", true),
            onClick = { updateBoolean("number_row", !prefs.getBoolean("number_row", true)) }
        ))
        content.addView(choice(
            value = "مسافة عريضة",
            selected = prefs.getBoolean("wide_spacebar", true),
            onClick = { updateBoolean("wide_spacebar", !prefs.getBoolean("wide_spacebar", true)) }
        ))
        content.addView(choice(
            value = "اهتزاز عند الضغط",
            selected = prefs.getBoolean("haptic", true),
            onClick = { updateBoolean("haptic", !prefs.getBoolean("haptic", true)) }
        ))
        content.addView(choice(
            value = "صوت الأزرار",
            selected = prefs.getBoolean("sound", false),
            onClick = { updateBoolean("sound", !prefs.getBoolean("sound", false)) }
        ))
        content.addView(choice(
            value = "تقليل الحركة",
            selected = prefs.getBoolean("reduce_motion", false),
            onClick = { updateBoolean("reduce_motion", !prefs.getBoolean("reduce_motion", false)) }
        ))
    }

    private fun addLanguageSection() {
        content.addView(section("اللغة الافتراضية"))
        content.addView(choice("عربي", prefs.getString("default_language", "ar") == "ar") { updateString("default_language", "ar") })
        content.addView(choice("English", prefs.getString("default_language", "ar") == "en") { updateString("default_language", "en") })
    }

    private fun addStoreSection() {
        content.addView(section("حول التطبيق"))
        content.addView(choice("مشاركة كيبورد مزخرف", false) { shareApp() })
        content.addView(text("الإصدار 5.5  •  زخرفة محفوظة  •  حافظة ذكية  •  حماية حقول كلمات المرور"))
    }

    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "كيبورد مزخرف")
            putExtra(
                Intent.EXTRA_TEXT,
                "جرّب كيبورد مزخرف للكتابة العربية المزخرفة، اقتراحات زخرفة، رموز، إيموجي، وحركات عربية:\n$PLAY_URL"
            )
        }
        startActivity(Intent.createChooser(intent, "مشاركة التطبيق"))
    }

    private fun addPreview() {
        content.addView(section("تجربة"))
        content.addView(EditText(this).apply {
            hint = "اضغط هنا وجرب الاقتراحات..."
            textSize = 18f
            setSingleLine(false)
            minLines = 3
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(0xFFFFFFFF.toInt(), dp(10).toFloat(), 0xFFDDE3EA.toInt())
        })
    }

    private fun keyboardPreview(): FrameLayout {
        val theme = selectedTheme()
        val isEnglish = prefs.getString("default_language", "ar") == "en"
        val keyHeight = prefs.getInt("key_height", 46)
        val labelScale = prefs.getInt("label_scale", 100)
        val showNumbers = prefs.getBoolean("number_row", true)
        val wideSpacebar = prefs.getBoolean("wide_spacebar", true)

        val frame = CroppedBackgroundFrame(this).apply {
            background = rounded(theme.background, dp(12).toFloat(), theme.borderColor)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val backgroundUri = prefs.getString("background_uri", "").orEmpty()
        if (backgroundUri.isNotBlank()) {
            val image = ImageView(this).apply {
                setImageURI(Uri.parse(backgroundUri))
                scaleType = ImageView.ScaleType.CENTER_CROP
                alpha = 0.32f
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            frame.backgroundView = image
            frame.addView(image)
        }

        val keyboard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(7))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            addView(LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(2), 0, dp(2), dp(5))
                addView(previewChip(if (isEnglish) "ℤ𝕒𝕜𝕙𝕣𝕒𝕗𝕒" else "زخہرفہة", theme, true))
                addView(previewChip(if (isEnglish) "♡ Zakhrafa ♡" else "♡ زخرفة ♡", theme, false))
                addView(previewChip(if (isEnglish) "꧁Zakhrafa꧂" else "꧁زخرفة꧂", theme, false))
            })

            addView(LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(2), 0, dp(2), dp(5))
                addView(previewControl("📋", theme, 0.8f))
                addView(previewControl("اختيار زخرفة", theme, 3f))
                addView(previewControl("إلغاء", theme, 1.4f))
            })

            previewRows(isEnglish, showNumbers, wideSpacebar).forEach { row ->
                addView(LinearLayout(this@SettingsActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    row.forEach { key -> addView(previewKey(key, theme, keyHeight, labelScale)) }
                })
            }
        }
        frame.addView(keyboard)
        return frame
    }

    private fun previewRows(isEnglish: Boolean, showNumbers: Boolean, wideSpacebar: Boolean): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        if (showNumbers) {
            rows.add(ENGLISH_NUMBER_ROW_LEFT_TO_RIGHT.map { it.toString() })
        }
        if (isEnglish) {
            rows.add("qwertyuiop".map { it.toString() })
            rows.add("asdfghjkl".map { it.toString() })
            rows.add("zxcvbnm".map { it.toString() } + "⌫")
            rows.add(if (wideSpacebar) listOf("123", "🌐", ",", "space", "😊", ".", "⏎") else listOf("123", "🌐", ",", "space", "😊", ".", "⏎"))
        } else {
            rows.add("ضصثقفغعهخحج".map { it.toString() })
            rows.add("شسيبلاتنمكط".map { it.toString() })
            rows.add(listOf("ذ", "ء", "ؤ", "ر", "ى", "ة", "و", "ز", "ظ", "د", "⌫"))
            rows.add(if (wideSpacebar) listOf("123", "🌐", "،", "مسافة", "😊", ".", "⏎") else listOf("123", "🌐", "،", "مسافة", "😊", ".", "⏎"))
        }
        return rows
    }

    private fun previewChip(label: String, theme: KeyboardTheme, primary: Boolean): TextView {
        return TextView(this).apply {
            text = label
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setSingleLine(true)
            setTextColor(if (primary) theme.background else theme.toolbarTextColor)
            background = rounded(
                if (primary) theme.primaryColor else theme.surface,
                dp(18).toFloat(),
                if (primary) theme.primaryColor else theme.borderColor
            )
            setPadding(dp(10), 0, dp(10), 0)
            layoutParams = LinearLayout.LayoutParams(0, dp(32), 1f).apply {
                setMargins(dp(2), 0, dp(2), 0)
            }
        }
    }

    private fun previewControl(label: String, theme: KeyboardTheme, weight: Float): TextView {
        return TextView(this).apply {
            text = label
            textSize = if (label == "⚙" || label == "🖼") 18f else 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(theme.toolbarTextColor)
            background = rounded(theme.surface, dp(10).toFloat(), theme.borderColor)
            layoutParams = LinearLayout.LayoutParams(0, dp(36), weight).apply {
                setMargins(dp(2), 0, dp(2), 0)
            }
        }
    }

    private fun previewKey(label: String, theme: KeyboardTheme, keyHeight: Int, labelScale: Int): TextView {
        return TextView(this).apply {
            text = label
            val baseSize = when {
                label == "مسافة" || label == "space" -> 11f
                label.length > 2 -> 11f
                else -> 16f
            }
            textSize = baseSize * (labelScale / 100f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            includeFontPadding = false
            val special = label in listOf("123", "🌐", "⌫", "⏎", "مسافة", "space")
            setTextColor(if (special) theme.toolbarTextColor else theme.keyTextColor)
            background = rounded(
                if (special) theme.specialKeyBackground else theme.keyBackground,
                dp(7).toFloat(),
                theme.borderColor
            )
            layoutParams = LinearLayout.LayoutParams(0, dp((keyHeight * 0.72f).toInt()), if (label == "مسافة" || label == "space") 4.2f else 1f).apply {
                setMargins(dp(1), dp(2), dp(1), dp(2))
            }
        }
    }

    private fun selectedTheme(): KeyboardTheme {
        val selected = prefs.getString("theme", Themes.pearl.name)
        val theme = Themes.all.firstOrNull { it.name == selected } ?: Themes.pearl
        return if (theme.isRewarded && !AdManager.isPremiumThemeUnlocked(this, theme.name)) Themes.pearl else theme
    }

    private fun updateString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
        render()
    }

    private fun updateInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
        render()
    }

    private fun updateBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        render()
    }

    private fun title(value: String, size: Float): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF101828.toInt())
            gravity = Gravity.END
            setPadding(0, 0, 0, dp(10))
        }
    }

    private fun section(value: String): TextView {
        return title(value, 18f).apply { setPadding(0, dp(22), 0, dp(8)) }
    }

    private fun text(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(0xFF667085.toInt())
            gravity = Gravity.END
            setPadding(0, 0, 0, dp(18))
        }
    }

    private fun primaryAction(value: String, onClick: () -> Unit): TextView {
        return choice(value = value, selected = false, prominent = true, onClick = onClick)
    }

    private fun choice(
        value: String,
        selected: Boolean,
        prominent: Boolean = false,
        onClick: () -> Unit
    ): TextView {
        val fill = when {
            prominent -> 0xFF136F63.toInt()
            selected -> 0xFFE7F4F1.toInt()
            else -> 0xFFFFFFFF.toInt()
        }
        val textColor = if (prominent) 0xFFFFFFFF.toInt() else 0xFF101828.toInt()
        val border = if (selected || prominent) 0xFF136F63.toInt() else 0xFFDDE3EA.toInt()
        return TextView(this).apply {
            text = if (selected && !prominent) "✓ $value" else value
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(textColor)
            background = rounded(fill, dp(10).toFloat(), border)
            setPadding(0, dp(13), 0, dp(13))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(5), 0, dp(5)) }
            setOnClickListener { onClick() }
        }
    }

    private fun rounded(color: Int, radius: Float, stroke: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            setStroke(dp(1), stroke)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
