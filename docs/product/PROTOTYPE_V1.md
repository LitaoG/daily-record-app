# 视觉原型 v1

状态：结构可评审；颜色语义与空状态待修订
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

已知数据债务：当前 PNG 的月次数合计为 118、月天数合计为 78，与汇总卡 128 / 74 不一致；画面还需明确演示参考日期。修订后月表、汇总卡和趋势图必须使用同一固定数据集，才能作为统计实现与可用性测试基线，详见[#14](https://github.com/LitaoG/daily-record-app/issues/14)。

## 4. 活动管理

![活动管理](assets/prototype-v1/04-activity-management.png)

验证点：飞机代表手冲；活动支持次数、完成、时长三种测量类型；可排序、编辑、归档并继续增加到 8+。

## 5. 日历标记组件规范

![日历标记组件规范](assets/prototype-v1/05-scalable-marker-system.png)

验证点：一日期一胶囊、最多 3 色段、总活动数、单活动筛选和不只靠颜色的四状态编码。

已知设计债务：当前图片把部分色段解释为状态色，且手冲在不同页面的活动色不一致。后续必须以[P0 原型体验审计](UX_AUDIT_AND_OPTIMIZATION.md)为准：活动颜色只表达身份，状态使用符号、文字、轮廓或纹理；空日期不显示 `0` 胶囊。

## 设计协作入口

- [FigJam 产品发现板](https://www.figma.com/board/QPalmez5kHyjeaLXeJeZ6y)
- [Figma 可编辑原型工作区](https://www.figma.com/design/PMtsNNL81BHl9HyJYhjbdw)
- [Canva 产品发现一页图](https://www.canva.com/d/XEOO0XaHd3IjtC_)

Figma 教育权限正在审核，暂未把仓库视觉稿转换为 Figma 原生可编辑图层。仓库 PNG 只作为结构与视觉方向参考，业务规则以 Markdown 和 ADR 为事实来源；迁移前先修复上述设计债务，再建立原生组件和变量。

## 评审结论门槛

进入 Android UI 实现前，至少完成 5 名目标用户的任务测试，并满足：核心任务完成率不低于 90%、常用记录中位用时不超过 5 秒、4/8+ 活动汇总数识别正确率不低于 90%。
