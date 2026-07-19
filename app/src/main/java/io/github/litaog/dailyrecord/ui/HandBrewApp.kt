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
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.account.AccountDialog
import io.github.litaog.dailyrecord.ui.account.AccountTopBar
import io.github.litaog.dailyrecord.ui.account.LocalAccountTopBar
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
    today: LocalDate? = null,
    accountEmail: String? = null,
    syncStatus: SyncStatus = SyncStatus.NotConfigured,
    onSyncNow: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onSignIn: (() -> Unit)? = null,
) {
    val effectiveToday = today ?: rememberCurrentDate()
    var destinationName by rememberSaveable { mutableStateOf(TopDestination.Calendar.name) }
    var selectedDateText by rememberSaveable { mutableStateOf<String?>(null) }
    var browseDateText by rememberSaveable { mutableStateOf(effectiveToday.toString()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showAccountDialog by rememberSaveable { mutableStateOf(false) }

    val currentMonth = YearMonth.from(effectiveToday)
    val destination = TopDestination.entries.firstOrNull { it.name == destinationName }
        ?: TopDestination.Calendar
    val selectedDate = selectedDateText
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?.takeIf { it in EarliestSupportedDate..effectiveToday }
    val browseDate = runCatching { LocalDate.parse(browseDateText) }
        .getOrDefault(effectiveToday)
        .takeIf { it in EarliestSupportedDate..effectiveToday }
        ?: effectiveToday
    val displayedMonth = YearMonth.from(browseDate)
    val recordsFlow = remember(repository, effectiveToday) {
        repository.observeRecords(EarliestSupportedDate, effectiveToday.plusDays(1))
    }
    val allRecords by recordsFlow.collectAsState(initial = emptyList())

    if (selectedDate != null) {
        RecordScreen(
            date = selectedDate,
            today = effectiveToday,
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
        topBar = {
            if (accountEmail != null) {
                AccountTopBar(status = syncStatus, onClick = { showAccountDialog = true })
            } else if (onSignIn != null) {
                LocalAccountTopBar(onClick = onSignIn)
            }
        },
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
                today = effectiveToday,
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
                            latestDate = effectiveToday,
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
                            latestDate = effectiveToday,
                        ).toString()
                    }
                },
                onToday = { browseDateText = effectiveToday.toString() },
                onOpenDatePicker = { showDatePicker = true },
                onDateSelected = {
                    browseDateText = it.toString()
                    selectedDateText = it.toString()
                },
            )

            TopDestination.Statistics -> StatisticsScreen(
                today = effectiveToday,
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
            latestDate = effectiveToday,
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                browseDateText = it.toString()
                showDatePicker = false
            },
        )
    }

    if (showAccountDialog && accountEmail != null) {
        AccountDialog(
            email = accountEmail,
            status = syncStatus,
            onSyncNow = onSyncNow,
            onSignOut = onSignOut,
            onDismiss = { showAccountDialog = false },
        )
    }
}
