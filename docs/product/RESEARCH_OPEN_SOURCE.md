# 开源产品调研

更新时间：2026-07-16

## 1. 调研问题

本轮不寻找可直接复制的完整应用，而是回答四个问题：

1. 怎样让每天记录足够快？
2. 次数型与完成型活动怎样共存？
3. 日历和统计最容易出现哪些语义错误？
4. 哪些模式可以借鉴，哪些代码受许可证限制不能直接搬用？

## 2. 项目对比

| 项目 | 可借鉴模式 | 风险或限制 | 许可证结论 |
|---|---|---|---|
| [Loop Habit Tracker](https://github.com/iSoron/uhabits) | 低摩擦记录、活动颜色、周视图、离线与导出 | 容易把周期目标压缩成单日状态；界面密度高 | GPL-3.0，只借鉴交互，不复制代码 |
| [MiniMoods](https://github.com/CampbellMG/MiniMoods) | 单日输入与月历反馈结合、简单导出、深色模式 | 单一场景较强，多活动扩展需要重做信息层级 | MIT，可参考实现思想但仍自行设计 |
| [Track & Graph](https://github.com/SamAmco/track-and-graph) | 通用自定义数据、图表与个人数据所有权 | 功能面较宽，首屏容易变成工具箱 | GPL-3.0，只借鉴产品模式 |
| [RoutineTracker](https://github.com/DanielRendox/RoutineTracker) | 清晰的日期议程、灵活计划、完成/失败反馈 | 偏计划执行，和本项目的“快速事实记录”不同 | GPL-3.0，只借鉴信息组织 |
| [Trakit](https://github.com/tylxr59/Trakit) | Material 3、GitHub 风格色块日历、自托管方向 | 项目体量和用户验证不足，不能作为成熟基准 | MIT，可作方向参考 |

## 3. 真实 Issue 证据

### 周期语义不能伪装成单日状态

- Loop [#2347](https://github.com/iSoron/uhabits/issues/2347)：可测量习惯的日历状态与每周目标不一致。
- Loop [#2070](https://github.com/iSoron/uhabits/issues/2070)：周/月目标被错误地按单日完成状态处理。
- Loop [#2115](https://github.com/iSoron/uhabits/issues/2115)：滑动窗口的“Y 天 X 次”容易让用户误判是否达成。

产品结论：MVP 不展示目标完成率；目标、分母、周期和跳过规则未定义前，只展示事实统计。

### 数量与状态必须同步

- Loop [#1857](https://github.com/iSoron/uhabits/issues/1857)：把次数改为 0 后仍被视为活跃状态。

产品结论：`quantity = 0` 必须对应 `MISSED`；清除记录才回到 `UNSET`。

### 日期上下文与本地化必须显式

- Loop [#1779](https://github.com/iSoron/uhabits/issues/1779)：横向滚动日期后失去月份上下文。
- Track & Graph [#155](https://github.com/SamAmco/track-and-graph/issues/155)：用户需要星期一作为一周起始日。

产品结论：月份标题始终可见，一周起始日可配置，默认星期一。

### 日历和输入是核心能力，不是装饰

- Track & Graph [#101](https://github.com/SamAmco/track-and-graph/issues/101) 与 [#258](https://github.com/SamAmco/track-and-graph/issues/258)：用户持续要求日历和热力图。
- Track & Graph [#333](https://github.com/SamAmco/track-and-graph/issues/333)：格式变化导致输入区域拥挤。
- RoutineTracker [#13](https://github.com/DanielRendox/RoutineTracker/issues/13)：无法直接重命名导致只能删除重建。

产品结论：月历是主入口；常用输入保持大触控区；活动必须可编辑并优先归档而非删除。

### 隐私本身是产品价值

- Track & Graph [#75](https://github.com/SamAmco/track-and-graph/issues/75)：用户主动要求加密与生物识别保护。

产品结论：首版无登录可用；应用锁、导出和同步安全进入明确路线图；默认遥测不采集活动名称、备注或记录数值。

## 4. 采用与拒绝

### 采用

- 快速单击/步进记录。
- 月历中的独立活动色点。
- 精确表格与趋势图并存，但表格是事实来源。
- 本地优先、可导出、可解释的统计。

### 拒绝

- 把计划、目标、连续天数和事实记录混在一个状态中。
- 用一种颜色同时表达活动类型、完成状态和次数强度。
- 把云端登录放在首次记录之前。
- 直接复制 GPL 项目的代码、资源或界面实现。
