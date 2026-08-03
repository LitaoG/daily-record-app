# Daily Record 主图标

这组资源以用户提供的 1254×1254 图稿为唯一视觉基准，保留紫红交织渐变、日历、双挂钩、人物、交织双心和黑色锁孔结构。原图不做圆角；圆角、阴影和启动器形状由 Android/Google Play 在对应平台处理。

## Android 自适应图标

运行时采用 Android adaptive icon 的双层声明：背景是从原图生成的柔和环境层，前景是从用户成品中提取的透明高保真图层。这样既保留霓虹玻璃视觉，又不会把商店上传图当成普通 launcher PNG 直接使用。

- `app/src/main/res/drawable/ic_launcher_background.xml`：全幅背景入口，引用经过柔化的环境背景位图。
- `app/src/main/res/drawable-nodpi/ic_launcher_background_blurred.png`：从原图生成的 1024×1024 柔化背景，只保留紫红环境光，不承担前景细节。
- `app/src/main/res/drawable/ic_launcher_foreground.xml`：生产入口，引用透明前景位图。
- `app/src/main/res/drawable-nodpi/ic_launcher_foreground_art.png`：从用户 1254×1254 成品提取的 1024×1024 RGBA 透明前景，非透明包围盒为 `(203,203)-(820,819)`，主体缩进中心约 66×66 安全区，保留日历、挂钩、人物、双心、锁和霓虹边缘。
- `app/src/main/res/drawable/ic_launcher_foreground_vector.xml`：108×108dp、约 66×66 安全区的可编辑矢量备份稿，供后续需要完全矢量化时使用；当前生产前景使用高保真 PNG。
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`：相同构图和安全区的单色版本，锁孔通过 `evenOdd` 保留透明挖空。
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`：Android 8.0+ 自适应图标声明。
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`：圆形启动器的自适应图标声明。
- `app/src/main/res/mipmap-anydpi/`：项目现有的兼容声明，继续引用相同资源。

单色层和备用矢量前景使用 108×108 的 viewport；彩色前景位图按相同安全区比例输出，避免不同启动器遮罩裁掉挂钩或锁孔。背景位图只是柔化的环境层，不是 Play 512 图的直接复用。Android 12+ 启动画面引用 `@mipmap/ic_launcher`，因此启动图标与桌面图标使用同一套 adaptive 资源。

这与 Android 官方 adaptive icon 约定一致：背景与前景分层、108×108dp 画布、中心安全区以及可选的 monochrome 层均由资源明确表达。

## 设计/商店导出

- `daily-record-icon-1254.png`：用户提供的 1254×1254 原始方形图稿，作为不可变设计源稿保存；不直接作为商店图标或普通 launcher PNG 复用。
- `daily-record-icon-google-play-512.png`：Google Play Console 上传文件，512×512、32-bit RGBA PNG、全不透明 alpha、sRGB、357,664 bytes（低于 1 MB），不添加圆角、文字或徽章。

校验值：

- 原始源稿 SHA-256：`B52FC5FF1FB7316D6018E09FFC51CBA07CD948619FF3136F51AF1169F9302088`
- Play 512 导出 SHA-256：`C9DC5F90B01F05BFEE4E5298F7BC5DA8A982EFDE5B2A91DF7478377666D625F8`
- Android 透明前景 SHA-256：`B492D8B04A3ACD1547590F19563FD3CA1A48180D03C44863ED7AE15CA67B4F7B`
- Android 柔化背景 SHA-256：`7CB680D3D9A4474BBC871FA8EA676E68C52A3AEA52AEA5D85EEEC1A74F826DE8`

Play 导出只做等比例缩小，保留原稿的全方形画布，让 Play 自动应用圆角遮罩和阴影。Android 运行时使用独立的环境背景位图和透明前景，前景、单色和 adaptive 声明仍是独立资源；没有预先圆角，也没有额外平台徽章。

## 官方依据

- [Android adaptive icon design](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)
- [Google Play icon design specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications)
- [Google Play Console preview asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en)

## 本轮落地优化

1. 从用户提供的高保真图稿提取透明前景，保留霓虹玻璃、光晕、人物、交织双心和黑色锁孔，不再用低保真的线稿替代成品。
2. 使用独立环境背景位图、透明前景 PNG、monochrome 矢量和 adaptive XML，满足 Android 8.0+ 分层资源结构及主题化入口要求，同时避免把 Play 512 普通图标直接当 launcher 图标。
3. 保留用户原图与 Play 512 导出作为可复核的发布素材；不对源稿预先做圆角、外部阴影或平台徽章。

后续如需继续打磨，优先检查：不同 OEM 遮罩下的安全区实机对比、单色图标在浅色/深色壁纸上的对比度，以及 48dp/32dp 小尺寸下双心与锁孔的间距。
