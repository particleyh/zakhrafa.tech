package com.zakhrafa.keyboard

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardPhysicalOrderTest {
    @Test
    fun allKeyRowsUsePhysicalLeftToRightPlacement() {
        assertEquals(View.LAYOUT_DIRECTION_LTR, PHYSICAL_KEY_ROW_DIRECTION)
    }

    @Test
    fun arabicRowsRemainInStandardPhysicalScreenOrder() {
        assertEquals('ض', ARABIC_TOP_ROW_LEFT_TO_RIGHT.first())
        assertEquals('ج', ARABIC_TOP_ROW_LEFT_TO_RIGHT.last())
        assertEquals('ش', ARABIC_HOME_ROW_LEFT_TO_RIGHT.first())
        assertEquals('ط', ARABIC_HOME_ROW_LEFT_TO_RIGHT.last())
    }

    @Test
    fun numberRowsUseDigitsForTheirLanguage() {
        assertEquals("١٢٣٤٥٦٧٨٩٠", ARABIC_NUMBER_ROW_LEFT_TO_RIGHT)
        assertEquals("1234567890", ENGLISH_NUMBER_ROW_LEFT_TO_RIGHT)
    }
}
