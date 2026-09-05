package io.github.litaog.dailyrecord.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.formatMinutesOfDay
import io.github.litaog.dailyrecord.core.model.visibleCharacterCount
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.components.BrandIconTheme
import io.github.litaog.dailyrecord.ui.components.ChevronIcon
import io.github.litaog.dailyrecord.ui.components.brandIconTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordBorders
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                if (index < entries.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(DailyRecordBorders.Standard)
                            .background(DailyRecordDivider)
                            .testTag("record_detail_divider_${index + 1}"),
                    )
                }
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
            .testTag("record_detail_${index + 1}"),
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
            val compactLayout = maxWidth < 300.dp || LocalDensity.current.fontScale >= 1.5f
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

@Composable
private fun DetailTimeFields(
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
private fun OccurrenceBadge(
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
            .clip(DailyRecordShapes.Control)
            .background(DailyRecordSurface)
            .border(
                DailyRecordBorders.Standard,
                accent.copy(alpha = .20f),
                DailyRecordShapes.Control,
            )
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
            style = MaterialTheme.typography.labelLarge,
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

@Composable
private fun FeelingAction(
    occurrence: Int,
    expanded: Boolean,
    accent: Color,
    theme: BrandIconTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val label = when {
        expanded -> AppCopy.Record.detailCollapseFeeling
        else -> AppCopy.Record.detailWriteFeeling
    }
    Row(
        modifier = modifier
            .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
            .clip(DailyRecordShapes.Control)
            .background(DailyRecordSurface)
            .border(
                DailyRecordBorders.Standard,
                accent.copy(alpha = .62f),
                DailyRecordShapes.Control,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = AppCopy.Record.detailFeelingActionDescription(occurrence, label)
            }
            .testTag("record_detail_${occurrence}_feeling")
            .padding(horizontal = DailyRecordSpacing.Compact),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Compact),
    ) {
        BrandIcon(
            asset = BrandIconAsset.Edit,
            theme = theme,
            // The edit asset is cropped to the pen glyph: the box hugs the
            // shape, so the icon sits at the content start and the label
            // keeps even breathing room on both sides.
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            color = accent,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun FeelingEditor(
    occurrence: Int,
    value: String,
    accent: Color,
    onValueChange: (String) -> Unit,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            delay(300)
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                }
                .clip(DailyRecordShapes.Control)
                .background(DailyRecordSurface)
                .border(DailyRecordBorders.Standard, accent.copy(alpha = .65f), DailyRecordShapes.Control)
                .semantics {
                    contentDescription = AppCopy.Record.detailFeelingEditorDescription(occurrence)
                }
                .testTag("record_detail_${occurrence}_feeling_editor")
                .padding(horizontal = DailyRecordSpacing.Inline, vertical = DailyRecordSpacing.Inline),
            textStyle = MaterialTheme.typography.bodyMedium.merge(TextStyle(color = DailyRecordText)),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = AppCopy.Record.detailFeelingHint,
                            color = DailyRecordTextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    innerTextField()
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = DailyRecordSpacing.Compact),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = AppCopy.Record.detailFeelingCounter(value.visibleCharacterCount()),
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
internal fun DetailEntryButton(
    count: Int,
    accent: Color,
    colors: RecordModuleColorTokens,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(DailyRecordShapes.Control)
            .dailyRecordGlass(
                shape = DailyRecordShapes.Control,
                moduleColors = colors,
                level = DailyRecordGlassLevel.Base,
                edgeColor = accent,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = AppCopy.Record.detailEntry
            }
            .padding(horizontal = DailyRecordSpacing.Inline),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Inline),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(DailyRecordShapes.Compact)
                .background(accent.copy(alpha = .10f))
                .border(
                    DailyRecordBorders.Standard,
                    accent.copy(alpha = .55f),
                    DailyRecordShapes.Compact,
                ),
        ) {
            BrandIcon(
                asset = BrandIconAsset.Clock,
                theme = colors.brandIconTheme,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(30.dp),
            )
            BrandIcon(
                asset = BrandIconAsset.Note,
                theme = colors.brandIconTheme,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(26.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = AppCopy.Record.detailEntry,
                color = accent,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = AppCopy.Record.detailEntryHint(count),
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        ChevronIcon(
            forward = true,
            modifier = Modifier.size(20.dp),
            color = accent,
            theme = colors.brandIconTheme,
        )
    }
}
