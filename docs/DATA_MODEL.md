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

不存在 `Activity`、`activityId`、`MeasurementType`、通用状态枚举、活动颜色、归档或同步版本字段。

## Room schema v2

业务表：`hand_brew_records`

- 主键：`id`
- 唯一索引：`local_date`
- 日历查询：按日期半开区间升序读取。
- 清除记录：删除对应日期行；读取结果为未填写。

## 状态推导

状态不单独存储：

```text
row missing   -> UNSET（尚未填写）
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
