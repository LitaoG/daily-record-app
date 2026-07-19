# 开源借鉴边界

- [Kizitonwose Calendar](https://github.com/kizitonwose/Calendar)（MIT）：支持周/月/年模式、日期边界、任意日期快速滚动和热力图。当前自研月历已通过历史跳转、未来日期禁用和 TalkBack 语义验收，因此真机反馈出现滑动换月或全年热力图的明确需求前不新增依赖。
- [Vico](https://github.com/patrykandpatrick/vico)（Apache-2.0）：Compose Multiplatform 图表候选。P0 继续以精确数字和明细表为事实源；用户积累足够历史数据并明确需要趋势分析后，再评估单一趋势图。
- [Loop Habit Tracker](https://github.com/iSoron/uhabits)（GPLv3）：借鉴“主任务足够快、离线可用、统计逐层展开和数据可导出”的体验，不复制其代码、截图或资源，也不引入连续天数和习惯评分等手冲范围外能力。
- [Now in Android](https://github.com/android/nowinandroid)（Apache-2.0）：只借鉴分层、测试和 Compose 状态管理，不复制与本产品无关的模块复杂度。

任何代码复用都必须核对许可证、保留版权通知并记录来源。GPL 项目只借鉴体验，不复制代码或资源。当前重构阶段不新增第三方业务依赖。

## 本轮结论

- 保留当前“两级导航”：月份箭头负责相邻月，点击大号年月直接跳到任意历史日期；不再为同一任务增加第三套入口。
- 保留周/月/年/全部四个精确明细视图；暂不增加会与空数据竞争注意力的趋势图。
- 保留本地优先和可选登录；开源产品的习惯评分、提醒、连续天数、无限活动等功能不进入仅手冲范围。
