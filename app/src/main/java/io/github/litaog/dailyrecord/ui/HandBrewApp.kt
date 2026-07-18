package io.github.litaog.dailyrecord.ui

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
import io.github.litaog.dailyrecord.ui.navigation.DateNavigationDialog
import io.github.litaog.dailyrecord.ui.navigation.shiftMonthAnchor
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
    var browseDateText by rememberSaveable { mutableStateOf(today.toString()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val currentMonth = YearMonth.from(today)
    val destination = TopDestination.entries.firstOrNull { it.name == destinationName }
        ?: TopDestination.Calendar
    val selectedDate = selectedDateText
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?.takeIf { it in EarliestSupportedDate..today }
    val browseDate = runCatching { LocalDate.parse(browseDateText) }
        .getOrDefault(today)
        .takeIf { it in EarliestSupportedDate..today }
        ?: today
    val displayedMonth = YearMonth.from(browseDate)
    val recordsFlow = remember(repository, today) {
        repository.observeRecords(EarliestSupportedDate, today.plusDays(1))
    }
    val allRecords by recordsFlow.collectAsState(initial = emptyList())

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
                focusedDate = browseDate,
                today = today,
                records = allRecords,
                earliestMonth = EarliestSupportedMonth,
                modifier = Modifier.padding(contentPadding),
                onPreviousMonth = {
                    val previous = displayedMonth.minusMonths(1)
                    if (!previous.isBefore(EarliestSupportedMonth)) {
                        browseDateText = shiftMonthAnchor(
                            browseDate,
                            months = -1,
                            earliestDate = EarliestSupportedDate,
                            latestDate = today,
                        ).toString()
                    }
                },
                onNextMonth = {
                    val next = displayedMonth.plusMonths(1)
                    if (!next.isAfter(currentMonth)) {
                        browseDateText = shiftMonthAnchor(
                            browseDate,
                            months = 1,
                            earliestDate = EarliestSupportedDate,
                            latestDate = today,
                        ).toString()
                    }
                },
                onToday = { browseDateText = today.toString() },
                onOpenDatePicker = { showDatePicker = true },
                onDateSelected = {
                    browseDateText = it.toString()
                    selectedDateText = it.toString()
                },
            )

            TopDestination.Statistics -> StatisticsScreen(
                today = today,
                anchorDate = browseDate,
                earliestDate = EarliestSupportedDate,
                records = allRecords,
                modifier = Modifier.padding(contentPadding),
                onAnchorDateChanged = { browseDateText = it.toString() },
                onOpenDatePicker = { showDatePicker = true },
                onOpenCalendar = { destinationName = TopDestination.Calendar.name },
            )
        }
    }

    if (showDatePicker) {
        DateNavigationDialog(
            initialDate = browseDate,
            earliestDate = EarliestSupportedDate,
            latestDate = today,
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                browseDateText = it.toString()
                showDatePicker = false
            },
        )
    }
}
