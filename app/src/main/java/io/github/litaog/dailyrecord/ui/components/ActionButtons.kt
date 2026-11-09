package io.github.litaog.dailyrecord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordOnAccent
import io.github.litaog.dailyrecord.ui.theme.dailyRecordAccentBrush

@Composable
fun PrimaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = DailyRecordDefaultAccent,
) {
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(DailyRecordShapes.Control)
            .then(
                if (enabled) {
                    Modifier.background(dailyRecordAccentBrush(accent))
                } else {
                    Modifier.background(DailyRecordSurfaceMuted)
                },
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button; contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = if (enabled) DailyRecordOnAccent else DailyRecordTextMuted, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun OutlineActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = DailyRecordDefaultAccent,
) {
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(DailyRecordShapes.Control)
            .background(if (enabled) DailyRecordSurface else DailyRecordSurfaceMuted)
            .border(
                width = 1.dp,
                color = if (enabled) accent.copy(alpha = .66f) else DailyRecordDivider,
                shape = DailyRecordShapes.Control,
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button; contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = if (enabled) accent else DailyRecordTextMuted, style = MaterialTheme.typography.labelLarge)
    }
}
