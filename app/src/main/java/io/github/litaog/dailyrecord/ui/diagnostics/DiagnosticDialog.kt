package io.github.litaog.dailyrecord.ui.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.components.DailyRecordDialog
import io.github.litaog.dailyrecord.ui.components.DailyRecordTextAction
import io.github.litaog.dailyrecord.ui.components.OutlineActionButton
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlass

@Composable
internal fun DiagnosticDialog(
    report: String,
    onDismiss: () -> Unit,
    onCopy: ((String) -> Boolean)? = null,
    onShare: ((String) -> Boolean)? = null,
) {
    val context = LocalContext.current
    val copyAction = onCopy ?: { copyReport(context, it) }
    val shareAction = onShare ?: { shareReport(context, it) }
    var feedback by remember { mutableStateOf<String?>(null) }

    DailyRecordDialog(
        title = AppCopy.Diagnostics.title,
        subtitle = AppCopy.Diagnostics.subtitle,
        testTag = "diagnostic_dialog",
        onDismissRequest = onDismiss,
    ) {
        SelectionContainer {
            Text(
                text = report,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 18.dp)
                    .dailyRecordGlass(
                        shape = RoundedCornerShape(14.dp),
                        level = DailyRecordGlassLevel.Muted,
                    )
                    .padding(14.dp)
                    .testTag("diagnostic_report"),
            )
        }
        if (feedback != null) {
            Text(
                text = feedback.orEmpty(),
                color = DailyRecordDefaultAccent,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("diagnostic_feedback"),
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrimaryActionButton(
                label = AppCopy.Diagnostics.copy,
                onClick = {
                    feedback = if (copyAction(report)) AppCopy.Diagnostics.copied else AppCopy.Diagnostics.copyFailed
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlineActionButton(
                label = AppCopy.Diagnostics.share,
                onClick = {
                    feedback = if (shareAction(report)) null else AppCopy.Diagnostics.noShareTarget
                },
                modifier = Modifier.fillMaxWidth(),
            )
            DailyRecordTextAction(
                label = AppCopy.Diagnostics.back,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            AppCopy.Diagnostics.shareHint,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun copyReport(context: Context, report: String): Boolean = runCatching {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(AppCopy.Diagnostics.clipboardLabel, report))
}.isSuccess

private fun shareReport(context: Context, report: String): Boolean = runCatching {
    val shareIntent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, AppCopy.Diagnostics.clipboardLabel)
        .putExtra(Intent.EXTRA_TEXT, report)
    context.startActivity(
        Intent.createChooser(shareIntent, AppCopy.Diagnostics.share)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}.isSuccess
