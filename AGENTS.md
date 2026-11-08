# AGENTS.md

> 本文件是**AI 助手的核心工作规范与强制入口**：强约束、条款化、每条都有对应原则与门禁。
> **具体事实与专题不在这里复制**——架构、数据模型、统计口径、UI/UX、测试矩阵、计费配额分别见
> [`docs/README.md`](docs/README.md)（唯一索引）；**教训**（这里以前栽过什么）见 [`docs/AI-PITFALLS.md`](docs/AI-PITFALLS.md)。
>
> **动手改任何东西前，先读 [`docs/WORKFLOW.md`](docs/WORKFLOW.md)**（§0 不确定先查文档 → Issue → 分支+worktree
> → 最小实现 → 本机验证 → 规范提交 → 推送 → PR/CI → 普通 merge 与清理八步链路）。
> **触及 Android 源码、Room 迁移、Rules 或 Functions 前，严格遵守下方各条纪律与红线。**

---

## 开始工作前（按顺序执行）

1. 读 [`docs/DOCUMENTATION_CATALOG.md`](docs/DOCUMENTATION_CATALOG.md)，确认哪些文档是当前事实、哪些只是历史证据。
2. 读 [`docs/README.md`](docs/README.md)，确认冲突时的优先级和文档职责边界。
3. 按任务范围读下表列出的权威文档；`历史证据` 默认不读，只有核对过去的实现、截图、提交或发布链时才读。
4. 检查当前分支与工作区：`git status --short --branch`。用户未提交的改动必须原样保留。
5. 从最新公共 `main` 建立短生命周期 `agent/<description>` 分支后再动手，优先用 `work/` 下的 worktree 隔离（[`docs/WORKFLOW.md`](docs/WORKFLOW.md) §3）。

| 任务范围 | 必读（除上面两项外） |
|---|---|
| 任何改动 | [`docs/WORKFLOW.md`](docs/WORKFLOW.md)（含 §0.3 反查速查表）、[`docs/AI-EVIDENCE-STANDARD.md`](docs/AI-EVIDENCE-STANDARD.md)、[`docs/PRODUCT.md`](docs/PRODUCT.md) |
| 代码实现 | [`docs/AI-PITFALLS.md`](docs/AI-PITFALLS.md)（动手前必查）、[`docs/ENGINEERING_STANDARDS.md`](docs/ENGINEERING_STANDARDS.md)、[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) |
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
| 仓库卫生与中间产物处理 | [`docs/REPOSITORY_HYGIENE.md`](docs/REPOSITORY_HYGIENE.md) |
| 排查“以前踩过的坑” | [`docs/AI-PITFALLS.md`](docs/AI-PITFALLS.md) |

---

## 一、AI 必守纪律

### 1.1 不确定 → 先查文档

**对任何东西不确定时，先去查阅文档看有没有，不要先猜。**

- 查证顺序：[`docs/README.md`](docs/README.md) 索引 → 命中的那一份权威文档（[`docs/WORKFLOW.md`](docs/WORKFLOW.md) §0.3 反查表）→ 代码/测试/运行接口（**代码优先于文档**）→ 仍不确定就**明说不确定并问人**。
- **禁止编造**数值、参数、文件路径、构建命令或日志入口；也不许凭印象沿用历史数值（历史提交或已合并 PR 中的参数可能会过期）。
- 答案早就写在权威文档里，返工几乎全来自“先猜后查”。

### 1.2 动手前必查踩坑清单

**动代码前必查 [`docs/AI-PITFALLS.md`](docs/AI-PITFALLS.md)——本仓同一根因反复栽过多次。**

- 集中记录三类内容：**反复踩的坑**（日期边界错位、同一规则日历与统计两处写漂移、文案硬编码与中文标点绕过、测试用 assumeTrue 静默跳过）、**单次高代价事故**（Issue 批量连发致账号被封 P1、跨模块快照误清数据 P2、Rules 必填字段破坏旧客户端写入 P6、v1 迁移数据失真 P14）、**必记方法**（修改规则同提交同步测试与文档、生产代码零日志、删除逻辑严格六阶段语义）。
- **收录门槛只有三条**：反复发生（≥2 次）、单次但高代价、已成强制门禁——**偶发小插曲不进**，清单**只增不删**。

### 1.3 多使用子代理分工

**总代理负责指挥、分配与检查，子代理承担不同分工。**

- 大任务（全仓文件搜索、跨模块对比、批量静态排查、长耗时编译验证）先拆成**互不重叠**的子任务，交给子代理并行执行。
- 总代理亲自做的是**分配、汇总与复核**，不包揽可并行的重复劳动。
- **关键结论必须亲自抽查**：子代理汇报的关键结论（代码路径、行号、构建与测试结果、错误原因）必须由总代理**亲自抽查复核**后才可采信，**不得原样转述当证据**；简单、单步任务不盲目拆分。

### 1.4 最小改动 · 优先复用（两档制）

改动代码尽可能改动最小部分的代码，不随意新建平行实现，注意精简。按改动性质分两档执行：

- **常规档（改行为：修 Bug / 调参数 / 改交互 / 改文案 / 优化逻辑）—— 最小改动全量生效**：
  - 改**已有**逻辑就**就地改**那一处，不在旁边再写一份平行实现；
  - 动手前先搜同类实现（函数名、组件名、扩展方法）：同一语义**只留一处**，能**复用**或扩展既有函数就绝不新建；
  - 只有语义确实不同才允许新增函数或文件，且要在提交/PR 里写清理由；
  - **绝不夹带**：顺手重构、顺手改名、批量格式化、清理看似无用的旧代码等与本次任务无关的一律不做；
  - 提交前自检 `git diff --stat`：改动文件数/行数与目标明显不匹配时，坚决砍回范围内。
- **重构档（拆文件 / 解耦 / 消除重复实现 / 降低圈复杂度）—— 显式豁免「就地改」与「不新建」**：
  - 重构必然要把代码从一处搬到另一处，重构档下**允许**新建模块、搬迁函数、抽取公共实现（这是消除重复的唯一途径，不属于新建平行实现）；
  - 仍须同时满足：① **纯搬迁/抽取，运行时可观察行为零变更**（UI 表现、数据语义、返回值、计算口径逐字不变；若顺带改行为必须拆分成常规档独立任务）；② **一次只搬一个内聚单元、单独成提交/PR**，测试全绿再进行下一块；③ 仍不夹带无关改动；④ 保持原有公共接口兼容。
- **分档判据**：**这次改动会不会改变运行时可观察行为？** 会 ⇒ 常规档；不会 ⇒ 重构档。拿不准按常规档从严执行。

### 1.5 及时清理中间产物

**临时文件与专属文档用后即删，防仓库与工作区臃肿。**

- 研发、排查或调试过程中产生的中间产物、临时脚本、测试数据、临时日志、logcat 输出等，用完后立即**彻底删除**；
- 仅服务于当前 Issue 或单次任务的临时调研笔记、分析草稿、一次性 Markdown 报告，任务结束前**清理干净**，不得随 commit 提交进 Git 或散落遗留在仓库根目录及 `docs/` 下；
- 长期有价值的架构、流程、事实与排查教训，统一并入既有权威文档（按 [`docs/README.md`](docs/README.md) 索引）或踩坑清单（[`docs/AI-PITFALLS.md`](docs/AI-PITFALLS.md)），**不为了单次任务新建游离 md**；
- 仓库内临时产物统一放在被 `.gitignore` 排除的 `work/` 下，用完即删；
- **覆盖仓库外创建的目录**：在桌面、用户目录、临时目录创建的 worktree、探针目录、一次性脚本工作区，**用完即删，连目录本身一起删除**（不仅删内部文件，连目录壳一同移除），严禁在桌面残留空目录或废弃工作区；
- 交付收尾必须运行 `node scripts/workspace-hygiene.mjs`，且必须通过。

### 1.6 假定用户会出错：当场指出

**默认假定用户会出错，不把用户的话默认当正确输入。当用户出现明显错误或决策冲突时，毫不留情地指出。**

- **什么算「明显错误或决策冲突」**（命中任一即须指出）：
  1. **事实错**：参数、字段、文件路径、构建命令、系统状态与权威文档/代码实际运行不符；
  2. **决策冲突**：本次要求与既有核心红线、已确认的 ADR 决策或门禁规则互斥；
  3. **违反本仓纪律**：要求做的事情会破坏先查文档、最小改动不夹带、及时清理中间产物等纪律；
  4. **前提已被推翻**：依据的历史机制或文件已被废弃或重构。
- **怎么指出**：**当场、具体、给证据**——一句话说清“哪一点错 + 权威出处（文档路径/代码行号）+ 不改的后果”，不含糊带过、不事后诸葛亮；
- **⚠️ 边界：指出 ≠ 越权**：
  - 用户是最终决策者。指出后用户仍坚持原口径的，**照用户口径执行**，并在提交说明与 PR 记录留痕；
  - 不许拿“我觉得不对”拒绝执行用户明确拍板的决策；
  - 不许无证据抬杠：证据不足时按查证顺序明说不确定并去核查，不把猜测当“用户的错误”指出去。

### 1.7 业务底线与红线驱动（产品核心价值契约）

本仓库的核心存在意义与产品底线是：**极简可靠、本地优先、绝对保护用户私密与隐私、数据零丢失零损坏**。

- **双独立垂直模块**：当前版本只记录**自慰**和**做爱**两个固定、互相独立的模块；严禁引入 `Activity`、`MeasurementType`、活动表、健身或自定义活动抽象；
- **本地优先**：无网络必须能完成全部核心记录与统计功能，离线操作必须可靠持久化；
- **生产零日志**：`app/src/` 没有任何日志输出，这是有意的隐私红线，严禁在生产代码引入 `Log`、`println` 或上报 SDK；
- **文案集中化**：用户可见文案只能存在于 `ZhStrings.kt` / `EnStrings.kt`，严禁在 UI 与业务逻辑硬编码中英文案，严禁硬编码中文标点；
- **数据一致性**：Room 升级必须提供显式迁移并保留真实旧数据，严禁清空重建代替迁移。

### 1.8 证据驱动：重要结论必须可复核

**重要结论必须经得起别人独立复核。**

- 对影响代码正确性、数据迁移、安全边界、UI 呈现或最终验收的结论，标出它是〔已验证〕/〔推断〕/〔未知〕，并给出**别人能独立复核**的证据（`文件路径:行号`、命令原文+关键输出、测试名+通过证明）；
- **禁止**：把推断说成事实、把未知补成看起来合理的答案、把“上一轮跑通过/代码看起来没毛病”当成本轮验证证据；
- **测试结果分五态说清**：已执行通过 / 已执行失败 / 尚未执行 / 无法执行 / 通过但覆盖不足；
- **交付成果分五类汇报**（A 已完成且验证 / B 已实施未充分验证 / C 未完成或被阻塞 / D 仅建议未实施 / E 剩余风险与盲区）；
- 防形式主义：普通描述不贴标签，不写冗长过程报告，重点在证据本身可查验。

### 1.9 先建隔离环境再动手：主仓不直接改源码

**主仓检出（`main`）不直接修改源码，改动一律在分支或 worktree 中进行。**

- 动手前自检：`git branch --show-current` + `git worktree list`；
- 隔离工作流：使用 `agent/<description>` 分支，worktree 统一建立在 `work/<description>` 下（已被 `.gitignore` 排除）；
- 允许在主仓做的事：读文件、跑只读测试与 Lint、执行收尾检查；
- **worktree 用完即删**：任务合并或分支废弃后，必须用 `git worktree remove` 清理，严禁长期残留。

### 1.10 盯住自己开的 Issue / PR 评论

**评审反馈必须主动看、逐条甄别、不管对错都得回。**

- 别的会话或审查 AI 可能会在 Issue / PR 上留下评论反馈，必须主动查看，严禁静默忽略；
- **三个查看时机**：① 继续动手前；② 开 PR 前；③ 合入 PR 与关 Issue 前；
- **逐条甄别（审查员评论不一定对）**：先按证据标准复核，确认可达路径与依据后再决定采纳、驳回或调整；驳回必须写明具体证据与不成立理由；
- **必须回复**：只要是有实质内容的评审评论，**必须回一条**（① 已处理并指出改动处；② 刻意不改并给出理由；③ 需人拍板并点名请求）；判定为错也要回，静默跳过视为未处理。

### 1.11 单文件行数硬顶与代码质量规约

**控制文件体量与单一职责，杜绝职责过载的大文件。**

- **行数控制**：Kotlin 文件目标控制在 400 行以内；新增代码文件物理总行数严禁超过 800 行；
- **历史大文件处理**：对于极少数历史遗留的超长文件（如部分长 Composable 屏幕或字符串映射文件），只在正在修改该文件且拆分能直接降低风险时，按**重构档**抽取子组件或子模块，不发起纯刷行数的无实质改动；
- **函数单一职责**：严禁在一个函数中同时揉杂“状态机 + I/O + 文案拼装”，遇到此类情况必须拆分；
- **Compose 规范**：状态提升、单向数据流、使用项目设计系统组件（`ui/components/`）、TalkBack 无障碍语义完整、禁止裸 collect Flow。

---

## 二、不可逾越的产品与数据边界

- 当前版本只记录**自慰**和**做爱**两个固定、互相独立的模块。
- **禁止**引入 `Activity`、`MeasurementType`、活动表、健身或自定义活动抽象；未来记录类型必须通过新 ADR 作为独立垂直模块加入，且不得污染现有模型。
- 每个模块每天最多一条记录；统计口径以 [`docs/STATISTICS.md`](docs/STATISTICS.md) 为唯一事实来源。
- 未进入 [`docs/PRODUCT.md`](docs/PRODUCT.md) 的功能不实现、不提前抽象。
- UI 只通过 Repository 读取 Room；颜色不是唯一状态编码，关键控件必须具备 TalkBack 描述。
- 本地优先：无网络必须能完成全部核心功能。
- 用户可见文案只存在于 `ZhStrings.kt` / `EnStrings.kt`；UI 与业务代码不硬编码文案，也不硬编码中文标点。

---

## 三、计费、配额与防滥用纪律

- **新增 Firebase 服务**：必须核对官方最新费用方案与配额（见 [`docs/operations/firebase-cost-and-quota-inventory.md`](docs/operations/firebase-cost-and-quota-inventory.md)）。
- **新增云函数**：必须显式配置资源限制（含 `maxInstances`、`timeoutSeconds`、`concurrency`、`memory`），严禁放开无界扩容；必须接入服务端限流与熔断门禁。
- **新增重试逻辑**：必须有明确终止条件（如 `runAttemptCount < MAX_ATTEMPTS`）、指数退避和错误分类；配额超限（`Quota` / `RESOURCE_EXHAUSTED`）严禁无限密集重试。
- **新增 Firestore 触发器**：必须证明幂等性、显式配置 `retry: false` 并检查触发器循环与级联副作用。
- **新增后台任务**：WorkManager 任务必须能够取消，严禁无界无限重试。
- **服务端防线优先**：严禁把客户端按钮禁用或倒计时当成服务端安全保护；服务端端点必须能防御脱机脚本攻击。
- **费用安全测试**：任何可能显著提高费用的代码改动都必须同步增加自动化回归测试（见 [`docs/operations/firebase-cost-safety-test-matrix.md`](docs/operations/firebase-cost-safety-test-matrix.md)）。

---

## 四、什么事实去哪查（速查表）

> 查证顺序：[`docs/README.md`](docs/README.md) 索引 → 本表命中的权威文档 → 代码/测试/运行接口 → 停下问人。
> 本表与 [`docs/WORKFLOW.md`](docs/WORKFLOW.md) §0.3 和 [`docs/README.md`](docs/README.md) 保持一致。

| 我不确定的事 | 去查的权威文档 |
|---|---|
| 自慰与做爱两个模块的边界、能不能加新功能 | [`docs/PRODUCT.md`](docs/PRODUCT.md) |
| 已接受的长期技术与产品决策（ADR） | [`docs/DECISIONS.md`](docs/DECISIONS.md) |
| Room 表结构、字段、迁移、聚合记录与详情的关系 | [`docs/DATA_MODEL.md`](docs/DATA_MODEL.md) |
| 周、月、年、全部历史的统计口径和图表规则 | [`docs/STATISTICS.md`](docs/STATISTICS.md) |
| 页面交互、颜色令牌、色阶、玻璃材质、无障碍 | [`docs/UI_UX.md`](docs/UI_UX.md) |
| 文案集中化、命名、中文标点禁令、注释写法 | [`docs/ENGINEERING_STANDARDS.md`](docs/ENGINEERING_STANDARDS.md) |
| 分层、ViewModel 与 Repository 的调用关系 | [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) |
| 云同步、离线行为、账号六阶段删除 | [`docs/SYNC_AND_PRIVACY.md`](docs/SYNC_AND_PRIVACY.md) |
| 改动后该跑哪些测试、完整门禁何时跑 | [`docs/TESTING.md`](docs/TESTING.md) |
| Gradle 命令、本机环境、SDK 版本 | [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md) |
| 版本号、签名、tag 与 GitHub Release | [`docs/RELEASE.md`](docs/RELEASE.md) |
| Firebase 资源、Rules 与 Functions 部署 | [`docs/FIREBASE_SETUP.md`](docs/FIREBASE_SETUP.md) |
| 云端费用、配额、防刷与止损 | [`docs/operations/firebase-cost-and-quota-inventory.md`](docs/operations/firebase-cost-and-quota-inventory.md)、[`docs/operations/firebase-abuse-monitoring-and-emergency-runbook.md`](docs/operations/firebase-abuse-monitoring-and-emergency-runbook.md)、[`docs/operations/firebase-cost-safety-test-matrix.md`](docs/operations/firebase-cost-safety-test-matrix.md) |
| 这个方向以前栽过什么坑（动手前必查） | [`docs/AI-PITFALLS.md`](docs/AI-PITFALLS.md) |
| 证据等级、汇报格式和停止条件（动手前必查） | [`docs/AI-EVIDENCE-STANDARD.md`](docs/AI-EVIDENCE-STANDARD.md) |
| 改码流程：分支、提交、PR、合并、清理（动手前必查） | [`docs/WORKFLOW.md`](docs/WORKFLOW.md) |
| Issue、PR 和贡献者需要遵守的约定 | [`CONTRIBUTING.md`](CONTRIBUTING.md) |
| AI 总规则、红线和高风险操作 | [`AGENTS.md`](AGENTS.md) |
| 某个文件算不算权威、归谁管 | [`docs/DOCUMENTATION_CATALOG.md`](docs/DOCUMENTATION_CATALOG.md)、[`docs/README.md`](docs/README.md) |
| 公共/私有边界、文档生命周期、生成物 | [`docs/REPOSITORY_HYGIENE.md`](docs/REPOSITORY_HYGIENE.md) |
| 面向使用者的隐私说明、漏洞报告 | [`PRIVACY.md`](PRIVACY.md)（[English](PRIVACY.en.md)）、[`SECURITY.md`](SECURITY.md) |

---

## 五、证据标准（摘要）

完整规范见 [`docs/AI-EVIDENCE-STANDARD.md`](docs/AI-EVIDENCE-STANDARD.md)。核心约束：

1. **三档主张**：对影响系统、数据或验收的结论，标出〔已验证〕/〔推断〕/〔未知〕。未经验证不得表述为事实；未知不得擅自补全；占位值不等于未知。
2. **测试五态**：汇报时测试结果必须分五态说清（已执行且通过 / 已执行但失败 / 尚未执行 / 无法执行 / 通过但覆盖不足），并带上实际输出行。
3. **交付五类（A~E）**：最终汇报必须分五类呈现（A 已完成且验证 / B 已实施未充分验证 / C 尚未完成或被阻塞 / D 仅为建议未实施 / E 风险与测试盲区），严禁把计划当完成。
4. **多轮评审收敛**：意见分四档（真实缺陷 / 必要修复 / 可选优化 / 个人偏好），独立复核；必回的是回音不是照办，驳回必须写明依据；同一问题连续失败 3 次即停下升级给人。
5. **防形式主义**：普通描述不贴标签，不写冗长报告，重点在证据本身可独立复核。

---

## 六、代码编写与注释规范

完整工程规范见 [`docs/ENGINEERING_STANDARDS.md`](docs/ENGINEERING_STANDARDS.md)。核心约束：

1. **改动两档制**：
   - 常规档（改行为）：就地改那一处，能复用扩展绝不新建，同一语义只留一处，绝不夹带格式化与重构；
   - 重构档（不改行为）：豁免就地改与不新建，允许新建模块抽取公共实现，但必须运行时可观察行为零变更、单步内聚、单独成 PR。
2. **单文件行数硬顶**：Kotlin 源码目标 400 行以内，物理总行数严禁超过 800 行。历史超长文件只在修改时且能降险才重构抽离；
3. **函数单一职责**：严禁在一个函数中揉杂状态机+I/O+文案拼装；
4. **注释规则**：注释语言为英文，解释**为什么**和边界，不复述代码；不写 TODO/FIXME；不留已被取代的历史实现说明。

---

## 七、高风险操作守则

以下操作必须格外谨慎，并遵守对应权威文档：

| 操作 | 必须遵守 |
|---|---|
| Room 版本、Entity、DAO、migration | 显式迁移 + 设备迁移测试 + 更新 schema 快照；禁止清空重建代替迁移（[`docs/DATA_MODEL.md`](docs/DATA_MODEL.md)） |
| Firestore Rules、云端字段、路径 | `pnpm test:firestore-rules`；不带规则改动不得改变线上写入契约（[`docs/FIREBASE_SETUP.md`](docs/FIREBASE_SETUP.md)） |
| 账号删除、云端数据删除、隐私语义 | 保持六阶段删除与恢复语义；不得留下可被晚期回调复活的路径（[`docs/SYNC_AND_PRIVACY.md`](docs/SYNC_AND_PRIVACY.md)） |
| 依赖或 SDK 升级 | 必须有依据并完成设备回归；不得为了追新版本改动发布基线（[`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md)） |
| 版本号、tag、签名、发布 | 严格遵守 [`docs/RELEASE.md`](docs/RELEASE.md)；tag 必须包含在 `main` 中，`versionCode` 必须严格递增 |

---

## 八、AI 完成任务（收尾）必做三件事

每次交付收尾时，必须严格执行三件事，缺一不可：

| # | 动作 | 执行标准与命令 |
|---|---|---|
| ① | **清理中间产物** | 1. 彻底删除临时脚本、测试副产物、中间日志；<br>2. 临时分析草稿与临时 Markdown 用完即删，长期事实写入权威文档；<br>3. 清空 `work/` 目录（巡检常驻状态文件 `agent-watch-state.json` 除外）；<br>4. 仓库外（桌面、临时目录）创建的 worktree 或探针目录**连同目录本身一起删除**，桌面不留残留；<br>5. 运行 `node scripts/workspace-hygiene.mjs` 并确保通过。 |
| ② | **该评论则评论** | 1. 对他人或审查 AI 在 Issue / PR 上提出的实质性评论逐条甄别；<br>2. 必须在评论中明确回复（已处理 / 刻意不改+理由 / 需人拍板）；<br>3. 驳回必须附带证据与理由，严禁静默跳过。 |
| ③ | **该关闭则关闭** | 1. 验收通过且已无遗留事项的 Issue / PR 予以关闭；<br>2. 未完成、待人决策或仍处于观察期的 Issue **严禁关闭**，必须保持 OPEN 状态；<br>3. 汇报包含改动内容、验证命令与结果、未执行项及剩余风险。 |


