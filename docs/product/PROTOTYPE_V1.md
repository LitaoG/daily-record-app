# 视觉原型 v1

状态：Figma 原生设计系统已完成；等待 5 人可用性测试
更新时间：2026-07-16

## 1. 月历概览

![月历概览](assets/prototype-v1/01-calendar-overview.png)

验证点：月历优先、活动筛选、单个汇总胶囊、`8+` 压力场景、当日快速记录、手冲飞机图标。

## 2. 日期详情

![日期详情](assets/prototype-v1/02-date-details.png)

验证点：6 项活动同屏仍可扫描；次数、时长、完成、跳过、未设置状态使用通用结构。

## 3. 统计

![统计](assets/prototype-v1/03-statistics.png)

验证点：周/月/年/全部历史周期；选择“手冲”后只展示手冲总次数、手冲天数和记录日均；趋势图为辅助，不展示无分母的完成率。

旧 PNG 的月次数合计为 118、月天数合计为 78，只保留为历史结构参考。Figma v1 已用同一 fixture 统一汇总卡、月表和趋势：128 次、74 天、记录日均 1.7；11—12 月保持空值且不预测未来。详见[#14](https://github.com/LitaoG/daily-record-app/issues/14)与[Figma 设计系统交接](FIGMA_DESIGN_SYSTEM.md)。

## 4. 活动管理

![活动管理](assets/prototype-v1/04-activity-management.png)

验证点：飞机代表手冲；活动支持次数、完成、时长三种测量类型；可排序、编辑、归档并继续增加到 8+。

## 5. 日历标记组件规范

![日历标记组件规范](assets/prototype-v1/05-scalable-marker-system.png)

验证点：一日期一胶囊、最多 3 色段、总活动数、单活动筛选和不只靠颜色的四状态编码。

旧 PNG 的颜色语义债务已在 Figma v1 中修复：活动颜色只表达身份，状态使用符号、文字和轮廓；空日期不显示 `0` 胶囊。旧图继续作为历史结构参考，正式 Android UI 交接以[Figma 设计系统交接](FIGMA_DESIGN_SYSTEM.md)为准。

## 设计协作入口

- [FigJam 产品发现板](https://www.figma.com/board/QPalmez5kHyjeaLXeJeZ6y)
- [Figma 可编辑原型工作区](https://www.figma.com/design/PMtsNNL81BHl9HyJYhjbdw)
- [Figma 设计系统与 Android 交接](FIGMA_DESIGN_SYSTEM.md)
- [Canva 产品发现一页图](https://www.canva.com/d/XEOO0XaHd3IjtC_)

Figma 教育权限已经生效，原生变量、文本/阴影样式、组件集、五张主页面、五张异常/无障碍页面和核心原型交互已完成。业务规则仍以 Markdown 和 ADR 为事实来源，Figma 是视觉与交互实现基线。

## 评审结论门槛

进入 Android UI 实现前，至少完成 5 名目标用户的任务测试，并满足：核心任务完成率不低于 90%、常用记录中位用时不超过 5 秒、4/8+ 活动汇总数识别正确率不低于 90%。
