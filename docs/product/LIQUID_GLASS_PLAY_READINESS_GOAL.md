# 流体玻璃与 Google Play 就绪 Goal

状态：`in progress — Phase 1–4 与 Phase 5 的应用锁、逐次详情已在 agent/b1-apple-glass-refactor 提交；2026-11-06 完整单测 51 个测试类 333 个用例、Android 测试编译与 Lint（0 错误）通过；设备测试、桌面伪装（ADR-024 待确认）与 AAB 未完成`

最后更新：2026-11-06

决策依据：[ADR-022](../DECISIONS.md)。原始目标书 b1 由产品负责人保存在仓库外；本文只记录范围、阶段状态、与目标书的差异和验证证据，不复制目标书全文。

## 目标

把界面升级为 Material 3 Expressive 加 iOS 式玻璃拟态：清爽的珍珠画布、奶白玻璃卡片、9 套可选主题和矢量图标；让高频记录少跳页；补齐应用锁、桌面伪装和 Google Play 上架材料。

颜色克制：模块色以纯色使用；渐变只作白色玻璃高光、一处淡色背景光晕和单色淡出，不在任何表面混合多种色相。

## 不变边界

- 只有自慰和做爱两个独立模块，统计永不相加；不引入 `Activity`、`MeasurementType` 或活动表。
- Room v6、同步、墓碑、账号删除六阶段语义和统计口径保持不变。
- 本地优先：离线冷启动不初始化 Firebase，全部核心功能离线可用。
- 新增持久化只在 SharedPreferences 保存主题 id、应用锁设置和伪装选择。
- 文案只在 `ZhStrings.kt` / `EnStrings.kt`；关键控件有 TalkBack 描述，点击范围不小于 48dp。

## 阶段与状态

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 1 | 9 套主题令牌、流体玻璃材质、设置“主题配色” | 已完成；编译与 `DesignTokensTest` 15/15 通过，Lint 与设备截图留到小版本末尾 |
| 2 | 15 个矢量符号替换 `drawable-nodpi` 的 26 张 PNG，矢量自适应启动图标替换 3 张 WebP | 已完成；编译通过，Play 512px 商店图标已于 2026-11-06 按新矢量图标重新导出 |
| 3 | 模块切换器滑块、周期胶囊、底栏、今日速记、长按加减、滑动切月、触感 | 已完成；编译通过，今日速记、长按快速调整、滑动切月和日历网格的 4 个测试类 19 个用例与 `DesignTokensTest` 15 个用例通过；设备交互与截图留到小版本末尾 |
| 4 | 月度脉冲图与年度折线图触控探针 | 已完成；探针、玻璃气泡与逐点触感已提交，`ChartScrubTest` 7/7 与完整单测通过；设备手势与截图留到小版本末尾 |
| 5 | 逐次详情卡片化、应用锁、桌面伪装、设置分组 | 进行中：应用锁（[ADR-023](../DECISIONS.md)）的锁定规则、系统认证、锁屏层、启动/返回接入和设置“数据与隐私”中的开关与“重新锁定”时长已提交，`AppLockSessionTest` 13/13、Android 测试编译与 Lint 通过，三条认证路径待设备测试；逐次详情改为每次一张轻玻璃卡片、感受框随文字增高，时间与日期滚轮加了逐值轻触感和停下时的回弹，已提交并通过编译，待设备测试；设置页保持四组玻璃卡片，“数据与隐私”新增独立的应用锁卡片，“本机优先”改用新增的手机符号；桌面伪装方案写成 [ADR-024](../DECISIONS.md)（Proposed，待负责人确认 D6 后再改清单和代码） |
| 6 | 文案审查、Data Safety 与内容分级说明、完整门禁、AAB | 进行中：[Google Play 上架就绪清单](PLAY_STORE_READINESS.md)已写，列出建议答案与决策项 D1–D6；英文模块名已按目标书 5.1 改为中性的 Solo / Duo，设置“关于”的隐私政策入口（按界面语言打开中文或英文版本）已提交，二者编译与单测通过、待设备测试；隐私说明已补齐 Firebase 技术信息、逐次详情和可选应用锁并新增英文译本；512px 商店图标已从矢量图层导出，待负责人目视确认；2026-11-06 完整单测、Android 测试编译与 Lint 通过，设备测试（`connectedDebugAndroidTest`）与签名 AAB 未做 |

## 与目标书的差异

目标书要求所有组合满足 WCAG AA。下列色值按目标书原值达不到 AA，实现以 AA 为准：

| 项目 | 目标书 | 实现 | 原因 |
| --- | --- | --- | --- |
| 极光紫 4+ 档 | `#A855F7` | `#C084FC` | 白字 3.95:1，深字 3.70:1 |
| 注释灰 | `#94A3B8` | `#586880` | 白底 2.57:1 |
| 玫瑰主色 | `#E11D48` | `#DE1B46` | 珍珠画布上 4.49:1 |
| 天青主色（冰川蜜桃、暖阳冰川） | `#0284C7` | `#0369A1` | 白字 4.10:1 |
| 翡翠主色 | `#059669` | `#047857` | 白字 3.77:1 |
| Pantone 方案主色 | 浅色渐变 | 同色系深色 | 浅色上白字不足 4.5:1 |

产品负责人在实施中追加了“慎用渐变”的要求，因此以下目标书写法也被收敛：

| 项目 | 目标书 | 实现 |
| --- | --- | --- |
| 选中态、按钮、次数色阶 | 多色标渐变 | 纯色 |
| 背景画布 | 两色扩散加两颗浮动光球 | 珍珠底色、顶部 10% 模块淡色和一处静态淡色光晕 |
| 主题色块 | 渐变预览 | 两个模块主色各占半圆 |
| 启动图标 | 淡紫至香槟的渐变背景 | 纯色极光紫加白色玻璃高光 |

主题里的多色标渐变仍作为配色来源保存在令牌中，只用于小面积装饰，不铺满表面。“未填写”底色由 1 次档与白色各半混合得到，保证比 1 次更浅、比未来日期更亮。

目标书 4.3 写“计数减到 0 时把主记录标为墓碑”，与 ADR-004“0 次与未填写分离”冲突。实现保持 ADR-004：今日速记和长按加减从 1 减到 0 都保存为明确 0 次；回到“未填写”只能在记录页确认“清除记录”。计数减少时多出的逐次详情由既有的只存次数路径自动裁剪，不新增删除逻辑。

目标书 4.2 写长按弹层“手指松开后 0.8 秒平滑消隐”。实现不设自动消失计时：弹层在点“完成”、点弹层外、按返回或切换月份/模块时关闭。原因是限时消失会让 TalkBack 和开关控制用户来不及操作（WCAG 2.2.1 可调时限）。

目标书 4.5 要求图表气泡显示“当时记录的心情摘要”。实现只显示日期或月份与次数：逐次感受不参与统计（ADR-016），统计页也不读取详情表；在图表上随手指浮出私密感受，旁人一眼就能看到，违背应用的隐私定位。年度折线图的探针按月份显示，同样不读取详情。

目标书 5.3 写“通过 `androidx.biometric` 接入”并提供“备用 PIN 锁”。实现按 ADR-023 使用平台认证接口：androidx.biometric 的稳定版仍是 2021 年的 1.1.0 且要求 `FragmentActivity`，新版仍在 beta；备用方式使用设备锁屏密码，不另设应用 PIN，避免自管密码的存储、找回和防暴力破解问题。

## 验证记录

- 2026-11-05：会话临时脚本计算 9 套主题 18 组模块色板，调整上表色值后全部达到 AA；脚本不进入仓库，长期验证由 `DesignTokensTest` 逐套执行。
- 2026-11-05：`:app:compileDebugKotlin` 与 `:app:testDebugUnitTest --tests io.github.litaog.dailyrecord.ui.theme.DesignTokensTest` 通过（15 个测试，0 失败）。本机可用内存约 2 GB，Gradle 以 `--no-daemon --max-workers=1`、1 GB 堆和进程内 Kotlin 编译运行。
- 2026-11-06：Phase 3 定向验证：`:app:testDebugUnitTest --tests "io.github.litaog.dailyrecord.ui.calendar.*" --tests io.github.litaog.dailyrecord.ui.theme.DesignTokensTest` 通过（`TodayQuickLogTest` 5、`CalendarQuickAdjustTest` 4、`CalendarMonthSwipeTest` 6、`CalendarGridTest` 4、`DesignTokensTest` 15，共 34 个测试，0 失败），同一次运行完成主代码与单测编译，改动文件无编译警告。
- 2026-11-06：完整门禁：`:app:testDebugUnitTest`（51 个测试类、333 个用例，0 失败）、`:app:compileDebugAndroidTestKotlin` 与 `:app:lintDebug` 通过。Lint 0 个错误，唯一的警告是 ADR-018 既有的 `OldTargetApi`（targetSdk 36）。首次 Lint 报出的 `InvalidFragmentVersionForActivityResult` 错误（见 ADR-023 的 Lint 后果）和两条 b1 新增警告已修复。Gradle 以 `--no-daemon --max-workers=1`、1–1.5 GB 堆和进程内 Kotlin 编译运行。
- 待执行：`connectedDebugAndroidTest` 等设备测试（含 `RecordModuleIntegrationTest` 设置图标取色和应用锁三条认证路径）、TalkBack 与 200% 字体实机检查、目标页面截图、桌面伪装（ADR-024 待确认）和签名 `bundleRelease`。
