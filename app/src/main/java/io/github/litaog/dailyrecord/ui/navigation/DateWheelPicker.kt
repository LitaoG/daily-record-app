package io.github.litaog.dailyrecord.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import java.time.LocalDate
import java.time.YearMonth

/**
 * Date navigation deliberately uses a different mental model from the home
 * calendar: three compact wheels answer "which date?" without putting a
 * second month grid on top of the first one.
 */
@Composable
internal fun DateWheelPicker(
    selectedDate: LocalDate,
    earliestDate: LocalDate,
    latestDate: LocalDate,
    colors: RecordModuleColorTokens,
    onDateSelected: (LocalDate) -> Unit,
) {
    val years = (earliestDate.year..latestDate.year).toList()
    val firstMonth = if (selectedDate.year == earliestDate.year) earliestDate.monthValue else 1
    val lastMonth = if (selectedDate.year == latestDate.year) latestDate.monthValue else 12
    val months = (firstMonth..lastMonth).toList()
    val days = (1..YearMonth.of(selectedDate.year, selectedDate.monthValue).lengthOfMonth())
        .filter { day ->
            LocalDate.of(selectedDate.year, selectedDate.monthValue, day) in earliestDate..latestDate
        }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DateWheelColumn(
                values = years,
                selectedValue = selectedDate.year,
                label = AppCopy.Navigation.yearUnit,
                valueLabel = AppCopy.Navigation::yearTitle,
                optionEnabled = { true },
                onValueSelected = { year ->
                    onDateSelected(
                        clampWheelDate(
                            year = year,
                            month = selectedDate.monthValue,
                            day = selectedDate.dayOfMonth,
                            earliestDate = earliestDate,
                            latestDate = latestDate,
                        ),
                    )
                },
                colors = colors,
                modifier = Modifier.weight(1.16f).testTag("date_wheel_year"),
            )
            DateWheelColumn(
                values = months,
                selectedValue = selectedDate.monthValue,
                label = AppCopy.Navigation.monthUnit,
                valueLabel = AppCopy.Navigation::monthLabel,
                optionEnabled = { month ->
                    YearMonth.of(selectedDate.year, month) in
                        YearMonth.from(earliestDate)..YearMonth.from(latestDate)
                },
                onValueSelected = { month ->
                    onDateSelected(
                        clampWheelDate(
                            year = selectedDate.year,
                            month = month,
                            day = selectedDate.dayOfMonth,
                            earliestDate = earliestDate,
                            latestDate = latestDate,
                        ),
                    )
                },
                colors = colors,
                modifier = Modifier.weight(1f).testTag("date_wheel_month"),
            )
            DateWheelColumn(
                values = days,
                selectedValue = selectedDate.dayOfMonth,
                label = AppCopy.Navigation.dayUnit,
                valueLabel = AppCopy.Navigation::dayLabel,
                optionEnabled = { day ->
                    LocalDate.of(selectedDate.year, selectedDate.monthValue, day) in earliestDate..latestDate
                },
                onValueSelected = { day ->
                    onDateSelected(
                        clampWheelDate(
                            year = selectedDate.year,
                            month = selectedDate.monthValue,
                            day = day,
                            earliestDate = earliestDate,
                            latestDate = latestDate,
                        ),
                    )
                },
                colors = colors,
                modifier = Modifier.weight(.84f).testTag("date_wheel_day"),
            )
        }
        Text(
            text = AppCopy.Navigation.dateWheelHint,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            textAlign = TextAlign.Center,
        )
    }
}

internal fun clampWheelDate(
    year: Int,
    month: Int,
    day: Int,
    earliestDate: LocalDate,
    latestDate: LocalDate,
): LocalDate {
    val safeMonth = month.coerceIn(1, 12)
    val safeDay = day.coerceIn(1, YearMonth.of(year, safeMonth).lengthOfMonth())
    return LocalDate.of(year, safeMonth, safeDay).coerceIn(earliestDate, latestDate)
}
