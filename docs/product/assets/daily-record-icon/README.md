# Daily Record 主图标

这组资源以参考图为唯一视觉基准，保留紫红交织渐变、日历、双挂钩、人物、交织双心和锁孔结构。

## Android 资源

- `app/src/main/res/drawable/ic_launcher_foreground.xml`：透明自适应前景，避免启动器对复杂图稿做二次位移。
- `app/src/main/res/drawable/ic_launcher_foreground_vector.xml`：保留的可编辑矢量源稿，供后续拆分图层或制作单色版本使用。
- `app/src/main/res/drawable/ic_launcher_background.xml`：高保真紫红渐变图稿入口。
- `app/src/main/res/drawable/ic_launcher_background_gradient.xml`：不依赖位图时使用的轻量渐变备用资源。
- `app/src/main/res/drawable-nodpi/ic_launcher_background_art.png`：运行时高保真图稿，内容与最终源图逐字节一致，不做圆角、裁剪、重绘或调色。
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`：保留日历、人物、双心和锁结构的单色版本，锁孔使用透明挖空。
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`：Android 8.0+ 自适应图标。
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`：圆形启动器的自适应图标。

Android 12+ 启动画面也引用同一份高保真图稿，避免启动时短暂显示旧的线框图标。

`mipmap-anydpi` 下同时保留兼容声明，两个 adaptive XML 都引用同一套前景、背景和单色资源。

## 营销导出

- `daily-record-icon-1254.png`：用户提供的 1254×1254 原始方形图稿，作为设计资产保存；运行时资源与它逐字节一致。

两份正式图稿的 SHA-256 均为 `B52FC5FF1FB7316D6018E09FFC51CBA07CD948619FF3136F51AF1169F9302088`，用于确认 APK 中没有被重新绘制或压缩成另一张图。

背景的渐变、光晕和玻璃质感适合用位图表现，因此营销图与运行时背景都使用用户提供的同一份 1254×1254 PNG；图标源文件没有预先做圆角，圆角形状交给 Android 启动器遮罩处理。透明前景只负责占位，避免 Android 启动器对复杂前景做额外缩放后出现“只剩细线、壁纸透出”的失真。矢量源稿仍保留在 `ic_launcher_foreground_vector.xml`，后续需要真正拆分前景/背景时可以从它继续拆分。

## 本轮落地优化

1. 以用户提供的完整 1254×1254 图稿作为 adaptive icon 背景，保证桌面实际看到紫红渐变、日历、挂钩、人物、双心和黑色锁孔，而不是稀疏的线框。
2. 透明前景不再重复绘制主体，避免启动器的自适应缩放把主体推到安全区外或让壁纸从日历内部透出。
3. 保留干净的矢量源稿和独立单色资源，方便后续制作真正的主题化前景，而不牺牲当前正式图标的一致性。

后续优先打磨：桌面不同形状遮罩的实机对比、单色图标在浅色/深色壁纸上的对比度、以及 48dp/32dp 小尺寸下双心与锁孔的间距。
