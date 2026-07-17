# Figma：仅手冲 v2 执行计划

状态：Figma v2 与 Android Compose 实现完成，进入用户验收
Run ID：`hand-brew-v2-20260717`  
目标文件：<https://www.figma.com/design/PMtsNNL81BHl9HyJYhjbdw>

## 1. 事实来源

- 产品只记录手冲，不提供活动选择、活动管理、健身指标或新增记录类型入口。
- 每个本地日期最多一条记录；`brewCount > 0` 表示已手冲，`brewCount = 0` 表示明确没冲，没有数据库行表示尚未填写。
- 主导航只有“日历”和“统计”；日期详情从日历进入。
- 统计周期固定为周、月、年、全部历史；核心指标固定为总次数、手冲天数、记录日均。
- 画布使用 390 × 844，视觉基线为暖白纸张、深咖文字、陶土橙主色与飞机手冲符号。

## 2. Phase 0 发现

### Android 代码

- Room v2 与 Repository 已经是仅手冲模型。
- 可见 UI 仍是 Android Studio 模板 `Hello Android`。
- Compose 主题仍是模板紫色、动态取色和默认字体；这些不是已确认的产品视觉事实。
- 代码库没有 Code Connect 文件，也没有可复用的 Compose 业务组件。

### 现有 Figma

- 5 个本地变量集合、51 个变量、7 个文本样式、2 个阴影样式。
- 暖纸色、陶土橙、Noto Sans SC 和 4/8dp 间距体系可作为 v2 基线。
- 仍包含健身、阅读、冥想、学习、睡眠等活动颜色。
- 仍包含通用活动行、状态枚举、多活动日期胶囊、四项底部导航和活动筛选器。
- 构建期间旧页面临时保留作对照，HB2 验收完成后已从当前文件删除。

### 可用设计库

- 文件当前未订阅团队设计库。
- Material 3 Design Kit 提供导航栏、分段按钮和底部表单等候选。
- 决策：只借鉴 Material 3 的触控尺寸、语义和交互模式，不直接导入其组件；原因是其视觉 API 与本项目暖纸色定制系统不一致。

## 3. 代码与 Figma 冲突

| 项目 | Android 当前值 | 旧 Figma | v2 决议 |
| --- | --- | --- | --- |
| 产品范围 | 数据层仅手冲，UI 尚未实现 | 通用活动记录器 | 仅手冲产品契约获胜 |
| 颜色 | 模板紫色、动态取色 | 暖纸色、陶土橙、多活动色 | 保留暖纸色与陶土橙，删除 v2 多活动色 |
| 字体 | `FontFamily.Default` | Noto Sans SC | Figma 使用 Noto Sans SC；Compose 映射系统 Sans Serif |
| 导航 | 尚未实现 | 日历/统计/活动/设置 | 仅日历/统计 |
| 状态 | `count > 0`、`0`、无行 | UNSET/DONE/MISSED/SKIPPED | v2 只表达未填写/0 次/具体次数 |
| 日期标记 | 一日期一条手冲记录 | 最多三活动色段 | 单一手冲强度与精确次数 |

## 4. Foundations 锁定范围

### 变量集合

1. `HB2 Primitives`，单模式 `Default`
   - `paper/0 #FFFEFB`
   - `paper/50 #FFF9EF`
   - `paper/100 #F5E9D8`
   - `ink/900 #2D2926`
   - `ink/700 #514A45`
   - `ink/500 #756C65`
   - `terracotta/400 #D77959`
   - `terracotta/500 #C45F3C`
   - `terracotta/600 #A94A2E`
   - `neutral/300 #D8CEBF`
   - `white/1000 #FFFFFF`
2. `HB2 Color`，单模式 `Light`
   - 背景：`color/bg/canvas`、`surface`、`subtle`
   - 文字：`color/text/primary`、`secondary`、`muted`、`on-accent`
   - 边框：`color/border/subtle`、`focus`
   - 强调：`color/accent/soft`、`primary`、`pressed`
   - 记录：`color/brew/zero`、`one`、`two`、`strong`
   - 状态：`color/state/future`
3. `HB2 Dimensions`，单模式 `Default`
   - 间距：0、4、8、12、16、20、24、32
   - 圆角：8、12、16、24、999
   - 描边：1、2
   - 最小触控：48

所有原始变量隐藏作用域；语义颜色、间距、圆角、描边和尺寸变量使用明确作用域；所有变量设置 Android code syntax。

### 样式

- 文本：`HB2/Display/Data`、`Title/Large`、`Title/Medium`、`Body/Large`、`Body/Medium`、`Label/Large`、`Label/Small`、`Caption`。
- 阴影：`HB2/Elevation/Card`、`HB2/Elevation/Sheet`。
- Figma 字体：Noto Sans SC；Android：系统 Sans Serif。

## 5. 组件锁定范围

1. `HB2/CalendarDay`：未填写、0、1、2、9+、今天、选中、未来禁用。
2. `HB2/BrewCountControl`：减一、精确数字、加一，最小触控 48dp。
3. `HB2/PeriodTabs`：周、月、年、全部历史。
4. `HB2/MetricCard`：总次数、手冲天数、记录日均。
5. `HB2/StatisticsRow`：周期标签、次数、天数。
6. `HB2/ActionButton`：主按钮与描边按钮。
7. `HB2/BottomNavigation`：日历、统计两种选中态。

## 6. 页面锁定范围

- `10 HB2 Cover`
- `11 HB2 Getting Started`
- `12 HB2 Foundations`
- `--- HB2 Components ---`
- 每个组件一个独立页面
- `--- HB2 Product Screens ---`
- `20 HB2 Screens`：月历、日期记录面板、周/月统计、年度统计、全部历史统计。
- `21 HB2 States & Accessibility`：空数据、明确 0 次、9+、未来禁用、大字体 200%。

旧多活动页面已在 HB2 验收后删除；当前文件只保留 HB2 页面，并禁止出现健身、活动管理、活动筛选、活动状态枚举和多色活动胶囊。

## 7. Phase 0 差距分析

- 代码有、Figma v2 没有：仅手冲数据语义、0 次与未填写的严格区分、周/月/年/全部历史的完整事实表。
- Figma 有、代码没有：暖纸色 Foundations、组件样式和可访问性示例；将在 Compose UI 阶段实现。
- 两边都缺少：可运行的月历、日期记录面板、统计屏幕与 UI 测试。
- 必须移除的旧概念：活动颜色、多活动汇总胶囊、活动筛选器、活动管理、健身指标、DONE/MISSED/SKIPPED。
- 质量门槛：触控目标至少 48dp；颜色不是唯一状态表达；支持 TalkBack 和 200% 字体；统计明细加总必须等于汇总。

## 8. Android 实现与验收

- Compose 已实现月历、日期记录、周/月/年/全部历史统计和双入口底部导航。
- UI 使用真实 Room Flow 数据，不包含演示数据或其他活动入口。
- API 34 模拟器完成 1 次保存、0 次保存、清除记录和四周期统计同步的真实交互验收。
- `testDebugUnitTest`、`lintDebug`、Debug APK、AndroidTest APK 与 8 个设备测试全部通过。
- Figma 与 Android 对照截图保存在 `assets/hand-brew-v2/`。
