# 手冲日历（Android）

一个只记录手冲次数的 Android 日历应用。每天可以记录手冲次数，并按周、月、年和全部历史查看总次数、手冲天数与明细表。

当前版本已经完成仅手冲重构、历史年月快速跳转、Room 本地存储、邮箱密码账号与跨设备云恢复。仓库当前文件树、产品文档和界面不包含健身或其他活动模块；早期通用原型只存在于 Git 历史中。

## 当前交付状态

- Android 版本持续迭代，合并门禁由自动化测试、API 34 模拟器交互回归、截图对比、语义树检查和 GitHub CI 共同承担；本人和少量使用者的日常反馈用于发现后续问题，不设置固定人数、完成率或用时门槛。
- 当前分支基线：39 项 JVM 单元测试通过，最新 API 34 同步弹窗定向回归 2/2 通过；此前完整 Android 设备基线共收集 55 项，其中 54 项通过、0 失败、1 项生产冒烟按设计跳过；Firestore 规则测试和 Lint 均通过。
- 当前月历只显示本月日期，年月与统计周期标题仍可点击快速跳转；周/月统计使用直接标注的分布卡，年/全部历史继续保留精确明细表。
- 当前确认弹窗、日期跳转、账号、登录和错误反馈均使用统一的应用内纸感组件，不再露出默认 Material 确认框或文字按钮。
- 顶部账号区域提供可复制、可分享的本机诊断摘要；摘要只含应用/设备/同步/数据库状态，不含邮箱、UID、手冲日期、次数、密码或原始异常。
- 视觉证据见 [2026-07-22 找回密码与同步可靠性审计](docs/product/audit/2026-07-22-password-reset-sync/README.md)、[应用内 UI 一致性审计](docs/product/audit/2026-07-22-native-ui/README.md)与[月历与统计审计](docs/product/audit/2026-07-22-calendar-statistics/README.md)，此前完整状态覆盖见 [2026-07-19 深度审计](docs/product/audit/2026-07-19-deep-ux/README.md)。

## P0 范围

- 月历查看与日期选择。
- 当天手冲次数 `+1`、`-1`，并可显式保存 0 次或清除记录。
- `count > 0` 表示当天手冲过，`count = 0` 表示明确没冲，没有记录表示尚未填写。
- 总手冲次数、总手冲天数、记录日均次数。
- 周、月、年、全部历史统计与精确明细表。
- Room 本地数据库，完全离线可用。
- 可选邮箱密码账号；登录后自动把本机记录合并到个人云端，并可在新手机恢复。
- 登录页支持通过注册邮箱发送密码重置邮件；成功提示不泄露该邮箱是否已注册。
- 同步状态、离线待上传、手动重试、删除墓碑和确定性冲突处理。
- 账号弹窗支持二次确认和密码重验后删除本人云端记录与 Firebase 账号；本机记录默认保留为离线记录，也可明确选择一并删除。

P0 不包含健身、喝水、学习、睡眠、跑步、自定义活动、短信验证码、提醒、社交、目标或付费功能。本项目也不为其他记录类型预留通用活动模型。

## 技术基线

- Kotlin、Jetpack Compose、Material 3
- Coroutines、Flow、单向数据流
- Room v3；唯一业务表为 `hand_brew_records`
- Firebase Authentication（仅邮箱密码）、Cloud Firestore、WorkManager
- `minSdk 26`，技术包名保持 `io.github.litaog.dailyrecord`

## 文档

- [产品契约](docs/PRODUCT.md)
- [界面与交互](docs/UI_UX.md)
- [架构](docs/ARCHITECTURE.md)
- [数据模型](docs/DATA_MODEL.md)
- [统计口径](docs/STATISTICS.md)
- [决策记录](docs/DECISIONS.md)
- [路线图](docs/ROADMAP.md)
- [开发与测试](docs/DEVELOPMENT.md)
- [Firebase 配置](docs/FIREBASE_SETUP.md)
- [同步与隐私](docs/SYNC_AND_PRIVACY.md)
- [交付与验证记录](docs/product/HAND_BREW_REFACTOR_LOG.md)
- [产品交付索引](docs/product/README.md)

## 本地验证

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebugAndroidTest --no-parallel
pnpm test:android-connected
pnpm test:firestore-rules
```

第二条命令会自动启动隔离的 Firebase Auth/Firestore 模拟器，并在测试结束后关闭；同时需要一台已启动的 Android 模拟器或已连接真机。Android 模拟器应能通过标准宿主地址 `10.0.2.2` 访问本机，且不要残留会拦截本地端口的全局 HTTP 代理。

如需纯命令行启动本机已有的 API 34 模拟器，可先查看 AVD 名称，再启动：

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -list-avds
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd Pixel_4_API_34 -no-snapshot-save
```

## 开源与安全

项目使用 [Apache License 2.0](LICENSE)。`app/google-services.json` 不进入 Git；公开提交不得包含真实用户数据库、签名文件、测试账号口令或其他账号信息。
