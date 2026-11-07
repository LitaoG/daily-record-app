package io.github.litaog.dailyrecord.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
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
    /** Material 3 Expressive pill track for the module selector (ADR-022). */
    val ModuleSelector = RoundedCornerShape(percent = 50)
    val ModuleSelectorSlider = RoundedCornerShape(percent = 50)
    val Control = RoundedCornerShape(16.dp)
    val Compact = RoundedCornerShape(12.dp)
    /** Large glass cards such as the today island and chart containers. */
    val Card = RoundedCornerShape(24.dp)
    /** Floating liquid glass bottom navigation capsule. */
    val BottomBar = RoundedCornerShape(28.dp)
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
