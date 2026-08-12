package io.github.litaog.dailyrecord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDanger
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import io.github.litaog.dailyrecord.ui.theme.DailyRecordOnAccent
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.dailyRecordAccentBrush

/** Branded confirmation surface used instead of a library-default confirmation dialog. */
@Composable
fun DailyRecordConfirmationDialog(
    title: String,
    subtitle: String,
    message: String,
    cancelLabel: String,
    confirmLabel: String,
    testTag: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
) {
    val stackActions = LocalDensity.current.fontScale >= 1.35f
    DailyRecordDialog(
        title = title,
        subtitle = subtitle,
        testTag = testTag,
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            shape = RoundedCornerShape(16.dp),
            color = DailyRecordSurfaceMuted,
            border = BorderStroke(1.dp, DailyRecordDivider),
        ) {
            Text(
                text = message,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp),
            )
        }
        if (stackActions) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DangerActionButton(
                    label = confirmLabel,
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlineActionButton(
                    label = cancelLabel,
                    onClick = onDismiss,
                    enabled = confirmEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlineActionButton(
                    label = cancelLabel,
                    onClick = onDismiss,
                    enabled = confirmEnabled,
                    modifier = Modifier.weight(1f),
                )
                DangerActionButton(
                    label = confirmLabel,
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun DangerActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (enabled) {
                    Modifier.background(dailyRecordAccentBrush(DailyRecordDanger))
                } else {
                    Modifier.background(DailyRecordSurfaceMuted)
                },
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = label
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) DailyRecordOnAccent else DailyRecordTextSecondary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Low-emphasis app action without Material's default text-button styling. */
@Composable
fun DailyRecordTextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    accessibilityLabel: String = label,
) {
    Box(
        modifier = modifier
            .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = accessibilityLabel
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = when {
                !enabled -> DailyRecordTextSecondary
                danger -> DailyRecordDanger
                else -> DailyRecordDefaultAccent
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Branded transient feedback that follows the active record module. */
@Composable
fun DailyRecordSnackbarHost(
    hostState: SnackbarHostState,
    colors: RecordModuleColorTokens = HandBrewColorTokens,
) {
    SnackbarHost(hostState = hostState) { data ->
        Surface(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .testTag("daily_record_snackbar")
                .semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                },
            shape = RoundedCornerShape(16.dp),
            color = colors.strong,
            border = BorderStroke(1.dp, colors.primary),
            shadowElevation = 1.dp,
        ) {
            Text(
                text = data.visuals.message,
                color = colors.onPrimary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
    }
}
