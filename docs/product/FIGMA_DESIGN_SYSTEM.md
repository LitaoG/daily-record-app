# Figma 设计系统：Hand-brew-only v2

设计文件：<https://www.figma.com/design/PMtsNNL81BHl9HyJYhjbdw>

状态：`Historical — superseded`

> 本文和对应 Figma 文件记录 2026-07-17 的仅手冲设计阶段，只用于历史审计。当前双模块 UI v2 的唯一视觉入口是[紫色手冲＋深红做爱设计基线](design/quiet-private-journal-v2/README.md)，实现顺序以[分阶段 Goal](QUIET_PRIVATE_JOURNAL_GOALS.md)为准。旧陶土色、旧组件和旧画板不得覆盖当前目标、Compose 事实或已验证交互。

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

## 运行时新增覆盖

登录/注册、找回密码、账号同步、诊断和账号删除弹窗已经在 Compose 中使用同一套颜色、圆角、输入框和反馈组件完成，并有模拟器截图与测试。若后续回填 Figma，应按当前运行时迁移，不重新解释已经冻结的交互规则。

旧活动选择器、健身卡、活动管理页和多色活动胶囊不再属于设计系统。HB2 验收通过后，旧多活动页面已从当前 Figma 文件删除；需要时通过 Figma 版本历史审计。未来记录模块必须先建立独立产品契约，不复用旧通用活动画板作为需求来源。
