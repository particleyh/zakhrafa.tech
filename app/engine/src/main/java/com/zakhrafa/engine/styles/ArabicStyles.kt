package com.zakhrafa.engine.styles

import com.zakhrafa.engine.models.StyleMap

object ArabicStyles {
    private fun buildArMap(fn: (String) -> String): Map<String, String> {
        val letters = "ابتثجحخدذرزسشصضطظعغفقكلمنهويأإآءةىؤئ"
        return letters.associate { it.toString() to fn(it.toString()) }
    }

    val all = listOf(
        StyleMap("متصل - أشكال العرض", "ar-standard", mapOf(
            "ا" to "ﺍ", "ب" to "ﺑ", "ت" to "ﺗ", "ث" to "ﺛ", "ج" to "ﺟ", "ح" to "ﺣ", "خ" to "ﺧ", "د" to "ﺩ", "ذ" to "ﺫ", "ر" to "ﺭ", "ز" to "ﺯ", "س" to "ﺳ", "ش" to "ﺷ", "ص" to "ﺻ", "ض" to "ﺿ", "ط" to "ﻃ", "ظ" to "ﻇ", "ع" to "ﻋ", "غ" to "ﻏ", "ف" to "ﻓ", "ق" to "ﻗ", "ك" to "ﻛ", "ل" to "ﻟ", "م" to "ﻣ", "ن" to "ﻧ", "ه" to "ﻫ", "و" to "ﻭ", "ي" to "ﻳ", "ة" to "ﺔ", "ى" to "ﻰ", "ؤ" to "ﺅ", "ئ" to "ﺋ", "أ" to "ﺃ", "إ" to "ﺇ", "آ" to "ﺁ", "ء" to "ء"
        )),
        StyleMap("بـة سفلية (بہ)", "ar-standard", mapOf(
            "ب" to "بہ", "ت" to "تہ", "ث" to "ثہ", "ج" to "جہ", "ح" to "حہ", "خ" to "خہ", "س" to "سہ", "ش" to "شه", "ص" to "صه", "ض" to "ضه", "ط" to "طه", "ظ" to "ظه", "ع" to "عه", "غ" to "غه", "ف" to "فه", "ق" to "قه", "ك" to "كه", "م" to "مه", "ن" to "نه", "ي" to "يه"
        )),
        StyleMap("علامات تشكيل ثقيل", "ar-heavy", mapOf(
            "ا" to "ٵ̷ ", "ب" to "ب̷", "و" to "ۆ̷", "ي" to "ي̷", "ث" to "ث̷", "ت" to "ت̷", "خ" to "خ̷", "ج" to "ج̷", "د" to "د̷ِ", "ذ" to "ذ̷", "ز" to "ز̷", "ر" to "ر̷", "س" to "س̷", "ش" to "ش̷ُ", "ص" to "ص̷", "غ" to "غ̷", "ك" to "گ̷", "ف" to "ف̷َ", "م" to "م̷", "ل" to "ل̷", "ن" to "ن̷", "ه" to "ہ̷", "ح" to "ح̷", "ع" to "ع̷ٍ", "ق" to "ق̷", "ط" to "ط̷ُ", "ظ" to "ظ̷ً", "ى" to "ﮯ̷"
        )),
        StyleMap("قلوب ♥̨̥̬̩", "ar-symbols", mapOf(
            "ا" to "آإ", "ب" to "بـ♥̨̥̬̩", "ت" to "تـ♥̨̥̬̩", "ث" to "ثـ♥̨̥̬̩", "ج" to "جـ♥̨̥̬̩", "ح" to "حـ♥̨̥̬̩", "خ" to "خــ♥̨̥̬̩", "ك" to "گ♥̨̥̬̩", "م" to "مـ♥̨̥̬̩", "ه" to "هـ♥̨̥̬̩", "غ" to "غ♥̨̥̬̩", "ق" to "قـ♥̨̥̬̩", "ط" to "ط♥̨̥̬̩", "ظ" to "ظ♥̨̥̬̩", "ى" to "ے"
        )),
        StyleMap("نجوم ★", "ar-symbols", buildArMap { "★$it★" }),
        StyleMap("نجيمات ✦", "ar-symbols", buildArMap { "✦$it✦" }),
        StyleMap("ألف خنجرية (ٰ)", "ar-diacritic", buildArMap { "ٰ$it" }),
        StyleMap("شدة + كسرة (ِّ)", "ar-diacritic", buildArMap { "${it}ِّ" }),
        StyleMap("فتحة (َ)", "ar-diacritic", buildArMap { "${it}َ" }),
        StyleMap("ضمة (ُ)", "ar-diacritic", buildArMap { "${it}ُ" }),
        StyleMap("كسرة (ِ)", "ar-diacritic", buildArMap { "${it}ِ" }),
        StyleMap("احترافي - نادر ثقيل", "ar-trending", mapOf(
            "ا" to "ٱ̍", "ب" to "ٻۧ", "ت" to "ٿ", "ث" to "ٽ", "ج" to "ڄۚ", "ح" to "حۡ", "خ" to "څ", "د" to "ڊ", "ذ" to "ڏ", "ر" to "ڔ", "ز" to "ڗ", "س" to "ڛۣ", "ش" to "ڜ", "ص" to "ڝ", "ض" to "ڞ", "ط" to "ڟ", "ظ" to "ڠ", "ع" to "؏", "غ" to "ڧ", "ف" to "ڣ", "ق" to "ڦ", "ك" to "ڪ", "ل" to "ڵ", "م" to "مۭ", "ن" to "ڼ", "ه" to "ھ", "و" to "ۏ", "ي" to "ېْۧ"
        )),
        StyleMap("خط رقعة فاخر", "ar-trending", mapOf(
            "ا" to "أ", "ب" to "بَ", "ت" to "تِ", "ث" to "ثُ", "ج" to "جَ", "ح" to "حِ", "خ" to "خُ", "د" to "دَ", "ذ" to "ذُ", "ر" to "رِ", "ز" to "زَ", "س" to "سُ", "ش" to "شِ", "ص" to "صَ", "ض" to "ضُ", "ط" to "طِ", "ظ" to "ظَ", "ع" to "عُ", "غ" to "غِ", "ف" to "فَ", "ق" to "قُ", "ك" to "گِ", "ل" to "لَ", "م" to "مُ", "ن" to "نِ", "ه" to "ھَ", "و" to "ۆ", "ي" to "یَ"
        )),
        StyleMap("ناعم - لطيف", "ar-trending", mapOf(
             "ا" to "آ", "ب" to "بـ", "ت" to "تـ", "ث" to "ثـ", "ج" to "جـ", "ح" to "حـ", "خ" to "خـ", "د" to "د", "ذ" to "ذ", "ر" to "ر", "ز" to "ز", "س" to "سـ", "ش" to "شـ", "ص" to "صـ", "ض" to "ضـ", "ط" to "طـ", "ظ" to "ظـ", "ع" to "عـ", "غ" to "غـ", "ف" to "فـ", "ق" to "قـ", "ك" to "كـ", "ل" to "لـ", "م" to "مـ", "ن" to "نـ", "ه" to "ھ", "و" to "و", "ي" to "يـ"
        )),
        StyleMap("نادر - أندر الحروف", "ar-trending", mapOf(
            "ا" to "آ", "ب" to "ٮ", "ت" to "ٹ", "ث" to "ٿ", "ج" to "ڇ", "ح" to "ځ", "خ" to "څ", "د" to "ڊ", "ذ" to "ڌ", "ر" to "ړ", "ز" to "ژ", "س" to "ښ", "ش" to "ڜ", "ص" to "ڝ", "ض" to "ڞ", "ط" to "ڟ", "ظ" to "ڠ", "ع" to "؏", "غ" to "ؽ", "ف" to "ڣ", "ق" to "ڧ", "ك" to "ڪ", "ل" to "ڸ", "م" to "۾", "ن" to "ڼ", "ه" to "ھ", "و" to "ۄ", "ي" to "ے", "ة" to "ۃ"
        )),
        StyleMap("جليتش عربي", "ar-trending", buildArMap { "${it}҉" }),
        StyleMap("أرقام ١٢٣", "ar-numbers", mapOf("0" to "٠", "1" to "١", "2" to "٢", "3" to "٣", "4" to "٤", "5" to "٥", "6" to "٦", "7" to "٧", "8" to "٨", "9" to "٩")),
        StyleMap("ارقام محاطة ①", "ar-numbers", mapOf("0" to "⓪", "1" to "①", "2" to "②", "3" to "③", "4" to "④", "5" to "⑤", "6" to "⑥", "7" to "⑦", "8" to "⑧", "9" to "⑨"))
    )
}
