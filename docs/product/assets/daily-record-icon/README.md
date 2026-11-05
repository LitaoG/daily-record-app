# Daily Record 主图标资源

状态：`current asset handoff reference`
最后复核：2026-08-16

本目录是图标交接入口。完整源稿和导出物统一放在 [`docs/design/icon-source`](../../../design/icon-source)，避免同一张图在多个目录重复维护；本目录不再保存运行时副本。

## 当前来源

用户确认的高保真源图为 `docs/design/icon-source/daily-record-icon-1254.png`（1254×1254、无预先圆角）。Figma 源文件：
https://www.figma.com/design/WP2CrYM0XuAFOeojA46jZY

Figma 页面现在按四个可核对的画板排列：完整预览、`background.svg`、`foreground.svg`、`monochrome.svg`。前景和单色画板来自用户提供的透明 PNG，避免缩小后退化成线稿。

## Android 资源

自 ADR-022（2026-11-05）起，启动图标三层都是手写 VectorDrawable，不再引用 `drawable-nodpi` 下的 WebP（已删除）：

- `drawable/ic_launcher_background.xml`：纯色极光紫 `#6D28D9`，左上角一层柔和的白色玻璃高光。
- `drawable/ic_launcher_foreground.xml`：两枚交叠的磨砂白色圆环和一颗小星点，全部位于 66dp 安全区内。
- `drawable/ic_launcher_monochrome.xml`：同样的两枚圆环，供 Android 13+ 主题图标着色。
- `mipmap-anydpi/ic_launcher.xml` 与 `ic_launcher_round.xml`：自适应图标声明，未改动。

上文的霓虹源稿只代表 beta.3 的旧图标；颜色克制规则下，新图标不使用多色渐变。

## 商店导出

- [`google-play-icon-512.png`](../../../design/icon-source/google-play-icon-512.png)：512×512、32-bit/sRGB、无预先圆角、约 320 KB，供 Play Console 使用。

1024px 备份图、三层 PNG 副本和预览合成图不是运行时或交接输入，已在
2026-08-11 资源整理中移除；需要复核旧导出时可从 Git 历史恢复。

Google Play 与启动器会自行应用圆角和阴影，源图不重复添加外框。

## 规范

三层均按 108×108dp 自适应图标画布制作，主要图形保持在约 66×66dp 安全区内；背景无外部阴影，前景透明，锁孔为清晰黑色负形。参考：[Android adaptive icon](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive) 和 [Google Play icon specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications)。
