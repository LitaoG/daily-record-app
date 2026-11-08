package io.github.litaog.dailyrecord.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.components.BrandIconTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordBorders
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlass

internal enum class DetailTimeTarget {
    Start,
    End,
}

@Composable
internal fun RecordDetailsSection(
    entries: List<RecordDetailDraft>,
    accent: Color,
    theme: BrandIconTheme,
    onCollapse: () -> Unit,
    onTimeClick: (Int, DetailTimeTarget) -> Unit,
    onFeelingToggle: (Int) -> Unit,
    onFeelingChange: (Int, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("record_details_section"),
        verticalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Content),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DailyRecordBorders.Standard)
                .background(DailyRecordDivider),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
                .clip(DailyRecordShapes.Control)
                .clickable(role = Role.Button, onClick = onCollapse)
                .semantics {
                    role = Role.Button
                    contentDescription = AppCopy.Record.detailCollapse
                }
                .padding(vertical = DailyRecordSpacing.Compact),
        ) {
            Text(
                text = AppCopy.Record.detailSectionTitle,
                color = DailyRecordText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = AppCopy.Record.detailSectionHint,
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        // Each occurrence is its own light glass card (ADR-022); the spacing
        // between cards replaces the old divider lines.
        entries.forEachIndexed { index, entry ->
            key(entry.occurrenceIndex) {
                RecordDetailRow(
                    index = index,
                    entry = entry,
                    accent = accent,
                    theme = theme,
                    onTimeClick = onTimeClick,
                    onFeelingToggle = onFeelingToggle,
                    onFeelingChange = onFeelingChange,
                )
            }
        }
    }
}

@Composable
private fun RecordDetailRow(
    index: Int,
    entry: RecordDetailDraft,
    accent: Color,
    theme: BrandIconTheme,
    onTimeClick: (Int, DetailTimeTarget) -> Unit,
    onFeelingToggle: (Int) -> Unit,
    onFeelingChange: (Int, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dailyRecordGlass(
                shape = DailyRecordShapes.Card,
                level = DailyRecordGlassLevel.Base,
            )
            // Tagged before the padding, so the bounds are the whole card.
            .testTag("record_detail_${index + 1}")
            .padding(DailyRecordSpacing.Content),
        verticalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Inline),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Inline),
        ) {
            OccurrenceBadge(number = index + 1, accent = accent)
            Text(
                text = AppCopy.Record.detailOccurrence(index + 1),
                color = DailyRecordText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Measured inside the card's 12dp padding: 276dp here is the old
            // 300dp section width, so the layout still switches below a 340dp
            // wide screen.
            val compactLayout = maxWidth < 276.dp || LocalDensity.current.fontScale >= 1.5f
            Column(
                verticalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Inline),
            ) {
                if (compactLayout) {
                    DetailTimeFields(
                        occurrence = index + 1,
                        entry = entry,
                        accent = accent,
                        theme = theme,
                        onTimeClick = onTimeClick,
                        index = index,
                    )
                    FeelingAction(
                        occurrence = index + 1,
                        expanded = entry.feelingExpanded,
                        accent = accent,
                        theme = theme,
                        modifier = Modifier.align(Alignment.End),
                        onClick = { onFeelingToggle(index) },
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Compact),
                    ) {
                        DetailTimeFields(
                            occurrence = index + 1,
                            entry = entry,
                            accent = accent,
                            theme = theme,
                            onTimeClick = onTimeClick,
                            index = index,
                            modifier = Modifier.weight(1f),
                        )
                        FeelingAction(
                            occurrence = index + 1,
                            expanded = entry.feelingExpanded,
                            accent = accent,
                            theme = theme,
                            onClick = { onFeelingToggle(index) },
                        )
                    }
                }
                if (entry.feelingExpanded) {
                    FeelingEditor(
                        occurrence = index + 1,
                        value = entry.feeling,
                        accent = accent,
                        onValueChange = { onFeelingChange(index, it) },
                    )
                } else if (entry.feeling.isNotEmpty()) {
                    Text(
                        text = entry.feeling,
                        color = DailyRecordTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
