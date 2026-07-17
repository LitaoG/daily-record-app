package io.github.litaog.dailyrecord.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.data.HandBrewRecordRepository
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.ui.components.BrewCountControl
import io.github.litaog.dailyrecord.ui.components.OutlineActionButton
import io.github.litaog.dailyrecord.ui.components.PlaneIcon
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.Ink500
import io.github.litaog.dailyrecord.ui.theme.Ink700
import io.github.litaog.dailyrecord.ui.theme.Ink900
import io.github.litaog.dailyrecord.ui.theme.Neutral300
import io.github.litaog.dailyrecord.ui.theme.Paper0
import io.github.litaog.dailyrecord.ui.theme.Paper100
import io.github.litaog.dailyrecord.ui.theme.Paper50
import io.github.litaog.dailyrecord.ui.theme.Terracotta400
import io.github.litaog.dailyrecord.ui.theme.Terracotta500
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun RecordScreen(
    date: LocalDate,
    today: LocalDate,
    repository: HandBrewRecordRepository,
    monthRecords: List<HandBrewRecord>,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val recordFlow = remember(repository, date) { repository.observeRecord(date) }
    val record by recordFlow.collectAsState(initial = null)
    var draftCount by remember(date) { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val editable = date <= today

    LaunchedEffect(record?.id, record?.updatedAt) {
        draftCount = record?.brewCount ?: 0
    }

    val storedMonthCount = monthRecords.sumOf { it.brewCount }
    val storedMonthDays = monthRecords.count { it.brewCount > 0 }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("record_screen"),
        containerColor = Paper50,
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding(),
                color = Paper50,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PrimaryActionButton(
                        label = if (saving) "正在保存…" else "保存记录",
                        enabled = editable && !saving,
                        onClick = {
                            scope.launch {
                                saving = true
                                val now = Instant.now()
                                repository.saveRecord(
                                    HandBrewRecord(
                                        id = record?.id ?: UUID.randomUUID().toString(),
                                        localDate = date,
                                        brewCount = draftCount,
                                        createdAt = record?.createdAt ?: now,
                                        updatedAt = now,
                                    ),
                                )
                                saving = false
                                onSaved()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlineActionButton(
                        label = "清除记录",
                        enabled = editable && record != null && !saving,
                        onClick = {
                            scope.launch {
                                saving = true
                                repository.clearRecord(date)
                                saving = false
                                onSaved()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RecordHeader(date = date, onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Terracotta400),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                PlaneIcon(color = Terracotta500, modifier = Modifier.size(34.dp))
                Text(
                    text = if (date == today) "今天手冲了几次？" else "这天手冲了几次？",
                    color = Ink900,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    "调整次数后点击保存",
                    color = Ink700,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Paper0)
                    .border(1.dp, Neutral300, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = when {
                        !editable -> "未来日期 · 不可记录"
                        record == null -> "尚未填写"
                        record?.brewCount == 0 -> "已记录 · 0 次"
                        else -> "已记录 · " + record?.brewCount + " 次"
                    },
                    color = if (editable) Terracotta500 else Ink500,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            BrewCountControl(
                count = draftCount,
                enabled = editable && !saving,
                hasRecord = record != null,
                onDecrease = { draftCount = (draftCount - 1).coerceAtLeast(0) },
                onIncrease = { draftCount += 1 },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Paper0)
                    .border(1.dp, Neutral300, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("记录规则", color = Ink900, style = MaterialTheme.typography.labelLarge)
                Text("0 次＝明确没冲，会保留记录。", color = Ink700, style = MaterialTheme.typography.labelSmall)
                Text("清除记录＝恢复未填写，不进入统计。", color = Ink700, style = MaterialTheme.typography.labelSmall)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Paper100)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    YearMonth.from(date).monthValue.toString() + "月累计",
                    color = Ink900,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    "$storedMonthCount 次 · $storedMonthDays 天",
                    color = Ink900,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun RecordHeader(date: LocalDate, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Paper0)
                .border(1.dp, Neutral300, CircleShape)
                .clickable(onClick = onBack)
                .semantics { role = Role.Button; contentDescription = "返回日历" },
            contentAlignment = Alignment.Center,
        ) {
            Text("‹", color = Ink900, style = MaterialTheme.typography.headlineMedium)
        }
        Text(
            text = date.monthValue.toString() + "月" + date.dayOfMonth + "日 · " + weekdayName(date),
            color = Ink700,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun weekdayName(date: LocalDate): String = when (date.dayOfWeek.value) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    else -> "周日"
}
