package io.github.litaog.dailyrecord.core.statistics

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsBoundsTest {
    @Test
    fun earliestSupportedDateIsJanuaryFirstNineteenSeventy() {
        assertEquals(LocalDate.of(1970, 1, 1), EARLIEST_SUPPORTED_DATE)
    }
}
