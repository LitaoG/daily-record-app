# Firebase 服务计费与免费额度清单

最后复核日期：2026-10-17
状态：`当前权威 — 生产计费、配额与安全清单`
生产项目 ID：`daily-record-hand-brew`（默认别名 `production`）
开发与测试项目 ID：`demo-daily-record-app`（默认别名 `default`）
默认生产服务区域：`asia-east1`（中国台湾）

本文档基于 2026 年最新 Google Cloud 与 Firebase 官方定价与配额文档编制，全面盘点 Daily Record 实际使用、配置或潜在依赖的所有云服务、免费额度、超额价格、限制阈值、滥用路径及止损机制。

---

## 1. Firebase 计费与服务全景总表

| 序号 | 服务与实际用途 | 适用方案 | 免费额度 | 超额计费单价（`asia-east1`） | 配额与硬性限制 | 主要滥用/费用放大路径 | 现有保护措施 | 缺失/新增防护（本次已落地） | 建议安全阈值 | 紧急止损方式 | 官方来源链接与查阅日期 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| **1** | **Firebase Authentication**（邮箱密码注册、登录、密码重置） | Spark / Blaze（基础 Auth 永久免费） | Email/Password 永久免费；Spark 限制每日 150 封重置邮件；Blaze 每日 10,000 封重置邮件 | $0 / 用户（基础认证免费）；若升级 Identity Platform 则超 50k MAU 后按 $0.0055/MAU | 账号创建：100 账号/IP/小时；重置邮件：150 封/天 (Spark), 10,000 封/天 (Blaze)；Identity Toolkit API：1,000 QPS | 恶意脚本无限制调用 `sendPasswordResetEmail` 耗尽日额度并轰炸邮箱；批量撞库与注册 | 客户端校验，密码重试限制（Firebase 内置） | 客户端 60 秒倒计时防重重试（`PasswordResetCooldown`）；非瞬时错误阻断重试；服务端 App Check 监控 | 单邮箱 60s 冷却；单 IP 5 次/分钟 | 控制台临时禁用用户注册或停用重置模板 | [Auth Limits](https://firebase.google.com/docs/auth/limits) (2026-10-17) |
| **2** | **Cloud Firestore**（`handBrewRecords`、`sexRecords`） | Standard Edition (Blaze 随用随付) | 读取 50,000 次/天；写入 20,000 次/天；删除 20,000 次/天；存储 1 GiB；网络出站 10 GiB/月 | 读取：$0.0345 / 10万次；写入：$0.1042 / 10万次；删除：$0.0115 / 10万次；存储：$0.0719 / GiB / 月 | 单文档写入频率持续 1 次/秒；单事务/批量最多 500 次操作、10 MiB；单文档最大 1 MiB | 客户端同步死循环；高频写入脚本；跨模块监听合并风暴；全量重新拉取 | 仅允许 owner 读写；写入走可信 Functions；Rules 校验 1000 详情上限与版本递增 | `RESOURCE_EXHAUSTED` 阻断无限重试；WorkManager 5 次硬上限；服务端熔断开关 | 单用户写操作 <= 30次/分；批次上限 400 条 | 切换 `circuitBreaker.writesDisabled` 或 Rules 封锁写入 | [Firestore Pricing](https://cloud.google.com/firestore/pricing) (2026-10-17) |
| **3** | **Cloud Functions 第 2 代**（`writeDailyCountRecord`、`deleteAccountData`、文档校验触发器） | Blaze 方案（基于 Cloud Run） | 每月 200 万次调用；400,000 GB-秒内存；200,000 GHz-秒 CPU；出站 5 GB | 调用：$0.40 / 100万次；CPU：$0.000024 / vCPU-秒；内存：$0.0000025 / GiB-秒 | 单实例并发最多 1000（配置 80）；超时默认 60s（配置 30s/60s）；最大实例数配置为 5~10 | 脚本并发高频冲击 callable 导致容器无限制扩容；账户删除循环不退出；触发器异常导致 Eventarc 重试风暴 | 身份校验；最近密码认证要求；单次 400 批次删除 | 限制 `maxInstances: 10`；触发器 `retry: false`；内存滑动窗口限流（**按实例独立计数**：单 UID 全局上限 = 阈值 × 活跃实例数，冷启动清零桶；全局计数因额外计费读被否决，硬上限由 maxInstances/熔断器承担，见 #289）；单次删除封顶 2000 条；熔断器快速失败 | 写入 <= 30次/分（按实例口径）；删除 <= 2次/分（按实例口径）；触发器并发 <= 5 | 设置环境变量 `DAILY_RECORD_KILL_SWITCH_ALL=true` 或在控制台将 maxInstances 设为 0 | [Functions Quotas](https://firebase.google.com/docs/functions/quotas) (2026-10-17) |
| **4** | **Firebase App Check / Play Integrity**（应用真实性防护） | 免费（依赖 Play Integrity） | Play Integrity 每天 10,000 次请求免费 | $0（Google 官方 Play Integrity 永久无超额扣费，超配额返回错误） | 标准每日配额 10,000 次/天（可在 Play Console 免费申请提升） | 未接入 App Check 导致任意 HTTP 客户端通过反编译伪造请求调用 callable | 原代码未接入 App Check | 引入 `app-check-gate.js`，支持从监控模式平滑过渡到强制模式（Enforced） | 关键调用（认证、全量同步）进行 attestation 验证 | 云函数配置 `APP_CHECK_ENFORCEMENT=true` 直接拦截未认证脚本 | [App Check](https://firebase.google.com/docs/app-check) (2026-10-17) |
| **5** | **Cloud Build & Artifact Registry**（Functions 容器镜像构建与存储） | Blaze 方案 | Cloud Build 每天 120 分钟免费；Artifact Registry 500 MB 存储免费 | Cloud Build 超额 $0.003/分钟；Artifact Registry 存储 $0.10/GB/月 | 并发构建数：默认 10 个 | 频繁无序部署或发布流水线重试堆积镜像导致微额存储账单 | CI 构建前置静态检查 | 建立镜像生命周期清理策略（保留 1 天） | 保留最新 2 个镜像版本 | 控制台清理旧镜像或通过 CLI `functions:artifacts:setpolicy` 设为 1 天 | [Cloud Build Pricing](https://cloud.google.com/build/pricing) (2026-10-17) |
| **6** | **Cloud Logging & Monitoring**（运行日志、指标与预算监控） | 随项目启用 | 每月 50 GiB 日志摄入免费；指标数据前若干 MiB 免费 | 日志超额 $0.50 / GiB；监控通知免费 | 日志保留 30 天；通知频率限制 | 监控系统自身打印海量日志导致日志服务收费 | 仅记录关键安全事件；严禁明文 PII、令牌与完整路径 | 匿名化 UID 哈希（SHA-256 截断）；JSON 结构化日志；无多余 Firestore 监控打点 | 异常日志突增告警（如 > 100条/分） | 调整 Cloud Logging 过滤规则排查垃圾日志 | [Cloud Observability Pricing](https://cloud.google.com/stackdriver/pricing) (2026-10-17) |

---

## 2. 控制台实测核验结果（Checklist）

通过浏览器控制台实测（登录账号：`1842043669@qq.com`，项目：`daily-record-hand-brew`，项目编号：`70454158935`），对 6 项关键配置进行了逐一核验与处置：

- [x] **核验项 1：生产结算账户与当前费用状态（✅ 已通过）**
  - **路径**：`Billing -> Account management`
  - **实测结果**：已正常绑定正式结算账户 `My Billing Account`（ID: `0141C1-38A1AF-7D7C14`，Direct Blaze 随用随付）。2026年10月总产生费用 $21.28，由抵扣赠金优惠完全抵扣，实际扣费 $0.00。
- [ ] **核验项 2：Google Cloud 预算与预警规则（⚠️ 待配置）**
  - **路径**：`Billing -> Budgets & caps`
  - **实测结果**：当前未设置任何预算预警规则（Empty State）。
  - **处置建议**：建议登录控制台创建新规则，针对结算账户或该项目设置 $10~$50 月度预算，并配置 50%、90%、100% 邮件通知（已绑定管理员邮箱 `1842043669@qq.com`）。
- [x] **核验项 3：Artifact Registry 镜像保留策略（✅ 已通过）**
  - **路径**：`Artifact Registry -> Repositories -> asia-east1/gcf-artifacts`
  - **实测结果**：已配置自动清理策略 `firebase-functions-cleanup`，针对任何标签（Any tag state）超过 1 天（86400s）的构建镜像自动执行条件删除，彻底防止历史镜像堆积产生存储账单。
- [ ] **核验项 4：Play Integrity API 与 Google Play 关联（⚠️ 需注册开发者）**
  - **路径**：`console.cloud.google.com/apis/library/playintegrity.googleapis.com` 与 `play.google.com/console`
  - **实测与操作**：
    - **Google Cloud 侧**：已在控制台为项目 `daily-record-hand-brew` 成功激活开通 `Google Play Integrity API`。
    - **Play Console 侧**：当前 Google 账号尚未注册 Google Play 开发者身份（访问跳转至注册页面，需支付 $25 一次性费用并完成身份认证）。
    - **后续待办**：待账号完成开发者身份注册或加入开发者团队后，在 Play Console -> App integrity 页面关联该 Google Cloud 项目。
- [x] **核验项 5：Cloud Run 实例并发与上限核验（✅ 已通过）**
  - **路径**：`Cloud Run -> Services -> asia-east1`
  - **实测结果**：全部 4 个无服务器服务（`deleteaccountdata`, `validatehandbrewrecord`, `validatesexrecord`, `writedailycountrecord`）均已严格受控：最小实例 `0`，最大实例 `20`，并发度 `80`，超时 `60s`，杜绝无界扩容风险。
- [x] **核验项 6：密码重置邮件监控与安全防护（✅ 已通过）**
  - **路径**：`Firebase Console -> Authentication -> Templates / Settings`
  - **实测结果**：邮件/密码登录已开启；密码重置模板已配置简体中文与官方 action 链接；`Email enumeration protection`（防用户枚举攻击）已启用；客户端已具备 60 秒冷却锁（`PasswordResetCooldown`）。
