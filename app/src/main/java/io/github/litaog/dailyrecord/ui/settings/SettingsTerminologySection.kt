package io.github.litaog.dailyrecord.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.TerminologyStyle
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.components.DailyRecordDialog
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary

@Composable
internal fun TerminologyStyleSettingsRow(
    terminologyStyle: TerminologyStyle,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    tint: Color,
    onClick: () -> Unit,
) {
    val styleLabel = terminologyStyleDisplayName(terminologyStyle)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag("settings_terminology_row")
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = AppCopy.Components.joinSemantics(
                    AppCopy.Settings.terminologyTitle,
                    styleLabel,
                )
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBadge(
            icon = BrandIconAsset.Note,
            theme = theme,
            tint = tint,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = AppCopy.Settings.terminologyTitle,
            color = DailyRecordText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = styleLabel,
            color = DailyRecordTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        BrandIcon(
            asset = BrandIconAsset.Next,
            theme = theme,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun terminologyStyleDisplayName(style: TerminologyStyle): String = when (style) {
    TerminologyStyle.ELEGANT -> AppCopy.Settings.terminologyElegant
    TerminologyStyle.DIRECT -> AppCopy.Settings.terminologyDirect
}

private fun terminologyStyleOptionLabel(style: TerminologyStyle): String = when (style) {
    TerminologyStyle.ELEGANT -> AppCopy.Settings.terminologyElegantOption
    TerminologyStyle.DIRECT -> AppCopy.Settings.terminologyDirectOption
}

@Composable
internal fun TerminologyStyleSelectionDialog(
    currentStyle: TerminologyStyle,
    tint: Color,
    onDismiss: () -> Unit,
    onSelected: (TerminologyStyle) -> Unit,
) {
    DailyRecordDialog(
        title = AppCopy.Settings.terminologyDialogTitle,
        testTag = "settings_terminology_dialog",
        onDismissRequest = onDismiss,
    ) {
        TerminologyStyle.entries.forEach { style ->
            SettingsOptionRow(
                label = terminologyStyleOptionLabel(style),
                selected = style == currentStyle,
                tint = tint,
                testTag = "settings_terminology_option",
                onClick = { onSelected(style) },
            )
        }
    }
}
