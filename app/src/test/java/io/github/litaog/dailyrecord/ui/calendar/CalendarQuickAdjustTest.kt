package io.github.litaog.dailyrecord.ui.calendar

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarQuickAdjustTest {
    private val window = IntSize(1080, 2000)
    private val popup = IntSize(600, 200)
    private val gap = 10

    @Test
    fun stepperSitsCenteredAboveACellWithRoom() {
        val cell = IntRect(left = 400, top = 900, right = 540, bottom = 1040)

        // Cell center x is 470, so the 600px popup starts at 170; it ends
        // 10px above the cell top.
        assertEquals(IntOffset(170, 690), quickAdjustPopupOffset(cell, window, popup, gap))
    }

    @Test
    fun stepperStaysInsideTheWindowForEdgeColumns() {
        val mondayCell = IntRect(left = 0, top = 900, right = 140, bottom = 1040)
        val sundayCell = IntRect(left = 940, top = 900, right = 1080, bottom = 1040)

        assertEquals(0, quickAdjustPopupOffset(mondayCell, window, popup, gap).x)
        assertEquals(480, quickAdjustPopupOffset(sundayCell, window, popup, gap).x)
    }

    @Test
    fun stepperDropsBelowTheCellWhenThereIsNoRoomAbove() {
        val firstRowCell = IntRect(left = 400, top = 100, right = 540, bottom = 240)

        assertEquals(250, quickAdjustPopupOffset(firstRowCell, window, popup, gap).y)
    }

    @Test
    fun stepperNeverLeavesTheBottomOfASmallWindow() {
        val smallWindow = IntSize(1080, 300)
        val cell = IntRect(left = 400, top = 100, right = 540, bottom = 240)

        assertEquals(100, quickAdjustPopupOffset(cell, smallWindow, popup, gap).y)
    }
}
