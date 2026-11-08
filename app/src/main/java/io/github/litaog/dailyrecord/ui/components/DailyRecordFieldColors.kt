package io.github.litaog.dailyrecord.ui.components

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent

@Composable
internal fun dailyRecordFieldColors(): TextFieldColors {
    // Material3 exposes this factory as @Composable, so keep the first result
    // per composition instead of calling it for every recomposition.
    val cached = remember { arrayOfNulls<TextFieldColors>(1) }
    return cached[0] ?: OutlinedTextFieldDefaults.colors(
        focusedTextColor = DailyRecordText,
        unfocusedTextColor = DailyRecordText,
        disabledTextColor = DailyRecordTextMuted,
        focusedBorderColor = DailyRecordDefaultAccent,
        unfocusedBorderColor = DailyRecordDivider,
        disabledBorderColor = DailyRecordDivider,
        focusedLabelColor = DailyRecordDefaultAccent,
        unfocusedLabelColor = DailyRecordTextMuted,
        disabledLabelColor = DailyRecordTextMuted,
        cursorColor = DailyRecordDefaultAccent,
        focusedContainerColor = DailyRecordSurface,
        unfocusedContainerColor = DailyRecordSurface,
        disabledContainerColor = DailyRecordSurface,
    ).also { cached[0] = it }
}
