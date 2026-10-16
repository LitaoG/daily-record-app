package io.github.litaog.dailyrecord.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.SexColorTokens

/**
 * Settings entry icon backed by the locked Figma brand artwork.
 *
 * The asset is selected as Purple or Wine rather than being recoloured, so the
 * only tint callers supply is the active module tint that picks the variant.
 */
@Composable
fun PrivateSettingsIcon(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    moduleTint: Color = DailyRecordDefaultAccent,
) {
    BrandIcon(
        asset = BrandIconAsset.Settings,
        theme = if (moduleTint == SexColorTokens.primary) {
            BrandIconTheme.Wine
        } else {
            BrandIconTheme.Purple
        },
        modifier = modifier.size(DailyRecordSizes.SettingsIcon),
        contentDescription = contentDescription,
    )
}
