# 2026-08-03 Stage 5/6：最终审查与 beta.2 发布

状态：`completed`

本记录是 v1.0.0-beta.2 的最终交付证据。Stage 5/6 没有新增业务逻辑或 UI 改版；只修正了两个与集中式文案同步的 Android 回归断言，然后完成最终验证、公共合并、Release 和私有恢复镜像同步。

## 公共 Git 状态

- 仓库：[`LitaoG/daily-record-app`](https://github.com/LitaoG/daily-record-app)
- PR：[#67](https://github.com/LitaoG/daily-record-app/pull/67)，已 squash 合并到 `main`，合并提交为 `f95cca2f5f1a5f731ca50704021be75d2ef4ffa0`。
- beta.2 发布元数据 PR：[#68](https://github.com/LitaoG/daily-record-app/pull/68)，合并提交为 `12239b97851731d1e16cee817e5f917c4a7534cd`；tag `v1.0.0-beta.2` 指向该提交，两个 PR 分支均已删除。
- 发布后审计补充 PR：[#69](https://github.com/LitaoG/daily-record-app/pull/69)，合并提交为 `357556a7bb51105537caba178d5cf18035aa1087`。
- 审计链修正 PR：[#70](https://github.com/LitaoG/daily-record-app/pull/70)，合并提交为 `d211a171d5bdf6e0cce84ccd5f160574f1e79199`；当前公共 `main` 为该提交。
- 终稿审计 PR：[#71](https://github.com/LitaoG/daily-record-app/pull/71)，合并提交为 `92f3b859745a90c846bad4afb01ed86499b2d3d5`。
- 审计文档编码修正 PR：[#72](https://github.com/LitaoG/daily-record-app/pull/72)，合并提交为 `d5ed78b130c58437f7fb9b227ed8c2d14c49eba2`；当前公共 `main` 为该提交。
- 公共仓库未跟踪 `keystore.properties`、`app/google-services.json`、签名文件、真实用户数据或 APK。

## 定向问题收口

1. `DiagnosticDialogTest` 仍断言历史隐私文案；改为断言 `AppCopy.Diagnostics.subtitle` 的当前集中式文案。
2. `HandBrewSyncCoordinatorTest` 的数据异常提示断言使用了旧措辞；同步生产边界未改，测试改为匹配当前 `AppCopy.Account.dataFormatFailure`。

两项定向 Android 测试均通过。随后重启一次因系统服务崩溃的 API 34 模拟器，再运行最终完整设备套件。

## 最终测试矩阵

| 检查 | 结果 |
|---|---|
| `pnpm test:docs` | 通过，2/2 |
| `pnpm test:release-metadata` | 通过，4/4 |
| `testDebugUnitTest` | 通过 |
| `lintDebug` | 通过 |
| `assembleDebug` / `assembleDebugAndroidTest` | 通过 |
| Firestore Rules 模拟器 | 通过；拒绝用例日志属于预期结果 |
| `connectedDebugAndroidTest` | 93 项执行、1 项设计跳过、0 项失败 |
| GitHub CI | PR #67 `build` 通过 |
| 生产 Firebase 烟雾测试 | 按策略跳过，未改变本机优先结论 |

最终设备套件只在修复两个回归断言并重启模拟器后运行一次；纯文档和发布元数据变更不重复运行设备套件。

## 发布与恢复

- 版本：`v1.0.0-beta.2`，`versionCode=3`。
- GitHub Release：[v1.0.0-beta.2](https://github.com/LitaoG/daily-record-app/releases/tag/v1.0.0-beta.2)，公开 Release URL 已核对，`draft=false`、`prerelease=true`。
- Release 使用与 beta.1 相同的稳定 release keystore；发布工作流同时上传 APK 和 `.sha256`。
- 本机签名候选 APK：`app/build/outputs/apk/release/app-release.apk`；本机 SHA-256 为 `e015673e3861cdcbfa0f801a037c2ff872d2800d2b3ddf793b09ebd3b7dcc244`。
- 本机 `apksigner`：APK Signature Scheme v2、单一 RSA 4096 signer；证书 SHA-256 指纹与既有稳定指纹一致：`AF:A5:24:1B:F1:3C:9D:AA:6F:45:AE:7C:8D:69:9D:75:40:D0:11:F0:E2:19:E5:4E:5B:97:BF:2C:99:6B:3E:61`。
- GitHub Release APK 已重新下载并独立核对：远端 SHA-256 为 `effc4b3450947aa5b1cefa7f79f5c9dc597a3226f0b87f12063657914e862c2a`，与同页 `.sha256` 一致；远端 APK 的 v2 签名和稳定证书指纹一致。
- 公共 `main` 完整验证后才同步私有恢复仓库；私有签名、Firebase 生产配置和恢复材料只存在私有仓库/本机/Actions Secrets。
- 后续真人日常反馈不作为发布硬门槛；复现问题单独建 Issue，按影响范围定向测试。
