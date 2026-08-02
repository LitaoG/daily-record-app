# Design QA — 年度折线面积图

## Evidence

- Source visual truth: `docs/product/design/quiet-private-journal-v2/annual-line-chart-reference-rough.png`
- Rendered implementation: `docs/product/audit/2026-08-02-year-line-chart/implementation.png`
- Side-by-side focused comparison: `docs/product/audit/2026-08-02-year-line-chart/comparison.png`
- Viewport: Pixel 4 API 34 emulator；物理宽度 `1080px`、`440dpi`，应用宽度约 `393dp`；截图包含系统栏，共 `1080×2400px`。
- Source pixels: `1448×1592px`。
- State: 手冲模块，2026 年；6 月明确 0 次、7 月 4 次、8 月 1 次，9–12 月为未来。

## Full-view comparison

完整实现截图确认统计汇总、年度卡、底部导航和滚动关系没有溢出或遮挡；年度卡保持当前项目的暖白 Surface、细边界和紫色模块身份。

## Focused comparison

同图对照确认以下五个表面：

- 字体与排版：标题、月均、副轴刻度、节点数值和月份标签层级清楚；10–12 月完整显示。
- 间距与节奏：纵轴、绘图区、月份轴和说明文字互不挤压，12 个分区等宽。
- 颜色与 Token：折线、节点、辉光和面积渐变都来自当前模块色；网格与空心点使用低权重中性色。
- 图像质量：图表由 Compose Canvas 矢量绘制，没有位图放大、锯齿资产或占位图。
- 文案与内容：节点显示精确次数；未填写、未来和明确 0 没有被混同；TalkBack 描述覆盖全部月份。

参考图把未填写或未来月份画为 0；实现有意改为空心基线点且不连线。这是产品事实约束，不是视觉遗漏。

## Comparison history

1. 首次模拟器截图发现窄屏下 `10月`、`11月`、`12月` 的“月”可能被裁切，判定为 P2。
2. 横轴采用专用的紧凑字号与行高，重新安装并在同一设备宽度复拍。
3. 复拍确认 12 个月标签完整，无剩余 P0、P1 或 P2 问题。

## Primary interactions and runtime checks

- 日历切换到统计页并选择“年”。
- 年度折线仅对真实折线、渐变面积、实心节点和次数标签执行一次约 `0.9s` 的从左向右揭示；坐标轴、网格、月份和空心状态点保持静止。
- 动画结束帧与等待 `1.2s` 后的空闲帧 SHA-256 一致，确认没有循环；退出到“月”再进入“年”可重新捕捉到起始帧。
- 滚动查看年度折线、季度占比和月份摘要。
- 切换到做爱模块，确认统计语义仍存在且使用模块 Token。
- 打开年份选择器，确认“跳转到此年”。
- 打开月份选择器，确认“跳转到此月”。
- 检查 Android crash buffer：为空。

## Findings

没有未解决的 P0、P1 或 P2 差异。

## Follow-up polish

无必须在本轮继续处理的 P3；等待用户在真机上验收触感与实际数据密度。

final result: passed
