# 手冲日历（Android）

一个只记录手冲次数的 Android 日历应用。每天可以记录手冲次数，并按周、月、年和全部历史查看总次数、手冲天数与明细表。

> 当前分支正在把早期“通用活动记录器”原型重构为单一手冲产品。旧方案只保留在 Git 历史中，不再作为产品或代码事实来源。

## P0 范围

- 月历查看与日期选择。
- 当天手冲次数 `+1`、`-1`，并可显式保存 0 次或清除记录。
- `count > 0` 表示当天手冲过，`count = 0` 表示明确没冲，没有记录表示尚未填写。
- 总手冲次数、总手冲天数、记录日均次数。
- 周、月、年、全部历史统计与精确明细表。
- Room 本地数据库，完全离线可用。
- 可选邮箱密码账号；登录后自动把本机记录合并到个人云端，并可在新手机恢复。
- 同步状态、离线待上传、手动重试、删除墓碑和确定性冲突处理。

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
- [本次重构日志](docs/product/HAND_BREW_REFACTOR_LOG.md)

## 本地验证

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebugAndroidTest --no-parallel
pnpm test:android-connected
pnpm test:firestore-rules
```

第二条命令会自动启动隔离的 Firebase Auth/Firestore 模拟器，并在测试结束后关闭；同时需要一台已启动的 Android 模拟器或已连接真机。Android 模拟器应能通过标准宿主地址 `10.0.2.2` 访问本机，且不要残留会拦截本地端口的全局 HTTP 代理。

## 开源与安全

项目使用 [Apache License 2.0](LICENSE)。`app/google-services.json` 不进入 Git；公开提交不得包含真实用户数据库、签名文件、测试账号口令或其他账号信息。
