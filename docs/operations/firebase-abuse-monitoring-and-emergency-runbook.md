# Firebase 费用监测、反滥用与紧急响应手册

最后复核日期：2026-10-17
状态：`当前权威 — 生产运维、监测与紧急止损响应手册`
适用服务：Firebase Authentication, Cloud Firestore, Cloud Functions 第 2 代 (Cloud Run), Cloud Logging & Monitoring

---

## 1. 监测架构与设计原则

本项目的监控系统遵循**最小必要成本原则**，绝不为了监控而在 Firestore 中为每次调用额外写入监控文档，避免出现“监控自身放大账单”的灾难。

```mermaid
flowchart TD
    Client[Android App / 恶意脚本] -->|调用 Callable / Auth / 写入| CF[Cloud Functions v2 / Firestore]
    CF -->|1. 内存限流 / 熔断检查| Check{是否放行?}
    Check -->|被阻断 (429/503)| LogEvent[输出结构化 JSON 安全日志]
    Check -->|放行处理| Firestore[(Cloud Firestore 业务数据)]
    LogEvent --> CloudLogging[Google Cloud Logging (每月前 50 GiB 免费)]
    CloudLogging --> Metric[基于日志的指标 Log-based Metrics]
    Metric --> AlertPolicy[Cloud Monitoring 告警策略]
    AlertPolicy -->|邮件 / Webhook| Admin[运维人员 / 管理员]
```

### 1.1 结构化安全日志契约
所有云函数在拦截异常流量、限流或熔断触发时，统一以单行 JSON 打印至 `console.warn` 或 `console.info`。Google Cloud Logging 会自动解析顶级 JSON 字段，并根据 `severity` 对其分级：

```json
{
  "severity": "WARNING",
  "event": "RATE_LIMIT_EXCEEDED",
  "operation": "writeDailyCountRecord",
  "uidHash": "48e2b68e5e2f8300",
  "requestCount": 6,
  "maxAllowed": 5,
  "windowMs": 5000
}
```

### 1.2 隐私安全保障
- **严禁记录 PII**：日志中绝对不输出用户明文邮箱、明文 UID、密码、认证 Token 或具体记录内容。
- **不可逆 UID 摘要**：所有账号标识均经过 `SHA-256` 单向摘要并截取前 16 位十六进制字符，仅供排查同一来源的攻击频次。

---

## 2. 告警级别与指标阈值定义

通过 Google Cloud Console 中的 **Cloud Monitoring -> Alerting** 配置以下三级告警策略：

| 级别 | 监控指标 / 过滤条件 | 触发条件 | 潜在风险 | 建议处置行动 |
|---|---|---|---|---|
| **P2 警告 (Warning)** | `jsonPayload.event = "RATE_LIMIT_EXCEEDED"` | 5 分钟内出现 > 20 次 | 存在脚本尝试高频刷写或个别客户端重试异常 | 查看来源 UID 摘要与操作类型，观察流量趋势 |
| **P2 警告 (Warning)** | `jsonPayload.event = "APP_CHECK_UNVERIFIED"` | 5 分钟内占比 > 50% | 大量旧版客户端在线或存在第三方脚本脱机调用 | 评估是否准备开启 App Check 强制拦截 |
| **P1 严重 (Severe)** | `jsonPayload.event = "CIRCUIT_BREAKER_BLOCKED_WRITE"` | 1 分钟内 > 50 次 | 熔断器生效期间仍有大量请求涌入，确认攻击持续 | 检查恶意 IP / UID，准备通过防火墙或控制台切断流量 |
| **P1 严重 (Severe)** | Cloud Run 实例数 `run.googleapis.com/container/instance_count` | 连续 10 分钟达到最大限制 (10 个) | 遭遇持续并发攻击，可能产生计算费用 | 检查 Cloud Functions 是否有挂起或慢请求 |
| **P0 紧急 (Critical)** | Cloud Billing 预算提醒 `100% of budget` | 预算到达预设上限 (如 $10.00) | 真实资金消耗已超预期，持续放任将产生实质损失 | 立即执行第 3 节的【紧急止损操作】 |

---

## 3. 紧急止损操作指南（Runbook）

一旦监测发现恶意攻击、异常死循环或费用快速攀升，按严重程度依次执行以下止损手段：

### 方案 A：通过 Firestore 控制台激活服务端熔断开关（推荐，最快生效）

Daily Record 云函数内置了带 30 秒内存缓存的熔断器。当开关开启时，云函数将在执行任何数据库读取/事务前**毫秒级快速失败**（返回 `UNAVAILABLE`，零数据库写入费用）：

1. 打开 [Firebase Console -> Firestore Database](https://console.firebase.google.com/)。
2. 进入集合 `_system`，找到或创建文档 `circuitBreaker`。
3. 添加或更新布尔字段：
   - 暂停记录同步写入：`writesDisabled: true`
   - 暂停账号数据删除：`deletionsDisabled: true`
   - 暂停文档验证触发器：`triggersDisabled: true`
4. **生效时间**：由于云函数内存缓存机制，所有活跃实例在最多 30 秒内完全阻断新请求。
5. **客户端体验**：Android 客户端提示“云服务暂时无法处理请求，本机记录不会丢失”，Local-first 数据完好保存在本地 Room 中。

### 方案 B：通过 Cloud Run 将实例数限制为 0（彻底关闭云函数计算）

若需要完全暂停云函数执行，避免产生任何 CPU/内存计算费用：

1. 打开 [Google Cloud Console -> Cloud Run](https://console.cloud.google.com/run)。
2. 选择区域 `asia-east1`。
3. 点击服务名称（例如 `writedailycountrecord` 或 `deleteaccountdata`）。
4. 点击顶部 **编辑和部署新版本 (Edit & Deploy New Revision)**。
5. 在 **配置与扩缩 (Scaling)** 中，将 **最大实例数 (Maximum number of instances)** 设为 `0`。
6. 点击部署。服务将立即拒绝所有入口流量，实例数降为 0。

### 方案 C：更新 Firestore 安全规则切断客户端直接写入

若怀疑攻击者绕过 Callable 函数、直接使用合法 Token 通过 SDK 向 Firestore 集合写数据：

1. 打开 [Firebase Console -> Firestore -> Rules](https://console.firebase.google.com/)。
2. 将规则中的写权限临时修改为 `false`：
   ```javascript
   match /users/{userId}/handBrewRecords/{date} {
     allow read: if signedInAs(userId);
     allow write: if false; // 紧急熔断：阻断所有客户端写入
   }
   match /users/{userId}/sexRecords/{date} {
     allow read: if signedInAs(userId);
     allow write: if false; // 紧急熔断：阻断所有客户端写入
   }
   ```
3. 点击 **发布 (Publish)**。规则将在 10 秒内全局生效。

### 方案 D：终极停机手段（解除结算账户绑定）

在遭遇极端大规模 DDoS 且预算失控的危急情况下：

1. 打开 [Google Cloud Console -> Billing -> My Projects](https://console.cloud.google.com/billing/projects)。
2. 找到项目 `daily-record-hand-brew`。
3. 点击右侧操作菜单选择 **更改结算账号 (Change billing)**，或选择 **停用结算功能 (Disable billing)**。
4. **警告**：停用结算会使所有付费资源（Cloud Functions 2nd Gen、超额 Firestore）立即停止服务，仅保留 Spark 免费层级服务。恢复时需重新关联结算账户。

---

## 4. 事故后恢复与复盘流程

1. **解除熔断前验证**：
   - 查看 Cloud Logging 中攻击流量的 IP 与 UID 频率，确认攻击已停止。
   - 检查是否有积压的客户端请求，先将最大实例数恢复为保守值（如 5）。
2. **恢复服务**：
   - 将 Firestore `_system/circuitBreaker` 中的 `writesDisabled` 改为 `false`。
   - 或移除环境变量 `DAILY_RECORD_KILL_SWITCH_ALL`。
3. **事故复盘（Post-Mortem）**：
   - 记录攻击开始时间、峰值 QPS、消耗的 Firestore 读写次数和实际费用。
   - 分析攻击路径：是通过直接调用 HTTP Callable、暴力重置密码、还是客户端同步进入循环。
   - 补充针对该攻击手法的自动化测试用例，并将其归档至权威文档中。
