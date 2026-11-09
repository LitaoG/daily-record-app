package io.github.litaog.dailyrecord.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.components.DailyRecordDialog
import io.github.litaog.dailyrecord.ui.theme.DailyRecordColorTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlass
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp

@Composable
internal fun ColorThemeSettingsRow(
    colorTheme: DailyRecordColorTheme,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    onClick: () -> Unit,
) {
    val name = colorTheme.displayName()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag("settings_color_theme_row")
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = AppCopy.Components.joinSemantics(
                    AppCopy.Settings.themeTitle,
                    name,
                )
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorThemeSwatch(colorTheme = colorTheme, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        // Theme names can be long in English; keep the value on its own line
        // so 200% font wraps instead of squeezing the title.
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = AppCopy.Settings.themeTitle,
                color = DailyRecordText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = name,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        BrandIcon(
            asset = BrandIconAsset.Next,
            theme = theme,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun ColorThemeSelectionDialog(
    currentTheme: DailyRecordColorTheme,
    tint: Color,
    onDismiss: () -> Unit,
    onSelected: (DailyRecordColorTheme) -> Unit,
) {
    DailyRecordDialog(
        title = AppCopy.Settings.themeDialogTitle,
        testTag = "settings_color_theme_dialog",
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DailyRecordColorTheme.entries.forEach { option ->
                RichColorThemeCard(
                    colorTheme = option,
                    selected = option == currentTheme,
                    tint = tint,
                    onClick = { onSelected(option) },
                )
            }
        }
    }
}

/**
 * Visual preview card for a single theme displaying name, radio checkmark,
 * and dual-module swatches (primary, peak, glow) for both Solo and Duo (Feature 28).
 */
@Composable
internal fun RichColorThemeCard(
    colorTheme: DailyRecordColorTheme,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = colorTheme.displayName()
    val palette = colorTheme.palette
    val fontScale = LocalDensity.current.fontScale
    val useStackedSwatches = fontScale >= 1.5f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("settings_color_theme_option_${colorTheme.id}")
            .dailyRecordGlass(
                shape = DailyRecordShapes.Medium,
                level = if (selected) DailyRecordGlassLevel.Elevated else DailyRecordGlassLevel.Base,
            )
            .then(
                if (selected) {
                    Modifier.border(2.dp, tint, DailyRecordShapes.Medium)
                } else {
                    Modifier.border(1.dp, DailyRecordDivider, DailyRecordShapes.Medium)
                }
            )
            .clip(DailyRecordShapes.Medium)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.RadioButton
                contentDescription = AppCopy.selectedState(name, selected)
            },
        shape = DailyRecordShapes.Medium,
        color = if (selected) tint.copy(alpha = 0.06f) else Color.Transparent,
        border = null,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) tint else DailyRecordText,
                )
                ThemeRadioIndicator(
                    selected = selected,
                    tint = tint,
                )
            }

            if (useStackedSwatches) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModuleTokensPreviewPill(
                        moduleName = AppCopy.RecordModule.handBrewLabel,
                        tokens = palette.solo,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ModuleTokensPreviewPill(
                        moduleName = AppCopy.RecordModule.sexLabel,
                        tokens = palette.duo,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ModuleTokensPreviewPill(
                        moduleName = AppCopy.RecordModule.handBrewLabel,
                        tokens = palette.solo,
                        modifier = Modifier.weight(1f),
                    )
                    ModuleTokensPreviewPill(
                        moduleName = AppCopy.RecordModule.sexLabel,
                        tokens = palette.duo,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleTokensPreviewPill(
    moduleName: String,
    tokens: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = DailyRecordShapes.Small,
        color = tokens.unset,
        border = BorderStroke(1.dp, tokens.soft),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = moduleName,
                style = MaterialTheme.typography.labelSmall,
                color = DailyRecordTextSecondary,
                fontWeight = FontWeight.Medium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(tokens.primary)
                        .border(0.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(tokens.peak)
                        .border(0.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(tokens.glow.copy(alpha = 0.35f))
                        .border(1.2.dp, tokens.primary.copy(alpha = 0.55f), CircleShape),
                )
            }
        }
    }
}

@Composable
private fun ThemeRadioIndicator(
    selected: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (selected) tint else Color.Transparent)
            .border(
                width = if (selected) 2.dp else 1.5.dp,
                color = if (selected) tint else DailyRecordTextMuted.copy(alpha = 0.5f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

@Composable
internal fun ColorThemeOptionRow(
    colorTheme: DailyRecordColorTheme,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit,
) = RichColorThemeCard(
    colorTheme = colorTheme,
    selected = selected,
    tint = tint,
    onClick = onClick,
)

/**
 * Decorative swatch: a circle split between the masturbation accent (left)
 * and the sex accent (right) of the theme, flat colors only. The enclosing
 * row carries the semantics.
 */
@Composable
private fun ColorThemeSwatch(colorTheme: DailyRecordColorTheme, size: Dp) {
    val palette = colorTheme.palette
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .drawBehind {
                drawArc(
                    color = palette.solo.primary,
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = true,
                )
                drawArc(
                    color = palette.duo.primary,
                    startAngle = 270f,
                    sweepAngle = 180f,
                    useCenter = true,
                )
            }
            .border(1.dp, Color.White.copy(alpha = .8f), CircleShape),
    )
}

internal fun DailyRecordColorTheme.displayName(): String = when (this) {
    DailyRecordColorTheme.Aurora -> AppCopy.Settings.themeAurora
    DailyRecordColorTheme.GlacialCoral -> AppCopy.Settings.themeGlacialCoral
    DailyRecordColorTheme.TwilightLilac -> AppCopy.Settings.themeTwilightLilac
    DailyRecordColorTheme.EmeraldBerry -> AppCopy.Settings.themeEmeraldBerry
    DailyRecordColorTheme.TitaniumChampagne -> AppCopy.Settings.themeTitaniumChampagne
    DailyRecordColorTheme.CreamLavender -> AppCopy.Settings.themeCreamLavender
    DailyRecordColorTheme.SpringSky -> AppCopy.Settings.themeSpringSky
    DailyRecordColorTheme.BuffGlacier -> AppCopy.Settings.themeBuffGlacier
    DailyRecordColorTheme.DreamBlush -> AppCopy.Settings.themeDreamBlush
}
