# 端到端工程工作流

状态：`current — authoritative workflow`
最后复核：2026-10-16

本文件是 Daily Record 从任务开始到合并清理的唯一权威流程来源。其他文档（[`AGENTS.md`](../AGENTS.md)、[`CONTRIBUTING.md`](../CONTRIBUTING.md)、[`DEVELOPMENT.md`](DEVELOPMENT.md)、[`TESTING.md`](TESTING.md)）只保留各自职责范围内的简短摘要并链接回本文，不再各自维护一套互相矛盾的步骤。

测试范围与完成门槛以 [`TESTING.md`](TESTING.md) 为唯一事实来源；本文只规定**何时**进入哪个环节，不复制具体命令矩阵。产品与数据边界见 [`PRODUCT.md`](PRODUCT.md)。

## 0. 全流程一览

```
读文档 → 定界 → 分支 → 实现 → 定向验证 → 同步文档 → Diff 审查 → 提交/PR → CI → 合并 → 清理 → 汇报
```

每一步都有明确的退出条件。缺少任何一步的退出条件时不要进入下一步。

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
git switch -c agent/<description> origin/main
```

- 使用 `agent/<description>` 短生命周期分支；不在 `main` 直接改，也不在别人的分支上改。
- `main` 是唯一当前基线；旧本地分支不能作为开发基线。
- 不执行 `git clean -xfd`、`reset --hard`、强制推送等不可恢复操作。

**退出条件**：分支基于最新 `origin/main`，工作区没有需要保护的用户改动被破坏。

## 4. 实现

- 遵循 [`ENGINEERING_STANDARDS.md`](ENGINEERING_STANDARDS.md)。
- 保持改动最小：不做与本次目标无关的重构、格式化或依赖升级。
- 发现相邻问题时，记录并单独处理，不要顺手改。
- 修改业务代码时同步修正过时注释；注释只解释**为什么**，不复述代码。

**退出条件**：改动可编译，且每处改动都能对应到第 2 步定义的目标。

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

## 8. 提交、PR 与 CI

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

**退出条件**：CI 通过，或失败原因已定位并说明。

## 9. 合并与清理

- 合并到 `main` 使用**普通 merge**，保留分支里的每一笔原始提交；禁止 squash、rebase merge 和强推。
- 合并后确认 `main` 已包含完整提交链，再删除远端临时分支。
- 删除本次产生的临时产物：临时脚本、截图、UI XML、logcat、构建日志、导出数据、一次性报告。仓库内临时文件统一放 `work/`（已被 `.gitignore` 排除），用完即删。
- 不把本次审计的推理过程、中间日志和重复报告永久提交。

**退出条件**：分支已合并，临时产物已清理，`git status --short --branch` 干净。

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
