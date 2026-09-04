# 2026-09-01 双语适配运行证据

状态：`current audit — i18n branch verification`
最后更新：2026-09-01

来源：`agent/optim-perf` 分支，API 34 模拟器 `Pixel_4_API_34`，Debug 构建 `app-debug.apk`。

## 设备与前置

- 模拟器：`Pixel_4_API_34`（API 34，x86_64），`swiftshader_indirect`，`sys.boot_completed=1`
- 语言：默认中文 `ZH`，设置“通用 → 语言”切到 `EN` 后 `AppLanguageState.current = EnStrings` 并 `recreate()`；偏好持久化在 `daily_record_language.xml`
- 本机模式：`daily_record_local_mode.xml`（`enabled=true`）已写入，首屏直达日历
- 字体：`settings put system font_scale 1.0`（正常）与 `2.0`（200%）两档，`adb shell am force-stop` 后重建

## 截图

| 文件 | 语言 | 字体 | 说明 |
|---|---|---|---|
| `zh-calendar.png` | ZH | 1.0 | 日历：模块 `自慰/做爱`、标题 `2026年 9月`、提示“点击日期填写次数” |
| `zh-settings.png` | ZH | 1.0 | 设置：`账号与同步 → 通用（语言，中文）→ 数据与隐私 → 关于` |
| `en-calendar.png` | EN | 1.0 | 日历：模块 `Solo/Sex`、标题 `Sep 2026`、提示 `Tap a date to record`、摘要 `This month: 0 times · 0 days` |
| `en-settings.png` | EN | 1.0 | 设置：`Account & sync → General (Language, English) → Data & privacy → About` |
| `en-calendar-200.png` | EN | 2.0 | 同上，200% 字体：方格几何保持、`Solo/Sex` 半区不截断、今天标记为圆点 |
| `en-settings-200.png` | EN | 2.0 | 同上，200% 字体：`General/Language` 行与卡片不截断 |

## TalkBack 语义抽查（`uiautomator dump`）

EN 日历模块切换器（`en-calendar-final.png` 对应 dump）：

- 可见文本：`Solo`（选中半区 `TextView`）
- 选中半区语义：`content-desc="Masturbation records, selected"`
- 未选半区语义：`content-desc="Sex records, not selected"`

结论：可见短标签与 TalkBack 全称分离，符合简写策略（`docs/product/I18N_GOAL.md`）。

ZH 设置语言行（`zh-settings.png` 对应 dump）：

- `content-desc="语言，中文"`，选项 `中文（已选择）/English（未选择）`

EN 设置语言行（`en-settings.png` 对应 dump）：

- `content-desc="Language, English"`，`General` 分区标题、`Account & sync` 等全部为英文，`中文` 选项仅作为自名出现

## 自动化门禁

| 检查 | 结果 |
|---|---|
| `testDebugUnitTest` | 265 tests，0 failures |
| `lintDebug` / `assembleDebug` / `assembleDebugAndroidTest` | 通过 |
| `connectedDebugAndroidTest`（`DailyRecordAppTest` + `CalendarScreenTest` + `RecordScreenTest` + `DailyRecordAppEnglishTest` + `LanguagePreferenceTest`） | 49/49、43/43、4/4 通过 |
| `pnpm test:docs` (`docs-integrity` + `documentation-catalog`) | 4/4 通过 |
| `pnpm test:copy`（双语契约：中文仅 `ZhStrings.kt`，`EnStrings.kt` 仅 `languageZh="中文"` 自名） | 2/2 通过 |
| `pnpm test:release-metadata` | 4/4 通过 |

## 说明

- 音频 TalkBack 无法在无障碍无头环境自动化验证；以 `contentDescription` 语义树（`uiautomator dump`）为等价证据。
- 截图通过 `adb exec-out screencap -p` 采集，`font_scale` 通过 `settings put system` 切换。
- 恢复前 GitHub 远端 Issue（`#235`/`#236`/`#237`）与审计截图仅本地保存；远端恢复后按 `I18N_GOAL.md` 草稿补建。
