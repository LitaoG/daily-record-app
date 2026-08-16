# 2026-08-15 `main` 代码、安全与运行时审计（修复复核）

> 状态：历史审计记录。本文保留 2026-08-15 的基线、修复分支和验证证据；修复结果已进入后续公共 `main`。当前状态以最新 public `main`、当前文档和 CI 为准。

## 基线与范围

- 审计基线：公共仓库 `main`，commit `82d4111`（2026-08-15）。
- 当时修复分支：`agent/ds-pro-main-audit-fixes`，审计起点与上述 `main` 同基线。
- 范围：Android/Kotlin 代码、Firestore 规则、Room/同步生命周期、构建配置、文档一致性、单元/规则/设备测试和模拟器运行时检查。
- 本审计不读取或提交私有生产材料；本地 `google-services.json`、签名配置和日志均未加入 Git。

## 结论摘要

当时修复分支已把原报告中的 P1/P2 可执行项落地并完成验证：没有发现已证实的跨账号读取、Release 明文网络配置或持续性内存泄漏。剩余告警是需要单独做兼容性验证的 SDK/依赖升级建议，不是当时新增漏洞。

1. **P1 物理删除已修复。** `firestore.rules` 已移除客户端 `allow delete`；账号删除改为 `deleteAccountData` trusted callable，由 Admin SDK 按 UID 分批删除两个集合，并要求登录态、UID 匹配和最近 5 分钟内的重新认证。普通记录仍只能通过客户端写墓碑。Android/Firestore Emulator 已验证规则拒绝客户端物理删除，账号删除 callable 能清空两个集合。
2. **P1 `details` 逐项校验已移到可信写入协议。** 当前 Android 远端写入统一调用 `writeDailyCountRecord` callable；服务端事务逐项校验详情键集合、ID/occurrenceIndex 唯一性、次数边界、最多 1000 条详情、时间格式与顺序、感受字段 100 个 Unicode code point 上限、墓碑空详情和 revision/id 并发基线。为兼容 beta.2，Rules 仍允许旧文档省略 `details`；旧客户端直接写入的坏文档由 Functions trigger 在版本不变时删除，因此存在短暂落库窗口但不会长期被应用接受。规则脚本和 connected Android 测试均覆盖该边界。
3. **P2 详情写放大已收敛。** `RoomDailyCountRecordRepository` 只加载一次现有详情，按 occurrence 集合计算过期 ID，使用 DAO 的批量 `IN` 删除和一次 `upsertAll`；缩减次数也只删除越界详情。同步引擎的重复确认读取和远端快照 N+1 查询同时保持已修复状态。
4. **P2 WorkManager 串接队列已改为去抖。** 唯一工作改用 `REPLACE`，并设置 750ms 初始延迟；保存风暴会合并为一个活跃工作项。测试覆盖 20 次连续调度，Android Worker suite 通过。
5. **P2 维护性问题已完成安全清理。** 已按稳定边界拆出 `RecordDetailsComponents.kt` 与 `DateNavigationWheelComponents.kt`，并处理 Modifier 默认参数/顺序、`mutableIntStateOf` 装箱、可保留的 `UseKtx` 告警、重复品牌图标和重复 launcher 资源；没有进行大范围机械重构。同步删除状态机仍保持单一事实源，避免拆分后引入新的状态竞态。当前仍有 5 条 Lint 升级建议：compile/target SDK 37、`core-ktx` 1.19、Lifecycle 2.11、Activity Compose 1.13，需单独验证后升级。

## 本分支已处理

- 登录页的密码、确认密码、密码显示状态、错误和进行中状态改为普通 Compose `remember`，不再进入 Activity SavedState/Bundle；邮箱、模式和导航状态仍可按需要保存。
- 找回密码和账号删除弹窗的进行中/成功/错误状态不再保存到 SavedState，避免进程恢复后显示过期状态或按钮永久锁定。
- 登录、找回密码、账号删除回调统一通过保留取消语义的 `runCatchingPreservingCancellation` 包装；非取消异常会回到错误文案并复位 busy 状态，协程取消不会被吞掉。
- Firestore 规则限制 `localDate` 的月日形状，并禁止 `deleted` 墓碑携带详情；`details` 对 beta.2 旧客户端保持可选，新客户端写入时仍受类型和长度规则约束，规则测试覆盖了兼容缺失字段、非法日期和墓碑数据残留。
- Firestore 客户端物理删除已被规则拒绝；账号删除和当前客户端的记录写入分别通过 `deleteAccountData`、`writeDailyCountRecord` callable 完成。Functions trigger 会对旧客户端直写的详情文档做版本安全的坏文档清理。
- 详情草稿保留真实 `occurrenceIndex`，修复稀疏云端详情在下一次保存时被重排的问题；对超过 512 次的聚合记录不再按次数分配 UI 行，且计数保存会保留当前次数范围内未进入编辑器的详情。
- 详情草稿 Saver 明确区分当前五单元格式和旧四单元格式，进程恢复不会把旧版 `expanded` 标志误当成 `initialized`；512 行边界及“已有详情会保留”的用户文案已同步到产品契约。
- 同步引擎在无 pending 工作时跳过重复的全量确认读取；Room 远端快照应用改为每个快照一次本地记录加载，去除按远端记录的 N+1 查询。
- Firestore retry 错误码集合改为惰性初始化，保留热路径复用且避免 JVM 测试仅因加载同步类就触发 Firebase 静态初始化；远端详情解析也改为显式非空校验，去除生产解析路径的 `!!`。
- Room 详情保存改为按变更集合批量删除/写入；WorkManager 唯一任务改为 750ms 去抖的 `REPLACE` 策略，避免保存风暴形成串接队列。

## 安全基线中已确认正常的部分

- Firestore 读取、创建和更新按 UID 路径隔离，字段白名单、合法日期形状、次数非负、稳定 ID、服务器时间戳、revision 递增以及墓碑详情为空已有规则覆盖；客户端物理删除被规则拒绝。新客户端详情逐项校验由 callable 完成，旧客户端直写由版本安全的 Functions trigger 清理；Rules 仍只对兼容字段做列表/长度边界校验。
- Release Manifest 禁止明文流量；Debug 只对本地 Firebase Emulator 覆盖 HTTP。
- Android Manifest 关闭系统备份并排除数据库/偏好数据；仓库没有发现被 Git 跟踪的 `google-services.json`、签名配置、数据库或口令文件。
- 找回密码对不存在邮箱使用统一成功语义，降低账号枚举风险。
- 密码在当前 UI 代码中只作为短生命周期输入传入认证/重验流程；本分支进一步避免 SavedState 序列化。

## 内存与生命周期结论

静态检查未发现 `GlobalScope`、`observeForever` 或未注销的 `callbackFlow`；Firestore listener、NetworkCallback、同步 Job 和 Compose `DisposableEffect` 都有对应取消/注销路径。应用数据库按进程生命周期保持打开，属于正常单例用法，不是 Activity 泄漏。

模拟器手动检查从登录页进入本机模式，切换日历与统计页 5 轮后没有应用 `FATAL EXCEPTION`。进程 PSS 在统计/切换过程中约从 152 MB 到 192 MB，空闲并触发系统回收后回落到约 155 MB；这不支持持续性泄漏结论，但不是 LeakCanary 或 Android Studio profiler 级证明。若后续优化统计页，应在真实长列表数据和 release/profileable 构建上补充基准。

## 验证证据

| 检查 | 结果 |
|---|---|
| `testDebugUnitTest` | 39 个 suite，228 tests，0 failures，0 errors |
| `lintDebug` | 通过；0 errors，仅 5 条 SDK/依赖升级建议 |
| `assembleDebug`、`assembleDebugAndroidTest` | 通过 |
| 文档/文案/Release metadata Node tests | 8/8 通过 |
| Firestore rules + Functions emulator | 通过；包含所有权、字段形状、坏详情 trigger 清理和客户端物理删除拒绝 |
| `FirebaseEmulatorIntegrationTest` | 3/3 通过；callable 写入、账号物理删除和缺失文档重建 |
| `DailyRecordSyncWorkerTest` | 7/7 通过；包含保存风暴单活跃工作项和云端文档消失后的重建 |
| 本分支 Firebase emulator connected Android | 186 个测试项（185 个执行、1 个生产 Firebase smoke test 跳过），0 failures；通过 `test:android-connected:windows` 启动 Auth/Firestore/Functions emulator |
| 本分支 `AuthScreenTest` | 13 tests，0 failures；首次尝试因模拟器掉线中止，换用稳定 API 34 AVD 后重跑通过 |
| 手动启动 | `io.github.litaog.dailyrecord/.MainActivity` 启动成功；日历/统计 UI 树可见；无应用 Fatal Exception |

## 后续边界

1. 单独建立 SDK/AndroidX 升级变更，完成 compile/target SDK 37、`core-ktx` 1.19、Lifecycle 2.11 和 Activity Compose 1.13 的兼容性与设备回归。
2. 若要进一步优化统计页，应在真实长列表数据和 release/profileable 构建上补充基准；本轮只根据静态生命周期检查和模拟器烟测判断没有已证实泄漏。
3. 若未来彻底淘汰 beta.2 客户端，可把 Rules 的 `details` 可选兼容分支收紧为必填；在此之前，Functions trigger 是旧协议的隔离网。

旧的同步隐私 Goal 和历史审计目录保留为当时证据，不应再当作当前未修复结论；当前状态以本报告、代码、测试和最新公共 `main` 为准。
