package io.github.litaog.dailyrecord.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.DailyRecordCanvas
import io.github.litaog.dailyrecord.ui.theme.DailyRecordLiquidBackdrop
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.LiquidGlassElevation
import io.github.litaog.dailyrecord.ui.theme.appleLiquidGlass

/**
 * App lock screen (ADR-023). A full-screen dialog window, so it also covers
 * any app dialog or popup that was open when the app was left. It shows no
 * record data and no module name, only the lock state and the unlock action.
 * Back sends the app to the background instead of closing the lock.
 */
@Composable
internal fun AppLockScreen(
    attemptFailed: Boolean,
    onUnlock: () -> Unit,
    onLeave: () -> Unit,
) {
    // Module-neutral app accent: the lock must not reveal the last module.
    val colors = HandBrewColorTokens
    Dialog(
        onDismissRequest = onLeave,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DailyRecordCanvas)
                .testTag("app_lock_screen"),
            contentAlignment = Alignment.Center,
        ) {
            DailyRecordLiquidBackdrop(colors = colors)
            Column(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .appleLiquidGlass(
                        shape = DailyRecordShapes.Card,
                        glowColor = colors.primary,
                        elevation = LiquidGlassElevation.Emphasis,
                    )
                    .semantics { paneTitle = AppCopy.AppLock.lockedTitle }
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BrandIcon(
                    asset = BrandIconAsset.Lock,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    text = AppCopy.AppLock.lockedTitle,
                    color = DailyRecordText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = AppCopy.AppLock.lockedBody,
                    color = DailyRecordTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                if (attemptFailed) {
                    Text(
                        text = AppCopy.AppLock.notCompleted,
                        color = DailyRecordTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .testTag("app_lock_not_completed")
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                PrimaryActionButton(
                    label = AppCopy.AppLock.unlock,
                    onClick = onUnlock,
                    accent = colors.primary,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                        .testTag("app_lock_unlock"),
                )
            }
        }
    }
}
