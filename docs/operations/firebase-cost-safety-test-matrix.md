# 费用安全测试与保护覆盖矩阵

最后复核日期：2026-10-17
状态：`当前权威 — 计费安全防护与自动化测试矩阵`

本文档记录针对 Daily Record 识别出的各类攻击场景、失控重试、费用放大风险、已实施的分层防护措施、自动化测试覆盖及已知盲区。

---

## 1. 风险与防御覆盖矩阵

| 序号 | 攻击场景 / 费用失控路径 | 风险级别 | 防御生效层级 | 已实施防护机制 | 对应代码文件 | 对应自动化测试文件 | 测试命令 | 预期运行表现 |
|---|---|---|---|---|---|---|---|---|
| **1** | 恶意脚本并发高频刷写 `writeDailyCountRecord` | **高危** | Cloud Functions 后端 | 内存滑动窗口限流：5 秒内最多 5 次突发，1 分钟最多 30 次，1 小时最多 120 次；超出抛出 `RESOURCE_EXHAUSTED`。**计数按实例独立**：单 UID 全局实际上限 = 阈值 × 活跃实例数（写入 ≤10），实例冷启动清零桶；全局共享计数因每次校验额外产生计费读被否决（#289） | `functions/rate-limiter.js`<br>`functions/index.js` | `scripts/firebase-cost-safety.test.mjs` | `node --test scripts/firebase-cost-safety.test.mjs` | 正常请求毫秒级放行；突增第 6 次直接拒绝，产生 0 次 Firestore 事务费用 |
| **2** | 连续快速调用 `deleteAccountData` | **高危** | Cloud Functions 后端 | 账号删除严格限流：每分钟最多 2 次，每小时最多 5 次；单次调用最多删除 2,000 条文档，杜绝无界循环 | `functions/rate-limiter.js`<br>`functions/index.js` | `scripts/firebase-cost-safety.test.mjs` | `node --test scripts/firebase-cost-safety.test.mjs` | 超频删除被秒级阻断，海量文档时安全分批，不耗尽超时与内存 |
| **3** | 多实例并发扩容攻击导致计算账单失控 | **高危** | Cloud Run 基础资源 | 云函数显式配置 `maxInstances: 10`（写入）和 `maxInstances: 5`（删除），超时 30s/60s，单实例并发 80；触发器显式 `memory` 且不配 `concurrency`（事件函数不适用该选项） | `functions/resource-config.js`<br>`functions/index.js` | `scripts/firebase-cost-safety.test.mjs` | `node --test scripts/firebase-cost-safety.test.mjs` | 即使遭遇外部流量洪峰，Cloud Run 容器数最多扩容到 10 个，物理封顶算力消耗；测试断言选项真被传给 `onCall`/`onDocumentWritten` |
| **4** | 未经签名的伪造客户端调用 Callable | **高危** | Firebase App Check | 引入 `verifyAppCheck` 校验门禁，支持监控模式与强制模式（`APP_CHECK_ENFORCEMENT=true`） | `functions/app-check-gate.js`<br>`functions/index.js` | `scripts/firebase-cost-safety.test.mjs` | `node --test scripts/firebase-cost-safety.test.mjs` | 监控模式输出结构化日志供审计；强制模式直接拒绝无证明签名的非法请求 |
| **5** | 绕过 App 按钮直接向 Firestore 集合写入畸形数据 | **中危** | Firestore Rules & Functions Trigger | Firestore Rules 校验 schema、版本严格递增、1000 详情上限；Functions 触发器异步清除真实非法日期；触发器显式配置 `retry: false` | `firestore.rules`<br>`functions/index.js`<br>`functions/resource-config.js` | `scripts/firestore-rules.test.mjs` | `pnpm test:firestore-rules` | 畸形数据被 Rules 拦截；规则无法判定的非法日期由触发器安全清除且不触发 Eventarc 重试循环 |
| **6** | 密码重置邮件连点与窗口重开连续轰炸 | **中危** | Android Client | `PasswordResetCooldown` 实施 60 秒内存冷却锁；重开弹窗仍受冷却约束；按钮禁用并提示等待 | `app/.../PasswordResetCooldown.kt`<br>`app/.../PasswordResetDialog.kt` | `app/.../PasswordResetCooldownTest.kt` | `.\gradlew.bat testDebugUnitTest` | 60 秒内第二次提交被客户端拦截，不发起网络请求，不消耗 Firebase 每日 150 封邮件额度 |
| **7** | WorkManager 后台任务因超时无限重试 | **高危** | Android WorkManager | `TimeoutCancellationException` 严格受 `runAttemptCount < MAX_ATTEMPTS` (5 次) 约束；调度器配置指数退避 (30s) | `app/.../DailyRecordSyncWorker.kt`<br>`app/.../DailyRecordSyncScheduler.kt` | `app/.../DailyRecordSyncWorkerTest.kt` | `.\gradlew.bat testDebugUnitTest` | 超时不再无限重试，5 次尝试后返回 `Result.failure()` 并休眠 |
| **8** | 配额耗尽 (`RESOURCE_EXHAUSTED`) 导致客户端死循环重试 | **高危** | Android Sync Manager & Worker | 从 `RETRYABLE_FIRESTORE_CODES` 中剔除 `RESOURCE_EXHAUSTED`；WorkManager 遇配额耗尽直接返回 `Result.failure()` | `app/.../AccountSyncManager.kt`<br>`app/.../DailyRecordSyncWorker.kt` | `app/.../AccountSyncManagerTest.kt` | `.\gradlew.bat testDebugUnitTest` | 配额耗尽时不再以 1s..30s 持续轮询，显示友好文案，保护本地记录 |
| **9** | 紧急情况下需要立即停止云端数据处理 | **高危** | Cloud Functions 后端 | 服务端熔断开关：通过环境变量或 Firestore `_system/circuitBreaker` 控制，带 30 秒缓存，快速失败返回 503 | `functions/circuit-breaker.js`<br>`functions/index.js` | `scripts/firebase-cost-safety.test.mjs` | `node --test scripts/firebase-cost-safety.test.mjs` | 熔断开启后所有写请求在 0 毫秒/0 数据库写入下被拒绝，本地优先功能不受影响 |
| **10** | 监控日志输出导致用户隐私泄露与二次计费 | **高危** | Cloud Logging | 安全事件统一以标准 JSON 结构化输出；UID 使用 SHA-256 截断哈希；不产生额外 Firestore 文档写入 | `functions/rate-limiter.js`<br>`functions/circuit-breaker.js` | `scripts/firebase-cost-safety.test.mjs` | `node --test scripts/firebase-cost-safety.test.mjs` | 完整拦截记录进入 Cloud Logging 免费配额，无任何邮箱、明文 UID、Token 泄露 |

---

## 2. 自动化测试套件执行说明

开发者或 CI 执行计费安全验证时的统一命令：

```powershell
# 1. 运行 Node.js 费用安全单元测试（限流、熔断、App Check）
node --test scripts/firebase-cost-safety.test.mjs

# 2. 运行文档与文案门禁测试
node --test scripts/docs-integrity.test.mjs scripts/documentation-catalog.test.mjs scripts/copy-integrity.test.mjs

# 3. 运行 Android 客户端冷却、同步重试决策与本地单元测试
.\gradlew.bat testDebugUnitTest

# 4. 运行 Firestore 规则与云函数触发器集成测试（需本地启动模拟器）
pnpm test:firestore-rules
```

---

## 3. 已知盲区与控制台必要操作

以下属于客户端与函数代码无法单方面决定的边界，必须配合云平台配置：

1. **预算硬性封顶**：Google Cloud 预算告警本质为异步通知，非实时硬性断网。极端情况下若需绝对封顶，需在控制台配置 Pub/Sub 触发自动解绑结算账户的 Cloud Function。
2. **Play Integrity 密钥关联**：上架 Google Play 商店前，必须在 Google Play Console 关联生产 SHA-256 指纹，才能正式切换 App Check 为 Enforced 模式。
3. **Artifact Registry 镜像累积**：Cloud Functions 每次部署会产生容器镜像，需在控制台配置清理策略（保留 1~3 天），防止长期累积微额存储账单。
