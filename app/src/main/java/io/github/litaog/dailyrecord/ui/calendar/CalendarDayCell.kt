package io.github.litaog.dailyrecord.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.ui.RecordModuleUiSpec
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.RecordVisualState
import java.time.LocalDate

// combinedClickable is stable in current Compose; the opt-in only keeps older
// foundation versions compiling.
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CalendarDayCell(
    date: LocalDate,
    earliestDate: LocalDate,
    today: LocalDate,
    focused: Boolean,
    record: DailyCountEntry?,
    moduleSpec: RecordModuleUiSpec,
    cellHeight: androidx.compose.ui.unit.Dp,
    largeText: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Opens the quick-adjust stepper; null when quick log is unavailable. */
    onLongClick: (() -> Unit)? = null,
    /** The stepper popup anchored to this cell while it is open. */
    quickAdjustPopup: (@Composable () -> Unit)? = null,
) {
    val unsupported = date < earliestDate
    val future = date > today
    val count = record?.count
    val visualState = calendarRecordVisualState(
        unsupported = unsupported,
        future = future,
        count = count,
    )
    val visualColors = moduleSpec.colors.colorsFor(visualState)
    val background = visualColors.background
    val contentColor = visualColors.content
    val recordStatus = when {
        unsupported -> AppCopy.Calendar.unavailable
        future || record == null -> null
        count == 0 -> null
        else -> AppCopy.Calendar.countDescription(count ?: 0)
    }
    val visibleStatus = when {
        date == today && record == null -> AppCopy.Calendar.todayShort
        else -> recordStatus
    }
    val semanticStatus = AppCopy.Calendar.statusDescription(
        date = date,
        today = today,
        unsupported = unsupported,
        future = future,
        count = count,
        moduleLabel = moduleSpec.semanticCountLabel,
    )
    val borderColor = when {
        focused -> moduleSpec.colors.strong
        date == today -> moduleSpec.colors.primary
        else -> visualColors.outline
    }
    val showOutline = focused || date == today || visualState == RecordVisualState.ExplicitZero
    val borderWidth = if (focused || date == today) 2.dp else 1.dp
    val cellShape = DailyRecordShapes.Small
    val stateDescription = semanticStatus

    Box(
        modifier = modifier
            .height(cellHeight)
            .testTag("calendar_day_$date")
            .clip(cellShape)
            .combinedClickable(
                enabled = !future && !unsupported,
                role = Role.Button,
                // Long press opens the quick-adjust stepper (ADR-022); the
                // system long-press haptic comes with combinedClickable.
                onLongClickLabel = if (onLongClick != null) AppCopy.Calendar.quickAdjustAction else null,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                selected = focused
                contentDescription = AppCopy.Calendar.monthDateDescription(date, stateDescription, focused)
            }
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        val dayColor = if (date == today && record == null) moduleSpec.colors.primary else contentColor
        val visualModifier = Modifier
            .fillMaxSize()
            .clip(cellShape)
            .background(background)
            .then(
                if (showOutline) {
                    Modifier.border(borderWidth, borderColor, cellShape)
                } else {
                    Modifier
                },
            )

        Column(
            modifier = visualModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = dayColor,
                style = if (largeText) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            when {
                visualState == RecordVisualState.ExplicitZero -> Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .sizeIn(minWidth = 9.dp, minHeight = 9.dp)
                        .border(1.5.dp, moduleSpec.colors.primary, CircleShape),
                )
                visibleStatus != null -> Text(
                    text = visibleStatus,
                    color = dayColor,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
        // Placed inside the cell so the popup anchors to this date.
        if (quickAdjustPopup != null) {
            quickAdjustPopup()
        }
    }
}

internal fun calendarRecordVisualState(
    unsupported: Boolean,
    future: Boolean,
    count: Int?,
): RecordVisualState = when {
    unsupported || future -> RecordVisualState.Disabled
    count == null -> RecordVisualState.Unset
    count == 0 -> RecordVisualState.ExplicitZero
    count == 1 -> RecordVisualState.One
    count == 2 -> RecordVisualState.Two
    count == 3 -> RecordVisualState.Three
    else -> RecordVisualState.FourPlus
}
