# 仅手冲 v2 设计与实现证据

- `01-figma-cover.png`：Figma v2 封面。
- `02-figma-foundations.png`：颜色、字体、间距、圆角与阴影规范。
- `03-figma-product-screens.png`：月历、记录和四类统计共六张 390×844 产品屏幕。
- `04-figma-states-accessibility.png`：空数据、0 次、高次数、未来日期与 200% 字体状态。
- `05-android-calendar-api34.png`：API 34 模拟器真实 Room 数据日历。
- `06-android-statistics-api34.png`：API 34 模拟器真实统计页。
- `07-android-splash-api34.png`：陶土橙飞机启动图标与暖纸色启动画面。
- `08-android-calendar-final-api34.png`：依赖升级、模板清理后的最终 API 34 月历。
- `09-canva-product-overview.png`：正式保存的一页式手冲日历产品概览。
- `10-android-record-hardened-api34.png`：加载保护、矢量控件与防误触确认完成后的日期记录页。
- `11-android-statistics-font200-api34.png`：系统字体 200% 下自动纵向排列的统计指标与可滚动明细。
- `12-android-date-picker-fixed-api34.png`：API 34 最新 Debug 包的历史日期选择器；自定义标题已恢复 Material 3 安全内边距，不再被圆角裁切。
- `13-android-calendar-audit-api34.png`：真实 Room 数据下的 0/1/2 次月历状态，汇总为 3 次、2 天。
- `14-android-statistics-week-api34.png`：全新本机数据下的周统计空态和未来日期边界。
- `15-android-statistics-month-switch-api34.png`：从 7 月切到 6 月后重新生成的周范围，不保留原月份行。
- `16-android-statistics-year-api34.png`：当前年度截至当天的月份明细和未来月份空值语义。
- `17-android-statistics-all-api34.png`：全部历史无记录时的解释文案和返回日历入口。
- `18-android-statistics-seeded-api34.png`：0/1/2 次组合得到 3 次、2 天、记录日均 1.5 的周汇总。
- `19-android-statistics-details-api34.png`：同一周的未填写、明确 0 次、1 次和 2 次逐日明细。

Figma 文件：<https://www.figma.com/design/PMtsNNL81BHl9HyJYhjbdw>

Canva 产品概览：<https://www.canva.com/d/In7LZUcRTdAXbFU>

早期多活动图片已从当前分支删除，只能通过 Git 历史审计。
