# 2026-08 性能与测试加固日志

状态：`current log — agent/optim-perf 分支`
最后更新：2026-09-01

来源：2026-08-16 用户要求“做完双语适配后继续规划并执行优化长任务”，本文件记录已完成与进行中的优化证据。

## 完成项（全部在 `agent/optim-perf` 分支，前缀 `[MS:1.2]`/`[ds:v4pro]`）

### 性能（提交 `f3e1d24`、`9fca165`、`d307cf4`）

- `RecordModuleUiSpec` 改为 `remember(selectedModule, language)` 按需构建：消除每次重组新建 spec 实例导致 `backdropBrush` 缓存失效与全树参数不稳定；语言作为 key 使无 recreate 场景下文案也能即时切换。
- `DailyRecordApp` 日期解析（`selectedDate`/`browseDate`）改为 `remember`，不再每条记录发射时重复 `LocalDate.parse`。
- `StatisticsModels.buildWeek` 的 O(7n) 线性扫描改为 `associateBy` 映射（与 buildMonth 对齐）。
- 年图揭示动画：`revealProgress` 不再在组合阶段读取（原导致整卡每帧重组），改为在 Canvas draw 块与 `graphicsLayer` 内读取，动画只触发重绘。
- 月/年图 `offsets` 列表 `remember` 化，避免每帧重建 Offset 数组；记录页 `storedMonthCount/Days` 聚合 `remember` 化。
- 周期胶囊去掉每个 tab 各自创建的 `MutableInteractionSource`；记录详情行增加 `key(occurrenceIndex)` 防止删除行后输入焦点错位。
- 日历/记录页改按月范围收集：`monthRecordsFlow` 按 `displayedMonth` 裁剪，全量流仅统计页与首屏加载使用；`distinctUntilChanged()` 去重，已验证 43/43 设备回归。

### 测试与门禁（提交 `45cdf47`、`f20b4de`、`8692612`、`edae318`）

- 新增 `DatabaseConvertersTest`：null 往返、垃圾输入抛错、Instant 边界、带秒/纳秒时间解析。
- 新增 `TimeFormatTest`：0/1439/60 进位与 `%02d` 格式。
- 新增 `SyncModelsIdTest`：`localCopyId`/`localCopySourceId`/`recoveryOwnerId` 往返与命名空间隔离。
- 新增 `RemoteDetailCodecTest`：秒/纳秒精度拒绝、非字符串时间、结束早于开始、空白 id、空感受、`detailToMap` 回程。
- `YearLineChartTest` 补 5-step 档位与空年份/最大值 4 边界。
- `StatisticsPeriodAnchorsTest` 补跨年周/月、next 钳制（含“下一周期包含今天→钳到今天”）、`shiftMonthAnchor` 负偏移与两端 coerce。
- `StatisticsModelsTest` 补年末跨年周、并列最低月份、空年/空月极值空态。
- `copy-integrity` 守卫更新为双语契约（ZhStrings 放中文，EnStrings 仅 `languageZh="中文"` 自名）。

## 验证证据

| 检查 | 结果 |
|---|---|
| `testDebugUnitTest` | 265 tests，0 failures（含双语矩阵与统计扩展） |
| `lintDebug` | 通过（0 errors） |
| `assembleDebug` / `assembleDebugAndroidTest` | 通过 |
| `pnpm test:docs` / `test:copy` / `test:release-metadata`（Node 22.23.2 便携版） | 4/4、2/2、4/4 通过 |
| `pnpm test:firestore-rules`（Firestore + Functions 模拟器） | 通过（ownership/shape/revision/detail validation） |
| API 34 模拟器 connected（`DailyRecordAppTest`/`CalendarScreenTest`/`RecordScreenTest`/`DailyRecordAppEnglishTest`/`LanguagePreferenceTest` 定向） | 49/49、43/43、4/4 通过 |
| API 34 全量 `connectedDebugAndroidTest`（194 tests，`test:android-connected:windows`） | 194 中 1 注入抖动 `Failed to inject touch input`（环境，非代码），CI 重试通过；定向子集 53/53 全绿 |
| 双语言截图与 TalkBack 语义抽查 | 完成（`Pixel_4_API_34`，ZH/EN 正常+200% 6 张，`uiautomator dump` 验证 `Solo` 可见/`Masturbation records, selected` 语义，见 `docs/product/audit/2026-09-01-i18n-bilingual/`） |