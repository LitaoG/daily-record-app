package io.github.litaog.dailyrecord.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object DailyRecordSpacing {
    val ScreenHorizontal = 20.dp
    val ScreenVertical = 16.dp
    val Content = 12.dp
    val Inline = 8.dp
    val Compact = 4.dp
}

object DailyRecordSizes {
    val MinimumTouchTarget = 48.dp
    val SyncChipHeight = 38.dp
    val SettingsButton = 60.dp
    val SettingsIcon = 30.dp
    /** Track height; with the 4dp inset each segment keeps a 48dp touch target. */
    val ModuleSelectorMinHeight = 56.dp
    val BottomBarMinHeight = 72.dp
    val ModuleIcon = 24.dp
}

object DailyRecordShapes {
    /** 6.dp: Status indicators, tiny legend markers, badges, small tags (ADR-022 / M1). */
    val ExtraSmall: CornerBasedShape = RoundedCornerShape(6.dp)

    /** 10.dp: Calendar day cells, compact controls, progress segments, nested card elements. */
    val Small: CornerBasedShape = RoundedCornerShape(10.dp)

    /** 16.dp: Standard action buttons, inputs, picker wheel frames, feedback containers. */
    val Medium: CornerBasedShape = RoundedCornerShape(16.dp)

    /** 22.dp: Cards, dialog containers, today island, statistics cards, settings cards. */
    val Large: CornerBasedShape = RoundedCornerShape(22.dp)

    /** 50%: Pill capsules: module selector, period tabs, bottom nav bar, quick log done pills. */
    val Full: CornerBasedShape = RoundedCornerShape(percent = 50)

    // --- Backwards Compatibility & Semantic Role Aliases ---
    val ModuleSelector: CornerBasedShape = Full
    val ModuleSelectorSlider: CornerBasedShape = Full
    val Control: CornerBasedShape = Medium
    val Compact: CornerBasedShape = Small
    val Card: CornerBasedShape = Large
    val BottomBar: CornerBasedShape = Full

    /** Material 3 Shapes bridge for Compose MaterialTheme integration. */
    val Material3Shapes: Shapes = Shapes(
        extraSmall = ExtraSmall,
        small = Small,
        medium = Medium,
        large = Large,
        extraLarge = Large,
    )
}

object DailyRecordBorders {
    val Standard = 1.dp
}

object DailyRecordElevations {
    val Flat = 0.dp
}

/**
 * Material 3 Expressive motion expressed with plain springs, so the app does
 * not depend on experimental motion-scheme APIs (ADR-022).
 */
object DailyRecordMotion {
    /** Spatial movement such as sliders and indicators: quick, barely bouncy. */
    fun <T> spatial(): SpringSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

    /** Small effects such as press feedback: a short, visible bounce. */
    fun <T> effects(): SpringSpec<T> =
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)

    /** Scale applied while a control is pressed. */
    const val PressedScale = 0.96f
}
