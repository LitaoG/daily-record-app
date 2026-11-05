package io.github.litaog.dailyrecord.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.SexColorTokens

/**
 * The record module an icon belongs to. The actual tint comes from the
 * selected color theme (ADR-022), so icons never fall back to a fixed palette.
 */
internal enum class BrandIconTheme {
    Solo,
    Duo,
}

/** Shared vector symbols; see [DailyRecordIcons]. */
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

/** Select the icon module that belongs to the active record module colors. */
internal val RecordModuleColorTokens.brandIconTheme: BrandIconTheme
    get() = if (primary == SexColorTokens.primary) {
        BrandIconTheme.Duo
    } else {
        BrandIconTheme.Solo
    }

/** Module accent of the selected theme; white text and icons stay AA on it. */
internal val BrandIconTheme.tint: Color
    get() = when (this) {
        BrandIconTheme.Solo -> HandBrewColorTokens.primary
        BrandIconTheme.Duo -> SexColorTokens.primary
    }

internal val BrandIconAsset.vector: ImageVector
    get() = when (this) {
        BrandIconAsset.Calendar -> DailyRecordIcons.Calendar
        BrandIconAsset.Statistics -> DailyRecordIcons.Statistics
        BrandIconAsset.Previous -> DailyRecordIcons.Previous
        BrandIconAsset.Next -> DailyRecordIcons.Next
        BrandIconAsset.Down -> DailyRecordIcons.Down
        BrandIconAsset.Clock -> DailyRecordIcons.Clock
        BrandIconAsset.Note -> DailyRecordIcons.Note
        BrandIconAsset.Edit -> DailyRecordIcons.Edit
        BrandIconAsset.Settings -> DailyRecordIcons.Settings
        // Back navigation uses the same iOS-style chevron as "previous".
        BrandIconAsset.Return -> DailyRecordIcons.Previous
        BrandIconAsset.CloudSync -> DailyRecordIcons.CloudSync
        BrandIconAsset.Lock -> DailyRecordIcons.Lock
        BrandIconAsset.Shield -> DailyRecordIcons.Shield
    }

/**
 * Draws a vector symbol tinted with the module accent of the selected theme,
 * or with [tint] when the caller needs another color (for example white on an
 * accent fill). Sizing stays with the caller's modifier.
 */
@Composable
internal fun BrandIcon(
    asset: BrandIconAsset,
    modifier: Modifier = Modifier,
    theme: BrandIconTheme = BrandIconTheme.Solo,
    contentDescription: String? = null,
    alpha: Float = 1f,
    tint: Color? = null,
) {
    Icon(
        imageVector = asset.vector,
        contentDescription = contentDescription,
        modifier = modifier.alpha(alpha),
        tint = tint ?: theme.tint,
    )
}
