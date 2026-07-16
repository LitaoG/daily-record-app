# 数据模型

本文档描述逻辑模型。实际 Room Entity 和远端表名可在实现阶段调整，但语义必须保持一致。

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
| measurementType | `BOOLEAN`、`COUNT`，未来可增加 `DURATION`、`DECIMAL` |
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
| quantity | 非负数；次数型 MVP 使用整数 |
| note | 可选私人备注 |
| colorOverrideArgb | 可选的当天颜色覆盖；MVP 是否开放由产品决策决定 |
| timezoneId | 写入时的 IANA 时区 |
| occurredAt | 最近一次实际记录时间，可为空 |
| createdAt / updatedAt / deletedAt | 同步字段 |
| revision | 同步冲突字段 |

数据库唯一约束：`(ownerId, activityId, localDate)`。

约束建议：

- `quantity >= 0`。
- BOOLEAN 活动的 `DONE` 表示完成；quantity 可统一映射为 1，但业务含义以 status 为准。
- COUNT 活动 quantity 大于 0 时通常推导为 DONE。
- `UNSET` 表示没有明确记录，不等同于 `MISSED`。

## 4. CalendarMarker

可选扩展，用于与活动无关的日期标签。如果产品决定所有颜色必须来源于活动，MVP 不创建此表。

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
- 禁止默认使用 destructive migration。
- 新增计量方式时优先增加类型和校验，不改变旧记录含义。
- 如果未来需要记录每次行为的具体时间，可新增 `ActivityEvent`，并继续保留 `DailyRecord` 作为可重建聚合或缓存。

