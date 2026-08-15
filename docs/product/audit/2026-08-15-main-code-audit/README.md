# 2026-08-15 `main` 代码、安全与运行时审计

## 基线与范围

- 审计基线：公共仓库 `main`，commit `82d4111`（2026-08-15）。
- 当前修复分支：`agent/ds-pro-main-audit-fixes`，与上述 `main` 同基线创建。
- 范围：Android/Kotlin 代码、Firestore 规则、Room/同步生命周期、构建配置、文档一致性、单元/规则/设备测试和模拟器运行时检查。
- 本审计不读取或提交私有生产材料；本地 `google-services.json`、签名配置和日志均未加入 Git。

## 结论摘要

当前 `main` 可以构建、安装并在测试模拟器启动，核心本机日历与统计流程可用；没有发现已证实的跨账号读取、明文网络配置（Release）或持续性内存泄漏。仍有以下发布前应处理的风险：

1. **P1：Firestore 物理删除权限无法区分“账号删除”和普通客户端删除。** `firestore.rules` 对本人路径直接 `allow delete`，而普通清除的产品约定是写墓碑。当前客户端调用路径遵守约定，但任何持有该账号有效会话的客户端也可以直接物理删除本人文档，其他设备只能通过监听/重建逻辑自愈，不能恢复被删除内容。应把账号删除迁移到可信后端/受控 callable 流程，或设计规则可验证的删除协议；不能只在客户端约束。
2. **P1：云端 `details` 条目没有由规则逐项校验，且聚合次数允许 `Int.MAX_VALUE`。** 规则目前只检查 `details` 是列表且长度不超过次数；`RemoteDetailCodec` 会在客户端拒绝坏条目，但恶意或旧客户端仍可写入形状错误、超大内容或非法日期文档。更严重的是，`RecordDetailsDraft.resize()` 按次数创建空行；合法的超大次数文档被打开时可能触发大量分配并导致 OOM。需要先确定产品级明细上限/惰性编辑策略，再同步收紧规则、解析器和 UI；本轮不凭空添加上限。
3. **P2：同步存在全量快照和 Room N+1 查询。** `DailyCountSyncEngine.syncOnce()` 在一次同步中会再次拉取远端集合确认；`RoomDailyCountSyncStoreBase.applyRemoteRecords()` 对每条远端记录查询本地行并逐条替换详情。记录量增长后会放大网络读取、SQLite 往返和详情写放大，建议改成按 owner/date 批量加载、按变更集合增量合并，并减少重复 fetch。
4. **P2：WorkManager 调度可能形成串接队列。** 本地每次保存都会调度唯一工作，当前 `APPEND_OR_REPLACE` 能保留期间产生的变更，但高频编辑会增长任务链；应加入去抖/合并策略，并用测试覆盖“保存风暴”与进程重启。
5. **P2：代码维护成本偏高。** `RecordScreen.kt`、`DateNavigationDialog.kt`、统计卡片和同步屏障类过大；Lint 还报告 Modifier 参数顺序/默认值、`mutableStateOf(Int)` 装箱、重复资源以及 SDK/依赖升级提示。建议按稳定边界拆分，并在独立提交中处理 Lint，不把大规模机械重构和安全修复混在一起。

## 本分支已处理

- 登录页的密码、确认密码、密码显示状态、错误和进行中状态改为普通 Compose `remember`，不再进入 Activity SavedState/Bundle；邮箱、模式和导航状态仍可按需要保存。
- 找回密码和账号删除弹窗的进行中/成功/错误状态不再保存到 SavedState，避免进程恢复后显示过期状态或按钮永久锁定。
- 登录、找回密码、账号删除回调统一通过保留取消语义的 `runCatchingPreservingCancellation` 包装；非取消异常会回到错误文案并复位 busy 状态，协程取消不会被吞掉。

## 安全基线中已确认正常的部分

- Firestore 读取、创建和更新按 UID 路径隔离，字段白名单、次数非负、稳定 ID、服务器时间戳和 revision 递增已有规则覆盖。
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
| `testDebugUnitTest` | 39 个 suite，223 tests，0 failures，0 errors |
| `lintDebug`、`assembleDebug`、`assembleDebugAndroidTest` | 通过；存在上述非致命 Lint warnings/hints |
| 文档/文案/Release metadata Node tests | 8/8 通过 |
| Firestore rules emulator | 通过；包含所有权、字段形状、revision 和删除路径断言 |
| `main` 基线 connected Android emulator | 184 tests，1 个生产 Firebase smoke test 跳过，0 failures |
| 本分支 `AuthScreenTest` | 13 tests，0 failures；首次尝试因模拟器掉线中止，换用稳定 API 34 AVD 后重跑通过 |
| 手动启动 | `io.github.litaog.dailyrecord/.MainActivity` 启动成功；日历/统计 UI 树可见；无应用 Fatal Exception |

## 建议执行顺序

1. 先解决 Firestore 物理删除协议和 `details`/超大次数的服务端边界；这两项直接影响数据完整性与可用性。
2. 再做同步批量合并与 WorkManager 调度去抖，并用基准数据验证网络读取、Room 查询和内存峰值。
3. 最后按文件边界拆分 Compose/同步大文件，逐项清理 Lint warnings；升级 compile/target SDK 和依赖时单独跑兼容性与设备回归。

旧的同步隐私 Goal 和历史审计目录保留为当时证据，不应再当作当前未修复结论；当前状态以本报告、代码、测试和最新公共 `main` 为准。
