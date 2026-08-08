# 数据模型

当前 schema：Room v4。最后复核：2026-08-08。

## 两个独立领域实体

`HandBrewRecord` 表示一个本地日期的聚合手冲次数；`SexRecord` 表示一个本地日期的聚合做爱次数。二者不共享业务表，也没有活动类型字段。

| 字段 | 含义 |
|---|---|
| id | 稳定 UUID；同日更新沿用原 ID |
| localDate | `YYYY-MM-DD`，唯一 |
| brewCount / sexCount | 非负整数；0 表示明确没有发生对应行为 |
| createdAt | 首次创建时间 |
| updatedAt | 最近修改时间 |
| ownerId | `__local__` 或 Firebase UID，用于本机账号隔离 |
| isDeleted | 同步清除操作的墓碑；墓碑不进入 UI/统计 |
| syncState | `PENDING` / `SYNCED`，仅同步基础设施使用 |
| remoteRevision | 最近确认的云端修订号 |

不存在 `Activity`、`activityId`、`MeasurementType`、通用业务状态枚举、活动颜色或归档字段。未来记录类型应使用自己的实体和表，不向现有表追加用于区分活动种类的字段。

## Room schema v4

业务表：`hand_brew_records`、`sex_records`

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
moduleCount = 0 -> EXPLICIT_ZERO（明确没有）
moduleCount > 0 -> OCCURRED（已发生）
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

## v3 → v4 迁移

1. 原样保留 `hand_brew_records` 和 legacy 表。
2. 创建空的 `sex_records`，字段包含 `sex_count` 与完整同步元数据。
3. 建立 `owner_id + local_date` 唯一索引和 `owner_id + sync_state` 待同步索引。
4. 不推断或复制任何历史手冲行为为做爱记录。
5. 自动化测试覆盖 v1→v4、v2→v4、v3→v4，禁止 destructive migration。

## Firestore 文档

两个集合的文档 ID 都与 `localDate` 相同。手冲文档使用 `brewCount`，做爱文档使用 `sexCount`；其余字段为 `id`、`localDate`、`createdAtMillis`、`clientUpdatedAtMillis`、`deleted`、`revision`、`schemaVersion` 和服务器时间。规则分别强制所有权、字段白名单、非负次数、时间范围、不可变身份和修订号逐次加一。普通记录清除必须写墓碑；永久删除账号时，已登录本人可以物理删除自己路径下的全部文档。
