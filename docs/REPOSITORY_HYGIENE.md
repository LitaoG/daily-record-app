# 仓库整理与文档生命周期

最后复核：2026-08-08

本文是 Daily Record 的仓库维护说明，解决“当前事实在哪里、历史材料如何保留、什么时候清理分支和生成物”这三个问题。它不替代产品、架构、数据模型或统计契约。

## 当前事实来源

- 日常开发、提交、Pull Request、CI、审查和合并只在公共仓库 [`LitaoG/daily-record-app`](https://github.com/LitaoG/daily-record-app) 完成。
- 整理开始时的公共 `main` 基线为 `9e2aa5fd3a737fe40ed31d0b5a7d2657ee0426e7`（短写 `9e2aa5f`）；它包含 `v1.0.0-beta.2` 发布后的 weekly chart TalkBack 语义修复（PR #90）。本次 PR 合并后，以 GitHub `main` 的最新提交为当前事实。
- 当前公开发布仍是 [`v1.0.0-beta.2`](https://github.com/LitaoG/daily-record-app/releases/tag/v1.0.0-beta.2)，tag 指向发布时的提交；发布后的无障碍修复不改变版本号和 APK。
- `main` 是唯一当前代码事实来源。旧本地分支不得作为开发基线；开始工作前应从最新公共 `main` 建立短生命周期分支。

## 文档分类与处理规则

### 当前事实文档

这些文档描述现在的代码、数据、交互、安全和发布规则，必须随实现变化同步更新：

- `README.md`、`PRIVACY.md`、`SECURITY.md`、`CONTRIBUTING.md`、`AGENTS.md`
- `docs/PRODUCT.md`、`docs/UI_UX.md`、`docs/ARCHITECTURE.md`、`docs/DATA_MODEL.md`
- `docs/STATISTICS.md`、`docs/SYNC_AND_PRIVACY.md`、`docs/TESTING.md`
- `docs/DEVELOPMENT.md`、`docs/FIREBASE_SETUP.md`、`docs/RELEASE.md`、`docs/ROADMAP.md`

### 当前产品协作文档

`docs/product/README.md` 和 `docs/product/QUIET_PRIVATE_JOURNAL_GOALS.md` 是产品协作入口。UI v2 Stage 0–6 已关闭；新的视觉、功能或记录类型必须新建 Issue/Goal/ADR，不能继续在已完成阶段下追加隐含范围。

`docs/product/IMPLEMENTATION_READINESS.md`、`MONTH_STATISTICS_REDESIGN.md`、`BRIGHT_GLASS_THEME.md` 和 `design/quiet-private-journal-v2/README.md` 是当前冻结契约或实现基线。若代码与其中任一项冲突，先修正文档的当前状态或建立 ADR，再改代码。

### 历史审计与设计资产

`docs/product/audit/`、`docs/product/assets/hand-brew-v2/`、`docs/product/FIGMA_DESIGN_SYSTEM.md` 和 `docs/product/HAND_BREW_REFACTOR_LOG.md` 保留截图、命令、阶段结论和旧方案，不能当作当前待办。历史文档保留当时的状态，但必须有“归档/被后续版本取代”的说明；不得为了让旧截图看起来像当前 UI 而改写证据。

`docs/design/icon-source/` 是图标源文件交接目录：SVG/PNG 设计源可以保留，运行时只引用轻量自适应图标层；营销大图不进入 APK 资源。

## 本次整理决策

- 删除仓库根目录未被任何文档引用、且内容与 `docs/product/audit/2026-08-02-year-line-chart/README.md` 重复的 `design-qa.md`；历史证据仍在审计目录中。
- 保留 Stage 1–5、旧月统计、旧 Figma 和仅手冲资产，因为它们包含可追溯的截图、测试和迁移背景；在索引和文档状态中明确它们是历史材料。
- 将 UI v2 Goal、设计基线、玻璃主题、README、路线图、Backlog 和 UI 事实文档更新到 Stage 0–6 已完成、公共 `main` 已完成 beta.2 后维护的状态。
- 不删除 GitHub Release、tag、已关闭 Issue、审计截图或 Room 迁移历史；删除这些内容会损失恢复和审查证据。

## 分支与 PR 清理

- PR 合并后删除远端临时分支；仅当分支没有未合并提交且没有打开 PR 时才删除。
- 本次整理后应只保留公共 `main` 和明确仍在工作的分支。旧本地 `main` 若与公共 `main` 分叉且没有待保留提交，应在创建可恢复的归档引用后删除，避免误从过时代码继续开发。
- 不使用 `reset --hard`、强推或重写公共历史；所有清理通过普通分支删除和新提交完成。

## 生成物、私密材料与存储

- `build/`、`.gradle/`、`node_modules/`、Firebase 调试日志、测试临时报告和模拟器截图缓存只存在本地，使用后清理；不把测试文件写到桌面。
- `app/google-services.json`、`local.properties`、`keystore.properties`、签名文件、APK/AAB、真实数据库、token 和日志均由 `.gitignore` 排除，并通过 `git ls-files` 定期确认没有进入历史。
- 公共仓库不保存真实用户数据或生产配置。私有恢复仓库只在公共 `main` 完成一个连贯小版本、或私有恢复材料发生变化时同步；本次仅为公共文档/分支整理，不产生新的私有生产材料，因此不触发私有镜像同步。

## 每次整理的最小验证

```powershell
git status --short --branch
git diff --check
node --test scripts/docs-integrity.test.mjs
node --test scripts/release-metadata.test.mjs
node --test scripts/copy-integrity.test.mjs
git ls-files | rg -i 'google-services\\.json|local\\.properties|keystore|\\.apk$|\\.aab$|\\.jks$|\\.log$'
```

代码、数据库、同步、Manifest 或发布配置发生变化时，再按 [`TESTING.md`](TESTING.md) 扩大到受影响的 Gradle、规则和 Android 设备测试；纯文档或历史截图整理不机械重复无关的完整设备套件。
