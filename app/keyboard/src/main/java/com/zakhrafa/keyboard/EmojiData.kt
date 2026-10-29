package com.zakhrafa.keyboard

import android.content.Context
import android.graphics.Paint

object EmojiData {

    data class EmojiCategory(val icon: String, val name: String, val emojis: List<String>)

    private const val RECENT_PREFS = "emoji_recent"
    private const val RECENT_KEY = "recent"
    private const val MAX_RECENT = 48

    private val skinTones = listOf("\uD83C\uDFFB", "\uD83C\uDFFC", "\uD83C\uDFFD", "\uD83C\uDFFE", "\uD83C\uDFFF")

    private fun parse(source: String): List<String> = source.split(' ').filter { it.isNotEmpty() }

    private val skinToneBaseSet: Set<String> by lazy {
        parse(GENERATED_EMOJI_SKIN_TONE_BASES).toSet()
    }

    fun supportsSkinTones(emoji: String): Boolean = emoji in skinToneBaseSet

    private val glyphPaint = Paint()
    private val glyphCache = HashMap<String, Boolean>()

    /**
     * True when this device's fonts actually contain a glyph for the emoji.
     * Emojis from recent Unicode releases render as an empty "tofu" square on
     * fonts that predate them, so the picker hides whatever can't be drawn.
     * Results are cached for the process lifetime.
     */
    fun rendersOnDevice(emoji: String): Boolean =
        glyphCache.getOrPut(emoji) { glyphPaint.hasGlyph(emoji) }

    fun skinToneVariants(base: String): List<String> =
        if (!supportsSkinTones(base)) emptyList()
        else skinTones.map { base + it }.filter { rendersOnDevice(it) }

    private val kaomoji = listOf(
        "(｡•́‿•̀｡)", "(╯°□°）╯︵ ┻━┻", "¯\\_(ツ)_/¯", "(づ｡◕‿‿◕｡)づ",
        "(｡♥‿♥｡)", "(≧◡≦)", "(¬‿¬)", "(•̀ᴗ•́)و", "(✿◠‿◠)", "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧",
        "ʕ•ᴥ•ʔ", "(๑˃̵ᴗ˂̵)و", "(╥﹏╥)", "(ಥ﹏ಥ)", "(¬_¬)", "(ʘ‿ʘ)",
        "(☞ﾟヮﾟ)☞", "☜(ﾟヮﾟ☜)", "(ง'̀-'́)ง", "ᕙ(⇀‸↼‶)ᕗ", "(✧ω✧)", "(｡•̀ᴗ-)✧"
    )

    val categories: List<EmojiCategory> by lazy {
        listOf(
            EmojiCategory("😀", "Smileys", parse(GENERATED_EMOJI_SMILEYS)),
            EmojiCategory("👋", "People", parse(GENERATED_EMOJI_PEOPLE)),
            EmojiCategory("🐶", "Animals", parse(GENERATED_EMOJI_ANIMALS)),
            EmojiCategory("🍔", "Food", parse(GENERATED_EMOJI_FOOD)),
            EmojiCategory("✈️", "Travel", parse(GENERATED_EMOJI_TRAVEL)),
            EmojiCategory("⚽", "Activities", parse(GENERATED_EMOJI_ACTIVITIES)),
            EmojiCategory("💡", "Objects", parse(GENERATED_EMOJI_OBJECTS)),
            EmojiCategory("💚", "Symbols", parse(GENERATED_EMOJI_SYMBOLS)),
            EmojiCategory("🏁", "Flags", parse(GENERATED_EMOJI_FLAGS)),
            EmojiCategory("(•‿•)", "Kaomoji", kaomoji)
        )
    }

    fun getRecent(context: Context): List<String> {
        val prefs = context.getSharedPreferences(RECENT_PREFS, Context.MODE_PRIVATE)
        return (prefs.getString(RECENT_KEY, "") ?: "").split(',').filter { it.isNotEmpty() }
    }

    fun addRecent(context: Context, emoji: String) {
        val prefs = context.getSharedPreferences(RECENT_PREFS, Context.MODE_PRIVATE)
        val recent = getRecent(context).toMutableList()
        recent.remove(emoji)
        recent.add(0, emoji)
        while (recent.size > MAX_RECENT) recent.removeAt(recent.lastIndex)
        prefs.edit().putString(RECENT_KEY, recent.joinToString(",")).apply()
    }
}
