package io.github.litaog.dailyrecord.ui.navigation

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DateNavigationTest {
    private val earliest = LocalDate.of(1970, 1, 1)
    private val today = LocalDate.of(2026, 7, 17)

    @Test
    fun jumpButtonLabelMatchesSelectionGranularity() {
        assertEquals("跳转到此日", navigationJumpLabel(DateNavigationSelection.Date))
        assertEquals("跳转到此月", navigationJumpLabel(DateNavigationSelection.Month))
        assertEquals("跳转到此年", navigationJumpLabel(DateNavigationSelection.Year))
    }

    @Test
    fun wheelDateClampsMonthDayAndSupportedRange() {
        assertEquals(
            LocalDate.of(2025, 2, 28),
            clampWheelDate(
                year = 2025,
                month = 2,
                day = 31,
                earliestDate = earliest,
                latestDate = today,
            ),
        )
        assertEquals(
            earliest,
            clampWheelDate(
                year = 1969,
                month = 12,
                day = 31,
                earliestDate = earliest,
                latestDate = today,
            ),
        )
        assertEquals(
            today,
            clampWheelDate(
                year = 2026,
                month = 12,
                day = 31,
                earliestDate = earliest,
                latestDate = today,
            ),
        )
    }
}
