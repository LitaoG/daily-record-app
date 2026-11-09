package io.github.litaog.dailyrecord.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.LiquidGlassElevation
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.appleLiquidGlass
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun PinnedStickySaveActionBar(
    saveLabel: String,
    saveEnabled: Boolean,
    clearVisible: Boolean,
    clearEnabled: Boolean,
    accent: Color,
    colors: RecordModuleColorTokens,
    onSave: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .appleLiquidGlass(
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                glowColor = colors.primary,
                elevation = LiquidGlassElevation.Elevated,
            )
            .testTag("record_actions"),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = DailyRecordSpacing.ScreenHorizontal,
                    end = DailyRecordSpacing.ScreenHorizontal,
                    top = DailyRecordSpacing.Content,
                    bottom = DailyRecordSpacing.ScreenVertical,
                ),
            verticalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Content),
        ) {
            PrimaryActionButton(
                label = saveLabel,
                enabled = saveEnabled,
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_record_button"),
                accent = accent,
            )
            if (clearVisible) {
                RecordTextAction(
                    label = AppCopy.Record.clear,
                    enabled = clearEnabled,
                    accent = accent,
                    onClick = onClear,
                )
            }
        }
    }
}

@Composable
internal fun RecordMonthSummaryCard(
    date: LocalDate,
    storedMonthCount: Long,
    storedMonthDays: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DailyRecordSpacing.Inline)
            .height(1.dp)
            .background(DailyRecordDivider),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("record_month_summary"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Compact),
    ) {
        Text(
            text = AppCopy.Record.monthSaved(YearMonth.from(date).monthValue),
            color = DailyRecordTextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            AppCopy.Record.monthSummary(storedMonthCount, storedMonthDays),
            color = DailyRecordText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}
