# Daily Record 主图标

本目录保存用户确认的正版图标源稿，以及 Android 自适应图标和 Google Play 的导出资源。

## 当前正版源稿

当前唯一有效源稿是用户放在桌面的 `主图标.png`（1254×1254，中心图案已为启动器安全区缩小版，SHA-256：
`471C796C4524A6FBBCF0918D04C2530BCF876876A5E1CD08FFAC0C90782FA471`）。
此前版本的图标不再使用，也不应恢复到运行时资源。

## Android 资源

- `app/src/main/res/drawable/ic_launcher_background.xml`：引用正版完整画面的背景层。
- `app/src/main/res/drawable-nodpi/ic_launcher_background_art.png`：正版源稿的 1024×1024 运行时导出。
- `app/src/main/res/drawable/ic_launcher_foreground.xml`：自适应图标前景入口。
- `app/src/main/res/drawable-nodpi/ic_launcher_foreground_art.png`：当前为空的透明前景占位层。完整画面暂由背景层保真承载，待 Figma 分层 SVG 可写入后再替换为真实透明前景。
- `app/src/main/res/drawable/ic_launcher_foreground_vector.xml`：108×108dp 安全区内的可编辑结构备份。
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`：单色图标，保留日历、人物、爱心和锁孔轮廓。
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` 与 `ic_launcher_round.xml`：Android 8.0+ 自适应图标声明。

背景和前景入口仍保持独立，便于 Figma 分层资产接入；当前背景使用完整正版画面，避免任何旧图标残留或错误的自动抠图伪影。

## Google Play 资源

- `daily-record-icon-1254.png`：正版 1254×1254 源稿副本。
- `daily-record-icon-1024.png`：1024×1024 高质量备份导出。
- `daily-record-icon-google-play-512.png`：512×512、32-bit RGBA、sRGB 的 Play Console 导出，文件小于 1MB，不预先添加圆角或外部阴影。

Google Play 会自动应用圆角和阴影；Android 启动器则按 adaptive icon 声明应用自己的遮罩。

## Figma 分层计划

Figma 写入连接可用后，使用正版源稿建立三个可编辑 SVG 层：

```text
background.svg       紫红渐变背景
foreground.svg       日历、挂钩、人物、爱心和锁
monochrome.svg       简化单色图标
```

届时只替换透明前景和单色资源，不改变已确认的彩色源稿或 Android 资源命名。

## 官方依据

- [Android adaptive icon design](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)
- [Google Play icon design specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications)
- [Google Play Console preview asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en)
