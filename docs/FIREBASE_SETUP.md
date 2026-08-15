# Firebase 配置与运维

最后复核：2026-08-15

## 已建立的生产资源

- Firebase 项目：`daily-record-hand-brew`
- Android 包名：`io.github.litaog.dailyrecord`
- Authentication：仅启用 Email/Password
- Cloud Firestore：Standard、Native、生产模式、`asia-east1`
- Firestore 规则：仓库根目录 `firestore.rules` 是事实来源，同时覆盖 `handBrewRecords` 与 `sexRecords`；客户端物理删除被拒绝
- Cloud Functions：仓库 `functions/` 是 callable 写入、账号数据删除和旧协议坏文档清理的事实来源；生产部署前必须先完成 Functions 与 Rules 的同版本发布

## 密码重置邮件

- Firebase Authentication 的 Email/Password 提供方必须保持启用；应用调用官方 `sendPasswordResetEmail`，不自建口令或邮件服务器。
- 控制台 Authentication → Templates → Password reset 可配置发件人名称、主题和正文。模板不得声称邮箱一定存在，也不要加入账号口令或敏感数据。
- 应用使用设备语言请求模板；默认 Firebase 托管重置页完成新密码设置，用户随后返回应用登录。
- 发送失败按断网、请求过频、项目额度和通用错误处理。真实送达、垃圾邮件分类和生产限额只在明确的真机测试账号上验证，不纳入普通 CI。

## Android Studio 本地配置

1. 从 Firebase 控制台下载该 Android App 的 `google-services.json`。
2. 放到 `app/google-services.json`。该文件已被 `.gitignore` 排除，不要提交。
3. Sync Project with Gradle Files，然后运行 Debug 构建。
4. 登录页不再显示“云端开发项目尚未完成配置”即表示配置被识别。

没有此文件时，工程仍可编译并进入纯本地模式；会使用 `demo-daily-record-app` 占位配置，生产登录按钮禁用。

## 本地安全规则测试

```powershell
pnpm install --frozen-lockfile
pnpm --dir functions --ignore-workspace install --frozen-lockfile
pnpm test:firestore-rules
```

默认 Firebase alias 故意保持 `demo-daily-record-app`，避免测试或误操作写入生产。生产 alias 是 `production`。

## 生产 Rules 与 Functions 发布

先运行规则测试并检查差异，再显式指定生产项目：

```powershell
pnpm exec firebase deploy --only functions,firestore:rules --project daily-record-hand-brew
```

不得把 `default` alias 改成生产项目。Functions 发布需要已配置生产项目权限和运行时；如果只在控制台发布 Rules 而没有同步发布 Functions，新的 Android 云写入和账号删除流程不能视为完成。当前机器若 Firebase CLI OAuth 回调不可用，可在 Firebase 控制台 Rules 页粘贴同一文件并发布，但 Functions 仍需通过受控 CI/CLI 发布，发布后必须回读 Rules 历史、Functions 版本和正文。

## 显式生产烟雾测试

`ProductionFirebaseSmokeTest` 默认跳过，避免普通设备测试误触生产。需要已安装 Debug 与 AndroidTest APK，并显式传参：

```powershell
adb shell am instrument -w `
  -e runProductionFirebaseSmoke true `
  -e class io.github.litaog.dailyrecord.core.cloud.ProductionFirebaseSmokeTest `
  io.github.litaog.dailyrecord.test/androidx.test.runner.AndroidJUnitRunner
```

测试会创建随机邮箱账号，验证两个集合的本账号空查询、跨账号拒绝和再次登录，随后删除测试账号；不会写入生产记录文档。普通模拟器套件会在隔离环境中分别写入、恢复和删除自慰与做爱文档，并读取一次性重置码设置新密码，不会发送真实邮件。

2026-07-28 发布双集合规则后，另以一次性随机账号完成了 `handBrewRecords` 与 `sexRecords` 的生产写入/读取闭环；两条虚构记录和临时账号均在同一测试中删除。

## 中国大陆网络门槛

- 当前 Firebase 账号注册、登录、密码重置和云同步在中国大陆普通网络下可能不可达；应用明确提示用户打开 VPN（梯子）。
- 选择“暂不登录，先使用本机记录”不需要 VPN（梯子），日历、记录与统计继续由 Room 离线提供，但不会上传或恢复云端数据。

当前开发网络中，Android 模拟器直连百度成功，但直连 `identitytoolkit.googleapis.com:443` 与 `firestore.googleapis.com:443` 超时；通过宿主机代理后，上述生产烟雾测试 1/1 通过。这证明生产配置与规则有效，也说明 Firebase 不能被视为“中国大陆无需代理可用”的发布后端。

本地记录功能不受影响，登录也保持可选。若未来产品目标改为“中国大陆普通网络无需代理也能使用云功能”，必须另立迁移项目，在对外承诺前完成二选一并做两台真机验证：

1. 明确产品只支持可访问 Google/Firebase 的网络环境；或
2. 在现有 `AuthRepository`、`HandBrewRemoteDataSource`、`SexRemoteDataSource` 接口后替换为大陆可达服务，并重新完成账户隔离、迁移和双集合删除测试。

没有完成该决策前，不得在商店文案中承诺中国大陆无代理跨设备恢复。

## 当前运维边界与后续事项

- 稳定 release keystore、证书指纹、GitHub tag 工作流、隐私说明和账号/云数据删除已经建立并用于 `v1.0.0-beta.1`；私钥和生产配置只存在于受控恢复镜像、本机安全文件或 GitHub Actions Secrets，不进入公开仓库。
- App Check 评估结论：Play Integrity 可配置为支持 Play 外分发，但仍需要 Play Console 应用/Cloud 项目关联并关闭不适合侧载的默认识别要求。当前 GitHub-only 分发暂不接入、不强制；不能用 debug provider 保护生产 APK。
- 本人和少量使用者继续通过日常真实使用反馈问题；不再设置固定人数的正式真人测试门槛。
- 中国大陆无代理云恢复不属于当前承诺；本机模式不受影响。若未来要求无代理云功能，必须另立后端迁移项目。
