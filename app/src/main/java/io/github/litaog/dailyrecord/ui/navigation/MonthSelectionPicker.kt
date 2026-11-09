package io.github.litaog.dailyrecord.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.components.brandIconTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import java.time.YearMonth

@Composable
internal fun MonthSelectionPicker(
    selectedMonth: YearMonth,
    earliestMonth: YearMonth,
    latestMonth: YearMonth,
    colors: RecordModuleColorTokens,
    onMonthSelected: (YearMonth) -> Unit,
) {
    var displayedYear by remember(selectedMonth.year) { mutableIntStateOf(selectedMonth.year) }
    var showYears by remember(selectedMonth.year) { mutableStateOf(false) }

    if (showYears) {
        YearWheelPicker(
            selectedYear = displayedYear,
            years = (earliestMonth.year..latestMonth.year).toList(),
            colors = colors,
            onYearSelected = { year ->
                displayedYear = year
                val candidate = YearMonth.of(year, selectedMonth.monthValue).coerceIn(earliestMonth, latestMonth)
                onMonthSelected(candidate)
                showYears = false
            },
        )
        return
    }

    val canGoBack = displayedYear > earliestMonth.year
    val canGoForward = displayedYear < latestMonth.year
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            YearArrow(
                forward = false,
                enabled = canGoBack,
                colors = colors,
                onClick = { displayedYear-- },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClick = { showYears = true })
                    .semantics {
                        role = Role.Button
                        contentDescription = AppCopy.Navigation.switchYearDescription(displayedYear)
                    }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = AppCopy.Navigation.yearTitle(displayedYear),
                        color = DailyRecordText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    DownChevronIcon(theme = colors.brandIconTheme)
                }
            }
            YearArrow(
                forward = true,
                enabled = canGoForward,
                colors = colors,
                onClick = { displayedYear++ },
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DailyRecordDivider.copy(alpha = .42f)),
        )
        Spacer(Modifier.height(10.dp))
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().height(252.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items((1..12).map { YearMonth.of(displayedYear, it) }, key = { it.toString() }) { month ->
            val enabled = month in earliestMonth..latestMonth
            val selected = month == selectedMonth
            Box(
                modifier = Modifier
                    .height(54.dp)
                    .clip(DailyRecordShapes.Full)
                    .background(if (selected) colors.primary.copy(alpha = .92f) else Color.Transparent)
                    .clickable(enabled = enabled, role = Role.Button) { onMonthSelected(month) }
                    .semantics {
                        role = Role.Button
                        this.selected = selected
                        contentDescription = AppCopy.Navigation.monthDescription(month)
                        if (!enabled) disabled()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = AppCopy.Navigation.monthLabel(month.monthValue),
                    color = when {
                        selected -> colors.onPrimary
                        enabled -> DailyRecordTextSecondary
                        else -> DailyRecordTextMuted
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}
