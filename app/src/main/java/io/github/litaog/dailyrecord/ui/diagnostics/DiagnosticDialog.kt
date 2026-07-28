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
import io.github.litaog.dailyrecord.ui.components.HandBrewDialog
import io.github.litaog.dailyrecord.ui.components.HandBrewTextAction
import io.github.litaog.dailyrecord.ui.components.OutlineActionButton
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.Ink500
import io.github.litaog.dailyrecord.ui.theme.Ink700
import io.github.litaog.dailyrecord.ui.theme.Neutral300
import io.github.litaog.dailyrecord.ui.theme.Paper100
import io.github.litaog.dailyrecord.ui.theme.Terracotta500

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

    HandBrewDialog(
        title = "本机诊断信息",
        subtitle = "不包含邮箱、私密记录日期、次数或密码",
        testTag = "diagnostic_dialog",
        onDismissRequest = onDismiss,
    ) {
        SelectionContainer {
            Text(
                text = report,
                color = Ink700,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 18.dp)
                    .background(Paper100, RoundedCornerShape(14.dp))
                    .border(1.dp, Neutral300, RoundedCornerShape(14.dp))
                    .padding(14.dp)
                    .testTag("diagnostic_report"),
            )
        }
        if (feedback != null) {
            Text(
                text = feedback.orEmpty(),
                color = Terracotta500,
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
                label = "复制诊断信息",
                onClick = {
                    feedback = if (copyAction(report)) "诊断信息已复制" else "复制失败，请手动选择文字"
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlineActionButton(
                label = "分享诊断信息",
                onClick = {
                    feedback = if (shareAction(report)) null else "没有找到可用的分享应用"
                },
                modifier = Modifier.fillMaxWidth(),
            )
            HandBrewTextAction(
                label = "返回",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            "发送前仍可长按检查或选择其中的文字。",
            color = Ink500,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun copyReport(context: Context, report: String): Boolean = runCatching {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText("私密日历诊断信息", report))
}.isSuccess

private fun shareReport(context: Context, report: String): Boolean = runCatching {
    val shareIntent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, "私密日历诊断信息")
        .putExtra(Intent.EXTRA_TEXT, report)
    context.startActivity(
        Intent.createChooser(shareIntent, "分享诊断信息")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}.isSuccess
