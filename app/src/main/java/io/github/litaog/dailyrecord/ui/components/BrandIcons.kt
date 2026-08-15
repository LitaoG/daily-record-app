package io.github.litaog.dailyrecord.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.R
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.SexColorTokens

/** The two locked colour variants provided by the icon library. */
internal enum class BrandIconTheme {
    Purple,
    Wine,
}

/** Shared icon artwork supplied by the Daily Record Figma icon library. */
internal enum class BrandIconAsset {
    Calendar,
    Statistics,
    Previous,
    Next,
    Down,
    Clock,
    Note,
    Edit,
    Settings,
    Return,
    CloudSync,
    Lock,
    Shield,
}

/** Select the icon-library colour that belongs to the active record module. */
internal val RecordModuleColorTokens.brandIconTheme: BrandIconTheme
    get() = if (primary == SexColorTokens.primary) {
        BrandIconTheme.Wine
    } else {
        BrandIconTheme.Purple
    }

@Composable
internal fun BrandIcon(
    asset: BrandIconAsset,
    modifier: Modifier = Modifier,
    theme: BrandIconTheme = BrandIconTheme.Purple,
    contentDescription: String? = null,
    alpha: Float = 1f,
) {
    Image(
        painter = painterResource(asset.resource(theme)),
        contentDescription = contentDescription,
        modifier = modifier.alpha(alpha),
        contentScale = ContentScale.Fit,
    )
}

private fun BrandIconAsset.resource(theme: BrandIconTheme): Int = when (theme) {
    BrandIconTheme.Purple -> when (this) {
        BrandIconAsset.Calendar -> R.drawable.ic_brand_calendar_purple
        BrandIconAsset.Statistics -> R.drawable.ic_brand_statistics_purple
        BrandIconAsset.Previous -> R.drawable.ic_brand_previous_purple
        BrandIconAsset.Next -> R.drawable.ic_brand_next_purple
        BrandIconAsset.Down -> R.drawable.ic_brand_down_purple
        BrandIconAsset.Clock -> R.drawable.ic_brand_clock_purple
        BrandIconAsset.Note -> R.drawable.ic_brand_note_purple
        BrandIconAsset.Edit -> R.drawable.ic_brand_edit_purple
        BrandIconAsset.Settings -> R.drawable.ic_brand_settings_purple
        BrandIconAsset.Return -> R.drawable.ic_brand_previous_purple
        BrandIconAsset.CloudSync -> R.drawable.ic_brand_cloud_sync_purple
        BrandIconAsset.Lock -> R.drawable.ic_brand_lock_purple
        BrandIconAsset.Shield -> R.drawable.ic_brand_shield_purple
    }
    BrandIconTheme.Wine -> when (this) {
        BrandIconAsset.Calendar -> R.drawable.ic_brand_calendar_wine
        BrandIconAsset.Statistics -> R.drawable.ic_brand_statistics_wine
        BrandIconAsset.Previous -> R.drawable.ic_brand_previous_wine
        BrandIconAsset.Next -> R.drawable.ic_brand_next_wine
        BrandIconAsset.Down -> R.drawable.ic_brand_down_wine
        BrandIconAsset.Clock -> R.drawable.ic_brand_clock_wine
        BrandIconAsset.Note -> R.drawable.ic_brand_note_wine
        BrandIconAsset.Edit -> R.drawable.ic_brand_edit_wine
        BrandIconAsset.Settings -> R.drawable.ic_brand_settings_wine
        BrandIconAsset.Return -> R.drawable.ic_brand_previous_wine
        BrandIconAsset.CloudSync -> R.drawable.ic_brand_cloud_sync_wine
        BrandIconAsset.Lock -> R.drawable.ic_brand_lock_wine
        BrandIconAsset.Shield -> R.drawable.ic_brand_shield_wine
    }
}
