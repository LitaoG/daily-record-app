# 工程规范

状态：`current — authoritative engineering standards`
最后复核：2026-11-05

本文件定义本仓库的代码组织、分层、异步、数据、安全与测试规范。它回答“怎么写”，不重复回答“为什么这样设计”。

已由专门文档拥有的事实一律**引用而不复制**：

| 主题 | 权威来源 |
|---|---|
| 产品范围与验收 | [`PRODUCT.md`](PRODUCT.md)、[`product/USER_STORIES_AND_ACCEPTANCE.md`](product/USER_STORIES_AND_ACCEPTANCE.md) |
| 分层、数据流、包边界 | [`ARCHITECTURE.md`](ARCHITECTURE.md) |
| Room 表结构、字段、迁移 | [`DATA_MODEL.md`](DATA_MODEL.md) |
| 统计口径 | [`STATISTICS.md`](STATISTICS.md) |
| 同步、冲突、删除语义 | [`SYNC_AND_PRIVACY.md`](SYNC_AND_PRIVACY.md) |
| 界面、状态、无障碍 | [`UI_UX.md`](UI_UX.md) |
| 测试范围与门禁 | [`TESTING.md`](TESTING.md) |
| 长期决策 | [`DECISIONS.md`](DECISIONS.md) |

## 1. 代码改动两档制（最小改动 · 优先复用）

改动代码尽可能改动最小部分的代码，不随意新建平行实现，注意精简。遵循“最小改动 · 优先复用”原则，按改动性质分常规档与重构档执行，判据为：**“这次改动会不会改变运行时可观察行为？”**

- **常规档（改行为：修 Bug、调参数、改交互、改文案、优化逻辑）**：
  - **就地改**：在已有逻辑处就地改那一处，不在旁边另写一份平行实现；
  - **优先复用**：动手前先搜索仓库内同类实现（函数、组件、转换扩展）；同一语义**只留一处**，能复用或扩展既有函数就绝不新建；
  - **绝不夹带**：顺手重构、顺手改名、批量格式化、清理看似无用的旧代码一律不做；
  - **自检控制**：提交前自检 `git diff --stat`，改动明显超出目标时砍回范围内。
- **重构档（不改行为：拆文件、解耦、消除重复实现、降低圈复杂度）**：
  - **显式豁免**“就地改”与“不新建”：重构必然要把代码搬迁或抽取，允许新建模块抽取公共实现（这是消除重复的唯一途径，不属于平行实现）；
  - **零行为变更**：纯搬迁与抽取，运行时可观察行为、UI 呈现、返回值与数据计算逐字逐像素保持不变；
  - **单步内聚**：一次只搬迁一个内聚单元，单独成提交或 PR，测试全绿再进行下一块；
  - **接口兼容**：原模块通过保留调用转发或统一导出来保持原有外部调用方稳定。

## 2. Kotlin 语言规范与单文件硬顶

- 遵循官方 Kotlin 风格：4 空格缩进、行宽约 100 字符、表达式体用于简单函数。
- **单文件行数硬顶**：Kotlin 源码文件目标控制在 400 行以内；新增代码文件物理总行数严禁超过 800 行；
- **历史超长文件拆分**：存量超标文件已于 2026-11 全量拆分治理完毕（Issue #299），400 行基准自此对全部存量与新增文件生效，不再有历史豁免；后续仍按重构档在单一提交内搬迁抽离，不发起纯刷行数的无实质改动；
- **函数单一职责**：一个函数/Composable 严禁同时揉杂“状态机 + I/O + 文案拼装”，遇到此类情况必须拆分；
- 命名按模块区分：共享的每日次数机制用 `DailyCount*`，模块专有类型用 `HandBrew*` / `Sex*`。新增共享类型前先确认两个模块确实完全同形，否则保留两份。
- 可见性从紧：默认 `internal`；只有确实需要跨模块公开的才放宽。测试可见性用 `internal` 而不是 `public`。
- 不做投机抽象：只有一个调用方时不引入接口；出现第二个同形实现时再提取。
- 禁止 `!!`，除非紧邻的判空已经成立。禁止吞掉异常（空 `catch` 必须有说明其意图的注释）。

## 3. Jetpack Compose

- 状态提升 + 单向数据流：页面持有状态，组件只接收值和回调。
- 需要跨配置变更保留的状态用 `rememberSaveable`；否则用 `remember`。
- `Flow` 一律用 `collectAsStateWithLifecycle` 收集，禁止在 Composable 里裸 `.collect`。
- 派生数据用 `remember`/`derivedStateOf` 缓存，不要在重组中重复计算 O(n) 以上的工作。
- 定时刷新等循环逻辑用 `produceState` + `delay`，随组合销毁自动取消；禁止 `GlobalScope`。
- 组件不得直接访问 Repository 或数据库，只通过参数/状态接收数据。
- 弹窗、按钮、输入框一律使用 `ui/components/` 中的项目组件，禁止在运行时源码里直接使用默认 `AlertDialog` / `TextButton`。
- 颜色不得作为唯一状态编码；每个关键控件具备 TalkBack 可读的 `contentDescription` 或语义。
- 依赖尺寸用 `DailyRecordSizes` / `DailyRecordSpacing` / `DailyRecordShapes`；魔法数字只在有测量依据并附带注释时使用（例如已记录的像素对齐偏移）。

## 4. 分层边界

- `ui/` → Repository → Room；`ui/` 不得绕过 Repository 读写数据库。
- `core/data/` 是唯一数据入口；`core/database/` 只暴露 DAO 与 Entity。
- `core/di/` 负责装配，不承载业务规则。
- 每个模块保持自己的 Store、RemoteDataSource 和映射；共享只发生在**已经证明完全相同**的算法上（见 [`ARCHITECTURE.md`](ARCHITECTURE.md)）。
- 不得引入 `Activity`、`MeasurementType`、通用活动表或自定义活动抽象。

## 5. Room 与数据一致性

- schema 版本只在一处声明：`DailyRecordDatabase.SCHEMA_VERSION`，`@Database(version = ...)` 引用它。
- 每次版本递增必须提供显式 `Migration`，禁止 `fallbackToDestructiveMigration`。
- 迁移必须处理真实旧数据（含 `status`/`quantity` 这类历史列语义），"空表重建"不是迁移。
- 每次版本变化必须同时更新 `app/schemas/` 快照并跑设备迁移测试。
- 每个模块每天最多一条记录，由唯一索引保证；日期范围读取固定使用半开区间 `[start, endExclusive)`。
- 事务内的多次写入必须原子；跨模块操作不得共享事务。
- 删除详情行时分批，单批不超过 `DETAIL_DELETE_CHUNK_SIZE`（SQLite 绑定变量上限）。

## 6. 协程与异步

- 结构化并发：用 `coroutineScope` / `supervisorScope` 而不是裸 `launch` 泄漏；禁止 `GlobalScope`、生产代码中的 `runBlocking` 和 `Thread.sleep`。
- 阻塞 I/O 放到 `Dispatchers.IO`；不要把磁盘或网络调用留在 `Dispatchers.Main.immediate` 上。
- 取消必须传播：捕获异常时先 `catch (error: CancellationException) { throw error }`。
- 失败隔离：一个模块失败不得跳过另一个模块；失败收集后在所有模块跑完后重新抛出（`CombinedSyncCoordinator` 的既有语义）。
- 重试必须有界：WorkManager 用 `Result.retry()` 时配合尝试次数上限。
- 交互式云操作用 `runInteractiveCloudOperation` 统一超时（5 秒交互 / 30 秒后台），不要各自发明超时。

## 7. Firebase 与同步

- 写入必须经可信 Cloud Functions callable；客户端不得直接写生产文档（Rules 不提供 `allow delete`）。
- Rules 与客户端字段名必须一致；改动任一侧都要跑 `pnpm test:firestore-rules`。
- 远端快照必须**声明来源**：`RemoteSnapshot.sourceCollection` 由读取方写入，模块只处理属于自己集合的快照。任何"空快照即删除"的判断都必须同时校验来源集合，禁止从记录列表反推集合。
- 文档物理消失只有在新、完整、无拒收文档的服务端快照下才允许清理本地镜像行（ADR-020）；pending 编辑和从未上传的行永不清理。
- 离线优先：任何同步失败都必须保留本机数据，并给出可区分网络/权限/服务/数据的用户可见状态。

## 8. 隐私与安全

- 密码、重新认证口令，以及登录、找回密码、账号删除弹窗的进行中和错误状态只用 `remember`，不进入 `rememberSaveable` / SavedState；进程恢复后宁可让用户重新输入。邮箱、模式和弹窗是否打开这类非敏感状态可以保存。
- **生产代码不允许日志输出**。当前 `app/src/` 没有任何日志调用，这是有意的隐私约束，不要为了调试引入 `Log`、`println` 或崩溃上报 SDK；临时排查完成后必须移除。
- Functions 中唯一的日志点必须经过 `delete-log.js` 的脱敏白名单，且不得包含文档路径、UID 或日期。
- 用户可见文案只存在于 `ZhStrings.kt` / `EnStrings.kt`；`scripts/copy-integrity.test.mjs` 会拒绝其他源码文件里的中日韩文字与中文标点、全角符号。
- 不硬编码中文标点作为列表分隔符；用 `AppCopy` 中的语言相关分隔符。
- 严禁提交 `local.properties`、`google-services.json`、`keystore.properties`、签名文件、Token、真实用户数据、备份或日志。
- 本地 Firebase 模拟器使用合成占位 key；不要为了"更真实"替换成任何形似真实凭据的字符串。
- 新增依赖前确认它不引入分析、广告或崩溃上报能力。

## 9. 测试

- 测试分层与执行时机见 [`TESTING.md`](TESTING.md)。
- 测试数据必须是虚构数据；不得使用真实账号、真实邮箱或真实记录。
- 设备套件只跑在可重置模拟器上；运行前用 `adb devices -l` 排除日常真机。
- 断言行为，不断言实现细节：优先断言用户可观察结果、数据语义和边界，而不是内部调用序列。
- 禁止断言“常量等于常量”这类没有行为覆盖的测试；这类测试只增加维护成本。
- 单元测试替身不得悄悄简化生产规则：如果替身与生产实现有已知差异，必须在测试中显式注释说明，并由另一层测试覆盖真实规则。
- 必须用硬失败而非跳过表达前置条件（生产烟雾测试等显式 opt-in 例外除外）；不要用 `assumeTrue` 让关键回归在环境不可用时静默通过。
- 泄漏的进程级全局状态（如语言状态）必须在测试中恢复，避免测试顺序依赖。

## 10. 注释

- 注释语言为英文，与现有 KDoc 保持一致；用户可见文案不进入注释。
- 注释解释**为什么**和**边界条件**，不复述代码。
- 不写 TODO/FIXME/XXX；要么现在就做，要么写成 Issue 再删除注释。
- 删除已被取代的实现说明，不留“曾经这样做”的注释。
- 注释不得与代码行为矛盾；发现即修正。
- 涉及产品/数据边界的注释只引用权威文档（例如 ADR 编号），不复制一份可能过时的规则。
- 不做只改措辞、排版或注释的无关大规模改写。

## 11. 文档、依赖与自动化门禁

- 一个事实一个权威来源；其他文档给摘要和链接。
- 新增/移动/重命名/删除文档必须同步 [`DOCUMENTATION_CATALOG.md`](DOCUMENTATION_CATALOG.md)。
- 依赖版本一律精确固定（Gradle catalog、`package.json` 均不使用范围）；升级必须有依据并完成设备回归。
- 能被机械检查的规则优先写成脚本，而不是只写在文档里。当前门禁：

| 门禁 | 保护对象 |
|---|---|
| `scripts/documentation-catalog.test.mjs` | 文档目录与实际跟踪文档一致 |
| `scripts/docs-integrity.test.mjs` | 本地链接/图片有效；版本、SDK、Room schema、Functions 事实一致；反查速查表覆盖全部当前必读文档且两处一致；踩坑清单编号连续、索引与正文一致、七个小节齐全 |
| `scripts/workspace-hygiene.mjs`（`pnpm check:hygiene`） | 交付前 `work/` 为空、没有未跟踪文件、没有失效 worktree；手动收尾自检，不进 CI |
| `scripts/copy-integrity.test.mjs` | 文案只在语言文件；无中文标点外泄；无已退役术语 |
| `scripts/release-metadata.test.mjs` | 版本、tag、`versionCode` 递增、GitHub 输出键名 |
| `scripts/firestore-rules.test.mjs` | 规则的所有权、字段形状、修订与删除边界 |
| `functions/delete-log.test.js` | 日志不泄漏路径、UID、日期 |
| `check-node-runtime.mjs` | Node 运行时与 `functions/package.json` 一致 |

新增门禁优先扩展现有脚本；只有现有脚本确实覆盖不到时才新建，并避免脆弱或高误报的规则。
