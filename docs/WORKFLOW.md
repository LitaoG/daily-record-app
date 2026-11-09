# 端到端工程工作流

状态：`current — authoritative workflow`
最后复核：2026-11-05

本文件是 Daily Record 从任务开始到合并清理的唯一权威流程来源。其他文档（[`AGENTS.md`](../AGENTS.md)、[`CONTRIBUTING.md`](../CONTRIBUTING.md)、[`DEVELOPMENT.md`](DEVELOPMENT.md)、[`TESTING.md`](TESTING.md)）只保留各自职责范围内的简短摘要并链接回本文，不再各自维护一套互相矛盾的步骤。

测试范围与完成门槛以 [`TESTING.md`](TESTING.md) 为唯一事实来源；本文只规定**何时**进入哪个环节，不复制具体命令矩阵。产品与数据边界见 [`PRODUCT.md`](PRODUCT.md)。

## 0. 全流程一览

### 0.1 流程

```
读文档 → 定界 → 分支 → 实现 → 定向验证 → 同步文档 → Diff 审查 → 提交/PR → CI → 合并 → 清理 → 汇报
```

每一步都有明确的退出条件。缺少任何一步的退出条件时不要进入下一步。

### 0.2 不确定先查文档

对任何事实不确定时，先查文档，不要先猜。查证顺序固定为：

```
docs/README.md 与文档目录 → 0.3 表中命中的那一份权威文档 → 代码、测试和实际运行结果 → 仍不确定就停下来问人
```

- 代码优先于文档：当前事实以 `main` 上的代码、测试和运行行为为准。文档与正确运行的代码冲突时改文档，不改代码去迁就旧文档。
- 不凭印象编造数值、参数、文件路径或命令。历史 PR、Issue 和提交说明里的数字会过期，引用前在现场重新查询，并在汇报里附上查询命令。
- 证据怎样才算数、什么时候必须停下，见 [`AI-EVIDENCE-STANDARD.md`](AI-EVIDENCE-STANDARD.md)。

### 0.3 权威文档反查速查表

按“我不确定的是什么”查找唯一权威文档。表里没有的问题，先查[文档目录](DOCUMENTATION_CATALOG.md)。

| 我不确定的事 | 去查的权威文档 |
|---|---|
| 自慰与做爱两个模块的边界、能不能加新功能 | [`PRODUCT.md`](PRODUCT.md) |
| 已接受的长期技术与产品决策（ADR） | [`DECISIONS.md`](DECISIONS.md) |
| Room 表结构、字段、迁移、聚合记录与详情的关系 | [`DATA_MODEL.md`](DATA_MODEL.md) |
| 周、月、年、全部历史的统计口径和图表规则 | [`STATISTICS.md`](STATISTICS.md) |
| 页面交互、颜色令牌、色阶、玻璃材质、无障碍 | [`UI_UX.md`](UI_UX.md) |
| 文案集中化、命名、中文标点禁令、注释写法 | [`ENGINEERING_STANDARDS.md`](ENGINEERING_STANDARDS.md) |
| 分层、ViewModel 与 Repository 的调用关系 | [`ARCHITECTURE.md`](ARCHITECTURE.md) |
| 云同步、离线行为、账号六阶段删除 | [`SYNC_AND_PRIVACY.md`](SYNC_AND_PRIVACY.md) |
| 改动后该跑哪些测试、完整门禁何时跑 | [`TESTING.md`](TESTING.md) |
| Gradle 命令、本机环境、SDK 版本 | [`DEVELOPMENT.md`](DEVELOPMENT.md) |
| 版本号、签名、tag 与 GitHub Release | [`RELEASE.md`](RELEASE.md) |
| Firebase 资源、Rules 与 Functions 部署 | [`FIREBASE_SETUP.md`](FIREBASE_SETUP.md) |
| 云端费用、配额、防刷与止损 | [`firebase-cost-and-quota-inventory.md`](operations/firebase-cost-and-quota-inventory.md)、[`firebase-abuse-monitoring-and-emergency-runbook.md`](operations/firebase-abuse-monitoring-and-emergency-runbook.md)、[`firebase-cost-safety-test-matrix.md`](operations/firebase-cost-safety-test-matrix.md) |
| 这个方向以前栽过什么坑（动手前必查） | [`AI-PITFALLS.md`](AI-PITFALLS.md) |
| 证据等级、汇报格式和停止条件（动手前必查） | [`AI-EVIDENCE-STANDARD.md`](AI-EVIDENCE-STANDARD.md) |
| 改码流程：分支、提交、PR、合并、清理（动手前必查） | [`WORKFLOW.md`](WORKFLOW.md) |
| Issue、PR 和贡献者需要遵守的约定 | [`CONTRIBUTING.md`](../CONTRIBUTING.md) |
| AI 总规则、红线和高风险操作 | [`AGENTS.md`](../AGENTS.md) |
| 某个文件算不算权威、归谁管 | [`DOCUMENTATION_CATALOG.md`](DOCUMENTATION_CATALOG.md)、[`README.md`](README.md) |
| 公共/私有边界、文档生命周期、生成物 | [`REPOSITORY_HYGIENE.md`](REPOSITORY_HYGIENE.md) |
| 面向使用者的隐私说明、漏洞报告 | [`PRIVACY.md`](../PRIVACY.md)（[English](../PRIVACY.en.md)）、[`SECURITY.md`](../SECURITY.md) |

`scripts/docs-integrity.test.mjs` 检查本表链接到文档目录里每一份“当前必读”文档，并检查 [`docs/README.md`](README.md) 里的同名表与本表逐行一致。

### 0.4 八步标准链路

所有改动（代码或文档）都按这八步走，不跳步直接改 `main`。每一步的细则在后面的章节：

| 步 | 做什么 | 细则 |
|---|---|---|
| 0 | 不确定先查文档（0.2、0.3） | §1 |
| 1 | Issue：立项并写清范围边界 | §2 |
| 2 | 分支 + worktree：隔离开发，主检出不直接改 | §3 |
| 3 | 最小实现：按常规档或重构档，不夹带无关修改 | §4 |
| 4 | 本机验证：定向测试、Lint、文档门禁 | §5–§7 |
| 5 | 规范提交：`[<模型标识>:<型号>]: <类型>: <内容>` | §8 |
| 6 | 推送远端分支 | §8 |
| 7 | 发起 PR，等 CI 通过 | §8 |
| 8 | 普通 merge 合入 `main`，删除分支、worktree 和全部中间产物，然后汇报 | §9、§10 |

推送、合并和删除远端分支属于不可轻易撤销的远端写操作；AI 只在用户明确同意后执行（见 [`AI-EVIDENCE-STANDARD.md`](AI-EVIDENCE-STANDARD.md) 第 5 节）。

### 0.5 AI 必守纪律

本仓建立与 [`AGENTS.md`](../AGENTS.md) 严格对齐的 AI 工作纪律体系：

1. **不确定先查文档**：按 0.2 的查证顺序核查，不凭印象动手，不编造数值、路径或命令（§1）。
2. **动手前必查踩坑清单**：动代码前按业务与技术方向查 [`AI-PITFALLS.md`](AI-PITFALLS.md)；清单只增不删。
3. **多用子代理分工，总代理亲自复核**：大任务拆成互不重叠的子任务交子代理并行执行；总代理负责指挥、汇总并对关键结论（路径、行号、构建与测试结果）亲自抽查复核，不原样转述当证据。
4. **最小改动、优先复用（两档制）**：分常规档（改行为就地改、先搜同类实现只留一处、不夹带）与重构档（纯搬迁解耦零行为变更、单独成提交/PR），判据与操作细则见 §4。
5. **及时清理中间产物**：临时脚本、日志、测试数据、分析草稿用完即删；仓库内放 `work/`，仓库外（桌面、临时目录）创建的 worktree 或探针目录**连同目录本身一起删除**，桌面不留残留（§9）。
6. **假定用户会出错，当场指出**：不把用户的话默认当正确输入；发现事实错、决策冲突、违反纪律、前提被推翻时，当场、具体、给证据指出；指出 ≠ 越权，用户拍板后照用户口径执行并在记录留痕。
7. **业务核心底线驱动**：本地优先、隐私第一（生产代码零日志）、数据零损坏、双独立垂直模块（禁止引入通用活动/测量抽象）、文案集中化字典。
8. **证据驱动，重要结论必须可复核**：结论标注〔已验证〕/〔推断〕/〔未知〕并附证据；测试结果五态表达；交付成果 A~E 分类；防形式主义。
9. **隔离工作流**：主检出不直接改源码，改动在 worktree 或独立分支进行，worktree 用完即删。
10. **盯住 Issue / PR 评论**：评审反馈主动查看（继续动手前、开 PR 前、合入关 Issue 前三个时机）；逐条甄别；不管对错都必须回复一条明确交代（已处理 / 刻意不改+理由 / 需人拍板）。
11. **单文件行数硬顶与代码质量规约**：Kotlin 文件目标 400 行以内，硬顶不超过 800 行；函数职责单一，禁止揉合状态机+IO+文案拼装；Compose 遵守单向数据流与无障碍语义规范。
12. **改码必同步文档，不留过时描述**：代码改动影响任何文档描述的行为、口径、流程或数字时，在同一任务内更新所有受影响的当前事实文档，引用同一事实的摘要与速查表一并核对；收尾退出条件是不存在“代码已改、文档还写着旧行为”的已知差异（§6）。

## 1. 任务开始：先查事实

1. 读 [`DOCUMENTATION_CATALOG.md`](DOCUMENTATION_CATALOG.md)，确认任务涉及哪些当前事实文档、哪些只是历史证据。
2. 读 [`AGENTS.md`](../AGENTS.md) 的路由表，按任务范围补齐权威文档。
3. 查现有事实，避免重复劳动和重复决策：
   - 代码与测试：先确认行为到底是怎样的，再判断它是否“错误”。
   - `docs/product/PRODUCT_BACKLOG.md` 与 `docs/ROADMAP.md`：该需求是否已经完成或被取代。
   - GitHub Issues/PR：是否已有同样的问题、结论或未合并的分支。
   - [`AI-PITFALLS.md`](AI-PITFALLS.md)：同一类问题以前是否踩过、当时的正确做法是什么。
4. 检查工作区：`git status --short --branch`。用户未提交的改动属于用户，不得覆盖、还原或顺手提交。

**退出条件**：能说清“这个事实目前在哪里维护、我要改的是代码还是文档”。

## 2. 任务定界：先写清楚再动手

在动手前明确四件事，并把它写进提交说明或 Issue：

- **目标**：一句话说明完成后什么行为会改变。
- **范围边界**：明确**不**做什么。与两个模块无关的功能、未立项的记录类型、发布基线升级都默认不在范围内。
- **风险等级**：是否触碰增长的高风险面（[`AGENTS.md`](../AGENTS.md) 的高风险表）。
- **验收条件**：什么可观察结果算完成；哪些测试会证明它。

若需求会改变产品范围、数据语义或长期方向，先建立 ADR（[`DECISIONS.md`](DECISIONS.md)），再改代码。

**退出条件**：目标、边界、验收条件三句话可写清。

## 3. 分支与工作区

```powershell
git fetch origin main
git worktree add -b agent/<description> work/<description> origin/main
```

- 优先用 worktree 隔离开发：主检出里可能有用户自己的改动，AI 不在主检出里直接改。worktree 放在被 `.gitignore` 排除的 `work/` 下，不放桌面或其他用户目录。
- 只需要一个检出时，也可以用 `git switch -c agent/<description> origin/main`。
- 使用 `agent/<description>` 短生命周期分支；不在 `main` 直接改，也不在别人的分支上改。
- `main` 是唯一当前基线；旧本地分支不能作为开发基线。
- 不执行 `git clean -xfd`、`reset --hard`、强制推送等不可恢复操作。

**退出条件**：分支基于最新 `origin/main`，工作区没有需要保护的用户改动被破坏。

## 4. 实现约束与编码规范（代码应该怎么写）

遵循 [`ENGINEERING_STANDARDS.md`](ENGINEERING_STANDARDS.md) 与 [`ARCHITECTURE.md`](ARCHITECTURE.md)，代码编写严格遵守以下纪律：

### 4.1 最小改动 · 优先复用（两档制）

改动性质严格分档执行，判据为：**“这次改动会不会改变运行时可观察行为？”**

| 维度 | 常规档（改行为、修 Bug、调参数、改交互） | 重构档（拆文件、解耦、消除重复实现） |
|---|---|---|
| **判据** | 会改变运行时可观察行为 | 纯搬迁抽取，运行时可观察行为零变化 |
| **就地改** | 在已有逻辑处就地改，不在旁边另写平行实现 | 显式豁免“就地改”，允许新建高内聚子模块 |
| **复用性** | 动手前先搜同类实现，同一语义只留一处，优先复用扩展既有函数 | 允许提取公共方法与组件，消除多头重复维护 |
| **范围** | 绝不夹带无关的排版格式化、重命名或顺手重构 | 一次只搬迁一个内聚单元，绝不夹带无关业务改动 |
| **节奏** | 一个提交解决一个明确问题 | 单独成提交或 PR，搬迁前后同一组测试全绿再搬下一块 |
| **自检** | `git diff --stat` 明显超出目标时坚决砍回范围内 | 外部调用保持原有接口或纯 re-export 兼容 |

### 4.2 单文件行数硬顶与函数职责

- **单文件行数硬顶**：Kotlin 源码文件目标控制在 400 行以内；新增代码文件物理总行数严禁超过 800 行；
- **历史超长文件治理**：历史遗留的超长 Compose 屏幕与字符串映射文件已于 2026-11 全量拆分治理完毕（Issue #299）；此后不再有历史豁免，超标文件一律按重构档抽离子组件修复，不发起纯刷行数的无实质改动；
- **函数单一职责**：一个函数/Composable 严禁同时揉杂“状态机 + I/O + 文案拼装”，遇到此类情况必须拆分为状态持有、纯计算与界面渲染子函数；
- **禁止投机抽象**：只有一个调用方时不引入接口；出现第二个同形实现时再提取公共逻辑；严禁引入预留抽象。

### 4.3 架构分层与核心红线

- **分层边界**：`ui/` → Repository → Room；UI 层严禁绕过 Repository 直接读写数据库；
- **不可逾越的模型红线**：当前版本只记录**自慰**和**做爱**两个固定、互相独立的垂直模块，**严禁**引入 `Activity`、`MeasurementType`、活动表、健身或通用记录抽象；
- **本地优先**：离线必须能完成全部核心记录与统计功能，网络操作绝不阻塞本地读写；
- **生产代码零日志**：`app/src/` 严禁引入任何 `Log.d`、`println` 或上报 SDK，排查完毕必须彻底清除调试输出；
- **文案与标点集中化**：用户可见文案只能存在于 `ZhStrings.kt` / `EnStrings.kt`，严禁在 UI 或业务代码硬编码中英文案，严禁硬编码中文标点作为列表分隔符；
- **Jetpack Compose 规约**：状态提升与单向数据流；禁止裸 `.collect` Flow；弹窗、按钮使用 `ui/components/` 项目组件，禁止直接使用默认 Material 弹窗；关键控件必须具备 TalkBack 无障碍 `contentDescription`，颜色不得作为唯一状态编码。

**退出条件**：改动可编译，每处改动都能对应定界目标，无多余夹带。

## 5. 定向验证

先按 [`TESTING.md`](TESTING.md) 的影响范围矩阵选择最小测试集，只运行能证明本次行为的那些。

| 改动类型 | 最低验证 |
|---|---|
| 纯文档、链接、审计截图 | `pnpm test:docs`、`pnpm test:copy` |
| 文案、双语字符串 | `pnpm test:copy` + 相关 `AppStrings`/`AppCopy` 单测 |
| Compose 组件、间距、图标 | Debug/AndroidTest 编译 + 相关 Compose 测试类 |
| 纯函数、统计口径、日期边界 | 对应 JVM 测试类 |
| Repository、共享领域模型 | 编译 + 对应单测 + 受影响设备流程 |
| Room Entity/DAO/schema/migration | 迁移测试 + DAO/Repository 测试（设备） |
| 同步、Worker、冲突合并 | 同步单测 + 隔离 Auth/Firestore 设备测试 |
| Firestore Rules、云端字段 | `pnpm test:firestore-rules` + 远端解析测试 |
| Manifest、权限、网络安全 | Manifest 处理 + Lint + 相关构建 |
| Gradle、依赖、SDK | 相关编译 + Lint + 受影响测试 |
| 版本、签名、发布工作流 | `pnpm test:release-metadata` + Release 构建 |

修复缺陷时：先复现，再修复，再补可自动化的回归测试。缺陷无法自动化时至少保留修复后截图或稳定语义断言。

**退出条件**：本次改动的定向证据已运行并通过；失败已区分“本次引入”还是“改动前已存在”。

## 6. 文档与注释同步

- 行为变化 → 更新对应当前事实文档（产品、界面、架构、数据模型、统计、同步、发布）。
- 新增/移动/重命名/删除文档 → 同一提交更新 [`DOCUMENTATION_CATALOG.md`](DOCUMENTATION_CATALOG.md)。
- 版本、schema、SDK、截图资产变化 → `pnpm test:docs` 必须通过。
- 可机械检查的新规则 → 优先补进现有脚本/测试，不要只写在文档里。

**退出条件**：不存在“代码已改、文档还写着旧行为”的已知差异。

## 7. Diff 审查

```powershell
git status --short --branch
git diff --stat
git diff --check
```

逐项确认：

- 每个改动都能对应到第 2 步的目标，没有夹带无关业务改动。
- 没有临时文件、备份文件、调试输出、构建产物或敏感材料。
- 新增/删除/重命名的文件清单与实际意图一致。
- 没有把 `local.properties`、`google-services.json`、签名材料或真实数据带入。

**退出条件**：所有差异都能解释。

## 8. 提交、PR、CI 与审查反馈闭环

提交信息格式：`[<模型标识>:<型号>]: <类型>: <内容>`，例如 `[ds:v4flash]: fix: week boundary`。一个提交只解决一个明确阶段。

```powershell
git add <具体文件>
git commit
git push -u origin agent/<description>
```

PR 正文按 [`.github/pull_request_template.md`](../.github/pull_request_template.md) 填写，必须包含：改动原因、影响范围、实际运行的验证命令与结果、未执行项及原因。

CI 行为（见 [`.github/workflows/android-ci.yml`](../.github/workflows/android-ci.yml)）：

- 先分类改动路径。只有 `docs/`、`LICENSE` 和 `*.md` 的纯文档改动跳过 Gradle、Rules 和设备回归，只跑文档/文案/目录/发布元数据检查。
- 任何代码、配置、Rules、Functions、脚本或工作流改动都会运行完整门禁，包括 API 34 `connectedDebugAndroidTest`。
- 分类失败时按“有代码改动”处理，不会静默跳过门禁。

### 8.1 盯住 Issue / PR 评论反馈（审查意见闭环）

- **三个主动查看时机**：① 继续动手前；② 开 PR 前；③ 合入 PR 与关 Issue 前；
- **命令原文**：
  ```powershell
  gh issue view <Issue号> --comments
  gh pr view <PR号> --comments
  ```
- **逐条甄别**：审查 AI 或协作者的意见不一定完全正确，先按证据标准复核真实路径与上下文；驳回意见必须写明依据与不成立理由；
- **必须明确回复**：只要是有实质内容的评审评论，必须在 Issue / PR 上回复交代（已处理 / 刻意不改+理由 / 需人拍板），严禁静默跳过。

**退出条件**：CI 通过，所有实质性评审意见均已回复交代并闭环。

## 9. 合并、中间产物清理与收尾三件事

合并到 `main` 使用**普通 merge**，保留分支里的每一笔原始提交；禁止 squash、rebase merge 和强推。确认 `main` 已包含完整提交链后再删除远端临时分支。

### 9.1 中间产物应该怎么办（四层彻底清理）

为防止仓库和开发环境日益臃肿，所有中间产物必须按以下四层分类彻底清理：

1. **临时脚本、测试副产物与本地临时日志**：
   - 调试排查中写的一性脚本、采集的 UI XML、本地 logcat 文件、测试临时生成的数据库或导出文件，用完立即物理删除，严禁 commit，严禁散落遗留在任何目录；
2. **临时调研笔记、分析草稿与单次 Markdown 报告**：
   - 仅服务于单次任务的临时备忘、推理草稿，任务结束前彻底清除；
   - 长期有价值的事实、架构调整与踩坑教训，**直接合并进既有权威文档或 [`docs/AI-PITFALLS.md`](AI-PITFALLS.md)**，**严禁**为单次任务在仓库根目录或 `docs/` 下随意堆积游离的单次 md 文件；
3. **仓库内临时工作目录（`work/`）**：
   - 仓库内临时产物和本地 worktree 统一限制在已被 `.gitignore` 排除的 `work/` 目录下；任务完成时必须清空 `work/`（巡检自动化常驻状态文件 `agent-watch-state.json` 除外，详见 [`REPOSITORY_HYGIENE.md`](REPOSITORY_HYGIENE.md)）；
4. **仓库外创建的目录（桌面、临时目录、用户目录）**：
   - 为任务在桌面或临时目录创建的 worktree、探针目录、临时拷贝，**用完即删，连同目录本身一起彻底删除**；
   - 严格杜绝在桌面残留空壳目录或废弃代码文件夹；
   - 处于进行中的活严格保护，不误删他人正在使用的目录。

### 9.2 收尾必做三件事

AI 完成任务进行交付收尾时，必须严格执行三件事：

| # | 动作 | 命令与执行要求 |
|---|---|---|
| ① | **清理中间产物** | 1. 清理临时脚本与日志；<br>2. 清理临时草稿 md；<br>3. 清空 `work/`；<br>4. 删除本次在桌面/临时目录建的 worktree 与目录本身（`git worktree remove` + `Remove-Item`）；<br>5. 运行 `node scripts/workspace-hygiene.mjs`，确保通过。 |
| ② | **该评论则评论** | 回看并回复 Issue / PR 上的所有实质性评论（已处理 / 刻意不改+理由 / 需人拍板），确保无遗留悬空意见。 |
| ③ | **该关闭则关闭** | 1. 验收通过且无遗留事项的 Issue 予以关闭（`gh issue close <号>`）；<br>2. 未完成、需用户拍板裁决或处于观察期的 Issue **严禁关闭**，保持 OPEN 状态；<br>3. 给出包含改动依据、验证结果、未执行项与剩余风险的标准汇报。 |

收尾自检命令：

```powershell
node scripts/workspace-hygiene.mjs
```

**退出条件**：分支已合并，临时产物与仓库外临时目录已彻底清理，`git status --short --branch` 干净，收尾自检通过。

## 10. 最终汇报

最终回复或 PR 描述必须包含：

1. **改了什么**，以及每项改动对应的依据（文档条款、失败测试、Issue、审计结论）。
2. **实际运行的验证命令与结果**，包含失败的项。
3. **未执行的验证及原因**（例如需要签名密钥、需要实体设备、CI 额度不可用）。
4. **仍然存在的风险与未解决问题**，不得隐瞒或美化。

不伪造测试数量、构建状态或 CI 状态；未验证的结论必须标注为未验证。

## 小版本完整验证

只有当一个连贯小版本的功能代码冻结、准备合并或交付时，才在最终功能 head 上运行一次完整套件（命令见 [`TESTING.md`](TESTING.md) 与 [`DEVELOPMENT.md`](DEVELOPMENT.md)）。

后续若只修改说明文字或审计截图，不使 Room、同步、规则或设备测试结果失效，不得机械重复全量套件；反之，若又修改了运行时代码，则重跑该代码影响的定向测试。
