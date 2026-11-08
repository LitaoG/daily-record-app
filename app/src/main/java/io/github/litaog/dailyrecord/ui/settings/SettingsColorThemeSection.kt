package io.github.litaog.dailyrecord.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
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
        // Nine options do not fit short screens at 200% font; the bounded
        // height keeps the nested scroll measurable inside the dialog.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            DailyRecordColorTheme.entries.forEach { option ->
                ColorThemeOptionRow(
                    colorTheme = option,
                    selected = option == currentTheme,
                    tint = tint,
                    onClick = { onSelected(option) },
                )
            }
        }
    }
}

@Composable
internal fun ColorThemeOptionRow(
    colorTheme: DailyRecordColorTheme,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit,
) {
    val name = colorTheme.displayName()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("settings_color_theme_option_${colorTheme.id}")
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.RadioButton
                contentDescription = AppCopy.selectedState(name, selected)
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorThemeSwatch(colorTheme = colorTheme, size = 32.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            color = if (selected) tint else DailyRecordText,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (selected) tint else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (selected) tint else DailyRecordDivider,
                    shape = CircleShape,
                ),
        )
    }
}

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
