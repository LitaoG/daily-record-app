# Room schema 说明

这里的 JSON 文件是 Room 自动导出的数据库版本快照，用于验证用户从旧版本升级到当前版本时不会丢失自慰或做爱记录。

- `5.json` 是当前双模块数据库结构，业务表为相互独立的 `hand_brew_records`、`hand_brew_record_details`、`sex_records` 和 `sex_record_details`。
- `4.json` 是加入逐次详情前的双模块数据库结构，业务表为相互独立的 `hand_brew_records` 和 `sex_records`。
- `3.json` 是加入做爱记录前的仅自慰结构；`1.json` 和 `2.json` 是更早的迁移测试夹具。它们都不是当前运行时模型。
- 旧快照中出现的 `activityId` 等字段仅用于覆盖历史升级路径，不代表产品仍支持通用活动或健身模块。

不要手工修改或删除历史快照；数据库版本升级时应由 Room 重新导出新快照，并补充相应迁移测试。
