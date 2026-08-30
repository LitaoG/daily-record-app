# 2026-08 性能与测试加固日志

状态：`current log — agent/optim-perf 分支`
最后更新：2026-08-16

来源：2026-08-16 用户要求“做完双语适配后继续规划并执行优化长任务”，本文件记录已完成与进行中的优化证据。

## 完成项（全部在 `agent/optim-perf` 分支，前缀 `[ds:v4pro]`）

### 性能（提交 `f3e1d24`、`9fca165`）

- `RecordModuleUiSpec` 改为 `remember(selectedModule, language)` 按需构建：消除每次重组新建 spec 实例导致 `backdropBrush` 缓存失效与全树参数不稳定；语言作为 key 使无 recreate 场景下文案也能即时切换。
- `DailyRecordApp` 日期解析（`selectedDate`/`browseDate`）改为 `remember`，不再每条记录发射时重复 `LocalDate.parse`。
- `StatisticsModels.buildWeek` 的 O(7n) 线性扫描改为 `associateBy` 映射（与 buildMonth 对齐）。
- 年图揭示动画：`revealProgress` 不再在组合阶段读取（原导致整卡每帧重组），改为在 Canvas draw 块与 `graphicsLayer` 内读取，动画只触发重绘。
- 月/年图 `offsets` 列表 `remember` 化，避免每帧重建 Offset 数组；记录页 `storedMonthCount/Days` 聚合 `remember` 化。
- 周期胶囊去掉每个 tab 各自创建的 `MutableInteractionSource`；记录详情行增加 `key(occurrenceIndex)` 防止删除行后输入焦点错位。

### 测试（提交 `45cdf47`、`f20b4de`、`8692612`）

- 新增 `DatabaseConvertersTest`：null 往返、垃圾输入抛错、Instant 边界、带秒/纳秒时间解析。
- 新增 `TimeFormatTest`：0/1439/60 进位与 `%02d` 格式。
- 新增 `SyncModelsIdTest`：`localCopyId`/`localCopySourceId`/`recoveryOwnerId` 往返与命名空间隔离。
- 新增 `RemoteDetailCodecTest`：秒/纳秒精度拒绝、非字符串时间、结束早于开始、空白 id、空感受、`detailToMap` 回程。
- `YearLineChartTest` 补 5-step 档位与空年份/最大值 4 边界。
- `StatisticsPeriodAnchorsTest` 补跨年周/月、next 钳制（含“下一周期包含今天→钳到今天”）、`shiftMonthAnchor` 负偏移与两端 coerce。
- `StatisticsModelsTest` 补年末跨年周、并列最低月份、空年/空月极值空态。

## 验证证据

| 检查 | 结果 |
|---|---|
| `testDebugUnitTest` | 265 tests，0 failures（含双语矩阵） |
| `lintDebug` | 通过（0 errors） |
| `assembleDebug` / `assembleDebugAndroidTest` | 通过 |
| API 34 模拟器 connected（DailyRecordAppTest + CalendarScreenTest + RecordScreenTest + DailyRecordAppEnglishTest + LanguagePreferenceTest，共三轮） | 49/49、43/43、4/4 通过 |

## 进行中 / 待办

- 统计页 per-period 范围 Flow（按周期缩小收集范围，减少全量历史列表触发全壳重组）——中等风险，待独立变更验证。
- `pnpm test:docs`/`test:copy` 等 Node 门禁：本机无 Node.js，待有 Node 22 环境执行。
- 双语言运行截图与 TalkBack 抽查：待常规会话补采。
- 设备端 200% 字体双语言长词验证：依赖上述截图会话。