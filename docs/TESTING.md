# 测试策略与执行矩阵

最后复核：2026-08-08

本文件是 Daily Record 测试范围、执行时机和完成门槛的唯一事实来源。目标是在不降低质量的前提下，避免每个小改动都重复运行与其无关的完整套件。

## 核心原则

1. **先按影响范围测试，再在小版本末尾全量验证。**
2. 每次代码修改必须先运行能够直接证明本次行为的最小测试集；修复缺陷时应增加或更新回归测试。
3. 完整测试套件只在一个连贯小版本的功能代码全部完成后、准备合并或交付前运行一次；Stage 5 的最终功能 head 已按此规则运行一次。
4. 测试证据只会被可能影响该结果的后续修改推翻：
   - 只改文档或审计截图，不会让已经通过的 Room、同步或设备测试失效。
   - 只改 Compose 组件，不会让 Firestore Rules 测试失效。
   - 修改共享数据模型、Gradle、Manifest、依赖、数据库或同步边界时，应扩大验证范围。
5. 同一个最终 head 不重复运行已经通过且未失效的完整套件。GitHub CI 是独立验证，不要求本机机械重复相同命令；本轮只对改动的两个 Android 测试类先做定向验证，再运行一次完整设备套件。
6. 设备测试只能运行在可重置模拟器；执行前必须用 `adb devices -l` 排除日常使用的实体手机。
7. 生产 Firebase 烟雾测试默认跳过，只有生产配置、规则部署或账号隔离需要显式验收时才单独执行。

## 三个执行层级

### 1. 修改中：定向验证

每完成一个小修改，只执行覆盖当前影响面的编译、测试和运行检查。失败时先修复，再继续扩大改动。

常用 Kotlin/Compose 编译：

```powershell
.\gradlew.bat compileDebugKotlin compileDebugUnitTestKotlin compileDebugAndroidTestKotlin --no-parallel
```

定向 JVM 测试：

```powershell
.\gradlew.bat testDebugUnitTest --tests "完整测试类名" --no-parallel
```

定向 Android 测试：

```powershell
pnpm exec firebase emulators:exec --project demo-daily-record-app --only auth,firestore `
  ".\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=完整测试类名 --no-parallel"
```

纯 UI 测试不依赖 Firebase 时，可以直接运行同一条 Gradle 定向命令；只有测试宿主会初始化账号或同步边界时才启动 Auth/Firestore 模拟器。

### 2. 提交或推送前：增量门禁

提交前至少完成：

- 本次变更涉及的编译与定向测试。
- `git diff --check`。
- 检查完整 diff、未跟踪文件、调试输出和敏感材料。
- 文档或截图变化运行 `pnpm test:docs`。
- 版本或发布元数据变化运行 `pnpm test:release-metadata`。

推送后查看 GitHub CI、机器人评论和真实审查问题。CI 通过不取代本次行为的定向运行证据，但也不需要在本机再次机械执行相同检查。

### 3. 小版本结束：一次完整验证

当一个小版本的功能代码已经冻结、准备合并到 `main` 或生成 APK 时，在最终功能 head 上执行一次：

```powershell
pnpm test:docs
pnpm test:release-metadata
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --no-parallel
pnpm test:firestore-rules
pnpm test:android-connected
```

同时完成：

- `git diff --check`、Markdown 本地链接检查和敏感信息扫描。
- 检查 Release 合并 Manifest；生产构建必须保持 `usesCleartextTraffic=false`。
- 对本小版本实际改变的主流程做一次截图或手工运行回归，并检查 Crash Buffer。
- 若涉及 Release，再按 [`RELEASE.md`](RELEASE.md) 验证签名、版本、覆盖升级与 SHA-256。

Stage 5 的测试日志只写入仓库 `build/tmp/stage5/` 临时目录，解析完成后删除；不在桌面或公共文档中留下机器日志。审计文档只保留可复核的命令、计数、结论和候选 APK 校验值。beta.2 的最终结果见[Stage 5/6 最终审查与发布审计](product/audit/2026-08-03-stage5-stage6-release/README.md)。

如果完整验证之后只修改说明文字或审计截图，只重跑文档检查和 CI；不重复 Room、Firestore 或设备全套。若之后又修改了运行时代码，则重跑该代码影响的定向测试；只有改动跨越多个核心边界或无法可靠界定影响时，才重新执行完整套件。

## 变更类型与必测范围

| 变更类型 | 每次修改必测 | 不需要机械重复 | 小版本末尾 |
|---|---|---|---|
| 纯 Markdown、链接、审计截图 | `pnpm test:docs`，人工检查链接/图片 | JVM、Lint、设备、Firestore | 沿用未失效的完整证据 |
| 单个 Compose 组件、间距、字号、图标 | Debug/AndroidTest 编译；相关 Compose 测试类；目标页面截图；Crash Buffer | Room 迁移、同步、Firestore Rules | 统一完整设备回归一次 |
| 页面导航、返回键、状态恢复 | 相关状态单测；相关设备测试类；冷启动/返回手工回归 | 未改动的 Firestore Rules | 完整设备回归一次 |
| 统计纯函数、日期范围 | 对应 JVM 测试类；固定数据集和边界用例 | 账号、Firebase、无关 UI 页面 | 全部 JVM 测试一次 |
| Repository 或共享领域模型 | 编译；对应 Repository/模型单测；受影响设备流程 | 未改动的发布签名和文档截图 | JVM、Lint、设备套件 |
| Room Entity、DAO、schema、migration | 迁移测试；DAO/Repository 测试；安装或升级设备测试 | 未改动的 Firestore Rules，除非云模型也变 | 完整套件并核对 schema |
| 登录、密码重置、账号删除 | 对应账号单测与 Auth 模拟器设备测试；超时和错误分级 | 无关统计截图 | 完整账号/设备回归 |
| 同步、Worker、冲突合并、远端映射 | 对应同步单测；隔离 Auth/Firestore 设备测试；离线/恢复 | 无关纯 UI 截图 | JVM、规则、设备套件 |
| `firestore.rules`、云端字段或路径 | `pnpm test:firestore-rules`；对应远端解析/设备测试 | 无关日历纯 UI 测试 | 完整规则与设备回归 |
| Manifest、网络安全、权限 | Manifest 处理；Lint；相关构建；必要的运行检查 | 无关业务测试 | 检查最终合并 Manifest |
| Gradle、依赖、SDK、混淆 | Debug/AndroidTest/Release 相关编译；Lint；受影响测试 | 无法证明不受影响的项目 | 通常执行完整套件 |
| 版本、签名、发布工作流 | 发布元数据测试；Release 构建；签名与升级验证 | 开发中每个小提交的设备全套 | 按 `RELEASE.md` 完整发布门禁 |
| 跨模块重构或影响范围不明确 | 从共享边界向外运行相关测试，必要时直接完整验证 | 无 | 完整套件 |

### 云端文档物理消失回归（Issue #104）

同步边界改动必须覆盖两种不同的本地基线：

- 已确认记录存在 pending 编辑且 `remoteRevision > 0` 时，云端同日文档被物理删除，手冲与做爱都必须以本机内容重建，revision 从 1 开始且 pending 清零。
- 另一台设备没有 pending 编辑、但仍缓存旧代际时，必须通过重建文档生成的新 `id` 接受新代际，即使新 revision 较低；同一 `id` 代际仍按 revision 做乐观并发校验。

最小证据为手冲与做爱 Android 同步测试（含 `AccountSyncManager` 恢复、显式清除墓碑和双设备代际收敛），以及 Auth/Firestore Emulator 的真实写入、物理删除、重建和读取测试。`DailyRecordSyncWorkerTest` 同时用测试 WorkerFactory 走真实 Worker 分支，验证物理删除后的 pending 编辑最终返回成功并清零。Worker 的删除闸门测试仍按上表独立执行；测试 provider 只替换 Firebase 初始化，绝不接触生产项目。

## 缺陷修复要求

- 先用最小可重复步骤证明问题，再修复。
- 能自动化的缺陷必须增加回归测试；视觉问题至少增加稳定边界/语义测试，并保留修复后截图。
- 先运行新增或直接相关测试；通过后再运行被修改共享边界的测试。
- 不因为“顺便测试”而启动生产 Firebase、真实邮箱、签名 Release 或日常真机。
- 测试数量不是质量目标；证据必须直接覆盖本次风险。

## 结果记录

PR 或审计记录应写明：

- 修改影响了哪些边界。
- 实际运行了哪些定向测试及结果。
- 哪些完整测试结果仍然有效，为什么未被本次修改推翻。
- 小版本最终完整套件的 head commit、测试数量、失败/跳过数量和 CI 链接。
- 明确未执行的生产或实体设备测试，以及不执行的原因。
