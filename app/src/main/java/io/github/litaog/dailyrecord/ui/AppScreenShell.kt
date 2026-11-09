package io.github.litaog.dailyrecord.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.litaog.dailyrecord.ui.theme.DailyRecordLiquidBackdrop
import io.github.litaog.dailyrecord.ui.theme.DailyRecordMotion
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import java.time.LocalDate

/**
 * The three full-screen surfaces of the app. Main hosts the scaffold with the
 * calendar and statistics destinations; Record and Settings are pushed above it.
 */
internal sealed interface AppScreen {
    data object Main : AppScreen
    data class Record(val date: LocalDate) : AppScreen
    data object Settings : AppScreen
}

/**
 * Root visual shell: the liquid glass backdrop shared by every screen plus the
 * horizontal push/pop transition between Main and the Record/Settings surfaces.
 */
@Composable
internal fun AppScreenShell(
    currentScreen: AppScreen,
    colors: RecordModuleColorTokens,
    content: @Composable (AppScreen) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        DailyRecordLiquidBackdrop(colors = colors)

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                when {
                    initialState is AppScreen.Main -> {
                        (slideInHorizontally(
                            initialOffsetX = { (it * 0.25f).toInt() },
                            animationSpec = DailyRecordMotion.spatial(),
                        ) + fadeIn(
                            animationSpec = tween(220, easing = LinearOutSlowInEasing),
                        )).togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { -(it * 0.15f).toInt() },
                                animationSpec = DailyRecordMotion.spatial(),
                            ) + fadeOut(
                                animationSpec = tween(180, easing = FastOutLinearInEasing),
                            )
                        )
                    }
                    targetState is AppScreen.Main -> {
                        (slideInHorizontally(
                            initialOffsetX = { -(it * 0.15f).toInt() },
                            animationSpec = DailyRecordMotion.spatial(),
                        ) + fadeIn(
                            animationSpec = tween(220, easing = LinearOutSlowInEasing),
                        )).togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { (it * 0.25f).toInt() },
                                animationSpec = DailyRecordMotion.spatial(),
                            ) + fadeOut(
                                animationSpec = tween(180, easing = FastOutLinearInEasing),
                            )
                        )
                    }
                    else -> {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    }
                }
            },
            label = "appScreenTransition",
        ) { screen ->
            content(screen)
        }
    }
}
