# 2026-07-30 UI v2 Stage 2 定向验收

状态：`Implementation verified — awaiting user acceptance`

范围：只验收方形次数热力日历，不提前实现 Stage 3 记录页或 Stage 4 统计页。

## 本阶段实现

- 月摘要收敛为“本月总次数 · 发生天数”和记录日均两组文字，删除大面积摘要胶囊。
- 日期格使用同尺寸轻圆角方格，整个 48dp 槽位均可点击。
- 未填写只显示日期与浅中性色背景；明确 0 使用主色描边和空心圆；1、2、3+ 使用模块色由浅到深的整格背景。
- 今天未填写显示“今”并保留边界；已记录日期显示精确次数，9 次以上只在格内缩写为 `9+`，TalkBack 保留真实值。
- 未来日期只弱化日期并禁用，不显示“未来”；相邻月份日期保持空位且不进入 TalkBack。
- 日历由真实 `YearMonth` 生成，覆盖五周月、六周月和闰年 2 月。
- 手冲紫色与做爱深红使用同一布局语法，但读取完全独立的数据。
- 删除仅为旧日历说明卡服务的模块文案字段，避免把已移除 UI 留成无效配置。

## 视觉证据

| 证据 | 说明 |
|---|---|
| [01-calendar-normal.png](01-calendar-normal.png) | 390dp 级视口、正常字号、空日历 |
| [02-calendar-seeded-hand.png](02-calendar-seeded-hand.png) | 手冲：未填写、明确 0、1、2、3+、今天、未来 |
| [03-calendar-seeded-sex.png](03-calendar-seeded-sex.png) | 做爱：独立数据与深红色阶 |
| [04-calendar-200-percent.png](04-calendar-200-percent.png) | 200% 字体首屏，无标题或控件裁切 |
| [05-calendar-200-percent-scrolled.png](05-calendar-200-percent-scrolled.png) | 200% 字体滚动后可到达完整日期网格与记录提示 |
| [06-calendar-narrow-360dp.png](06-calendar-narrow-360dp.png) | 360dp 窄屏，七列完整且文字不重叠 |
| [07-calendar-six-week-month.png](07-calendar-six-week-month.png) | 2026 年 3 月真实六周布局 |
| [08-reference-runtime-comparison.png](08-reference-runtime-comparison.png) | 上方为确认目标图，下方为同屏运行时对照 |

截图使用 API 34 模拟器；设备物理配置为 1080×2280、440 dpi，窄屏证据临时使用 990×2280 后已恢复，系统字体缩放也已恢复为 1.0。

## 定向验证

- `compileDebugKotlin`：通过。
- `compileDebugUnitTestKotlin compileDebugAndroidTestKotlin`：通过。
- `CalendarGridTest`：3 项通过，覆盖五周月、六周月和 2028 闰年 2 月 29 日。
- `CalendarScreenTest` 与 `RecordModuleIntegrationTest`：API 34 模拟器共 12 项通过、0 失败、0 跳过。
- 真实 UI 逐日保存 0、1、2、3 次后，两个模块分别得到 6 次、3 天、记录日均 2.0；切换模块没有混用日期数据。
- 视觉检查覆盖正常字号、200% 字体、360dp 窄屏、五周月、六周月、手冲和做爱两套颜色。
- Android Crash Buffer：空。

本阶段只改变 Compose 日历、纯日期网格函数与相关文档；没有修改 Room、同步、Firebase、Manifest、依赖或发布元数据，因此没有重复运行这些未受影响的完整套件。完整测试仍按 [`docs/TESTING.md`](../../../TESTING.md)在 Stage 5 小版本最终功能 head 上统一执行一次。

## 验收结论

目标图与运行时并排检查后，日期密度、方格比例、颜色层级、明确 0、今天、未来和模块切换均与已确认方向一致。Stage 2 已形成可验收实现；用户确认前不启动 Stage 3。
