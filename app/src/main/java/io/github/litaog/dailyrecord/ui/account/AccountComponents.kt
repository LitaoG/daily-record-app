package io.github.litaog.dailyrecord.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.theme.Ink500
import io.github.litaog.dailyrecord.ui.theme.Ink700
import io.github.litaog.dailyrecord.ui.theme.Ink900
import io.github.litaog.dailyrecord.ui.theme.Neutral300
import io.github.litaog.dailyrecord.ui.theme.Paper0
import io.github.litaog.dailyrecord.ui.theme.Terracotta400
import io.github.litaog.dailyrecord.ui.theme.Terracotta500

@Composable
internal fun AccountTopBar(
    status: SyncStatus,
    onClick: () -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.4f
    Surface(color = Paper0, shadowElevation = 2.dp) {
        if (largeText) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AccountTitle("只记录手冲次数")
                SyncStatusChip(
                    status = status,
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .heightIn(min = 58.dp)
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AccountTitle("只记录手冲次数")
                SyncStatusChip(status = status, onClick = onClick)
            }
        }
    }
}

@Composable
internal fun LocalAccountTopBar(onClick: () -> Unit) {
    val largeText = LocalDensity.current.fontScale >= 1.4f
    Surface(color = Paper0, shadowElevation = 2.dp) {
        if (largeText) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AccountTitle("本机记录可离线使用")
                TextButton(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "登录账号并开启云同步" },
                ) {
                    Text("登录同步", color = Terracotta500)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .heightIn(min = 58.dp)
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AccountTitle("本机记录可离线使用")
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.semantics { contentDescription = "登录账号并开启云同步" },
                ) {
                    Text("登录同步", color = Terracotta500)
                }
            }
        }
    }
}

@Composable
private fun AccountTitle(subtitle: String) {
    Column {
        Text(
            "手冲日历",
            color = Ink900,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(subtitle, color = Ink500, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SyncStatusChip(
    status: SyncStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Terracotta400)
            .border(1.dp, Neutral300, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .semantics {
                role = Role.Button
                contentDescription = "账号与云同步，${status.label()}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(8.dp)) {
            drawCircle(status.color())
        }
        Text(status.shortLabel(), color = Ink900, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun AccountDialog(
    email: String,
    status: SyncStatus,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (confirmSignOut) "确认退出登录？" else "账号与云同步") },
        text = {
            if (confirmSignOut) {
                Text("退出后云端记录不会删除；本机缓存仍按账号隔离，下次登录会继续同步。")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(email, color = Ink900, style = MaterialTheme.typography.bodyLarge)
                    Text(status.label(), color = status.color(), style = MaterialTheme.typography.labelLarge)
                    Text(
                        "本机修改先保存到 Room，断网时仍可记录；联网后自动上传并在其他手机恢复。",
                        color = Ink700,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            if (confirmSignOut) {
                TextButton(onClick = onSignOut) { Text("确认退出", color = MaterialTheme.colorScheme.error) }
            } else {
                TextButton(onClick = onSyncNow, enabled = status !is SyncStatus.Syncing) {
                    Text("立即同步", color = Terracotta500)
                }
            }
        },
        dismissButton = {
            if (confirmSignOut) {
                TextButton(onClick = { confirmSignOut = false }) { Text("取消") }
            } else {
                Row {
                    TextButton(onClick = { confirmSignOut = true }) { Text("退出登录") }
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
            }
        },
    )
}

internal fun SyncStatus.label(): String = when (this) {
    SyncStatus.NotConfigured -> "云端尚未配置"
    SyncStatus.Offline -> "当前离线，记录已保存在本机"
    SyncStatus.Syncing -> "正在同步"
    SyncStatus.UpToDate -> "云端已同步"
    is SyncStatus.Pending -> "有 $count 条记录等待同步"
    is SyncStatus.Failed -> "同步失败：$message"
}

private fun SyncStatus.shortLabel(): String = when (this) {
    SyncStatus.NotConfigured -> "未配置"
    SyncStatus.Offline -> "离线"
    SyncStatus.Syncing -> "同步中"
    SyncStatus.UpToDate -> "已同步"
    is SyncStatus.Pending -> "待同步 $count"
    is SyncStatus.Failed -> "需重试"
}

private fun SyncStatus.color() = when (this) {
    SyncStatus.UpToDate -> androidx.compose.ui.graphics.Color(0xFF2E7D5B)
    SyncStatus.Syncing -> Terracotta500
    SyncStatus.Offline, is SyncStatus.Pending -> androidx.compose.ui.graphics.Color(0xFF8A6A18)
    SyncStatus.NotConfigured, is SyncStatus.Failed -> androidx.compose.ui.graphics.Color(0xFF9B3A32)
}
