package io.github.litaog.dailyrecord.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateNavigationDialog(
    initialDate: LocalDate,
    earliestDate: LocalDate,
    latestDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val earliestMillis = earliestDate.toUtcDateMillis()
    val latestMillis = latestDate.toUtcDateMillis()
    val selectableDates = remember(earliestMillis, latestMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis in earliestMillis..latestMillis

            override fun isSelectableYear(year: Int): Boolean =
                year in earliestDate.year..latestDate.year
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.coerceIn(earliestDate, latestDate).toUtcDateMillis(),
        yearRange = earliestDate.year..latestDate.year,
        selectableDates = selectableDates,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let { onDateSelected(utcDateMillisToLocalDate(it)) }
                },
            ) {
                Text("查看此日期")
            }
        },
    ) {
        DatePicker(
            state = state,
            title = {
                Text(
                    text = "选择要查看的年份和日期",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 12.dp),
                )
            },
            showModeToggle = true,
        )
    }
}
