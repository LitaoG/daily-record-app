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

- 用户在 Android Studio/实体设备进行接受测试。
- 五人可用性测试与发布准备。
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

## 阶段 4：Figma 组件库

- 完成 CalendarDay、BrewCountControl、PeriodTabs、MetricCard、StatisticsRow、ActionButton 和 BottomNavigation。
- 共 30 个变体；组件属性轴、变体数量、变量绑定和命名均通过结构校验。
- CalendarDay 同时用数字、文字和颜色表达未填、0、具体次数、今天、选中与未来状态。
- BottomNavigation 只保留日历和统计，并将选中态修正为深陶土橙底与白色内容。
- 逐页截图检查已通过，HB2 页面词汇审计无旧多活动概念。

## 阶段 5：Figma 产品屏幕

- 完成月历、日期记录、周统计、月统计、年统计和全部历史六张 390×844 屏幕。
- 修正 CalendarDay 为 48×56dp、MetricCard 为 112×112dp，使七列月历和三张指标卡能真实落地 390dp Android 画布。
- 2026 年 1–7 月明细精确合计 128 次、74 天；8–12 月为空值。
- 全部历史 2024–2026 年明细精确合计 326 次、191 天。
- 完成空数据、明确 0 次、高次数、未来日期和 200% 大字体状态页。
- 最终 QA：六屏尺寸、禁止词、未来月份数量、统计一致性与整页截图全部通过。

## 阶段 6：Android Compose 产品实现

- 用真实 Room Flow 实现月历、日期记录、周/月/年/全部历史统计。
- 底部导航仅保留日历和统计；日期详情由日历进入。
- 保存 0 次与清除记录严格分离，未来日期不可记录。
- 主题映射 Figma 暖纸色、深咖文字和陶土橙，并补齐系统导航栏安全区。
- 添加统计纯函数单元测试与 Compose 日历语义测试。

## 阶段 6 验证结果

- `testDebugUnitTest`、`lintDebug`、`assembleDebug`、`assembleDebugAndroidTest`：通过。
- `connectedDebugAndroidTest`：Pixel API 34 上 8/8 通过。
- 模拟器真实路径：保存 1 次、保存 0 次、清除 0 次记录、周/月/年/全部统计同步，全部通过。
- 1 次记录后月历与四周期统计均为 1 次/1 天；0 次记录不增加汇总，清除后恢复未填写。

## 阶段 7：交付资产收口

- 产品契约、架构、统计口径、UI 说明、路线图和 Backlog 已与当前 Compose 行为对齐。
- Figma v2 四张总览与 API 34 日历/统计截图纳入 `assets/hand-brew-v2/`。
- 旧 `figma-v1/` 与 `prototype-v1/` 明确降级为历史审计资产。
- Figma 执行账本标记为完成，下一步为用户 Android Studio/实体设备验收。
