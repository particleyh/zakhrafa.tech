package com.zakhrafa.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPolicyTest {

    private val now = 1_000_000L

    private fun show(
        sessionCopies: Int,
        sessionShown: Int = 0,
        lastShownAt: Long = 0L,
        ratingDialogVisible: Boolean = false,
        adFree: Boolean = false
    ) = AdPolicy.shouldShow(
        sessionCopies = sessionCopies,
        sessionShown = sessionShown,
        lastShownAt = lastShownAt,
        now = now,
        ratingDialogVisible = ratingDialogVisible,
        adFree = adFree
    )

    @Test
    fun `no interstitial during the rating grace period`() {
        for (copies in 0 until AdPolicy.FIRST_INTERSTITIAL_AT_COPY) {
            assertFalse(show(sessionCopies = copies))
        }
    }

    @Test
    fun `first interstitial lands on the fifth copy`() {
        assertTrue(show(sessionCopies = AdPolicy.FIRST_INTERSTITIAL_AT_COPY))
    }

    @Test
    fun `then every third copy`() {
        assertFalse(show(sessionCopies = 6))
        assertFalse(show(sessionCopies = 7))
        assertTrue(show(sessionCopies = 8))
        assertTrue(show(sessionCopies = 11))
    }

    @Test
    fun `never while the rating dialog is open`() {
        assertFalse(show(sessionCopies = 5, ratingDialogVisible = true))
    }

    @Test
    fun `never when ad-free mode is active`() {
        assertFalse(show(sessionCopies = 5, adFree = true))
    }

    @Test
    fun `sixty second floor between interstitials`() {
        val justShown = now - AdPolicy.MIN_GAP_MS + 1
        assertFalse(show(sessionCopies = 8, lastShownAt = justShown))
        val longEnough = now - AdPolicy.MIN_GAP_MS
        assertTrue(show(sessionCopies = 8, lastShownAt = longEnough))
    }

    @Test
    fun `session cap of three`() {
        assertTrue(show(sessionCopies = 11, sessionShown = 2))
        assertFalse(show(sessionCopies = 14, sessionShown = AdPolicy.MAX_PER_SESSION))
    }
}
