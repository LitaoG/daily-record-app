# 数据模型

## 唯一领域实体

`HandBrewRecord` 表示一个本地日期的聚合手冲次数。

| 字段 | 含义 |
|---|---|
| id | 稳定 UUID；同日更新沿用原 ID |
| localDate | `YYYY-MM-DD`，唯一 |
| brewCount | 非负整数；0 表示明确没冲 |
| createdAt | 首次创建时间 |
| updatedAt | 最近修改时间 |
| ownerId | `__local__` 或 Firebase UID，用于本机账号隔离 |
| isDeleted | 同步清除操作的墓碑；墓碑不进入 UI/统计 |
| syncState | `PENDING` / `SYNCED`，仅同步基础设施使用 |
| remoteRevision | 最近确认的云端修订号 |

不存在 `Activity`、`activityId`、`MeasurementType`、通用业务状态枚举、活动颜色或归档字段。同步元数据只服务于手冲记录，不构成通用活动框架。

## Room schema v3

业务表：`hand_brew_records`

- 主键：`id`
- 唯一索引：`owner_id + local_date`
- 待同步索引：`owner_id + sync_state`
- 日历查询：按日期半开区间升序读取。
- 清除记录：把对应日期行标为墓碑并待同步；普通读取过滤墓碑，结果为未填写。

## 状态推导

状态不单独存储：

```text
row missing   -> UNSET（尚未填写）
isDeleted = 1 -> UNSET（已清除，仅同步层可见）
brewCount = 0 -> NO_BREW（明确没冲）
brewCount > 0 -> BREWED（已手冲）
```

这些名称只用于 UI 推导，不创建数据库枚举。

## v1 → v2 迁移

1. 创建 `hand_brew_records`。
2. 从旧记录中筛选旧活动名“手冲”或旧 `flight` 图标标识。
3. 同日记录按次数求和，保留最早创建时间与最后更新时间。
4. 将旧表改名为 `legacy_activities_v1` 和 `legacy_daily_records_v1`。
5. 自动化测试验证手冲数据、schema 版本和 legacy 表存在。

## v2 → v3 迁移

1. 建立带账号和同步元数据的新表。
2. 把全部 v2 记录迁入 `owner_id = '__local__'`。
3. 旧记录设为 `PENDING`、`remote_revision = 0`，登录时可上传。
4. 建立账号+日期唯一索引和账号+同步状态索引。
5. 自动化测试同时覆盖 v1→当前版本和 v2→当前版本。

## Firestore 文档

文档 ID 与 `localDate` 相同；字段为 `id`、`localDate`、`brewCount`、`createdAtMillis`、`clientUpdatedAtMillis`、`deleted`、`revision`、`schemaVersion` 和服务器时间。规则强制所有权、字段白名单、非负次数、时间顺序、时间戳位于 Unix epoch 至公历 9999 年末、不可变 ID/创建时间、修订号逐次加一和禁止物理删除。
