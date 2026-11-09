package io.github.litaog.dailyrecord.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.components.IconArrowButton
import io.github.litaog.dailyrecord.ui.components.brandIconTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import java.time.YearMonth

@Composable
internal fun MonthHeader(
    month: YearMonth,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onOpenDatePicker: () -> Unit,
    colors: RecordModuleColorTokens,
) {
    val fontScale = LocalDensity.current.fontScale
    val largeText = fontScale >= 1.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (largeText) 108.dp else DailyRecordSizes.MinimumTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonthArrow(
            forward = false,
            description = AppCopy.Calendar.previousMonth,
            enabled = canGoPrevious,
            theme = colors.brandIconTheme,
            onClick = onPreviousMonth,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .sizeIn(minHeight = DailyRecordSizes.MinimumTouchTarget)
                .clip(DailyRecordShapes.Medium)
                .clickable(role = Role.Button, onClick = onOpenDatePicker)
                .semantics {
                    role = Role.Button
                    contentDescription = AppCopy.Calendar.monthSelectionDescription(month)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (largeText) {
                    AppCopy.Calendar.monthTitleMultiline(month)
                } else {
                    AppCopy.Calendar.monthTitle(month)
                },
                color = DailyRecordText,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = if (largeText) 2 else 1,
            )
        }
        MonthArrow(
            forward = true,
            description = AppCopy.Calendar.nextMonth,
            enabled = canGoNext,
            theme = colors.brandIconTheme,
            onClick = onNextMonth,
        )
        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .sizeIn(minWidth = DailyRecordSizes.MinimumTouchTarget, minHeight = DailyRecordSizes.MinimumTouchTarget)
                .clip(DailyRecordShapes.Medium)
                .clickable(role = Role.Button, onClick = onToday)
                .semantics { role = Role.Button; contentDescription = AppCopy.Calendar.backToToday },
            contentAlignment = Alignment.Center,
        ) {
            Text(AppCopy.today, color = colors.primary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MonthArrow(
    forward: Boolean,
    description: String,
    enabled: Boolean,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    onClick: () -> Unit,
) {
    IconArrowButton(
        forward = forward,
        description = description,
        enabled = enabled,
        theme = theme,
        onClick = onClick,
    )
}

@Composable
internal fun MonthlySummary(
    totalCount: Long,
    recordedDays: Int,
) {
    val summary = AppCopy.Calendar.monthSummary(totalCount, recordedDays)
    Text(
        text = summary,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .testTag("calendar_month_summary")
            .semantics { contentDescription = summary },
        color = DailyRecordText,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
    )
}
