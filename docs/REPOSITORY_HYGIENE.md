# 仓库整理与文档生命周期

状态：`current — repository and documentation policy`
最后复核：2026-11-05

本文是 Daily Record 的公共仓库维护说明，回答四个问题：当前事实在哪里、历史材料如何保留、机器相关文件为什么不入库、什么时候清理分支和生成物。它不替代产品、架构、数据模型或统计契约；文档的逐文件状态和阅读路由以[文档目录与 AI 阅读索引](DOCUMENTATION_CATALOG.md)为准。

## 当前事实来源

- 日常开发、提交、Pull Request、CI、审查和合并只在公共仓库 [`LitaoG/daily-record-app`](https://github.com/LitaoG/daily-record-app) 完成。
- 当前公开发布为 [`v1.0.0-beta.3`](https://github.com/LitaoG/daily-record-app/releases/tag/v1.0.0-beta.3)，对应 `versionCode = 4`；发布后的当前状态以公共 `main`、代码、测试和 CI 为准。
- `main` 是唯一当前代码事实来源。每次工作开始前从最新公共 `main` 建立短生命周期 `agent/<description>` 分支；旧本地分支不能作为开发基线。
- 当前文档的入口、优先级和 AI 阅读顺序由 [`docs/DOCUMENTATION_CATALOG.md`](DOCUMENTATION_CATALOG.md) 集中维护。新增、移动、重命名或删除文档必须同步更新该目录。

## 文档分类与处理规则

### 当前事实和契约

`README.md`、根目录协作规则、`docs/PRODUCT.md`、`docs/UI_UX.md`、`docs/ARCHITECTURE.md`、`docs/DATA_MODEL.md`、`docs/STATISTICS.md`、`docs/SYNC_AND_PRIVACY.md`、`docs/DECISIONS.md`、`docs/DEVELOPMENT.md`、`docs/TESTING.md`、`docs/FIREBASE_SETUP.md`、`docs/RELEASE.md` 和 `docs/ROADMAP.md` 描述当前行为或当前流程，必须随代码、测试、配置和发布变化同步更新。

`docs/product/IMPLEMENTATION_READINESS.md`、`MONTH_STATISTICS_REDESIGN.md` 和 `USER_STORIES_AND_ACCEPTANCE.md` 是当前产品协作契约；视觉令牌和界面规则只写在 `docs/UI_UX.md`。代码与其中任一项冲突时，先确认代码/测试事实，再修正文档或建立 ADR；不能默默带着冲突继续开发。

### 历史审计与发布证据

`docs/product/HAND_BREW_REFACTOR_LOG.md`、`OPTIMIZATION_2026-08_LOG.md`、`RUNTIME_UX_AUDIT.md`、`GOAL_SYNC_PRIVACY_HARDENING_AUDIT.md` 和 `docs/releases/` 中的旧版本记录保留命令、提交、分支、测试数量和当时结论。它们必须标明“历史/已取代/发布历史”，不能作为当前待办或当前分支状态。

2026-11 起仓库不再新增阶段审计目录、审计截图或一次性报告：验证证据写进 PR 描述，仍然有效的规则写进对应权威文档，发布事实写进 `docs/releases/`。2026-07 至 2026-09 的 `docs/product/audit/`、`docs/product/design/` 以及明亮玻璃主题、日常使用反馈和旧 Figma 设计说明草案已在 2026-11 移除，恢复方法见下文“文档删除与历史保留”。

历史文档允许补充归档说明和指向当前事实的链接，但不得为了与现在一致而改写当时的证据。需要确认过去发生了什么时，才按目录中的 AI 阅读标记读取它们。

### 生成物与资产说明

`app/schemas/README.md` 说明 Room 自动导出的 schema 快照；`docs/design/` 和产品资产 README 说明设计源和运行时轻量资源。它们不是业务事实的替代品，也不能把导出图、预览图或机器截图误当作当前实现证明。

首页展示的运行截图集中在 [`docs/product/assets/readme/README.md`](product/assets/readme/README.md)。它们是经过脱敏的专用模拟器 Debug 产物，可以作为当前 UI 展示提交；采集命令输出、UI XML、logcat、模拟器缓存和真实账号状态仍是机器临时文件，不入库。

## 资源分层

资源按用途分三层，混用是仓库臃肿和误判的主要来源：

1. **运行时资源**：只有 `app/src/main/res` 会进入 APK。`docs/` 下的任何图片都不会被打包，也不被 Android 引用。
2. **当前设计源**：`docs/design/icon-source/` 只保留自适应图标的交接输入（`background.svg`、`foreground.svg`、`monochrome.svg`）和用户确认的高分辨率导出（`daily-record-icon-1254.png`、`google-play-icon-512.png`）。三层 PNG 副本、1024px 备份和自适应预览都只是派生导出：它们不被 Android、设计工具或发布流程引用，需要比较旧版本时用 Git 历史而不是把副本放回当前目录。
3. **历史证据**：工作树只保留少量历史文档和 `docs/releases/` 发布说明；审计截图不再入库，需要时从 Git 历史查看。

判断规则：一个资源如果既不被运行时代码引用，也不是唯一的当前设计源，也没有独立验收或合规作用，就应当从工作树移除并由 Git 历史保存。删除时必须确认引用关系，且不得删除当前 README 截图、Room schema 快照或唯一的发布与安全证据。

## 为什么 `local.properties`、SDK/cache 等不入库

这些文件不是“漏提交”，而是有意排除的机器状态：

- `local.properties` 通常包含本机 Android SDK 的绝对路径。换电脑后路径、用户名和 SDK 安装位置都会变化；提交它会让其他机器继承错误路径，并暴露本机环境信息。Android Studio 或 Gradle 会在新电脑本地重新生成它。
- Android SDK、Gradle cache、`build/`、`.gradle/`、`node_modules/` 和模拟器缓存是可由版本化的 Gradle wrapper、锁文件和构建配置重建的下载物/产物。它们体积大、跨平台差异明显，也不属于源代码。
- `google-services.json`、`keystore.properties`、签名文件、token、真实数据库、APK/AAB 和日志可能包含生产标识、密钥、用户数据或机器信息，不能进入公共仓库或公共 Git 历史。
- 仓库只提交能在新机器上复现开发环境的内容：源码、测试、Gradle wrapper、锁文件、Rules/Functions 源码、Room schema 快照、必要的设计源和文档。新电脑按 [`DEVELOPMENT.md`](DEVELOPMENT.md) 与 [`FIREBASE_SETUP.md`](FIREBASE_SETUP.md) 安装 SDK，并在本机生成 `local.properties`；需要生产联调的配置通过受控渠道提供。

公共仓库不保存真实用户数据或生产配置。私有恢复镜像不是日常开发源，也不能把其中的私有生产材料反向推送到公共仓库；只有完成一个连贯发布里程碑或私有恢复材料发生变化时，才按单独流程同步。纯公共文档整理不自动产生私有同步内容。

## 分支、提交与 PR 清理

- 使用 `agent/<description>` 短生命周期分支；不在 `main` 直接修改。
- PR 合并必须保留分支中的每一笔原始提交，使用普通 merge；不使用 squash merge、rebase 合并、强推或其他历史重写方式。
- PR 合并后，只有在没有未合并提交且没有打开 PR 时才删除远端临时分支；删除前先确认 `main` 已包含完整提交链。
- 不使用 `reset --hard`、`git checkout --` 覆盖用户改动或任何不可恢复的批量删除。清理前先检查 Git 跟踪、引用和文档链接。

## 文档删除与历史保留

- 删除前检查 `DOCUMENTATION_CATALOG.md`、本地链接、脚本、Issue/PR 引用和发布/迁移用途。
- 仍然有效的规则先并入对应权威文档，再删除；`docs/releases/` 发布说明、Room schema 快照和当前 README 截图不删除。
- 删除范围必须写入提交说明和 PR，并保证 `scripts/docs-integrity.test.mjs` 没有悬空链接。
- 一次性清理的范围和结论写在提交说明里即可，不再单独创建一份很快过时的清理记录；资源判废规则见上方“资源分层”。

已删除的历史材料可以只读找回，不需要放回仓库：

```powershell
git log --oneline --diff-filter=D -- docs/product/audit docs/product/design
git show <删除提交>^:docs/product/audit/README.md
git restore --source <删除提交>^ --worktree -- docs/product/audit
```

最后一条只用于本机临时查看；看完用 `git clean -n` 确认范围后再清理，不要把它们重新提交。

## 最小验证

```powershell
git status --short --branch
git diff --check
node --test scripts/documentation-catalog.test.mjs
node --test scripts/docs-integrity.test.mjs
node --test scripts/release-metadata.test.mjs
node --test scripts/copy-integrity.test.mjs
git ls-files | rg -i 'google-services\\.json|local\\.properties|keystore|\\.apk$|\\.aab$|\\.jks$|\\.log$'
```

代码、数据库、同步、Manifest 或发布配置发生变化时，再按 [`TESTING.md`](TESTING.md) 扩大到受影响的 Gradle、Rules/Functions 和 Android 设备测试；纯文档或历史资料整理不机械重复无关的完整设备套件。
