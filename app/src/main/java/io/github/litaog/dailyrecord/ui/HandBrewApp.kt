package io.github.litaog.dailyrecord.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.litaog.dailyrecord.core.data.HandBrewRecordRepository
import io.github.litaog.dailyrecord.ui.calendar.CalendarScreen
import io.github.litaog.dailyrecord.ui.components.HandBrewBottomBar
import io.github.litaog.dailyrecord.ui.record.RecordScreen
import io.github.litaog.dailyrecord.ui.statistics.StatisticsScreen
import io.github.litaog.dailyrecord.ui.theme.Paper50
import java.time.LocalDate
import java.time.YearMonth

private val EarliestSupportedDate: LocalDate = LocalDate.of(1970, 1, 1)
private val EarliestSupportedMonth: YearMonth = YearMonth.from(EarliestSupportedDate)

internal enum class TopDestination {
    Calendar,
    Statistics,
}

@Composable
fun HandBrewApp(
    repository: HandBrewRecordRepository,
    today: LocalDate = LocalDate.now(),
) {
    var destinationName by rememberSaveable { mutableStateOf(TopDestination.Calendar.name) }
    var selectedDateText by rememberSaveable { mutableStateOf<String?>(null) }
    var displayedMonthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }

    val destination = TopDestination.valueOf(destinationName)
    val selectedDate = selectedDateText?.let(LocalDate::parse)
    val displayedMonth = YearMonth.parse(displayedMonthText)
    val recordsFlow = remember(repository, today) {
        repository.observeRecords(EarliestSupportedDate, today.plusDays(1))
    }
    val allRecords by recordsFlow.collectAsState(initial = emptyList())

    BackHandler(enabled = selectedDate != null) {
        selectedDateText = null
    }

    if (selectedDate != null) {
        RecordScreen(
            date = selectedDate,
            today = today,
            repository = repository,
            monthRecords = allRecords.filter { YearMonth.from(it.localDate) == YearMonth.from(selectedDate) },
            onBack = { selectedDateText = null },
            onSaved = { selectedDateText = null },
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Paper50,
        bottomBar = {
            HandBrewBottomBar(
                selected = destination,
                onSelected = { destinationName = it.name },
            )
        },
    ) { contentPadding ->
        when (destination) {
            TopDestination.Calendar -> CalendarScreen(
                month = displayedMonth,
                today = today,
                records = allRecords,
                earliestMonth = EarliestSupportedMonth,
                modifier = Modifier.padding(contentPadding),
                onPreviousMonth = {
                    val previous = displayedMonth.minusMonths(1)
                    if (!previous.isBefore(EarliestSupportedMonth)) displayedMonthText = previous.toString()
                },
                onNextMonth = {
                    val next = displayedMonth.plusMonths(1)
                    if (!next.isAfter(YearMonth.from(today))) displayedMonthText = next.toString()
                },
                onToday = { displayedMonthText = YearMonth.from(today).toString() },
                onDateSelected = { selectedDateText = it.toString() },
            )

            TopDestination.Statistics -> StatisticsScreen(
                today = today,
                records = allRecords,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}
