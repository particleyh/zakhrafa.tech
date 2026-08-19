package com.zakhrafa.keyboard.theme

import android.graphics.Color

data class KeyboardTheme(
    val name: String,
    val label: String,
    val background: Int,
    val surface: Int,
    val keyBackground: Int,
    val keyTextColor: Int,
    val specialKeyBackground: Int,
    val toolbarBackground: Int,
    val toolbarTextColor: Int,
    val primaryColor: Int,
    val borderColor: Int,
    val isRewarded: Boolean = false
)

object Themes {
    val pearl = KeyboardTheme(
        name = "Pearl",
        label = "لؤلؤي",
        background = Color.parseColor("#F4F6F8"),
        surface = Color.parseColor("#FFFFFF"),
        keyBackground = Color.parseColor("#FFFFFF"),
        keyTextColor = Color.parseColor("#20242B"),
        specialKeyBackground = Color.parseColor("#E8EDF2"),
        toolbarBackground = Color.parseColor("#FAFBFC"),
        toolbarTextColor = Color.parseColor("#4A5565"),
        primaryColor = Color.parseColor("#087E6C"),
        borderColor = Color.parseColor("#DDE3EA")
    )

    val midnight = KeyboardTheme(
        name = "Midnight",
        label = "منتصف الليل",
        background = Color.parseColor("#101521"),
        surface = Color.parseColor("#182030"),
        keyBackground = Color.parseColor("#222D40"),
        keyTextColor = Color.parseColor("#F8FAFC"),
        specialKeyBackground = Color.parseColor("#33425C"),
        toolbarBackground = Color.parseColor("#141C2A"),
        toolbarTextColor = Color.parseColor("#E5E7EB"),
        primaryColor = Color.parseColor("#63B3FF"),
        borderColor = Color.parseColor("#33425A")
    )

    val royal = KeyboardTheme(
        name = "Royal",
        label = "ملكي",
        background = Color.parseColor("#1B1626"),
        surface = Color.parseColor("#241E33"),
        keyBackground = Color.parseColor("#302842"),
        keyTextColor = Color.parseColor("#F8FAFC"),
        specialKeyBackground = Color.parseColor("#42365A"),
        toolbarBackground = Color.parseColor("#221C30"),
        toolbarTextColor = Color.parseColor("#F8FAFC"),
        primaryColor = Color.parseColor("#C4A7FF"),
        borderColor = Color.parseColor("#4B3A65"),
        isRewarded = true
    )

    val aurora = KeyboardTheme(
        name = "Aurora",
        label = "شفق",
        background = Color.parseColor("#15232B"),
        surface = Color.parseColor("#1C3038"),
        keyBackground = Color.parseColor("#26424A"),
        keyTextColor = Color.parseColor("#F0FDFA"),
        specialKeyBackground = Color.parseColor("#385B64"),
        toolbarBackground = Color.parseColor("#17282F"),
        toolbarTextColor = Color.parseColor("#D8FFF6"),
        primaryColor = Color.parseColor("#5EEAD4"),
        borderColor = Color.parseColor("#3B6269"),
        isRewarded = true
    )

    val neon = KeyboardTheme(
        name = "Neon",
        label = "نيون",
        background = Color.parseColor("#171327"),
        surface = Color.parseColor("#231B38"),
        keyBackground = Color.parseColor("#31264C"),
        keyTextColor = Color.parseColor("#FFF7FF"),
        specialKeyBackground = Color.parseColor("#49366E"),
        toolbarBackground = Color.parseColor("#1C1630"),
        toolbarTextColor = Color.parseColor("#F1DEFF"),
        primaryColor = Color.parseColor("#F472D0"),
        borderColor = Color.parseColor("#503B76"),
        isRewarded = true
    )

    /** Two immediately available themes and three single-ad unlocks. */
    val free = listOf(pearl, midnight)
    val rewarded = listOf(royal, aurora, neon)
    val all = free + rewarded
}
