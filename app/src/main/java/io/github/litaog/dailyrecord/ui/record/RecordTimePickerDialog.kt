package io.github.litaog.dailyrecord.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.components.DailyRecordDialog
import io.github.litaog.dailyrecord.ui.components.OutlineActionButton
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import java.util.Locale

private const val HOURS_PER_DAY = 24
private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = HOURS_PER_DAY * MINUTES_PER_HOUR
internal const val MINIMUM_FLING_VELOCITY_DP_PER_SECOND = 90f

@Composable
internal fun RecordTimePickerDialog(
    initialMinutes: Int,
    colors: RecordModuleColorTokens,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val boundedInitialMinutes = initialMinutes.coerceIn(0, MINUTES_PER_DAY - 1)
    var selectedHour by remember(boundedInitialMinutes) {
        mutableIntStateOf(boundedInitialMinutes / MINUTES_PER_HOUR)
    }
    var selectedMinute by remember(boundedInitialMinutes) {
        mutableIntStateOf(boundedInitialMinutes % MINUTES_PER_HOUR)
    }
    var hourSettling by remember(boundedInitialMinutes) { mutableStateOf(false) }
    var minuteSettling by remember(boundedInitialMinutes) { mutableStateOf(false) }
    val wheelSettling = hourSettling || minuteSettling
    val fontScale = LocalDensity.current.fontScale
    val stackActions = fontScale >= 1.35f
    val wheelRowHeight = if (fontScale >= 1.6f) 72.dp else 56.dp

    DailyRecordDialog(
        title = AppCopy.Record.detailTimePickerTitle,
        subtitle = AppCopy.Record.detailTimePickerSubtitle,
        testTag = "time_picker_dialog",
        onDismissRequest = onDismiss,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeWheelColumn(
                initialValue = selectedHour,
                resetKey = boundedInitialMinutes,
                valueCount = HOURS_PER_DAY,
                unit = AppCopy.Record.detailTimePickerHour,
                colors = colors,
                rowHeight = wheelRowHeight,
                surfaceTestTag = "time_picker_hour_wheel_surface",
                onValueChanged = { selectedHour = it },
                onSettlingChanged = { hourSettling = it },
                modifier = Modifier.weight(1f).testTag("time_picker_hour_wheel"),
            )
            Text(
                text = ":",
                color = colors.primary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 22.dp),
            )
            TimeWheelColumn(
                initialValue = selectedMinute,
                resetKey = boundedInitialMinutes,
                valueCount = MINUTES_PER_HOUR,
                unit = AppCopy.Record.detailTimePickerMinute,
                colors = colors,
                rowHeight = wheelRowHeight,
                surfaceTestTag = "time_picker_minute_wheel_surface",
                onValueChanged = { selectedMinute = it },
                onSettlingChanged = { minuteSettling = it },
                modifier = Modifier.weight(1f).testTag("time_picker_minute_wheel"),
            )
        }
        Text(
            text = AppCopy.Record.detailTimePickerHint,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
        Spacer(Modifier.height(18.dp))
        if (stackActions) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PrimaryActionButton(
                    label = AppCopy.Record.detailTimePickerConfirm,
                    onClick = {
                        if (!wheelSettling) {
                            onConfirm(selectedHour * MINUTES_PER_HOUR + selectedMinute)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !wheelSettling,
                    accent = colors.primary,
                )
                OutlineActionButton(
                    label = AppCopy.Auth.cancel,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    accent = colors.primary,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlineActionButton(
                    label = AppCopy.Auth.cancel,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    accent = colors.primary,
                )
                PrimaryActionButton(
                    label = AppCopy.Record.detailTimePickerConfirm,
                    onClick = {
                        if (!wheelSettling) {
                            onConfirm(selectedHour * MINUTES_PER_HOUR + selectedMinute)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !wheelSettling,
                    accent = colors.primary,
                )
            }
        }
    }
}

internal fun wrapTimeWheelValue(value: Int, valueCount: Int): Int {
    require(valueCount > 0) { "Time wheel must contain at least one value." }
    return Math.floorMod(value, valueCount)
}

internal fun formatTimeWheelValue(value: Int): String =
    String.format(Locale.ROOT, "%02d", value)

internal data class TimeWheelOffsetState(
    val value: Int,
    val offsetPx: Float,
)

internal fun advanceTimeWheelOffset(
    state: TimeWheelOffsetState,
    deltaPx: Float,
    rowHeightPx: Float,
    valueCount: Int,
): TimeWheelOffsetState {
    require(rowHeightPx > 0f) { "Time wheel row height must be positive." }

    var nextValue = wrapTimeWheelValue(state.value, valueCount)
    var nextOffset = state.offsetPx + deltaPx
    while (nextOffset <= -rowHeightPx) {
        nextValue = wrapTimeWheelValue(nextValue + 1, valueCount)
        nextOffset += rowHeightPx
    }
    while (nextOffset >= rowHeightPx) {
        nextValue = wrapTimeWheelValue(nextValue - 1, valueCount)
        nextOffset -= rowHeightPx
    }
    return TimeWheelOffsetState(nextValue, nextOffset)
}
