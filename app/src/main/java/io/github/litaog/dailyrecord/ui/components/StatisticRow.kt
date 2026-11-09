package io.github.litaog.dailyrecord.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlass

@Composable
fun StatisticRow(
    label: String,
    countText: String,
    daysText: String,
    future: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .dailyRecordGlass(
                shape = RoundedCornerShape(12.dp),
                level = if (future) DailyRecordGlassLevel.Muted else DailyRecordGlassLevel.Base,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = AppCopy.Components.joinSemantics(label, countText, daysText)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = if (future) DailyRecordTextMuted else DailyRecordText,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = countText,
            color = if (future) DailyRecordTextMuted else DailyRecordTextSecondary,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(.8f),
        )
        Text(
            text = daysText,
            color = if (future) DailyRecordTextMuted else DailyRecordTextSecondary,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(.8f),
        )
    }
}
