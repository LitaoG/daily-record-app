# 2026-08-15 `main` 代码、安全与运行时审计

## 基线与范围

- 审计基线：公共仓库 `main`，commit `82d4111`（2026-08-15）。
- 当前修复分支：`agent/ds-pro-main-audit-fixes`，审计起点与上述 `main` 同基线。
- 范围：Android/Kotlin 代码、Firestore 规则、Room/同步生命周期、构建配置、文档一致性、单元/规则/设备测试和模拟器运行时检查。
- 本审计不读取或提交私有生产材料；本地 `google-services.json`、签名配置和日志均未加入 Git。

## 结论摘要

当前 `main` 可以构建、安装并在测试模拟器启动，核心本机日历与统计流程可用；没有发现已证实的跨账号读取、明文网络配置（Release）或持续性内存泄漏。仍有以下发布前应处理的风险：

1. **P1：Firestore 物理删除权限无法区分“账号删除”和普通客户端删除。** `firestore.rules` 对本人路径直接 `allow delete`，而普通清除的产品约定是写墓碑。当前客户端调用路径遵守约定，但任何持有该账号有效会话的客户端也可以直接物理删除本人文档，其他设备只能通过监听/重建逻辑自愈，不能恢复被删除内容。应把账号删除迁移到可信后端/受控 callable 流程，或设计规则可验证的删除协议；不能只在客户端约束。
2. **P1：云端 `details` 条目仍没有由规则逐项校验。** Rules 已限制日期形状，并禁止墓碑保留详情；为兼容 beta.2 旧客户端，`details` 字段在 Rules 边界保持可选，缺失时解析器按空列表处理。Firestore Rules 不能逐项遍历数组，`RemoteDetailCodec` 仍是客户端/解析器边界。持有本人会话的恶意或旧客户端仍可能写入形状错误、超大内容或非法日期文档，之后被解析器拒绝并计入 rejected。聚合次数仍允许 `Int.MAX_VALUE`，但 `RecordDetailsDraft` 现在最多物化 512 行，超大次数只保留聚合值并保持详情折叠，避免由远端数据触发 OOM。残余风险是服务端无法阻止坏详情文档落库，需后端协议或可验证的细粒度规则。
3. **P2：同步的重复 fetch 与 Room N+1 已在本分支消除，详情写放大仍存在。** `DailyCountSyncEngine.syncOnce()` 只有在存在 pending 上传时才做第二次确认拉取；`RoomDailyCountSyncStoreBase.applyRemoteRecords()` 每个快照先一次性加载本地记录，再按日期在内存中合并。当前仍对每条变化逐条删除/写入详情，记录量增长后会放大 SQLite 往返和详情写入；可继续按变更集合批量删除/插入并做基准验证。
4. **P2：WorkManager 调度可能形成串接队列。** 本地每次保存都会调度唯一工作，当前 `APPEND_OR_REPLACE` 能保留期间产生的变更，但高频编辑会增长任务链；应加入去抖/合并策略，并用测试覆盖“保存风暴”与进程重启。
5. **P2：代码维护成本偏高。** `RecordScreen.kt`、`DateNavigationDialog.kt`、统计卡片和同步屏障类过大；Lint 还报告 Modifier 参数顺序/默认值、`mutableStateOf(Int)` 装箱、重复资源以及 SDK/依赖升级提示。建议按稳定边界拆分，并在独立提交中处理 Lint，不把大规模机械重构和安全修复混在一起。

## 本分支已处理

- 登录页的密码、确认密码、密码显示状态、错误和进行中状态改为普通 Compose `remember`，不再进入 Activity SavedState/Bundle；邮箱、模式和导航状态仍可按需要保存。
- 找回密码和账号删除弹窗的进行中/成功/错误状态不再保存到 SavedState，避免进程恢复后显示过期状态或按钮永久锁定。
- 登录、找回密码、账号删除回调统一通过保留取消语义的 `runCatchingPreservingCancellation` 包装；非取消异常会回到错误文案并复位 busy 状态，协程取消不会被吞掉。
- Firestore 规则限制 `localDate` 的月日形状，并禁止 `deleted` 墓碑携带详情；`details` 对 beta.2 旧客户端保持可选，新客户端写入时仍受类型和长度规则约束，规则测试覆盖了兼容缺失字段、非法日期和墓碑数据残留。
- 详情草稿保留真实 `occurrenceIndex`，修复稀疏云端详情在下一次保存时被重排的问题；对超过 512 次的聚合记录不再按次数分配 UI 行，且计数保存会保留当前次数范围内未进入编辑器的详情。
- 详情草稿 Saver 明确区分当前五单元格式和旧四单元格式，进程恢复不会把旧版 `expanded` 标志误当成 `initialized`；512 行边界及“已有详情会保留”的用户文案已同步到产品契约。
- 同步引擎在无 pending 工作时跳过重复的全量确认读取；Room 远端快照应用改为每个快照一次本地记录加载，去除按远端记录的 N+1 查询。
- Firestore retry 错误码集合改为惰性初始化，保留热路径复用且避免 JVM 测试仅因加载同步类就触发 Firebase 静态初始化；远端详情解析也改为显式非空校验，去除生产解析路径的 `!!`。

## 安全基线中已确认正常的部分

- Firestore 读取、创建和更新按 UID 路径隔离，字段白名单、合法日期形状、次数非负、稳定 ID、服务器时间戳、revision 递增以及墓碑详情为空已有规则覆盖；`details` 对 beta.2 旧客户端可省略、对新客户端若存在则校验类型和长度，规则仍不能逐项遍历条目。
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
| `lintDebug`、`assembleDebug`、`assembleDebugAndroidTest` | 通过；0 errors、19 warnings、1 hint |
| 文档/文案/Release metadata Node tests | 8/8 通过 |
| Firestore rules emulator | 通过；包含所有权、字段形状、revision 和删除路径断言 |
| 本分支 Firebase emulator connected Android | 184 tests（183 执行、1 个生产 Firebase smoke test 跳过），0 failures；通过 `test:android-connected:windows` 启动 Auth/Firestore emulator |
| 本分支 `AuthScreenTest` | 13 tests，0 failures；首次尝试因模拟器掉线中止，换用稳定 API 34 AVD 后重跑通过 |
| 手动启动 | `io.github.litaog.dailyrecord/.MainActivity` 启动成功；日历/统计 UI 树可见；无应用 Fatal Exception |

## 建议执行顺序

1. 先解决 Firestore 物理删除协议和 `details` 逐项服务端校验；这两项直接影响数据完整性。
2. 再做详情变更的批量合并与 WorkManager 调度去抖，并用基准数据验证网络读取、Room 查询和内存峰值。
3. 最后按文件边界拆分 Compose/同步大文件，逐项清理 Lint warnings；升级 compile/target SDK 和依赖时单独跑兼容性与设备回归。

旧的同步隐私 Goal 和历史审计目录保留为当时证据，不应再当作当前未修复结论；当前状态以本报告、代码、测试和最新公共 `main` 为准。
