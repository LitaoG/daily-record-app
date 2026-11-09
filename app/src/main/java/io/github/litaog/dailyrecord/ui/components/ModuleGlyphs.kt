package io.github.litaog.dailyrecord.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.SexColorTokens

@Composable
internal fun CalendarGlyph(
    color: Color = DailyRecordDefaultAccent,
    modifier: Modifier = Modifier,
    theme: BrandIconTheme = BrandIconTheme.Solo,
) {
    BrandIcon(
        asset = BrandIconAsset.Calendar,
        theme = theme,
        modifier = modifier,
        tint = color,
    )
}

@Composable
internal fun StatisticsGlyph(
    color: Color = DailyRecordDefaultAccent,
    modifier: Modifier = Modifier,
    theme: BrandIconTheme = BrandIconTheme.Solo,
) {
    BrandIcon(
        asset = BrandIconAsset.Statistics,
        theme = theme,
        modifier = modifier,
        tint = color,
    )
}

/**
 * Navigation chevron. The artwork ships as two locked brand variants, so
 * callers pick [theme] instead of a tint; a disabled state is expressed with
 * [modifier] alpha by the caller.
 */
@Composable
internal fun ChevronIcon(
    forward: Boolean,
    modifier: Modifier = Modifier,
    color: Color = DailyRecordText,
    theme: BrandIconTheme = BrandIconTheme.Solo,
) {
    BrandIcon(
        asset = if (forward) BrandIconAsset.Next else BrandIconAsset.Previous,
        theme = theme,
        modifier = modifier,
    )
}

@Composable
internal fun BackChevronIcon(
    modifier: Modifier = Modifier,
    color: Color = DailyRecordText,
    theme: BrandIconTheme = BrandIconTheme.Solo,
) {
    ChevronIcon(forward = false, modifier = modifier, color = color, theme = theme)
}

/** Masturbation module symbol (paper plane), tinted with the theme accent. */
@Composable
fun HandBrewIcon(
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    Icon(
        imageVector = DailyRecordIcons.SoloModule,
        contentDescription = null,
        modifier = modifier,
        tint = tint ?: HandBrewColorTokens.primary,
    )
}

/** Sex module symbol (two interlocking rings), tinted with the theme accent. */
@Composable
fun SexIcon(
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    Icon(
        imageVector = DailyRecordIcons.DuoModule,
        contentDescription = null,
        modifier = modifier,
        tint = tint ?: SexColorTokens.primary,
    )
}

/**
 * Shared chevron navigation button for calendar months and statistics
 * periods. The 48dp touch target, disabled alpha and 20dp chevron are the
 * single source of truth for both surfaces.
 */
@Composable
internal fun IconArrowButton(
    forward: Boolean,
    description: String,
    enabled: Boolean,
    theme: BrandIconTheme,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .sizeIn(
                minWidth = DailyRecordSizes.MinimumTouchTarget,
                minHeight = DailyRecordSizes.MinimumTouchTarget,
            )
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else .3f)
            .semantics { role = Role.Button; contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        ChevronIcon(
            forward = forward,
            modifier = Modifier.size(20.dp),
            theme = theme,
        )
    }
}
