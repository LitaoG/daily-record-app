package io.github.litaog.dailyrecord.ui.record

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
}
