# 数据模型

本文档描述逻辑模型。实际 Room Entity 和远端表名可在实现阶段调整，但语义必须保持一致。

## 当前实现基线

- Room schema 版本：`1`。
- Schema 文件：`app/schemas/io.github.litaog.dailyrecord.core.database.DailyRecordDatabase/1.json`，必须纳入版本控制。
- 通用领域模型位于 `core/model`；Room 与 DAO 位于 `core/database`；UI 只通过 `core/data` 中的 Repository 接口访问数据。
- 当前只建立 `activities` 和 `daily_records` 两张 P0 核心表；没有手冲、健身专属列，也没有提前加入云同步 Outbox。
- 首版没有旧 schema，因此迁移列表为空；从版本 2 开始，每次 schema 变化必须增加显式迁移或可审查的 AutoMigration，并保留从最早版本升级的设备测试。
- Room 使用稳定版 `2.8.4` 与 KSP2；禁止配置 destructive migration。

## 1. UserProfile

| 字段 | 说明 |
|---|---|
| id | UUID；游客模式使用本地用户 ID |
| remoteUserId | 登录后对应的云端身份，可为空 |
| createdAt | 创建时间 |
| updatedAt | 修改时间 |

## 2. Activity

| 字段 | 说明 |
|---|---|
| id | UUID |
| ownerId | 用户 ID |
| name | 用户可编辑名称 |
| iconKey | 应用内图标标识，不保存 Android 资源 ID |
| colorArgb | 活动主题色 |
| measurementType | P0 固定为 `BOOLEAN`、`COUNT`、`DURATION` |
| unit | 次、分钟、千米等，可为空 |
| sortOrder | 快速记录和列表排序 |
| isArchived | 是否归档 |
| createdAt / updatedAt / deletedAt | 同步和软删除字段 |
| revision | 版本或服务端修订号 |

不要将“手冲”和“健身”设计为数据库枚举。它们是 Activity 实例；计量方式才是有限类型。

## 3. DailyRecord

每个用户、活动和本地日期最多一条聚合记录。

| 字段 | 说明 |
|---|---|
| id | UUID |
| ownerId | 用户 ID |
| activityId | Activity ID |
| localDate | `YYYY-MM-DD` 语义日期 |
| status | `UNSET`、`DONE`、`MISSED`、`SKIPPED` |
| quantity | 可为空的非负数；次数型 MVP 使用整数 |
| note | 可选私人备注 |
| timezoneId | 写入时的 IANA 时区 |
| occurredAt | 最近一次实际记录时间，可为空 |
| createdAt / updatedAt / deletedAt | 同步字段 |
| revision | 同步冲突字段 |

数据库唯一约束：`(ownerId, activityId, localDate)`。

约束建议：

- quantity 非空时必须满足 `quantity >= 0`。
- BOOLEAN 活动的 `DONE` 表示完成；quantity 可统一映射为 1，但业务含义以 status 为准。
- COUNT 活动 quantity 大于 0 时状态必须为 `DONE`；quantity 为 0 时状态必须为 `MISSED`。
- DURATION 活动使用非负整数分钟；大于 0 时为 `DONE`，等于 0 时为 `MISSED`。
- `UNSET` 与 `SKIPPED` 的 quantity 为空；清除记录会回到 `UNSET`。
- `UNSET` 表示没有明确记录，不等同于 `MISSED`。

## 4. CalendarMarker

可选扩展，用于与活动无关的日期标签。P0 已决定所有颜色来源于活动，因此不创建此表；只有后续用户研究证明存在独立日期标签需求时才通过迁移加入。

| 字段 | 说明 |
|---|---|
| id | UUID |
| ownerId | 用户 ID |
| localDate | 日期 |
| label | 可选标签 |
| colorArgb | 标记色 |
| createdAt / updatedAt / deletedAt | 同步字段 |

## 5. UserSettings

使用 DataStore 保存设备级展示设置；需要跨设备同步的设置单独进入数据库或远端配置。

- 主题。
- 周起始日。
- 日期格式和语言。
- 日历默认筛选活动。
- 是否启用应用锁。
- 隐私显示模式。

## 6. OutboxOperation

| 字段 | 说明 |
|---|---|
| id | 幂等 UUID |
| entityType / entityId | 目标实体 |
| operation | `UPSERT` 或 `DELETE` |
| payload | 版本化同步载荷或生成载荷所需信息 |
| createdAt | 入队时间 |
| attemptCount | 尝试次数 |
| nextAttemptAt | 退避后的下次时间 |
| lastErrorCode | 脱敏错误码 |

Outbox 与业务写入必须处于同一 Room 事务中。

## 7. 索引

- `DailyRecord(ownerId, activityId, localDate)` 唯一索引。
- `DailyRecord(ownerId, localDate)` 日历查询索引。
- `DailyRecord(activityId, localDate)` 统计查询索引。
- `Activity(ownerId, isArchived, sortOrder)`。
- `OutboxOperation(nextAttemptAt, createdAt)`。

## 8. 演进策略

- Schema 导出文件纳入版本控制。
- 每次 schema 变化提供显式迁移。
- P0 不包含 `DailyRecord.colorOverrideArgb` 或 `CalendarMarker` 表；后续只有在研究证明独立日期标记有价值时才通过显式迁移加入。
- 禁止默认使用 destructive migration。
- 新增计量方式时优先增加类型和校验，不改变旧记录含义。
- 如果未来需要记录每次行为的具体时间，可新增 `ActivityEvent`，并继续保留 `DailyRecord` 作为可重建聚合或缓存。

## 9. 已自动验证的契约

- 新增第 7 个活动只新增数据，数据库版本仍为 1。
- `(ownerId, activityId, localDate)` 保持唯一；同日再次保存会更新原聚合记录。
- COUNT/DURATION 大于 0 自动归一为 `DONE`，等于 0 自动归一为 `MISSED`；清除操作写入软删除并在读取时表现为 `UNSET`。
- 归档活动不会删除历史记录，且默认活动列表不再显示该活动。
- 活动产生任何历史记录后不能直接修改 measurementType。
- 固定年度 fixture 可完整保存 128 次、74 天，并得到记录日均 1.7；11—12 月没有伪造记录。

