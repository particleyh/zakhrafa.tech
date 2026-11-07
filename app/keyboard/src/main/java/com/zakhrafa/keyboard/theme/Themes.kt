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

    val sand = KeyboardTheme(
        name = "Sand",
        label = "رملي",
        background = Color.parseColor("#F7F1E6"),
        surface = Color.parseColor("#FFFBF3"),
        keyBackground = Color.parseColor("#FFFDF8"),
        keyTextColor = Color.parseColor("#3B3226"),
        specialKeyBackground = Color.parseColor("#EDE2CF"),
        toolbarBackground = Color.parseColor("#FCF7EE"),
        toolbarTextColor = Color.parseColor("#6B5D45"),
        primaryColor = Color.parseColor("#B07C2F"),
        borderColor = Color.parseColor("#E4D9C4")
    )

    val rose = KeyboardTheme(
        name = "Rose",
        label = "وردي",
        background = Color.parseColor("#FDF0F4"),
        surface = Color.parseColor("#FFFFFF"),
        keyBackground = Color.parseColor("#FFFFFF"),
        keyTextColor = Color.parseColor("#3A1F2A"),
        specialKeyBackground = Color.parseColor("#F6DCE6"),
        toolbarBackground = Color.parseColor("#FFF7FA"),
        toolbarTextColor = Color.parseColor("#7A4658"),
        primaryColor = Color.parseColor("#D6457E"),
        borderColor = Color.parseColor("#F0D3DE")
    )

    val ice = KeyboardTheme(
        name = "Ice",
        label = "جليدي",
        background = Color.parseColor("#EDF5FA"),
        surface = Color.parseColor("#FFFFFF"),
        keyBackground = Color.parseColor("#FFFFFF"),
        keyTextColor = Color.parseColor("#16303F"),
        specialKeyBackground = Color.parseColor("#D8E8F2"),
        toolbarBackground = Color.parseColor("#F6FAFD"),
        toolbarTextColor = Color.parseColor("#45677C"),
        primaryColor = Color.parseColor("#2B8AC7"),
        borderColor = Color.parseColor("#CFE2EE")
    )

    val emerald = KeyboardTheme(
        name = "Emerald",
        label = "زمردي",
        background = Color.parseColor("#0C1F1A"),
        surface = Color.parseColor("#123029"),
        keyBackground = Color.parseColor("#1A4137"),
        keyTextColor = Color.parseColor("#EAFBF4"),
        specialKeyBackground = Color.parseColor("#26564A"),
        toolbarBackground = Color.parseColor("#0F2721"),
        toolbarTextColor = Color.parseColor("#CDEDE1"),
        primaryColor = Color.parseColor("#34D399"),
        borderColor = Color.parseColor("#2C5B4E"),
        isRewarded = true
    )

    val sunset = KeyboardTheme(
        name = "Sunset",
        label = "غروب",
        background = Color.parseColor("#241520"),
        surface = Color.parseColor("#301D2B"),
        keyBackground = Color.parseColor("#3E2536"),
        keyTextColor = Color.parseColor("#FFF1E8"),
        specialKeyBackground = Color.parseColor("#553349"),
        toolbarBackground = Color.parseColor("#2A1926"),
        toolbarTextColor = Color.parseColor("#FFD9C7"),
        primaryColor = Color.parseColor("#FF8A5B"),
        borderColor = Color.parseColor("#58374D"),
        isRewarded = true
    )

    val gold = KeyboardTheme(
        name = "Gold",
        label = "ذهبي",
        background = Color.parseColor("#1E1A12"),
        surface = Color.parseColor("#282216"),
        keyBackground = Color.parseColor("#362E1D"),
        keyTextColor = Color.parseColor("#FFF9E8"),
        specialKeyBackground = Color.parseColor("#4A402A"),
        toolbarBackground = Color.parseColor("#241F15"),
        toolbarTextColor = Color.parseColor("#F3E7C8"),
        primaryColor = Color.parseColor("#E7B53C"),
        borderColor = Color.parseColor("#4E442D"),
        isRewarded = true
    )

    /** Five immediately available themes and six single-ad unlocks. */
    val free = listOf(pearl, midnight, sand, rose, ice)
    val rewarded = listOf(royal, aurora, neon, emerald, sunset, gold)
    val all = free + rewarded
}
