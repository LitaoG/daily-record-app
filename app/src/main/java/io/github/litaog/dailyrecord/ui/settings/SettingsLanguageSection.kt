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
import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.components.DailyRecordDialog
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary

@Composable
internal fun LanguageSettingsRow(
    language: AppLanguage,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag("settings_language_row")
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = AppCopy.Components.joinSemantics(
                    AppCopy.Settings.languageTitle,
                    languageDisplayName(language),
                )
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBadge(
            icon = BrandIconAsset.Edit,
            theme = theme,
            tint = tint,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = AppCopy.Settings.languageTitle,
            color = DailyRecordText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = languageDisplayName(language),
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

@Composable
internal fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    tint: Color,
    onDismiss: () -> Unit,
    onSelected: (AppLanguage) -> Unit,
) {
    DailyRecordDialog(
        title = AppCopy.Settings.languageDialogTitle,
        testTag = "settings_language_dialog",
        onDismissRequest = onDismiss,
    ) {
        AppLanguage.entries.forEach { language ->
            SettingsOptionRow(
                label = languageDisplayName(language),
                selected = language == currentLanguage,
                tint = tint,
                testTag = "settings_language_option",
                onClick = { onSelected(language) },
            )
        }
    }
}

private fun languageDisplayName(language: AppLanguage): String = when (language) {
    AppLanguage.ZH -> AppCopy.Settings.languageZh
    AppLanguage.EN -> AppCopy.Settings.languageEn
}
