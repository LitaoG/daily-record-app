# 2026-07-30 UI v2 Stage 2 反馈修正

状态：`Implementation verified — awaiting user acceptance`

范围：只修正方形次数热力日历的状态区分、月摘要和页面收尾，不进入 Stage 3 记录页或 Stage 4 统计页。

## 反馈与处理

1. **过去未填写和未来日期看起来相同。**
   - 过去且可记录的未填写日期继续使用 `#F2EFEA`。
   - 未来且不可记录的日期改用独立的 `#F8F5EF`，同时保留更弱文字和禁用点击。
   - 日期格内仍不重复显示“未填/未来”；完整差异放在底部图例和 TalkBack 语义中。
2. **月摘要右侧记录日均多余。**
   - 日历首页只保留“本月总次数 · 发生天数”。
   - 记录日均仍属于统计页事实，不再在日历首页重复。
3. **短月份网格结束后像页面没有做完。**
   - 没有重新加入大卡片，也没有添加新功能。
   - 页面改为“月份主体 + 底部收尾”两段：短月份时，轻分隔线、点击提示和四态图例稳定落在底部导航上方；六周月和 200% 字体时自然进入滚动。
   - 图例只集中说明一次未填写、未来、明确 0 和正次数色阶，避免把汉字塞回每个日期格。

## 视觉证据

| 证据 | 说明 |
|---|---|
| [06-user-feedback-summary.png](06-user-feedback-summary.png) | 用户反馈：未来与未填写同色，且月摘要存在日均 |
| [07-user-feedback-empty-footer.png](07-user-feedback-empty-footer.png) | 用户反馈：点击提示之后缺少页面收尾 |
| [01-calendar-normal.png](01-calendar-normal.png) | 390dp 级视口：月摘要已去除日均，30 日未填写与 31 日未来使用不同底色，图例落在底部导航上方 |
| [02-calendar-200-percent.png](02-calendar-200-percent.png) | 200% 字体首屏：年月、摘要和日期格未裁切 |
| [03-calendar-200-percent-bottom.png](03-calendar-200-percent-bottom.png) | 200% 字体滚动底部：四态图例改为两行且完整可达 |
| [04-calendar-narrow-360dp.png](04-calendar-narrow-360dp.png) | 360dp 窄屏：四项图例单行可读，无重叠 |
| [05-calendar-six-week.png](05-calendar-six-week.png) | 2026 年 3 月六周月：完整网格、收尾区和底部导航同屏可见 |

截图使用 API 34 模拟器；正常视口为 1080×2280、440 dpi，窄屏证据使用 480 dpi。截图结束后已恢复 440 dpi 和 1.0 字体缩放。

## 定向验证

- `compileDebugKotlin compileDebugUnitTestKotlin compileDebugAndroidTestKotlin`：通过。
- `DesignTokensTest`：新增未来与未填写底色不得相同的回归断言，5 项通过。
- `CalendarScreenTest`：6 项通过，覆盖摘要内容、日期可见语法、未来禁用、48dp 点击范围和 200% 字体。
- 正常字号、200% 字体、360dp 窄屏、五周月和六周月均已截图检查。
- Android Crash Buffer：空。
- 目标反馈图与运行时截图已在同一次视觉比较中检查；三个反馈点均有直接证据。

本次只修改 Compose 日历、状态色 token、对应测试和文档。Room、同步、Firebase、Manifest、依赖与发布元数据均未改变，因此不重复运行这些无关完整套件；完整验证继续留在 Stage 5 最终功能 head。

## 结论

日历首页不再重复记录日均；未来日期和过去未填写日期现在有独立状态底色；短月份底部由轻量图例收尾，不再让“点击日期记录”悬在一块未完成的空白上。Stage 2 继续等待用户验收，验收前不启动 Stage 3。
