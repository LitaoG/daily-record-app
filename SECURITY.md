# 安全策略

最后复核：2026-07-28

## 报告安全问题

请不要在公开 Issue 中披露可利用漏洞、访问令牌、真实用户记录或数据库文件。仓库维护者启用私密漏洞报告后，应优先使用 GitHub Private Vulnerability Reporting；在此之前，请只提交不包含敏感细节的联系 Issue。

## 敏感信息边界

公开仓库永远不能包含：

- 数据库管理员密钥、服务账号私钥或 GitHub Token。
- Android 签名文件及其密码。
- 真实 `.env`、`local.properties`、`google-services.json` 或服务账号配置。
- 真实用户记录、导出文件、数据库快照或含隐私的崩溃日志。

Android Firebase 客户端配置本身不是管理员凭据，但仍不进入此仓库；真正的安全边界是 Firebase Authentication、Firestore Security Rules，以及未来满足侧载识别条件后可能重新评估的 App Check。具备绕过规则能力的服务账号私钥只能存在于受控服务端。即使密钥已从最新提交删除，也必须按泄露处理并立即轮换，因为它可能仍存在于 Git 历史中。

## 应用安全基线

- 只启用 Firebase 邮箱密码登录和 Cloud Firestore；不启用短信登录、广告或业务分析 SDK。
- 手冲与做爱记录只通过各自 Repository 访问 Room；UI 不直接读取 Firestore。
- Firestore 路径按 Firebase UID 隔离，规则校验所有权、字段白名单、修订递增和禁止物理删除。
- 系统云备份与设备迁移关闭，避免个人记录被隐式复制。
- Release Manifest 禁止明文网络；仅 Debug 构建为本机 Firebase 模拟器允许 HTTP，Debug APK 不对外分发。
- Room 当前依赖 Android 应用沙箱、系统文件级加密和设备锁，不宣称应用层端到端加密。引入数据库加密前必须先验证密钥恢复和无损迁移。
- 日志不得输出任一私密记录的日期、次数、文档内容或完整数据库行。
- Room schema 变化必须使用显式迁移；禁止 destructive migration。
- 隐私政策、账号/云数据删除、稳定签名和生产规则回归必须持续保持有效。当前 GitHub 侧载阶段按 ADR-013 暂不接入或强制 App Check；未来只有在 Play Console/侧载识别条件可验证并先观察真实指标后，才重新决定是否强制。
