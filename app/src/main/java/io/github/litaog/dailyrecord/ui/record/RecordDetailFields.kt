package io.github.litaog.dailyrecord.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.formatMinutesOfDay
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.components.BrandIconTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted

@Composable
internal fun DetailTimeFields(
    occurrence: Int,
    entry: RecordDetailDraft,
    accent: Color,
    theme: BrandIconTheme,
    onTimeClick: (Int, DetailTimeTarget) -> Unit,
    index: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        DetailTimeField(
            occurrence = occurrence,
            target = DetailTimeTarget.Start,
            label = AppCopy.Record.detailStartTime,
            placeholder = AppCopy.Record.detailStartTimeUnset,
            minutes = entry.startMinutes,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = { onTimeClick(index, DetailTimeTarget.Start) },
        )
        TimeRangeArrow(
            theme = theme,
            modifier = Modifier.size(width = 24.dp, height = 24.dp),
        )
        DetailTimeField(
            occurrence = occurrence,
            target = DetailTimeTarget.End,
            label = AppCopy.Record.detailEndTime,
            placeholder = AppCopy.Record.detailEndTimeUnset,
            minutes = entry.endMinutes,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = { onTimeClick(index, DetailTimeTarget.End) },
        )
    }
}

@Composable
internal fun OccurrenceBadge(
    number: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(accent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DetailTimeField(
    occurrence: Int,
    target: DetailTimeTarget,
    label: String,
    placeholder: String,
    minutes: Int?,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
            // Corners stay concentric with the 24dp card and its 12dp padding.
            .clip(DailyRecordShapes.Compact)
            // A light flat tint instead of a white box with an outline; the
            // text on it is graphite or muted gray, which stays AA.
            .background(accent.copy(alpha = .08f))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = AppCopy.Record.detailTimeDescription(
                    occurrence = occurrence,
                    label = label,
                    value = minutes?.let(::formatMinutesOfDay) ?: placeholder,
                )
            }
            .testTag("record_detail_${occurrence}_${target.name.lowercase()}_time")
            .padding(horizontal = DailyRecordSpacing.Compact),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = minutes?.let(::formatMinutesOfDay) ?: placeholder,
            color = if (minutes == null) DailyRecordTextMuted else DailyRecordText,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TimeRangeArrow(
    theme: BrandIconTheme,
    modifier: Modifier = Modifier,
) {
    BrandIcon(
        asset = BrandIconAsset.Next,
        theme = theme,
        modifier = modifier,
    )
}
