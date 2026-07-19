# Firebase 配置与运维

## 已建立的生产资源

- Firebase 项目：`daily-record-hand-brew`
- Android 包名：`io.github.litaog.dailyrecord`
- Authentication：仅启用 Email/Password
- Cloud Firestore：Standard、Native、生产模式、`asia-east1`
- Firestore 规则：仓库根目录 `firestore.rules`，已在生产控制台发布

## Android Studio 本地配置

1. 从 Firebase 控制台下载该 Android App 的 `google-services.json`。
2. 放到 `app/google-services.json`。该文件已被 `.gitignore` 排除，不要提交。
3. Sync Project with Gradle Files，然后运行 Debug 构建。
4. 登录页不再显示“云端开发项目尚未完成配置”即表示配置被识别。

没有此文件时，工程仍可编译并进入纯本地模式；会使用 `demo-daily-record-app` 占位配置，生产登录按钮禁用。

## 本地安全规则测试

```powershell
pnpm install --frozen-lockfile
pnpm test:firestore-rules
```

默认 Firebase alias 故意保持 `demo-daily-record-app`，避免测试或误操作写入生产。生产 alias 是 `production`。

## 生产规则发布

先运行规则测试并检查差异，再显式指定生产项目：

```powershell
pnpm exec firebase deploy --only firestore:rules --project daily-record-hand-brew
```

不得把 `default` alias 改成生产项目。当前机器若 Firebase CLI OAuth 回调不可用，可在 Firebase 控制台 Rules 页粘贴同一文件并发布，发布后必须回读规则历史和正文。

## 显式生产烟雾测试

`ProductionFirebaseSmokeTest` 默认跳过，避免普通设备测试误触生产。需要已安装 Debug 与 AndroidTest APK，并显式传参：

```powershell
adb shell am instrument -w `
  -e runProductionFirebaseSmoke true `
  -e class io.github.litaog.dailyrecord.core.cloud.ProductionFirebaseSmokeTest `
  io.github.litaog.dailyrecord.test/androidx.test.runner.AndroidJUnitRunner
```

测试会创建随机邮箱账号，验证本账号空查询、跨账号拒绝和再次登录，随后删除测试账号；不会写入手冲文档。

## 中国大陆网络门槛

当前开发网络中，Android 模拟器直连百度成功，但直连 `identitytoolkit.googleapis.com:443` 与 `firestore.googleapis.com:443` 超时；通过宿主机代理后，上述生产烟雾测试 1/1 通过。这证明生产配置与规则有效，也说明 Firebase 不能被视为“中国大陆无需代理可用”的发布后端。

本地记录功能不受影响，登录也保持可选。若正式发行面向中国大陆普通网络，必须在发布前完成二选一并做两台真机验证：

1. 明确产品只支持可访问 Google/Firebase 的网络环境；或
2. 在现有 `AuthRepository`、`HandBrewRemoteDataSource` 接口后替换为大陆可达服务，并重新完成账户隔离、迁移和删除测试。

没有完成该决策前，不得在商店文案中承诺中国大陆无代理跨设备恢复。

## 发布前仍需完成

- 配置正式 release keystore、SHA 指纹和 Play Console App Signing。
- 接入并观察 App Check，再决定强制执行日期；调试构建使用官方 debug provider/token。
- 建立隐私政策、账号和云数据删除流程、支持邮箱、预算/配额告警。
- 在至少两台真实设备完成注册、离线编辑、重连、跨设备恢复和删除不复活回归。
