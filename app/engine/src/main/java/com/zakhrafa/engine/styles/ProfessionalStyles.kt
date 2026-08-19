package com.zakhrafa.engine.styles

import com.zakhrafa.engine.models.StyleMap

object ProfessionalStyles {
    private fun mapAr(m: Map<String, String>): Map<String, String> = m
    private fun mapEn(m: Map<String, String>): Map<String, String> = m

    val arabic = listOf(
        StyleMap("زخرفة رقعة", "ar-pro", mapAr(mapOf("ا" to "أ", "ب" to "بہ", "و" to "و", "ي" to "يہ", "ث" to "ثہ", "ت" to "تہ", "خ" to "خہ", "ج" to "جہ", "د" to "د", "ذ" to "ذ", "ز" to "ز", "س" to "سہ", "ش" to "شہ", "ص" to "صہ", "غ" to "غہ", "ك" to "كہ", "ف" to "فہ", "م" to "مہ", "ل" to "ل", "ن" to "نہ", "ه" to "ه", "ح" to "حہ", "ع" to "عہ", "ق" to "قہ", "ط" to "طہ", "ظ" to "ظہ"))),
        StyleMap("زخرفة فنية", "ar-pro", mapAr(mapOf("ا" to "آ", "ب" to "بـٌـٌٌـٌٌٌـٌٌـٌ", "و" to "وُ", "ي" to "ي", "ث" to "ثُ", "ت" to "تـٌـٌٌـ", "خ" to "ځـٌٌـٌٌ", "ج" to "جـ,ـ", "د" to "ڊ", "ذ" to "ڏ", "ز" to "ڒٍ", "س" to "ڛـ,ـ", "ش" to "شُـُـُُـُ", "ص" to "صُـ,ـ", "غ" to "غٍـُـُُـُُُـُُُُـُُُـُُـُ", "ك" to "كُـُ", "ف" to "فُـ,ـ", "م" to "مـْ-ْْ-ْ", "ل" to "لُـِـِِـِِِـِِـِـ", "ن" to "نـِِـِـ", "ه" to "ﮩ", "ح" to "حـًـًًـًًًـًًـًـ", "ع" to "عٌـِـِِـِـ", "ق" to "قٌـ,ـ", "ط" to "طُـٌـٌٌـٌ", "ظ" to "ظً", "ى" to "ﮯ"))),
        StyleMap("زخرفة مشكلة 2", "ar-pro", mapAr(mapOf("ا" to "ا", "ب" to "بٰٰ", "و" to "و", "ي" to "يٰ", "ث" to "ثہٰـ", "ت" to "تہٰ", "خ" to "خ", "ج" to "ج", "د" to "د", "ذ" to "ذ", "ز" to "ز", "س" to "سہٰ", "ش" to "ش", "ص" to "صہٰ", "ع" to "ع", "ك" to "كہٰ", "ف" to "فہٰ", "م" to "م", "ل" to "لہٰ", "ن" to "ن", "ه" to "ه", "ح" to "حہٰ", "غ" to "غ", "ق" to "ق", "ط" to "طہٰ", "ظ" to "ظ", "ى" to "ي"))),
        StyleMap("زخرفة منوعة", "ar-pro", mapAr(mapOf("ا" to "أ", "ب" to "ب", "ت" to "ت", "ث" to "ث", "ج" to "ج", "ح" to "ح", "خ" to "خ", "د" to "د", "ذ" to "ذ", "ر" to "ر", "ز" to "ز", "س" to "س", "ش" to "ش", "ص" to "ص", "ض" to "ض", "ط" to "ط", "ظ" to "ظ", "ع" to "ع", "غ" to "غ", "ف" to "ف", "ق" to "ق", "ك" to "ك", "ل" to "ل", "م" to "م", "ن" to "ن", "ه" to "ه", "و" to "و", "ي" to "ي")))
    )

    val english = listOf(
        StyleMap("مائل عريض (Italic)", "en-pro", mapEn(mapOf("a" to "𝒂", "b" to "𝒃", "c" to "𝒄", "d" to "𝒅", "e" to "𝒆", "f" to "𝒇", "g" to "𝒈", "h" to "𝒉", "i" to "𝒊", "j" to "𝒋", "k" to "𝒌", "l" to "𝒍", "m" to "𝒎", "n" to "𝒏", "o" to "𝒐", "p" to "𝒑", "q" to "𝒒", "r" to "𝒓", "s" to "𝒔", "t" to "𝒕", "u" to "𝒖", "v" to "𝒗", "w" to "𝒘", "x" to "𝒙", "y" to "𝒚", "z" to "𝒛"))),
        StyleMap("كيرسيف (Cursive)", "en-pro", mapEn(mapOf("a" to "𝓪", "b" to "𝓫", "c" to "𝓬", "d" to "𝓭", "e" to "𝓮", "f" to "𝓯", "g" to "𝓰", "h" to "𝓱", "i" to "𝓲", "j" to "𝓳", "k" to "𝓴", "l" to "𝓵", "m" to "𝓶", "n" to "𝓷", "o" to "𝓸", "p" to "𝓹", "q" to "𝓺", "r" to "𝓻", "s" to "𝓼", "t" to "𝓽", "u" to "𝓾", "v" to "𝓿", "w" to "𝔀", "x" to "𝔁", "y" to "𝔂", "z" to "𝔃"))),
        StyleMap("فراكتور برو", "en-pro", mapEn(mapOf("a" to "𝔞", "b" to "𝔟", "c" to "𝔠", "d" to "𝔡", "e" to "𝔢", "f" to "𝔣", "g" to "𝔤", "h" to "𝔥", "i" to "𝔦", "j" to "𝔧", "k" to "𝔨", "l" to "𝔩", "m" to "𝔪", "n" to "𝔫", "o" to "𝔬", "p" to "𝔭", "q" to "𝔮", "r" to "𝔯", "s" to "𝔰", "t" to "𝔱", "u" to "𝔲", "v" to "𝔳", "w" to "𝔴", "x" to "𝔵", "y" to "𝔶", "z" to "𝔷"))),
        StyleMap("مربعات سوداء برو", "en-pro", mapEn(mapOf("a" to "🅐", "b" to "🅑", "c" to "🅒", "d" to "🅓", "e" to "🅔", "f" to "🅕", "g" to "🅖", "h" to "🅗", "i" to "🅘", "j" to "🅙", "k" to "🅚", "l" to "🅛", "m" to "🅜", "n" to "🅝", "o" to "🅞", "p" to "🅟", "q" to "🅠", "r" to "🅡", "s" to "🅢", "t" to "🅣", "u" to "🅤", "v" to "🅥", "w" to "🅦", "x" to "🅧", "y" to "🅨", "z" to "🅩"))),
        StyleMap("مقلوب", "en-pro", mapEn(mapOf("a" to "ɐ", "b" to "q", "c" to "ɔ", "d" to "p", "e" to "ǝ", "f" to "ɟ", "g" to "ɓ", "h" to "ɥ", "i" to "ᴉ", "j" to "ɾ", "k" to "ʞ", "l" to "ן", "m" to "ɯ", "n" to "u", "o" to "o", "p" to "d", "q" to "b", "r" to "ɹ", "s" to "s", "t" to "ʇ", "u" to "n", "v" to "ʌ", "w" to "ʍ", "x" to "x", "y" to "ʎ", "z" to "z"))),
        StyleMap("فونيتيك برو", "en-pro", mapEn(mapOf("a" to "ɑ", "b" to "ɓ", "c" to "ç", "d" to "ɗ", "e" to "ɛ", "f" to "ʄ", "g" to "ɠ", "h" to "ɦ", "i" to "ɨ", "j" to "ʝ", "k" to "ƙ", "l" to "ɭ", "m" to "ɱ", "n" to "ɳ", "o" to "ɵ", "p" to "ƥ", "q" to "ʠ", "r" to "ɽ", "s" to "ʂ", "t" to "ƭ", "u" to "ʉ", "v" to "ʋ", "w" to "ɯ", "x" to "χ", "y" to "ʎ", "z" to "ʐ")))
    )
}
