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

## 2026-07-19 复核

- 再次核对四个项目的官方仓库与许可证：Kizitonwose Calendar 为 MIT，Vico 与 Now in Android 为 Apache-2.0，Loop Habit Tracker 为 GPL-3.0。
- 本轮只采用交互与工程判断：远距离日期定位、日期边界、离线优先、状态分层、可访问语义和测试策略。没有复制任何第三方代码、图标、截图或其他资产，因此无需新增运行时依赖或版权文件。
- 当前自研月历已经提供相邻月切换和 1970 年至今天的年月日快速跳转。只有真机反馈证明需要手势连续滚动、全年视图或热力图时，才重新评估 Kizitonwose Calendar。
- 当前统计以精确数字和可访问明细表为事实源。数据稀疏阶段不引入 Vico；只有用户已有足够历史且趋势图能回答明确问题时，才评估一张不含预测的轻量趋势图。
- Now in Android 继续只作为分层、状态管理和测试参考；Loop Habit Tracker 只作为快速记录与离线体验参考，GPL 内容不进入本仓库。

## 2026-07-22 周/月统计复核

- [Kizitonwose Calendar](https://github.com/kizitonwose/Calendar) 官方仓库约 5.6k stars，MIT，最新 2.10.1 发布于 2026-03-28。继续借鉴“月份边界明确、日期视图由产品自定义”的原则：本月网格不再展示前后月份日期，但保留箭头和任意日期跳转。
- [Vico](https://github.com/patrykandpatrick/vico) 官方仓库约 3.1k stars，Apache-2.0，最新 3.2.1 发布于 2026-05-31。采用直接标注、统一比例和轻量视觉层级的图表思路；当前只有 7 个日点或 4–6 个周点，自研 Compose 分布卡即可清楚表达，不新增图表依赖。
- [Now in Android](https://github.com/android/nowinandroid) 官方仓库约 21.3k stars，Apache-2.0。继续借鉴不可变 UI 模型、真实数据驱动和截图/设备回归方法，不复制其模块规模。
- 周/月统计从同权重指标卡与重复表格，调整为“主摘要 + 直接标注分布”。明确 0 次、未填写、未来仍由文字区分；年与全部历史继续使用精确表格。
- 本轮没有复制第三方代码、截图、图标或资源，也没有新增运行时依赖和版权文件。
