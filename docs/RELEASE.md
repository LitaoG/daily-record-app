# GitHub Release 发布与签名

最后复核：2026-08-16

本项目只通过 GitHub Releases 向本人和少量使用者提供签名 APK。当前候选版本为 `v1.0.0-beta.3`，对应 `versionCode = 4`；`v1.0.0-beta.1` 与 `v1.0.0-beta.2` 保留为历史发布。发布物不是 Debug APK。

公开 Release 与当前 `main` 是两个需要明确区分的事实：`main` 在 beta.3 之后可以继续接收维护，但只有显式递增 `versionCode`、更新发布说明并通过被打 tag 源码的 API 34 connected 回归、签名和文档门禁后，才会产生新的 Release。README 的当前画面属于 Debug 运行证据，不是发布物证明。

## 版本规则

- `gradle.properties` 中的 `dailyRecord.versionName` 使用语义化版本；预发布版本使用 `-beta.N`。
- `dailyRecord.versionCode` 每次发布必须严格递增，即使只修改文案或重新发布候选版。
- Git tag 必须等于 `v${dailyRecord.versionName}`；发布脚本会在构建前拒绝不匹配的 tag。
- 已经发布的 tag、APK 和校验文件不覆盖；需要修复时发布新版本。

## 稳定签名

Android 只有在新旧 APK 的包名和签名证书一致、且新 APK 的 `versionCode` 更高时才允许覆盖升级。GitHub 侧载版因此必须在整个生命周期内使用同一份 release keystore。

签名配置支持两种私有来源：

1. 本机根目录中被 Git 忽略的 `keystore.properties`：

   ```properties
   storeFile=C:/private/path/daily-record-release.jks
   storePassword=<private>
   keyAlias=daily-record-release
   keyPassword=<private>
   ```

2. GitHub Actions 环境变量：

   - `DAILY_RECORD_KEYSTORE_FILE`
   - `DAILY_RECORD_KEYSTORE_PASSWORD`
   - `DAILY_RECORD_KEY_ALIAS`
   - `DAILY_RECORD_KEY_PASSWORD`

`assembleRelease` 在配置不完整或 keystore 不存在时会直接失败，不能生成一个容易误发的未签名 APK。构建 release 时使用 `--no-configuration-cache`，避免签名值进入可复用配置缓存。

keystore、密码、`keystore.properties`、`google-services.json`、APK 和 AAB 均不得提交。keystore 至少保留两份加密备份；如果自主管理的签名私钥丢失，已安装用户无法再覆盖升级。

## 电脑丢失或重装后的恢复边界

GitHub 保存完整源码、构建脚本、版本历史和 Release 工作流，但以下私密材料按设计不进入 Git：

| 私密材料 | 只从 GitHub 克隆后的影响 | 恢复方式 |
|---|---|---|
| `app/google-services.json` | 工程仍可编译并运行纯本机模式，但生产登录与同步禁用 | 从 Firebase 控制台重新下载同一 Android App 的配置 |
| release keystore | 本机不能生成可覆盖现有安装的正式 APK | 从自己保存的加密备份恢复原文件 |
| keystore 密码与 alias | 即使仍有 keystore，本机也不能签名 | 从独立的密码管理器或加密备份恢复 |
| `keystore.properties` | 只影响本机签名配置，不影响 Debug/本机模式 | 按本文件模板重新创建并指向恢复后的 keystore |

当前 GitHub Actions Secrets 能继续让仓库工作流生成正式 APK，但 GitHub 不允许把 Secret 原值读取出来，因此它不能替代可导出的个人备份。如果本机备份、GitHub 仓库/Secrets 或账号访问同时丢失，原签名私钥无法重新生成，已安装用户将不能覆盖升级。

最低备份要求：

1. release keystore 做两份加密副本，存放在两个不同位置。
2. 密码与 alias 存入密码管理器，不与 keystore 放在同一个未加密目录。
3. 保存 Firebase 项目 ID、Android 包名和签名证书公开 SHA-256；`google-services.json` 本身可从控制台重新下载。
4. 每次恢复后先用 `apksigner --print-certs` 核对下方证书指纹，再构建或发布。

当前稳定 release 证书的公开 SHA-256 指纹为：

```text
AF:A5:24:1B:F1:3C:9D:AA:6F:45:AE:7C:8D:69:9D:75:40:D0:11:F0:E2:19:E5:4E:5B:97:BF:2C:99:6B:3E:61
```

每次发布都必须用 `apksigner --print-certs` 得到同一指纹；不一致时立即停止发布。

## GitHub Actions Secrets

仓库 `Settings → Secrets and variables → Actions` 需要以下五个 Repository secrets：

| Secret | 内容 |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | release keystore 文件的单行 Base64 |
| `RELEASE_KEYSTORE_PASSWORD` | keystore 密码 |
| `RELEASE_KEY_ALIAS` | release key alias |
| `RELEASE_KEY_PASSWORD` | key 密码 |
| `GOOGLE_SERVICES_JSON_BASE64` | 生产 `app/google-services.json` 的单行 Base64 |

工作流不会回显 secret。Tag 推送后，`.github/workflows/release.yml` 会先在被打 tag 的源码上运行 API 34 Android connected instrumentation（使用隔离 Firebase Emulator），再验证版本、运行 JVM 测试和 Lint、构建并验证签名 APK、生成 SHA-256 文件，并用仓库范围的短期 `GITHUB_TOKEN` 创建 GitHub prerelease。Connected 失败时会上传 JUnit 报告、logcat 和 emulator properties。

## 发布步骤

1. 确认 `main` 干净且 CI 通过；发布工作流仍会对实际 tag 源码重复执行 Connected 门禁。
2. 更新 `dailyRecord.versionCode`、`dailyRecord.versionName` 和对应 `docs/releases/v*.md`。
3. 本机运行：

   ```powershell
   pnpm test:release-metadata
   .\gradlew.bat testDebugUnitTest lintDebug assembleRelease --no-configuration-cache
   pnpm test:firestore-rules
   ```

4. 用 `apksigner verify --verbose --print-certs` 检查 APK，并记录公开的证书 SHA-256 指纹。
5. 创建并推送与版本一致的 tag，例如 `v1.0.0-beta.3`。
6. 等待 `Release signed APK` 工作流完成，下载 APK 与 `.sha256` 后再复算一次。
7. 从旧 release APK 覆盖安装到测试设备，确认 Room 记录、账号状态和日历统计保持。

## 首次从 Debug 版迁移

Android Studio 安装的 Debug APK 使用 Android Debug 证书，不能被正式 release 证书直接覆盖。首次切换前应登录并确认云端已同步，再卸载 Debug 版、安装 GitHub Release APK并登录恢复。只保存在 Debug 本机空间、尚未同步的数据不能靠普通覆盖安装迁移。

从 `v1.0.0-beta.1` 开始，后续 GitHub Release 使用同一份稳定 release 证书，可正常覆盖安装并保留 Room 数据。

## App Check 结论

Firebase App Check 的 Play Integrity provider 可以支持 Google Play 以外的分发，但仍需要 Play Console 中的应用与 Cloud/Firebase 项目关联，并正确配置侧载所需的识别判定。当前项目只通过 GitHub Releases 分发，尚未建立 Play Console 应用。

因此当前版本：

- 不加入 App Check 运行时依赖；
- 不在 Authentication 或 Firestore 开启强制执行；
- 不使用 debug provider 保护生产侧载包；
- 等建立 Play Console/可验证的侧载配置后，先发布“仅采集指标、不强制”的候选版，观察真实侧载请求，再决定是否强制。

在这之前直接强制 App Check 可能让全部 GitHub 侧载用户无法登录或同步，风险高于当前少量用户场景的收益。

