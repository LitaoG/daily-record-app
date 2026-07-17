# Figma 设计系统：Hand-brew-only v2

设计文件：<https://www.figma.com/design/PMtsNNL81BHl9HyJYhjbdw>

## Foundations

- 暖白纸张背景、深咖正文、陶土橙主色、低饱和边线。
- 4/8dp 间距体系，12/16/24dp 圆角层级。
- 标题、正文、标签、数据数字四级文字样式。
- 所有颜色、间距、圆角和文字使用 Figma Variables/Styles。

## Components

- `CalendarDay`：未填写、0、1、2、9+、今天、选中、未来禁用。
- `BrewCountControl`：减一、数值、加一。
- `PeriodTabs`：周、月、年、全部历史。
- `MetricCard`：总次数、手冲天数、记录日均。
- `StatisticsRow`：周期标签、次数、天数。
- `BottomNavigation`：日历、统计。

## Screens

1. 日历月视图。
2. 日期记录面板。
3. 周/月统计。
4. 年度统计。
5. 空数据与极端状态。

旧活动选择器、健身卡、活动管理页和多色活动胶囊不再属于设计系统。重构时在同一文件新建 `Hand Brew Only v2` 页面保留审计历史，验收后再归档旧页面。
