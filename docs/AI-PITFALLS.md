# AI 与开发者踩坑清单

状态：`current — authoritative pitfalls register`
最后复核：2026-11-05

本文件记录**已经付出真实代价、值得防止复发**的问题。它不是开发流水账，也不是普通 bug 列表。

收录标准（满足其一）：

1. 同一根因反复出现；
2. 只发生过一次，但造成高代价或高风险后果（数据丢失、发布事故、账号封禁）；
3. 已经形成长期强制规范，必须避免被无声移除。

已修复的条目**不删除**：保留教训价值，并标明修复状态与防复发机制。只有教训、没有可执行措施的条目，应当继续分析直到能写成测试或门禁。

## 维护规则

- **动代码前先查本表**：按改动方向（日期边界、日历与统计绘制、Room 迁移、文案、同步、Rules、发布）找相关条目，按“正确做法”执行。
- **只增不删**：编号只增不复用；修复后只改索引表的“状态”列，并补写“修复状态”和“防复发机制”，不删除条目。删掉条目等于让同一个坑重新开放。
- **准入门槛**：只收满足上面三条收录标准之一的问题；偶发小问题写进 PR 描述即可，防止本表退化成流水账。
- **新条目格式**：在索引表末尾追加一行，并按下方模板写正文，七个小节缺一不可。

```markdown
## P<编号> <一句话标题>

**背景与证据**：发生了什么；Issue、PR 或提交 SHA。

**影响与代价**：对用户、数据、发布或账号的实际影响。

**根因**：为什么会发生。

**正确做法**：以后怎么做（可执行的规则）。

**修复状态**：已修复 / 已建立纪律 / 已记录。

**防复发机制**：哪个测试、脚本或门禁在保护它；没有就写“暂无自动门禁”。

**权威规则**：链接到对应的当前事实文档。
```

## 条目索引

| 编号 | 标题 | 严重度 | 状态 |
|---|---|---|---|
| [P1](#p1-github-issue-批量连发导致主账号被封) | GitHub Issue 批量连发导致主账号被封 | 严重 | 已建立纪律 |
| [P2](#p2-跨模块快照被当成空集合清掉本地已确认数据) | 跨模块快照被当成空集合清掉本地已确认数据 | 严重 | 已修复并加测试 |
| [P3](#p3-本地-gitinfoattributes-让-crlf-进入提交) | 本地 `.git/info/attributes` 让 CRLF 进入提交 | 中 | 已验证，需注意本机状态 |
| [P4](#p4-中文标点绕过只匹配汉字的文案门禁) | 中文标点绕过只匹配汉字的文案门禁 | 中 | 已修复并加门禁 |
| [P5](#p5-关键回归测试用-assumetrue-静默跳过) | 关键回归测试用 `assumeTrue` 静默跳过 | 高 | 已修复 |
| [P6](#p6-给-firestore-rules-增加必填字段会拒绝旧客户端全部写入) | 给 Firestore Rules 增加必填字段会拒绝旧客户端全部写入 | 高 | 已修复并加测试 |
| [P7](#p7-生产-callable-未部署导致同步瞬时全量失败) | 生产 callable 未部署导致同步瞬时全量失败 | 高 | 已修复 |
| [P8](#p8-移除看起来没用的-gradle-依赖会破坏-androidtest-解析) | 移除"看起来没用"的 Gradle 依赖会破坏 androidTest 解析 | 中 | 已记录，需保留 |
| [P9](#p9-云端-details-无逐项校验可致客户端-oom) | 云端 details 无逐项校验可致客户端 OOM | 高 | 已修复并加测试 |
| [P10](#p10-退役术语与硬编码文案回归) | 退役术语与硬编码文案回归 | 中 | 已加门禁 |
| [P11](#p11-文档目录测试与版本号规则只是散文没有门禁) | 文档目录测试与版本号规则只是散文，没有门禁 | 中 | 已加门禁 |
| [P12](#p12-同一状态规则在日历与统计两处各写一份反复漂移) | 同一状态规则在日历与统计两处各写一份，反复漂移 | 中 | 已修复并加测试 |
| [P13](#p13-位图图标自带透明边靠反复手调偏移对齐) | 位图图标自带透明边，靠反复手调偏移对齐 | 低 | 已修复，无自动门禁 |
| [P14](#p14-v1-迁移把未填写日期迁成明确-0-次) | v1 迁移把未填写日期迁成明确 0 次 | 高 | 已修复并加设备测试 |
| [P15](#p15-内存限流按实例计数宣称的全局阈值并未全局生效) | 内存限流按实例计数，宣称的全局阈值并未全局生效 | 中 | 已如实披露并加门禁 |

---

## P1 GitHub Issue 批量连发导致主账号被封

**背景与证据**：2026-08-16 双语适配（Issue #235 系列）时，AI 在几分钟内并行调用 `gh issue create` 创建多个长篇 Issue，触发 GitHub 滥用/自动化检测，用户主账号 `LitaoG` 被暂停、token 失效，远端推送与 Issue 创建全部中断，只能用申诉渠道缓慢恢复。

**影响与代价**：账号级封禁，全部远端写入能力中断，恢复需要人工申诉，且期间无法发布或接受 PR。

**根因**：把 Issue 创建当成无成本操作，忽略了 GitHub 对自动化写入的频率与并发判定。

**正确做法**：
- 一次会话内创建 Issue 一次只建一个，间隔至少 60 秒。
- 出现任何一次 403、限流或异常响应，立即停止并如实报告，绝不并行重试或换方式绕过。
- 需要批量创建（含长正文）时先取得用户明确同意。
- 推送、PR 等远端写操作同样保持克制节奏。

**修复状态**：已建立纪律，未再复发。

**防复发机制**：`AGENTS.md` 的"分支、提交与远端操作"章节为强制规则，任何代理开工前必须读到。

**权威规则**：[`AGENTS.md`](../AGENTS.md)

---

## P2 跨模块快照被当成空集合清掉本地已确认数据

**背景与证据**：`CombinedSyncCoordinator.observeRemote` 把两个模块的实时流 `merge` 成一条账号级流，`applySnapshot` 又把其中**一个模块**的快照转发给**所有**模块。`DailyCountSyncEngine` 在转发后计算 `completeServerSnapshot = !fromCache && rejectedRecordCount == 0`，于是当 `handBrewRecords` 产出一份新鲜的完整快照时，做爱模块收到 `records = []` 且"完整"，按 ADR-020 的"完整快照权威于缺失"语义删除了自己全部已确认的 Room 行与详情。Firestore 监听器只在该集合发生服务端变化时才重新推送，因此数据会静默消失到下一次做爱集合写入或手动同步为止。

**影响与代价**：静默的跨模块数据可见性丢失；受影响模块的日历显示"未填写"、统计全部读为 0。属于隐私记录应用最严重的一类缺陷（用户会认为自己的记录丢了）。

**根因**：把"快照的完整性"当成与集合无关的属性。空集合与"不是我的集合"在数据上无法区分，而代码却从记录列表反推集合归属。

**正确做法**：远端快照必须声明来源集合（`RemoteSnapshot.sourceCollection`），由读取方写入；模块只处理属于自己集合的快照。"空快照即删除"的判断必须同时校验来源集合。

**修复状态**：已修复。`DailyCountSyncEngine.applySnapshot` 对非本模块集合的快照直接返回 0。

**防复发机制**：`DailyCountSyncEngineTest.completeSnapshotOfTheSiblingCollectionNeverCollectsThisModuleRows` 覆盖该回归；`sourceCollection` 是无默认值的必填参数，所有构造点由编译器强制声明来源。

**权威规则**：[`ARCHITECTURE.md`](ARCHITECTURE.md)、[`SYNC_AND_PRIVACY.md`](SYNC_AND_PRIVACY.md)、[`ENGINEERING_STANDARDS.md`](ENGINEERING_STANDARDS.md) 第 6 节

---

## P3 本地 `.git/info/attributes` 让 CRLF 进入提交

**背景与证据**：本机检出存在一个**未被 Git 跟踪**的 `.git/info/attributes`，其中把若干文件标为 `-text`：

```
.github/workflows/android-ci.yml -text
.github/workflows/release.yml -text
docs/RELEASE.md -text
docs/TESTING.md -text
app/src/main/java/.../FirebaseServices.kt -text
app/src/androidTest/java/.../FirebaseEmulatorIntegrationTest.kt -text
```

`-text` 关闭了行尾转换，因此即使仓库根 `.gitattributes` 声明 `*.yml text eol=lf`，这些文件的 CRLF 仍会原样进入提交；同时 `git diff --check` 会把每一行新增内容都误报为 "trailing whitespace"（实际上是行尾的 `\r`）。

**影响与代价**：提交里混入与仓库其余部分不一致的 CRLF，产生难以理解的 diff 与误报的 whitespace 失败；如果直接把 `git diff --check` 的报错当成真实问题，会浪费大量排查时间，甚至被诱导向错误方向修改文件内容。

**根因**：`.git/info/attributes` 优先级高于工作树 `.gitattributes`，而它是本机状态、不随仓库分发，因此这种差异只在特定机器上出现，且从仓库内容里完全看不出来。

**正确做法**：
- 提交前用 `git diff --cached --check` 检查**已暂存**内容，而不是只看工作树状态。
- 发现上述文件被标记时，把工作树文件规范化为 LF 再暂存；不要为了让报错消失而删改代码内容。
- 不要擅自删除用户的 `.git/info/attributes`；它是本机配置。

**修复状态**：本次治理已验证该机制并规范化了受影响的文件；本机配置本身保持原样。

**防复发机制**：`.gitattributes` 显式声明 `.gitignore`、`*.js`、`*.mjs` 的 `eol=lf`，减少需要转换的文件面；收尾清单要求检查已暂存 diff。

**权威规则**：[`REPOSITORY_HYGIENE.md`](REPOSITORY_HYGIENE.md)、[`WORKFLOW.md`](WORKFLOW.md) 第 7 节

---

## P4 中文标点绕过只匹配汉字的文案门禁

**背景与证据**：`scripts/copy-integrity.test.mjs` 原本用 `/[\u3400-\u9fff]/` 扫描 `app/src/main`，只覆盖汉字本体。`StatisticsStage4Components.kt` 因此可以长期硬编码 `months.joinToString("、")`（U+3001 顿号，属 CJK 标点，不在该区间），在 English 界面渲染出中文标点，而门禁全程通过。

**影响与代价**：双语契约被静默破坏；英文用户看到中日韩标点，而自动化检查给出"通过"的假信号。

**根因**：把"文案泄漏"等同于"出现汉字"，忽略了标点、全角符号同样是语言相关字符。

**正确做法**：门禁字符集必须覆盖 CJK 标点（U+3000–U+303F）与全角形式（U+FF00–U+FFEF）；语言相关分隔符一律放进字符串目录（如 `StatisticsStrings.monthListSeparator`），由 `AppCopy` 暴露。

**修复状态**：已修复。分隔符移入 `ZhStrings`/`EnStrings`，门禁字符集已扩展。

**防复发机制**：扩展后的 `copy-integrity.test.mjs` 已用临时探针文件验证确实会失败（探针随后删除）。

**权威规则**：[`ENGINEERING_STANDARDS.md`](ENGINEERING_STANDARDS.md) 第 7 节、[`UI_UX.md`](UI_UX.md)

---

## P5 关键回归测试用 `assumeTrue` 静默跳过

**背景与证据**：`docs/TESTING.md` 把 `DailyRecordSyncWorkerTest` 的"物理删除后 pending 编辑恢复"明确列为必测项。该测试的前置检查 `assumeAuthEmulatorReachable()` 在异常分支调用 `assumeTrue(..., false)`，JUnit 会把 `assumeTrue(false)` 记为**跳过**（而不是失败）；`connectedAndroidTest` 在"0 failures"下报告绿色。同一仓库的 `FirebaseEmulatorIntegrationTest` 对同一前置条件使用 `assertEquals(200, ...)` 硬失败，两套测试对同一条件给出不同结论，证明这是实现缺陷而非约定。此外原实现接受 `responseCode in 200..499`，非模拟器的 4xx 监听也会满足前置条件，导致后续以晦涩方式失败。

**影响与代价**：设备套件可以在缺少 Auth 模拟器时报告全部通过，而实际上最重要的同步恢复回归根本没有执行；门禁形同虚设。

**根因**：把"环境未就绪"当成可以跳过的理由，而该环境是运行该套件的必备前提（由 `firebase emulators:exec` 启动）。

**正确做法**：必需前提用硬断言表达失败；只有显式 opt-in 的测试（例如需要生产凭据的烟雾测试）才允许跳过。

**修复状态**：已修复。改为 `requireAuthEmulatorReachable()` 并硬断言 200。

**防复发机制**：与本仓库其他模拟器测试保持同一硬失败写法；`TESTING.md` 明确该回归为必测项。

**权威规则**：[`TESTING.md`](TESTING.md)、[`ENGINEERING_STANDARDS.md`](ENGINEERING_STANDARDS.md) 第 8 节

---

## P6 给 Firestore Rules 增加必填字段会拒绝旧客户端全部写入

**背景与证据**：Issue #216 记录：规则要求 `details` 必填后，`v1.0.0-beta.2` 及更早的已安装客户端所有写入都会被拒绝。

**影响与代价**：已发布版本在线功能整体失效；规则是服务端强约束，客户端无法即时修补，只能发新版或回滚规则。

**根因**：把客户端与服务端部署当成同时发生，忽略了已安装的旧版本会长期存在。

**正确做法**：
- 规则字段契约变更必须向后兼容（新增可选字段），或明确接受旧客户端写入被拒并据此规划发布。
- 任何 Rules 改动必须跑 `pnpm test:firestore-rules`（含旧形状拒收/接受用例）。
- 规则改动与非规则改动分开提交，避免"顺手收紧"。

**修复状态**：已修复（`details` 改为可缺省并按缺省处理）。

**防复发机制**：`scripts/firestore-rules.test.mjs` 的字段形状与墓碑用例；`SYNC_AND_PRIVACY.md` 记录线上写入契约。

**权威规则**：[`FIREBASE_SETUP.md`](FIREBASE_SETUP.md)、[`DATA_MODEL.md`](DATA_MODEL.md)

---

## P7 生产 callable 未部署导致同步瞬时全量失败

**背景与证据**：Issue #248 记录：已登录账号同步瞬时全部失败（`Unknown`），原因是生产 callable 未部署。

**影响与代价**：线上同步整体不可用，且客户端错误被归类为 `Unknown`，难以定位到"函数不存在"。

**根因**：客户端与本机模拟器测试都通过，但生产部署是独立的人工步骤，缺少发布前核对。

**正确做法**：改动 callable 契约、名称或区域时，把"部署"列入发布清单并与客户端改动一起验收；区域固定 `asia-east1`，客户端与函数配置必须一致。

**修复状态**：已恢复；部署作为显式人工步骤记录在权威文档中。

**防复发机制**：`delete-log` 与规则测试覆盖函数行为；`FIREBASE_SETUP.md` 记录资源与部署边界。

**权威规则**：[`FIREBASE_SETUP.md`](FIREBASE_SETUP.md)、[`SYNC_AND_PRIVACY.md`](SYNC_AND_PRIVACY.md)

---

## P8 移除"看起来没用"的 Gradle 依赖会破坏 androidTest 解析

**背景与证据**：Issue #256 期间验证过：`androidx.kotlinx-serialization-bom` 在主 classpath 上没有任何直接 import，看起来是零引用依赖；但移除它会把 release 传递依赖降级到 1.7.3 并破坏 androidTest 的一致解析。

**影响与代价**：如果按"零引用即删除"的直觉清理，会在 release 或 androidTest 阶段才暴露失败，且原因隐晦。

**根因**：用"有没有 import"判断依赖是否有用，忽略了 `platform()` 固定传递版本这一类非直接用途。

**正确做法**：删除依赖前先确认它在依赖图中的作用（`gradlew :app:dependencies --configuration <cfg>`）；对有意保留的"非依赖"在构建脚本里写明原因。

**修复状态**：保留并已在 `app/build.gradle.kts` 注释说明原因。

**防复发机制**：构建脚本内的解释性注释（这是注释被允许存在的正当理由）；依赖变更要求完成设备回归。

**权威规则**：[`DEVELOPMENT.md`](DEVELOPMENT.md)、[`DECISIONS.md`](DECISIONS.md)

---

## P9 云端 details 无逐项校验可致客户端 OOM

**背景与证据**：Issue #206 记录：云端 `details` 无逐项规则校验，聚合次数无上限，可导致客户端 OOM。

**影响与代价**：恶意或损坏的云端文档可以让客户端在解析时耗尽内存；属于可用性与数据安全双重风险。

**根因**：把"服务端会校验"当成默认事实，而 Rules 无法做数组逐项迭代，必须由可信函数与客户端双重设限。

**正确做法**：
- 写入路径只走可信 callable，由函数做逐项校验与数量上限。
- 客户端解析必须设可信上限（`MAX_REMOTE_DETAIL_COUNT`），超限文档按拒收处理而不是加载进 Room。
- 拒收的文档不得触发"完整快照"语义（见 P2）。

**修复状态**：已修复。

**防复发机制**：`FirebaseEmulatorIntegrationTest` 的详情用例、`parseRemoteHandBrewRecord`/`parseRemoteSexRecord` 的上限断言、`firestore-rules.test.mjs` 的 1000 行上限用例。

**权威规则**：[`DATA_MODEL.md`](DATA_MODEL.md)、[`FIREBASE_SETUP.md`](FIREBASE_SETUP.md)

---

## P10 退役术语与硬编码文案回归

**背景与证据**：早期模块使用现已退役的中文名称，产品改为“自慰”后曾出现旧术语回流（Issue #185 专门替换全部用户可见文案）；同时存在用户可见文案被写在 UI 代码里的历史。本仓库现有一条门禁：任何被跟踪文件都不得包含该退役术语（用码点匹配，避免门禁自身成为例外）。

**影响与代价**：用户看到与产品不一致的术语；双语适配后硬编码文案无法翻译。

**根因**：文案散落在业务代码里，没有单一来源，也没有机械检查。

**正确做法**：用户可见文案只存在于 `ZhStrings.kt` / `EnStrings.kt`；`app_name` 例外仅在 `values*/strings.xml`。

**修复状态**：已修复并加门禁。

**防复发机制**：`scripts/copy-integrity.test.mjs` 的两项检查——中文文案白名单，以及全部被跟踪文件中不得出现退役术语。

**权威规则**：[`UI_UX.md`](UI_UX.md)、[`ENGINEERING_STANDARDS.md`](ENGINEERING_STANDARDS.md) 第 7 节

---

## P11 文档目录测试与版本号规则只是散文，没有门禁

**背景与证据**：`scripts/documentation-catalog.test.mjs` 早已存在用来校验 `DOCUMENTATION_CATALOG.md` 覆盖全部被跟踪文档，但它**没有出现在任何 workflow** 中，因此新增或删除 Markdown 并留下过期目录可以正常合并。同样，`docs/RELEASE.md` 要求 `versionCode` 每次发布严格递增，而 `release-metadata.mjs` 只校验它是正整数，没有任何地方比较上一个版本。

**影响与代价**：规则写了却不会被执行；文档目录静默腐化，`versionCode` 复用或降低会直接导致已安装用户无法覆盖升级，而所有门禁都会通过。

**根因**：把"规则写在文档里"等同于"规则被执行"；缺少"每条重要规则都要有可执行措施"的验收标准。

**正确做法**：能被机械检查的规则必须写成脚本或测试，并接入 CI；新增规则时同步问"它由什么门禁保护"。

**修复状态**：已修复。`documentation-catalog.test.mjs` 加入两个工作流；`release-metadata.mjs` 读取上一个 release tag 的 `versionCode` 并拒绝复用或降低；发布工作流新增 tag 必须包含在 `main` 中的校验；tag 校验被移到模拟器回归之前。

**防复发机制**：上述门禁本身，以及在临时探针（把 `versionCode` 改成 3、制造中文标点、制造环境缺失）下验证过它们确实会失败。

**权威规则**：[`WORKFLOW.md`](WORKFLOW.md) 第 6、8 节、[`RELEASE.md`](RELEASE.md)、[`TESTING.md`](TESTING.md)

---

## P12 同一状态规则在日历与统计两处各写一份，反复漂移

**背景与证据**：周统计环图的次数色阶、未填写色和未来色曾在统计页单独维护，至少 6 笔提交在修它与主页日期格的不一致（`9fb1d97`、`c7336a5`、`7e0e424`、`0ab7f30`、`914546a`、`3f4d7b9`）。日期边界出现过同类问题：`5bab78b` 让日历改用统计核心的边界，Issue #144 修正 1970 年首周“上一周”按钮的禁用时机。

**影响与代价**：同一天在两个页面显示不同颜色或不同可浏览范围，用户会误读记录；每次只修一处，下一次又在另一处漂移，产生多轮小修和截图往返。

**根因**：把“看起来一样”的规则复制进第二个组件，而不是引用同一个来源。

**正确做法**：
- 记录状态到颜色只走 `RecordModuleColorTokens.colorsFor(RecordVisualState)`，不在图表里另写色值。
- 日期上下限只用统计核心（`core/statistics`）给出的同一组边界。
- 新增图表或页面前先找现有映射并复用；确实需要不同表现时，在 [`UI_UX.md`](UI_UX.md) 写明差异和原因。

**修复状态**：已修复。`weekRingColorForBand` 直接复用 `colorsFor(...)`。

**防复发机制**：`StatisticsPeriodCardsTest` 断言周环四档颜色等于主页日期格对应色阶的背景色。

**权威规则**：[`UI_UX.md`](UI_UX.md) 的“视觉材质与令牌”、[`STATISTICS.md`](STATISTICS.md)

---

## P13 位图图标自带透明边，靠反复手调偏移对齐

**背景与证据**：写感受按钮的笔形 PNG 自带约 21% 透明边，32dp 图标框让图标与文字间距过大；PR #255 把资源裁边（512×512 → 332×192）、改为 24dp 图标框并做 2dp 光学左移，之后又调成当前代码里的 1dp。底部导航日历图标、图例圆点和详情控件也经历了多轮对齐微调（`1a1608b`、`e5dfbc3`、`676ae85`、`fb66716`、`b58a6e7`、Issue #176）。

**影响与代价**：每次凭肉眼加减几 dp，换屏幕密度或字体缩放后又要重调，产生多次小 PR 和截图往返。

**根因**：把资源自身的透明边当成布局问题处理；偏移量没有测量依据，也没有写明原因。

**正确做法**：
- 先裁掉资源的透明边，再调布局。
- 必须保留的光学偏移写成具名常量，并在注释中写测量依据（[`ENGINEERING_STANDARDS.md`](ENGINEERING_STANDARDS.md) 第 2 节）。
- 用同视口前后截图验证；截图只放临时目录，不进仓库。

**修复状态**：已修复（裁边资源与带注释的光学偏移，见 `RecordDetailsComponents.kt`）。

**防复发机制**：暂无自动门禁；依靠第 2 节“魔法数字需测量依据并附注释”的规则和 PR 中的同视口截图。

**权威规则**：[`ENGINEERING_STANDARDS.md`](ENGINEERING_STANDARDS.md) 第 2 节、[`UI_UX.md`](UI_UX.md)

---

## P14 v1 迁移把未填写日期迁成明确 0 次

**背景与证据**：v1 为每个交互过的日期保留一行，并把 `status` 默认为 `'UNSET'`。`MIGRATION_1_2` 曾把这些行迁成 `brew_count = 0` 的明确记录，改变整段历史的记录天数统计（Issue #132，`4ab154a`）；随后又补了一次过滤，要求 v1 行同时具备 `quantity`（`3d42de7`）。

**影响与代价**：静默改写用户历史：未填写变成“0 次”，记录天数、分布和日均全部偏移；迁移一旦发布，无法从客户端撤回。

**根因**：迁移按“行存在即有记录”处理，忽略了历史列 `status` / `quantity` 的语义。

**正确做法**：迁移按旧列语义逐项映射；未填写绝不迁成明确 0；每个迁移都要用真实形状的旧数据做设备迁移测试（[`ENGINEERING_STANDARDS.md`](ENGINEERING_STANDARDS.md) 第 4 节）。

**修复状态**：已修复。`MIGRATION_1_2` 只迁移 `status != 'UNSET'` 且 `quantity IS NOT NULL` 的行。

**防复发机制**：设备测试 `DatabaseSchemaTest` 断言 UNSET 行迁移后不产生记录；第 4 节的迁移规则。

**权威规则**：[`DATA_MODEL.md`](DATA_MODEL.md)、[`ENGINEERING_STANDARDS.md`](ENGINEERING_STANDARDS.md) 第 4 节

---

## P15 内存限流按实例计数，宣称的全局阈值并未全局生效

**背景与证据**：`functions/rate-limiter.js` 用进程内 Map 做滑动窗口计数；Cloud Functions v2 按 `resource-config.js` 配置最多扩到 10 个（写入）/5 个（删除）实例，每个实例有独立计数桶。文档曾宣称「单 UID 每分钟最多 30 次写入」，实际单 UID 全局上限 = 阈值 × 活跃实例数，且实例冷启动清零桶（Issue #289，PR #280 评审发现）。

**影响与代价**：防刷阈值被按实例乘数放大，文档口径与真实防护不符；若按误信的全局阈值做容量/止损决策会低估实际费用上限。

**根因**：把进程内限流当成全局限流描述；「零数据库读写」的最小成本取向与「全局精确计数」天然冲突，但没有把这一边界写下来。

**正确做法**：
- 涉及实例内存态的防线，文档必须写明「按实例」口径与放大倍数（`maxInstances` 是硬上限）。
- 若要真正全局计数，先核算它自身的费用影响（每次校验多一次计费 Firestore 读），不能为防刷引入新账单；本项目因此选择保留按实例计数并如实披露。
- 单 UID 防刷的硬兜底是 `maxInstances`、App Check 与熔断器；限流器只是纵深防御的一层。

**修复状态**：已修复。`rate-limiter.js` 头注释、费用矩阵第 1 行与 inventory 第 3 行已如实披露按实例口径；未改变阈值（收缩阈值会误伤落在单个热实例上的正常用户）。

**防复发机制**：`scripts/firebase-cost-safety.test.mjs` 新增资源四件套断言（值锁定 + 确认真被传给 `onCall`/`onDocumentWritten`），矩阵第 3 行命令同步更正为该测试。

**权威规则**：[`docs/operations/firebase-cost-safety-test-matrix.md`](operations/firebase-cost-safety-test-matrix.md)、[`docs/operations/firebase-cost-and-quota-inventory.md`](operations/firebase-cost-and-quota-inventory.md)
