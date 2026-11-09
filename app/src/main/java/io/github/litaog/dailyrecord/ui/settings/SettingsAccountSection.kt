package io.github.litaog.dailyrecord.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.account.color
import io.github.litaog.dailyrecord.ui.account.label
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.components.brandIconTheme
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens

@Composable
internal fun AccountSettingsCard(
    accountEmail: String?,
    syncStatus: SyncStatus,
    moduleColors: RecordModuleColorTokens,
    onOpenAccount: () -> Unit,
    onSignIn: (() -> Unit)?,
) {
    val signedIn = accountEmail != null
    val title = accountEmail ?: AppCopy.Settings.localAccountTitle
    val status = if (signedIn) syncStatus.label() else AppCopy.Settings.localAccountSummary

    SettingsCard(
        modifier = Modifier
            .testTag("settings_account_card")
            .then(
                if (signedIn) {
                    Modifier
                        .clickable(role = Role.Button, onClick = onOpenAccount)
                        .semantics {
                            role = Role.Button
                            contentDescription = AppCopy.Settings.accountDescription(title, status)
                        }
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIconBadge(
                icon = BrandIconAsset.CloudSync,
                theme = moduleColors.brandIconTheme,
                tint = moduleColors.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    color = DailyRecordText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (signedIn) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(syncStatus.color()),
                        )
                        Text(
                            text = status,
                            color = DailyRecordTextSecondary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            if (signedIn) {
                BrandIcon(
                    asset = BrandIconAsset.Next,
                    theme = moduleColors.brandIconTheme,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Text(
            text = if (signedIn) {
                AppCopy.Settings.signedInAccountSummary
            } else {
                AppCopy.Settings.localAccountSummary
            },
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.bodyMedium,
        )

        if (!signedIn && onSignIn != null) {
            PrimaryActionButton(
                label = AppCopy.Account.signInSync,
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                accent = moduleColors.primary,
            )
        }
    }
}
