# 仅手冲重构日志

## 2026-07-17：方向冻结

- 用户将产品从通用活动记录器收缩为只记录手冲的 Android App。
- GitHub 连接暂不可用，因此建立本地分支 `agent/hand-brew-only-refactor`，所有阶段先本地提交。
- 旧分支 `agent/data-model-v1` 和远端 Draft PR 保留作历史，不强推、不改写。

## 阶段 1：领域与数据层

- 删除 `Activity`、`MeasurementType`、`RecordStatus` 和通用 Repository。
- 新增 `HandBrewRecord`、`HandBrewSummary`、专用 DAO/Repository。
- Room 升级到 v2，唯一业务表为 `hand_brew_records`。
- v1→v2 提取旧手冲数据，旧表改名为 legacy 表以便恢复审计。

## 阶段 2：产品事实源

- README、PRD、架构、数据模型、统计、UI、路线图和验收全部改为仅手冲。
- 删除活动管理、健身、喝水、学习、睡眠、跑步和未来活动扩展的有效承诺。

## 待完成

- Figma 重构并替换旧 PNG 资产。
- Android 日历、快速记录和统计功能实现。
- GitHub 恢复后推送分支并创建新的 Draft PR。

## 阶段 1 验证结果

- `testDebugUnitTest`：通过。
- `lintDebug`：通过。
- `assembleDebugAndroidTest`：通过。
- `connectedDebugAndroidTest`：Pixel 4 API 34 上 7/7 通过。
- Room v2 schema 已导出到 `app/schemas`。
- 固定数据集验证：128 次、74 天、记录日均约 1.7。

## 阶段 3：Figma 基础规范

- 在原文件中保留旧多活动页面作审计，新建 14 个 HB2 页面与分隔页。
- 创建 44 个仅手冲变量、8 个 Noto Sans SC 文字样式和 2 个柔和阴影样式。
- 完成 Cover、开始使用和基础规范页；颜色、间距、圆角均绑定真实变量。
- 视觉检查已修复长页面 1px 高度、圆角样本裁切和系统彩色 Emoji 问题。
- 新页面不引用健身、活动管理、活动筛选、多活动颜色或通用状态枚举。
