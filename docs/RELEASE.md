# GitHub Release 发布与签名

本项目只通过 GitHub Releases 向本人和少量使用者提供签名 APK。首个候选版本为 `v1.0.0-beta.1`，对应 `versionCode = 2`。发布物不是 Debug APK。

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

工作流不会回显 secret。Tag 推送后，`.github/workflows/release.yml` 会验证版本、运行 JVM 测试和 Lint、构建并验证签名 APK、生成 SHA-256 文件，并用仓库范围的短期 `GITHUB_TOKEN` 创建 GitHub prerelease。

## 发布步骤

1. 确认 `main` 干净且 CI 通过。
2. 更新 `dailyRecord.versionCode`、`dailyRecord.versionName` 和对应 `docs/releases/v*.md`。
3. 本机运行：

   ```powershell
   pnpm test:release-metadata
   .\gradlew.bat testDebugUnitTest lintDebug assembleRelease --no-configuration-cache
   pnpm test:firestore-rules
   ```

4. 用 `apksigner verify --verbose --print-certs` 检查 APK，并记录公开的证书 SHA-256 指纹。
5. 创建并推送与版本一致的 tag，例如 `v1.0.0-beta.1`。
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
