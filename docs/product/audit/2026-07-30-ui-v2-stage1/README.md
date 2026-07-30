# UI v2 Stage 1：设计 Token 与共享外壳验收

日期：2026-07-30

范围：只验收 [Stage 1](../../QUIET_PRIVATE_JOURNAL_GOALS.md#stage-1设计-token-与共享外壳) 的颜色 Token、排版与尺寸 Token、模块切换器、周期切换、页面边距和底部导航。本轮不把日历格、记录页主体或年度统计主体提前算作已经改版。

## 结论

- 手冲主色为 `#72517C`，深色为 `#4B3354`。
- 做爱主色为 `#8D3E45`，深色为 `#5F272C`。
- 模块切换器是单层严格二等分；选中背景、点击范围和语义范围均覆盖完整半区。
- 统计周期不再使用内部选中胶囊，改为文字加 2dp 下划线。
- 底部导航不再使用大面积选中胶囊，改为模块色图标、文字和 2dp 下划线。
- 页面水平边距统一为 20dp；共享触控目标不小于 48dp。
- 正常字号与 200% 字体均未发现共享壳层文字截断；Crash Buffer 为空。
- [设计 QA](DESIGN_QA.md)没有遗留 P0、P1 或 P2 问题，`final result: passed`。

## 运行环境

| 项目 | 值 |
|---|---|
| 设备 | Android Emulator `Pixel_4_API_34` |
| Android | API 34 |
| 物理画布 | 1080×2280 px |
| 密度 | 440 dpi，约 393×829 dp |
| 字体 | 100% 与 200% |
| 模块状态 | 手冲选中、做爱选中 |
| 页面状态 | 2026 年 7 月空日历、空周统计 |

## 几何与语义证据

正常字号下，UI Automator 记录的模块切换器外框为 `[55,280][1025,423]`：

- 手冲半区：`[55,280][540,423]`。
- 做爱半区：`[540,280][1025,423]`。

两半在 `x=540` 精确相接，左右边缘、顶部和底部均与外框一致。Compose 测试另外验证了：

- 两个半区宽度均为外框的 `1/2`。
- 外框与两个半区最小高度均为 52dp。
- 正常静止状态下，完整选中半区的采样色分别等于手冲主色和做爱主色。
- 200% 字体下模块半区、周期标签和底部导航仍满足 48dp/52dp 门槛。

语义树：

- [手冲日历](01-calendar-hand-semantics.xml)
- [做爱日历](02-calendar-sex-semantics.xml)
- [做爱统计](04-statistics-sex-semantics.xml)
- [做爱日历 200% 字体](05-calendar-sex-font200-semantics.xml)
- [做爱统计 200% 字体](06-statistics-sex-font200-semantics.xml)

## 视觉证据

正常字号：

- [手冲日历](01-calendar-hand.png)
- [做爱日历](02-calendar-sex.png)
- [手冲统计](03-statistics-hand.png)
- [做爱统计](04-statistics-sex.png)

200% 字体：

- [做爱日历](05-calendar-sex-font200.png)
- [做爱统计](06-statistics-sex-font200.png)

源设计与运行时同图对照：

- [日历共享外壳对照](07-calendar-comparison.png)
- [统计共享外壳对照](08-statistics-comparison.png)

源设计是视觉方向图，不是精确 Android 画布；对照只判断本阶段负责的共享外壳。运行时保留真实账号栏、系统状态栏和当前业务页面，日历格与统计主体的结构差异分别由 Stage 2 和 Stage 4 处理。

## 定向验证

| 验证 | 结果 |
|---|---|
| `compileDebugKotlin` / `assembleDebug` | 通过 |
| `compileDebugUnitTestKotlin` | 通过 |
| `compileDebugAndroidTestKotlin` | 通过 |
| `DesignTokensTest` | 4/4 通过 |
| `RecordModuleIntegrationTest` | 6/6 通过 |
| `lintDebug` | 通过 |
| Markdown 链接与发布元数据测试 | 6/6 通过 |
| `git diff --check` | 通过 |
| 敏感信息扫描 | 214 个文本文件，0 个命中 |
| Crash Buffer | 空 |

按照 [`TESTING.md`](../../../TESTING.md)，本阶段只运行与主题、共享组件和双模块切换直接相关的测试。Room、Firestore、认证、同步与完整设备套件的旧证据仍有效，因为本阶段没有改变这些边界；完整套件统一留在 Stage 5 的最终功能 head。
