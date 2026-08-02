# Design QA — 统计页年度折线与月度分析

## Evidence

- 标题移除参考：`docs/product/audit/2026-08-02-year-line-chart/title-removal-reference.png`
- 年度图视觉参考：`docs/product/design/quiet-private-journal-v2/annual-line-chart-reference-rough.png`
- 动画早期帧：`docs/product/audit/2026-08-02-year-line-chart/animation-early.png`
- 最终实现：`docs/product/audit/2026-08-02-year-line-chart/implementation.png`
- 设备：Pixel 4 API 34 模拟器，截图 `1080×2280px`，应用宽度约 `393dp`。
- 数据状态：手冲模块，2026 年 7 月 4 次、8 月 1 次，其余月份保持明确 0、未填写或未来的既有语义。

## 同屏对照

本轮将用户标注图和最终模拟器截图放在同一次视觉比较输入中检查，而不是分别凭记忆判断。结果如下：

- 页面级大标题“统计”已删除，底部导航的“统计”仍保留。
- 模块切换器下方直接进入“周 / 月 / 年 / 全部”筛选器，筛选器和后续内容整体自然上移，没有负间距或覆盖。
- 年度图中 7 月与 8 月以直线相连，不再进行曲线插值。
- 字体、卡片圆角、模块颜色、网格、横轴月份和底部导航均保持现有设计体系，未出现裁切或横向溢出。

## 动画验收

- 年度真实折线、渐变面积、实心节点和次数标签从左向右只揭示一次，时长约 `1.6s`（`70ms` 延迟 + `1500ms` 主动画）。
- 早期帧 SHA-256：`4A75AFE469957582EE0197663B9C74075157655CC4C88F3D79E58FBCB51AAD07`。
- 完成帧与额外等待 `1.2s` 后的空闲帧 SHA-256 均为 `79A9019CF4D6E9F54003DFD65031E90F1B578BEA350C7EB2E8A5F4A35E379240`，证明动画结束后保持静止、没有循环。
- 坐标轴、网格、月份、空心状态点始终静止，不会抢走真实数据的视觉焦点。

## 交互与语义

- “周 / 月 / 年 / 全部”四个入口仍可点击，筛选状态未改动。
- Android 定向设备测试确认统计页只有一个“统计”文本节点，即底部导航标签。
- 未填写、未来和明确 0 的统计语义没有因本轮视觉修改而改变。

## 月统计重构

- 设计目标：`docs/product/audit/2026-08-02-month-statistics/reference.png`
- Android 顶部实现：`docs/product/audit/2026-08-02-month-statistics/implementation-top.png`
- 手冲下半页：`docs/product/audit/2026-08-02-month-statistics/implementation-lower.png`
- 做爱主题：`docs/product/audit/2026-08-02-month-statistics/implementation-sex.png`
- `200%` 字体：`docs/product/audit/2026-08-02-month-statistics/implementation-font200.png`
- 设计目标与 Android 截图已合并到同一张对照图检查；组件顺序、信息层级、日坐标、主题色和卡片边界与目标一致。
- 月汇总、逐日脉冲图、次数构成和单日极值均可滚动到达；大字体下卡片标题与说明自动堆叠，没有标题互相覆盖或横向溢出。
- 模拟器崩溃缓冲区为空；手冲与做爱两个模块的空数据、当前日和未来日状态均完成视觉检查。

## Findings

没有未解决的 P0、P1 或 P2 视觉问题。

## Follow-up polish

本轮不继续扩大范围；等待用户真机验收触感与实际数据密度。

final result: passed
