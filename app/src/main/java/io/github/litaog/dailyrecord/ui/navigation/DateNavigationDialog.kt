package io.github.litaog.dailyrecord.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.components.DailyRecordDialog
import io.github.litaog.dailyrecord.ui.components.OutlineActionButton
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

internal enum class DateNavigationSelection { Date, Month, Year }

@Composable
internal fun DateNavigationDialog(
    initialDate: LocalDate,
    earliestDate: LocalDate,
    latestDate: LocalDate,
    colors: RecordModuleColorTokens = HandBrewColorTokens,
    selection: DateNavigationSelection = DateNavigationSelection.Date,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val boundedInitial = initialDate.coerceIn(earliestDate, latestDate)
    var selectedDate by remember(initialDate, earliestDate, latestDate) { mutableStateOf(boundedInitial) }

    DailyRecordDialog(
        title = if (selection == DateNavigationSelection.Year) {
            AppCopy.Navigation.selectYear
        } else {
            AppCopy.Navigation.title
        },
        subtitle = when (selection) {
            DateNavigationSelection.Date -> AppCopy.Navigation.dateWheelSubtitle
            DateNavigationSelection.Month -> AppCopy.Navigation.monthSubtitle
            DateNavigationSelection.Year -> null
        },
        testTag = "date_navigation_dialog",
        onDismissRequest = onDismiss,
    ) {
        when (selection) {
            DateNavigationSelection.Date -> SelectedDateSummary(selectedDate, colors)
            DateNavigationSelection.Month -> SelectedMonthSummary(YearMonth.from(selectedDate), colors)
            DateNavigationSelection.Year -> Unit
        }
        Spacer(Modifier.height(if (selection == DateNavigationSelection.Year) 8.dp else 16.dp))

        when (selection) {
            DateNavigationSelection.Date -> {
                DateWheelPicker(
                    selectedDate = selectedDate,
                    earliestDate = earliestDate,
                    latestDate = latestDate,
                    colors = colors,
                    onDateSelected = { selectedDate = it },
                )
            }

            DateNavigationSelection.Month -> MonthSelectionPicker(
                selectedMonth = YearMonth.from(selectedDate),
                earliestMonth = YearMonth.from(earliestDate),
                latestMonth = YearMonth.from(latestDate),
                colors = colors,
                onMonthSelected = { month ->
                    selectedDate = month
                        .atDay(selectedDate.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
                        .coerceIn(earliestDate, latestDate)
                },
            )

            DateNavigationSelection.Year -> YearWheelPicker(
                selectedYear = selectedDate.year,
                years = (earliestDate.year..latestDate.year).toList(),
                colors = colors,
                onYearSelected = { year ->
                    val month = selectedDate.monthValue
                    selectedDate = LocalDate.of(
                        year,
                        month,
                        minOf(selectedDate.dayOfMonth, YearMonth.of(year, month).lengthOfMonth()),
                    ).coerceIn(earliestDate, latestDate)
                },
            )
        }

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlineActionButton(
                AppCopy.Auth.cancel,
                onDismiss,
                Modifier.weight(1f),
                accent = colors.primary,
            )
            PrimaryActionButton(
                label = navigationJumpLabel(selection),
                onClick = { onDateSelected(selectedDate) },
                modifier = Modifier.weight(1.35f),
                accent = colors.primary,
            )
        }
    }
}

internal fun navigationJumpLabel(selection: DateNavigationSelection): String = when (selection) {
    DateNavigationSelection.Date -> AppCopy.Navigation.jumpToDate
    DateNavigationSelection.Month -> AppCopy.Navigation.jumpToMonth
    DateNavigationSelection.Year -> AppCopy.Navigation.jumpToYear
}

@Composable
private fun SelectedDateSummary(
    date: LocalDate,
    colors: RecordModuleColorTokens,
) {
    val locale = AppCopy.DISPLAY_LOCALE
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val largeText = LocalDensity.current.fontScale >= 1.4f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.soft.copy(alpha = .54f))
            .border(1.dp, colors.primary.copy(alpha = .20f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(AppCopy.Navigation.selected, color = DailyRecordTextSecondary, style = MaterialTheme.typography.labelSmall)
        if (largeText) {
            Text(
                text = AppCopy.Navigation.dateText(date),
                color = DailyRecordText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = weekday,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
        } else {
            Text(
                text = AppCopy.Navigation.dateLabel(date, weekday),
                color = DailyRecordText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SelectedMonthSummary(
    month: YearMonth,
    colors: RecordModuleColorTokens,
) {
    SelectionSummary(
        value = AppCopy.Navigation.monthTitle(month),
        colors = colors,
    )
}

@Composable
private fun SelectionSummary(
    value: String,
    colors: RecordModuleColorTokens,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.soft.copy(alpha = .54f))
            .border(1.dp, colors.primary.copy(alpha = .20f), RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(AppCopy.Navigation.selected, color = colors.primary, style = MaterialTheme.typography.labelSmall)
        Text(
            text = value,
            color = DailyRecordText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
