package io.github.litaog.dailyrecord.core.statistics

import io.github.litaog.dailyrecord.core.common.AppLanguageState
import io.github.litaog.dailyrecord.core.common.EnStrings
import io.github.litaog.dailyrecord.core.common.ZhStrings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsPeriodTest {
    @After
    fun resetLanguage() {
        AppLanguageState.current = ZhStrings
    }

    @Test
    fun labelFollowsActiveLanguage() {
        assertEquals("周", StatisticsPeriod.Week.label)
        assertEquals("月", StatisticsPeriod.Month.label)
        assertEquals("年", StatisticsPeriod.Year.label)
        assertEquals("全部", StatisticsPeriod.All.label)

        AppLanguageState.current = EnStrings
        try {
            assertEquals("Wk", StatisticsPeriod.Week.label)
            assertEquals("Mo", StatisticsPeriod.Month.label)
            assertEquals("Yr", StatisticsPeriod.Year.label)
            assertEquals("All", StatisticsPeriod.All.label)
        } finally {
            AppLanguageState.current = ZhStrings
        }
    }
}
