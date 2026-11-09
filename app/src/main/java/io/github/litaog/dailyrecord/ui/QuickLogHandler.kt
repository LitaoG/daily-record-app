package io.github.litaog.dailyrecord.ui

import androidx.compose.material3.SnackbarHostState
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.statistics.EARLIEST_SUPPORTED_DATE
import io.github.litaog.dailyrecord.ui.calendar.QuickLogAction
import io.github.litaog.dailyrecord.ui.calendar.nextCount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

internal fun todayQuickLogHandler(
    scope: CoroutineScope,
    mutex: Mutex,
    selectedController: RecordModuleController,
    effectiveToday: LocalDate,
    snackbarHostState: SnackbarHostState,
): (LocalDate, QuickLogAction) -> Unit = quickLog@{ date, action ->
    // Same bounds as the record page (also used by the long-press
    // stepper): never write a future or pre-1970 date.
    if (date.isAfter(effectiveToday) || date.isBefore(EARLIEST_SUPPORTED_DATE)) return@quickLog
    val controller = selectedController
    scope.launch {
        try {
            mutex.withLock {
                val current = controller.observeRecord(date).first()?.count
                val next = action.nextCount(current)
                if (next != null) controller.saveRecord(date, next)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            snackbarHostState.showSnackbar(AppCopy.Record.saveFailure)
        }
    }
}
