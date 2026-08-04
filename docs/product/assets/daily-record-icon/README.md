# Daily Record 主图标资源

本目录记录当前确认的图标源稿、Android 自适应图标接入方式和商店导出物。旧版简化线稿不再作为运行时资源。

## 当前来源

用户确认的高保真源图为桌面上的 `主图标.png`（1254×1254、无预先圆角）。Figma 源文件：
https://www.figma.com/design/WP2CrYM0XuAFOeojA46jZY

Figma 页面现在按四个可核对的画板排列：完整预览、`background.svg`、`foreground.svg`、`monochrome.svg`。前景和单色画板来自用户提供的透明 PNG，避免缩小后退化成线稿。

## Android 资源

- `drawable/ic_launcher_background.xml` → `drawable-nodpi/ic_launcher_background_art.webp`：紫红渐变背景层。
- `drawable/ic_launcher_foreground.xml` → `drawable-nodpi/ic_launcher_foreground_art.webp`：透明霓虹日历、人物、爱心和锁。
- `drawable/ic_launcher_monochrome.xml` → `drawable-nodpi/ic_launcher_monochrome_art.webp`：单色 alpha 形状，锁孔保持负形。
- `mipmap-anydpi-v26/ic_launcher.xml` 与 `ic_launcher_round.xml`：自适应图标声明。

运行时使用优化后的 WebP 层是有意的：霓虹光晕、细描边和抗锯齿在 Android 启动器尺寸下用手写 VectorDrawable 无法稳定复现；WebP 比运行时 PNG 更轻，SVG 和 PNG 源稿仍作为可编辑交接源保留。

## 商店导出

- `daily-record-icon-1024.png`：1024×1024 sRGB 备份图。
- `google-play-icon-512.png`：512×512、32-bit/sRGB、无预先圆角、约 320 KB，供 Play Console 使用。

Google Play 与启动器会自行应用圆角和阴影，源图不重复添加外框。

## 规范

三层均按 108×108dp 自适应图标画布制作，主要图形保持在约 66×66dp 安全区内；背景无外部阴影，前景透明，锁孔为清晰黑色负形。参考：[Android adaptive icon](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive) 和 [Google Play icon specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications)。
