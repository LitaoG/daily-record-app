package io.github.litaog.dailyrecord.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.components.ChevronIcon
import io.github.litaog.dailyrecord.ui.components.brandIconTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordBorders
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlass

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
            theme = colors.brandIconTheme,
        )
    }
}
