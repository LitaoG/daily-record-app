package io.github.litaog.dailyrecord.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlass

private val SettingsCardShape = RoundedCornerShape(18.dp)

@Composable
internal fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = DailyRecordTextSecondary,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .semantics { heading() },
    )
}

@Composable
internal fun SettingsCard(
    modifier: Modifier = Modifier,
    contentSpacing: androidx.compose.ui.unit.Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .dailyRecordGlass(
                shape = SettingsCardShape,
                level = DailyRecordGlassLevel.Base,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(contentSpacing),
        content = content,
    )
}

@Composable
internal fun SettingsInfoRow(
    icon: BrandIconAsset,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    title: String,
    summary: String,
    tint: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBadge(icon = icon, theme = theme, tint = tint)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                color = DailyRecordText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = summary,
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun SettingsIconBadge(
    icon: BrandIconAsset,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(DailyRecordShapes.Compact)
            .background(tint.copy(alpha = .10f)),
        contentAlignment = Alignment.Center,
    ) {
        BrandIcon(
            asset = icon,
            theme = theme,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** One choice in a single-choice dialog (language, relock delay). */
@Composable
internal fun SettingsOptionRow(
    label: String,
    selected: Boolean,
    tint: Color,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag(testTag)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.RadioButton
                contentDescription = AppCopy.selectedState(label, selected)
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = if (selected) tint else DailyRecordText,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(
                    if (selected) tint else Color.Transparent,
                )
                .border(
                    width = 1.dp,
                    color = if (selected) tint else DailyRecordDivider,
                    shape = CircleShape,
                ),
        )
    }
}

/** Flat module color when on, a muted outline when off; no gradients (ADR-022). */
@Composable
internal fun settingsSwitchColors(tint: Color) = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = tint,
    checkedBorderColor = tint,
    uncheckedThumbColor = DailyRecordTextMuted,
    uncheckedTrackColor = Color.Transparent,
    uncheckedBorderColor = DailyRecordTextMuted,
)
