# 双语适配 Goal（i18n：中文默认 + English）

状态：`in progress — 本地分支 agent/i18n-bilingual 实现中`
最后更新：2026-08-16

来源：2026-08-16 用户直接需求。GitHub 远端暂不可用，全部阶段在本地分支实现，远端恢复后按本文件末尾的 Issue 草稿补建 Issue 并推送。

## 目标

- 应用支持中文（默认）与英语；用户在设置中切换，选择持久化，重启保持。
- 默认中文，不跟随系统语言（v1 不做“跟随系统”模式）。
- 中文文案与 `v1.0.0-beta.3` 完全一致，零回归。
- 英文单词普遍长于中文：固定宽度容器（模块二等分、周期胶囊、日历格、状态 chips）使用可见短标签，TalkBack 无障碍语义使用全称；正常字号与 200% 系统字体下不截断关键信息。
- 全部文案只存在于两个语言文件 `ZhStrings.kt`（中文）与 `EnStrings.kt`（英文），UI 与业务代码零硬编码文案，切换只改变 `AppLanguageState.current` 指向的语言实现。

## 机制

- `AppLanguage`（ZH/EN）+ `LanguagePreference`（SharedPreferences，默认 ZH，StateFlow）。
- `AppStrings` 接口承载全部文案签名，`ZhStrings`/`EnStrings` 分别实现；漏译直接编译失败。
- `AppCopy` 保持历史调用形态，改为语言委托门面；非 UI 层（`AccountSyncManager`、`StatisticsModels`）与 UI 读取同一份 `AppLanguageState.current`。
- 类加载期不得固化文案：`RecordModule.uiSpec()` 与 `StatisticsPeriod.label` 按当前语言取值。
- 设置页“通用 → 语言”选择后：写入偏好 → 更新 `AppLanguageState.current` → 主 Activity `recreate()` 全量重组；`rememberSaveable` 状态（浏览日期、模块、目的地）在重建后保留。
- `res/values-en/strings.xml` 提供桌面图标名 `Private Calendar`；应用内标题走 AppCopy。

## 简写策略

| 位置 | 可见 | TalkBack |
|---|---|---|
| 模块切换器 | Solo / Sex | Masturbation / Sex |
| 统计周期胶囊 | Wk / Mo / Yr / All | Week / Month / Year / All history |
| 底部导航 | Calendar / Stats | 同左 |
| 日历格次数 | 数字，9+ | 9 times or more |
| 日历格今天 | •（圆点标记，200% 字体安全） | today |
| 月份标题 | Aug 2026 | August 2026 |
| 统计指标 | Total / Days / Avg / day | 同左 |
| 同步状态 | Not linked / Offline / Syncing / Synced / Sync failed | 同左 |
| 按钮 | Save / Clear / Cancel / OK | 同左 |

## 阶段与状态

| 阶段 | 提交 | 内容 |
|---|---|---|
| 1 | `ce92f54` | 语言基础设施：AppLanguage、偏好、AppStrings 接口与 ZhStrings 迁移、AppCopy 门面、文案解耦点、根接线；中文零回归 |
| 2 | `9be2c0d` | EnStrings 全量英文文案与格式双语化、设置页语言入口、values-en、双语单测矩阵 |
| 3 | `4f056f4` | 英文长词布局收口：EN 今天标记改圆点，保持 200% 字体方格几何 |
| 4 | `8f83ca1`、`495b975` | 双语 androidTest（EN 渲染、设置切换往返、持久化）、文档收口 |
| 5 | 本分支最终 head | 验证完成：unit 256 全绿（含双语矩阵与残留检查）、lintDebug/assemble 通过、API 34 模拟器设备测试 49/49 全绿（含 DailyRecordAppEnglishTest 4/4）；双语言截图与 TalkBack 音频抽查待常规会话补采（本机无 Node 工具链，`pnpm test:docs` 等 Node 门禁待有 Node 环境执行） |

## 验收

- 首次安装默认中文；切到 English 立即生效；杀进程重启保持；切回同理。
- 两语言下日历、记录、统计、登录/找回密码、账号与删除、日期跳转、设置全部可用；200% 字体与 360dp 窄屏不截断关键文案；TalkBack 语义完整（EN 模块切换器读全称）。
- 全部既有中文测试保持通过；新增双语矩阵覆盖 AppCopy 全表与格式函数；EN 定向 UI 测试覆盖模块切换、日历摘要、语言切换往返。
- Room 迁移、同步、统计口径、Firestore Rules 无变化，相关既有证据不失效。

## 执行纪律

- 分支：`agent/i18n-bilingual`（自公共 `main`）；提交前缀 `[ds:v4pro]`；合并保留全部提交历史。
- 测试遵循 `docs/TESTING.md`：开发中只跑定向测试；Stage 5 在最终 head 上跑一次完整套件。
- 远端恢复后：按下方草稿创建 Issue（一次一个、间隔 60 秒以上，见 `AI_COLLABORATION.md` 教训），推送分支并为每个阶段开 PR。

## GitHub Issue 草稿（远端恢复后按此创建，只写目标）

### #1 [P1][i18n] 双语适配总目标（Goal）
目标：支持中文默认 + 英语，设置内切换并持久化；英文固定宽度位置用短标签、TalkBack 用全称；中文零回归。子阶段见 #2–#6。

### #2 [P1][i18n] Stage 1 语言基础设施
目标：AppLanguage + LanguagePreference + AppStrings 接口双实现 + AppCopy 语言门面 + 文案解耦点 + 根接线，用户可见行为零变化。

### #3 [P1][i18n] Stage 2 英文文案与格式 + 设置语言入口
目标：EnStrings 全量英文、日期/星期/数字/分隔符双语化、设置页“通用 → 语言”切换入口、values-en 图标名。

### #4 [P1][i18n] Stage 3 英文长词布局适配
目标：逐页验证 EN + 正常字号 + 200% 字体不截断；必要时按简写策略回退，不破坏视觉基线。

### #5 [P1][i18n] Stage 4 双语测试与文档收口
目标：双语测试矩阵、EN 定向 UI 测试、UI_UX/PRODUCT/ARCHITECTURE/DECISIONS 文档与 ADR 收口。

### #6 [P1][i18n] Stage 5 集成 QA 与发布评估
目标：最终 head 一次完整套件、API 34 模拟器双语言截图与 TalkBack 抽查、发布建议交用户决策。
