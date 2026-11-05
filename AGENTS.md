# AGENTS.md

本文件是所有 AI 助手和自动化代理在本仓库开始工作时的强制入口。它只负责**约束与阅读路由**：具体事实在专门文档里维护，不在这里复制。

## 开始工作前（按顺序执行）

1. 读 [`docs/DOCUMENTATION_CATALOG.md`](docs/DOCUMENTATION_CATALOG.md)，确认哪些文档是当前事实、哪些只是历史证据。
2. 读 [`docs/README.md`](docs/README.md)，确认冲突时的优先级和文档职责边界。
3. 按任务范围读下表列出的权威文档；`历史证据` 默认不读，只有核对过去的实现、截图、提交或发布链时才读。
4. 检查当前分支与工作区：`git status --short --branch`。用户未提交的改动必须原样保留。
5. 从最新公共 `main` 建立短生命周期 `agent/<description>` 分支后再动手。

| 任务范围 | 必读（除上面两项外） |
|---|---|
| 任何改动 | [`docs/WORKFLOW.md`](docs/WORKFLOW.md)、[`docs/PRODUCT.md`](docs/PRODUCT.md) |
| 代码实现 | [`docs/ENGINEERING_STANDARDS.md`](docs/ENGINEERING_STANDARDS.md)、[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) |
| 数据模型、Room、迁移 | [`docs/DATA_MODEL.md`](docs/DATA_MODEL.md) |
| 统计口径 | [`docs/STATISTICS.md`](docs/STATISTICS.md) |
| 同步、账号、隐私、删除 | [`docs/SYNC_AND_PRIVACY.md`](docs/SYNC_AND_PRIVACY.md)、[`PRIVACY.md`](PRIVACY.md)、[`SECURITY.md`](SECURITY.md) |
| 界面、文案、无障碍 | [`docs/UI_UX.md`](docs/UI_UX.md) |
| 测试范围与门禁 | [`docs/TESTING.md`](docs/TESTING.md) |
| 环境与日常命令 | [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md) |
| 依赖、SDK、构建配置 | [`docs/DECISIONS.md`](docs/DECISIONS.md)、[`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md) |
| Firebase 资源与规则 | [`docs/FIREBASE_SETUP.md`](docs/FIREBASE_SETUP.md) |
| 计费、限流、配额与止损 | [`docs/operations/firebase-cost-and-quota-inventory.md`](docs/operations/firebase-cost-and-quota-inventory.md)、[`docs/operations/firebase-abuse-monitoring-and-emergency-runbook.md`](docs/operations/firebase-abuse-monitoring-and-emergency-runbook.md)、[`docs/operations/firebase-cost-safety-test-matrix.md`](docs/operations/firebase-cost-safety-test-matrix.md) |
| 版本、签名、发布 | [`docs/RELEASE.md`](docs/RELEASE.md) |
| 提交、PR、分支、清理 | [`docs/WORKFLOW.md`](docs/WORKFLOW.md)、[`CONTRIBUTING.md`](CONTRIBUTING.md) |
| 排查“以前踩过的坑” | [`docs/AI-PITFALLS.md`](docs/AI-PITFALLS.md) |

## 不可逾越的产品与数据边界

- 当前版本只记录**自慰**和**做爱**两个固定、互相独立的模块。
- **禁止**引入 `Activity`、`MeasurementType`、活动表、健身或自定义活动抽象；未来记录类型必须通过新 ADR 作为独立垂直模块加入，且不得污染现有模型。
- 每个模块每天最多一条记录；统计口径以 [`docs/STATISTICS.md`](docs/STATISTICS.md) 为唯一事实来源。
- 未进入 [`docs/PRODUCT.md`](docs/PRODUCT.md) 的功能不实现、不提前抽象。
- UI 只通过 Repository 读取 Room；颜色不是唯一状态编码，关键控件必须具备 TalkBack 描述。
- 本地优先：无网络必须能完成全部核心功能。
- 用户可见文案只存在于 `ZhStrings.kt` / `EnStrings.kt`；UI 与业务代码不硬编码文案，也不硬编码中文标点。

## 计费、配额与防滥用纪律

- **新增 Firebase 服务**：必须核对官方最新费用方案与配额（见 [`docs/operations/firebase-cost-and-quota-inventory.md`](docs/operations/firebase-cost-and-quota-inventory.md)）。
- **新增云函数**：必须显式配置资源限制（含 `maxInstances`、`timeoutSeconds`、`concurrency`、`memory`），严禁放开无界扩容；必须接入服务端限流与熔断门禁。
- **新增重试逻辑**：必须有明确终止条件（如 `runAttemptCount < MAX_ATTEMPTS`）、指数退避和错误分类；配额超限（`Quota` / `RESOURCE_EXHAUSTED`）严禁无限密集重试。
- **新增 Firestore 触发器**：必须证明幂等性、显式配置 `retry: false` 并检查触发器循环与级联副作用。
- **新增后台任务**：WorkManager 任务必须能够取消，严禁无界无限重试。
- **服务端防线优先**：严禁把客户端按钮禁用或倒计时当成服务端安全保护；服务端端点必须能防御脱机脚本攻击。
- **费用安全测试**：任何可能显著提高费用的代码改动都必须同步增加自动化回归测试（见 [`docs/operations/firebase-cost-safety-test-matrix.md`](docs/operations/firebase-cost-safety-test-matrix.md)）。

## 代码与文档冲突时

1. 先确认**当前代码、测试和实际运行行为**（事实优先级见 [`docs/README.md`](docs/README.md)）。
2. 判断哪一侧违反了预期契约：不要为了迁就过时文档去改正常工作的程序。
3. 修正错误的一侧；若这是长期方向变化，先建立新的 ADR 再改代码。
4. 不把猜测写成已确认缺陷；无法验证的结论必须显式标注为未验证。

## 分支、提交与远端操作

- 使用 `agent/<description>` 短生命周期分支；不在 `main` 直接修改，也不在别人的分支上改。
- 提交信息格式为 `[<模型标识>:<型号>]: <内容>`，例如 `[ds:v4flash]: fix: week boundary`、`[gpt:5.6sol]: docs: ...`、`[claude:5]: ...`。若本次任务明确给出提交前缀，以用户给出的完整字符串为准。
- 所有代码、文档、测试、分支和 PR 都在公共仓库完成；私有仓库只用于大版本同步，不把私有生产材料反向推入公共仓库。
- 文档与审计改动同样走"本地分支 → 提交 → PR"，不在 GitHub 页面直接改。
- 合并到 `main` 必须使用普通 merge，保留分支里的每一笔原始提交；禁止 squash、rebase merge 和强推。
- **远端写操作必须极度克制**：一次会话内创建 Issue 一次只建一个，间隔至少 60 秒；出现任何 403、限流或异常响应立即停止并如实报告，绝不并行重试。批量创建 Issue 前必须先征得用户明确同意。详见 [`docs/AI-PITFALLS.md`](docs/AI-PITFALLS.md) 的 P1。
- Issue 正文只写主题、目标、范围边界与验收步骤；完整方案放仓库文档并给出链接。
- 严禁把 `local.properties`、`google-services.json`、`keystore.properties`、签名文件、口令、Token、真实用户数据、备份或日志提交进仓库。

## 测试与完成标准

测试范围、执行时机和完整套件门槛以 [`docs/TESTING.md`](docs/TESTING.md) 为唯一事实来源。核心纪律：

- 修改中只运行能直接证明本次行为的最小测试集；不要为每个小改动跑整个设备套件。
- 修复缺陷必须先复现，再修复，并补上可自动化的回归测试。
- Room schema、迁移、同步边界、Firestore Rules 和发布配置的改动必须扩大验证范围。
- 未执行的验证必须如实列出，不能声称全部通过。
- 完成前必须有：未失效的单元测试、Lint、Android 测试编译，以及按影响范围需要的 Room 迁移、Rules 与设备测试证据。

## 中间产物纪律

- 临时脚本、截图、UI XML、logcat、构建日志、分析草稿和一次性报告，任务结束前必须删除，不留在仓库或用户磁盘上。
- **不要**为了让审计“看起来完整”而新增重复报告、每问题一份 Markdown、或很快过时的文件清单。
- 临时产物仓库内统一放在被 `.gitignore` 排除的 `work/` 下，用完即删。
- 有长期价值的内容直接写进既有权威文档；发布事实写进 `docs/releases/`，阶段审计、截图和一次性报告不再入库，验证证据写进 PR 描述（见 [`docs/REPOSITORY_HYGIENE.md`](docs/REPOSITORY_HYGIENE.md)）。

## 文档维护

- 新增、移动、重命名或删除任何 Markdown/文档文件时，必须在同一提交更新 [`docs/DOCUMENTATION_CATALOG.md`](docs/DOCUMENTATION_CATALOG.md)；`scripts/documentation-catalog.test.mjs` 会检查目录完整性，CI 会运行它。
- 一个事实只能有一个主要权威来源；其他文档给出简短摘要和链接即可。
- 修改版本、数据库 schema、SDK 或截图资产时，`pnpm test:docs` 必须同步通过。

## 高风险操作

以下操作必须格外谨慎，并遵守对应权威文档：

| 操作 | 必须遵守 |
|---|---|
| Room 版本、Entity、DAO、migration | 显式迁移 + 设备迁移测试 + 更新 schema 快照；禁止清空重建代替迁移（[`docs/DATA_MODEL.md`](docs/DATA_MODEL.md)） |
| Firestore Rules、云端字段、路径 | `pnpm test:firestore-rules`；不带规则改动不得改变线上写入契约（[`docs/FIREBASE_SETUP.md`](docs/FIREBASE_SETUP.md)） |
| 账号删除、云端数据删除、隐私语义 | 保持六阶段删除与恢复语义；不得留下可被晚期回调复活的路径（[`docs/SYNC_AND_PRIVACY.md`](docs/SYNC_AND_PRIVACY.md)） |
| 依赖或 SDK 升级 | 必须有依据并完成设备回归；不得为了追新版本改动发布基线（[`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md)） |
| 版本号、tag、签名、发布 | 严格遵守 [`docs/RELEASE.md`](docs/RELEASE.md)；tag 必须包含在 `main` 中，`versionCode` 必须严格递增 |

## 完成任务前

- 按 [`docs/WORKFLOW.md`](docs/WORKFLOW.md) 的收尾清单执行：Diff 审查、文档同步、测试证据、清理中间产物。
- 最终汇报必须包含：改了什么、依据是什么、实际运行的验证命令与结果、未执行项及原因、仍然存在的风险。
- 不隐瞒未解决的问题，不为让结果好看而伪造测试数量、构建状态或 CI 状态。
