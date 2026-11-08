package io.github.litaog.dailyrecord.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted

/** Public privacy notices of this open-source repository (Play listing requirement). */
internal const val PRIVACY_POLICY_URL_ZH = "https://github.com/LitaoG/daily-record-app/blob/main/PRIVACY.md"
internal const val PRIVACY_POLICY_URL_EN = "https://github.com/LitaoG/daily-record-app/blob/main/PRIVACY.en.md"

@Composable
internal fun AboutRow(
    label: String,
    value: String,
    testTag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .semantics(mergeDescendants = true) {
                contentDescription = AppCopy.Components.joinSemantics(label, value)
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = DailyRecordText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** About row that opens an external page; the chevron marks it as tappable. */
@Composable
internal fun AboutLinkRow(
    label: String,
    actionDescription: String,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag(testTag)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = AppCopy.Components.joinSemantics(label, actionDescription)
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = DailyRecordText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        BrandIcon(
            asset = BrandIconAsset.Next,
            theme = theme,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}
