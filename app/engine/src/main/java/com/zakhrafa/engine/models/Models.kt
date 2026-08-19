package com.zakhrafa.engine.models

enum class PlatformType {
    NONE, PUBG, FREEFIRE, FACEBOOK, INSTAGRAM, TIKTOK, ROBLOX, DISCORD
}

data class DecorationResult(
    val text: String,
    val style: String,
    val category: String
)

data class StyleMap(
    val name: String,
    val category: String,
    val map: Map<String, String> = emptyMap(),
    val range: Triple<Int, Int, Int?>? = null,
    val special: String? = null
)

data class ComplexDecoration(
    val left: String,
    val right: String,
    val map: String
)

data class SideDecoration(
    val left: String,
    val right: String,
    val label: String
)

data class Platform(
    val symbols: List<String>,
    val frames: List<Pair<String, String>>
)
