package io.github.litaog.dailyrecord.ui.record

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordTimePickerValueTest {
    @Test
    fun wheelValuesWrapAcrossBothEnds() {
        assertEquals(0, wrapTimeWheelValue(24, 24))
        assertEquals(23, wrapTimeWheelValue(-1, 24))
        assertEquals(0, wrapTimeWheelValue(60, 60))
        assertEquals(59, wrapTimeWheelValue(-1, 60))
    }

    @Test
    fun wheelValuesAlwaysUseTwoDigits() {
        assertEquals("00", formatTimeWheelValue(0))
        assertEquals("09", formatTimeWheelValue(9))
        assertEquals("23", formatTimeWheelValue(23))
        assertEquals("59", formatTimeWheelValue(59))
    }

    @Test
    fun fastDragAdvancesAcrossMultipleRowsAndKeepsResidualOffset() {
        val rowHeightPx = 56f

        val moved = advanceTimeWheelOffset(
            state = TimeWheelOffsetState(value = 22, offsetPx = 0f),
            deltaPx = -rowHeightPx * 3.25f,
            rowHeightPx = rowHeightPx,
            valueCount = 24,
        )

        assertEquals(1, moved.value)
        assertEquals(-rowHeightPx * .25f, moved.offsetPx, .001f)
    }

    @Test
    fun fastDragWrapsBackwardAcrossZero() {
        val rowHeightPx = 56f

        val moved = advanceTimeWheelOffset(
            state = TimeWheelOffsetState(value = 1, offsetPx = 0f),
            deltaPx = rowHeightPx * 2.5f,
            rowHeightPx = rowHeightPx,
            valueCount = 24,
        )

        assertEquals(23, moved.value)
        assertEquals(rowHeightPx * .5f, moved.offsetPx, .001f)
    }

    @Test
    fun emptyTimeUsesCurrentLocalMinuteWhileExistingTimeWins() {
        val now = LocalTime.of(17, 42, 59)

        assertEquals(17 * 60 + 42, initialTimePickerMinutes(null, now))
        assertEquals(9 * 60 + 15, initialTimePickerMinutes(9 * 60 + 15, now))
    }
}
