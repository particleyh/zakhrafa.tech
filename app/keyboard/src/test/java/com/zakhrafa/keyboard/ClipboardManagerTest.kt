package com.zakhrafa.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardManagerTest {
    @Test
    fun historyRoundTripsUnicodeMultilineAndDelimiters() {
        val values = listOf(
            "https://zakhrafa.tech/path?a=1:2",
            "سطر أول\nسطر ثانٍ\n\nنهاية",
            "👑 زخرفة 𝕋𝕖𝕩𝕥"
        )

        assertEquals(values, ClipboardHistoryCodec.decode(ClipboardHistoryCodec.encode(values)))
    }

    @Test
    fun malformedHistoryFailsClosed() {
        assertEquals(emptyList<String>(), ClipboardHistoryCodec.decode("10:short"))
        assertEquals(emptyList<String>(), ClipboardHistoryCodec.decode("x:value"))
    }

    @Test
    fun copiedWebLinksAreDetectedWithoutMatchingOrdinaryText() {
        assertTrue(ClipboardLinkDetector.isLink("https://zakhrafa.tech/download?v=55"))
        assertTrue(ClipboardLinkDetector.isLink("www.zakhrafa.tech"))
        assertFalse(ClipboardLinkDetector.isLink("كلمة عادية"))
        assertFalse(ClipboardLinkDetector.isLink("open https://zakhrafa.tech"))
    }
}
