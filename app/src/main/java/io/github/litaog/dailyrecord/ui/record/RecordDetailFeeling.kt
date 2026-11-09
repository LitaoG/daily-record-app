package io.github.litaog.dailyrecord.ui.record

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.model.visibleCharacterCount
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.components.BrandIconTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordBorders
import io.github.litaog.dailyrecord.ui.theme.DailyRecordMotion
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun FeelingAction(
    occurrence: Int,
    expanded: Boolean,
    accent: Color,
    theme: BrandIconTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val label = when {
        expanded -> AppCopy.Record.detailCollapseFeeling
        else -> AppCopy.Record.detailWriteFeeling
    }
    Row(
        modifier = modifier
            .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
            .clip(DailyRecordShapes.Compact)
            // A text button on the glass: no fill and no outline, because the
            // accent label is only checked for AA on the bare glass.
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = AppCopy.Record.detailFeelingActionDescription(occurrence, label)
            }
            .testTag("record_detail_${occurrence}_feeling")
            .padding(start = 0.dp, end = DailyRecordSpacing.Compact),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Compact),
    ) {
        BrandIcon(
            asset = BrandIconAsset.Edit,
            theme = theme,
            // Optical alignment: with zero start padding, a 1dp nudge lines
            // the pen up with the start edge of the button. The click target
            // and semantics stay on the full button.
            modifier = Modifier.size(24.dp).offset(x = (-1).dp),
        )
        Text(
            text = label,
            color = accent,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
internal fun FeelingEditor(
    occurrence: Int,
    value: String,
    accent: Color,
    onValueChange: (String) -> Unit,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                // Starts one line tall and grows with the text (ADR-022).
                .animateContentSize(animationSpec = DailyRecordMotion.spatial())
                .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            delay(300)
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                }
                .clip(DailyRecordShapes.Compact)
                .background(DailyRecordSurface)
                // The one outline left in the card marks the text input.
                .border(DailyRecordBorders.Standard, accent.copy(alpha = .65f), DailyRecordShapes.Compact)
                .semantics {
                    contentDescription = AppCopy.Record.detailFeelingEditorDescription(occurrence)
                }
                .testTag("record_detail_${occurrence}_feeling_editor")
                .padding(horizontal = DailyRecordSpacing.Inline, vertical = DailyRecordSpacing.Inline),
            textStyle = MaterialTheme.typography.bodyMedium.merge(TextStyle(color = DailyRecordText)),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = AppCopy.Record.detailFeelingHint,
                            color = DailyRecordTextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    innerTextField()
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = DailyRecordSpacing.Compact),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = AppCopy.Record.detailFeelingCounter(value.visibleCharacterCount()),
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
