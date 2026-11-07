package io.github.litaog.dailyrecord.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.components.brandIconTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.LiquidGlassElevation
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.appleLiquidGlass

@Composable
internal fun IntimacyOnboardingCard(
    colors: RecordModuleColorTokens,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("intimacy_onboarding_card")
            .appleLiquidGlass(
                elevation = LiquidGlassElevation.Elevated,
                shape = DailyRecordShapes.Card,
                glowColor = colors.primary,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandIcon(
                    asset = BrandIconAsset.Note,
                    theme = colors.brandIconTheme,
                    modifier = Modifier.size(18.dp),
                    contentDescription = null,
                    tint = colors.primary,
                )
                Text(
                    text = AppCopy.Onboarding.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = DailyRecordText,
                )
            }
            Text(
                text = AppCopy.Onboarding.dismissAction,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .testTag("intimacy_onboarding_dismiss")
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OnboardingModuleItem(
                label = AppCopy.Onboarding.soloLabel,
                description = AppCopy.Onboarding.soloDescription,
            )
            OnboardingModuleItem(
                label = AppCopy.Onboarding.duoLabel,
                description = AppCopy.Onboarding.duoDescription,
            )
        }

        Text(
            text = AppCopy.Onboarding.settingsHint,
            style = MaterialTheme.typography.bodySmall,
            color = DailyRecordTextMuted,
        )
    }
}

@Composable
private fun OnboardingModuleItem(
    label: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = DailyRecordText,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = DailyRecordTextSecondary,
        )
    }
}
