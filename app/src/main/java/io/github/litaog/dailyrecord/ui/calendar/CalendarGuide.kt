package io.github.litaog.dailyrecord.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.RecordModuleUiSpec
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceDisabled
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.RecordVisualState

@Composable
internal fun CalendarGuide(
    moduleSpec: RecordModuleUiSpec,
    largeText: Boolean,
) {
    val legendItems = listOf(
        CalendarLegendItem.Unset,
        CalendarLegendItem.Disabled,
        CalendarLegendItem.Zero,
        CalendarLegendItem.Recorded,
    )
    val legendRows = if (largeText) legendItems.chunked(2) else listOf(legendItems)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 2.dp)
            .testTag("calendar_guide")
            .clearAndSetSemantics {
                contentDescription =
                    AppCopy.Calendar.legendDescription(moduleSpec.semanticCountLabel)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DailyRecordDivider),
        )
        Text(
            text = AppCopy.Calendar.recordHint,
            color = DailyRecordTextSecondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        legendRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { item ->
                    CalendarLegendEntry(
                        item = item,
                        colors = moduleSpec.colors,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private enum class CalendarLegendItem {
    Unset,
    Disabled,
    Zero,
    Recorded,
}

/** Visible legend label; resolved at call time so language switches apply. */
private val CalendarLegendItem.label: String
    get() = when (this) {
        CalendarLegendItem.Unset -> AppCopy.Calendar.unset
        CalendarLegendItem.Disabled -> AppCopy.Calendar.future
        CalendarLegendItem.Zero -> AppCopy.Calendar.zero
        CalendarLegendItem.Recorded -> AppCopy.Calendar.recorded
    }

@Composable
private fun CalendarLegendEntry(
    item: CalendarLegendItem,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarLegendMarker(item = item, colors = colors)
        Spacer(Modifier.width(5.dp))
        Text(
            text = item.label,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun CalendarLegendMarker(
    item: CalendarLegendItem,
    colors: RecordModuleColorTokens,
) {
    when (item) {
        CalendarLegendItem.Unset -> Box(
            Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.colorsFor(RecordVisualState.Unset).background),
        )
        CalendarLegendItem.Disabled -> Box(
            Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DailyRecordSurfaceDisabled),
        )
        CalendarLegendItem.Zero -> Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DailyRecordSurface)
                .border(1.dp, colors.primary, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(5.dp)
                    .border(1.dp, colors.primary, CircleShape),
            )
        }
        CalendarLegendItem.Recorded -> Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf(colors.soft, colors.medium, colors.intense, colors.primary).forEach { color ->
                Box(
                    Modifier
                        .size(width = 5.dp, height = 14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color),
                )
            }
        }
    }
}
