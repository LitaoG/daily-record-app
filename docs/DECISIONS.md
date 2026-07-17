# 架构决策记录

## ADR-001：Android 原生

- 状态：Accepted
- 决策：Kotlin + Jetpack Compose，`minSdk 26`。
- 说明：当前只交付 Android；技术包名 `io.github.litaog.dailyrecord` 保持稳定。

## ADR-002：仅手冲专用模型

- 状态：Accepted，取代旧“通用活动模型”。
- 决策：产品只有 `HandBrewRecord`，不使用活动表、活动 ID 或计量类型。
- 原因：用户明确不会添加健身、喝水、学习或其他活动；专用模型更清楚、更易验证。
- 后果：旧 `Activity`、`MeasurementType`、活动管理和筛选全部删除。

## ADR-003：本地 Room 单一事实来源

- 状态：Accepted
- 决策：P0 所有记录先写 Room，UI 只从 Repository 暴露的 Flow 读取。
- 后果：核心功能完全离线；当前不实现登录、云同步、WorkManager 或远端数据层。

## ADR-004：零次与未填写分离

- 状态：Accepted
- 决策：数据库行且 `brewCount = 0` 表示明确没冲；没有行表示尚未填写。
- 后果：清除记录删除该日期行，统计只把 `brewCount > 0` 计入手冲天数。

## ADR-005：简化主导航

- 状态：Accepted
- 决策：主导航仅日历和统计；日期记录从日历打开。
- 后果：不保留活动、账户或通用设置入口。

## ADR-006：v1 数据非破坏迁移

- 状态：Accepted
- 决策：v1→v2 提取旧手冲记录，旧通用表改名为 legacy 表。
- 原因：项目虽未发布，仍避免静默丢失已有测试或本地数据。
- 后果：运行时代码不读取 legacy 表；日后移除必须另立迁移并验证。

## ADR-007：公开仓库与 Apache 2.0

- 状态：Accepted
- 决策：继续使用公开仓库与 Apache License 2.0，不提交真实数据库、密钥或签名。

## 已废止方向

通用活动、健身、三种计量方式、活动身份色、活动归档、登录、跨设备同步、端到端加密和通用日历标记均由本次产品收缩废止，只存在于 Git 历史，不再指导实现。
