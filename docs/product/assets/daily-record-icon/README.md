# Daily Record 主图标

这组资源以参考图为唯一视觉基准，保留紫红交织渐变、日历、双挂钩、人物、交织双心和锁孔结构。

## Android 资源

- `app/src/main/res/drawable/ic_launcher_foreground.xml`：自适应图标前景，使用 VectorDrawable，图形限制在安全区内。
- `app/src/main/res/drawable/ic_launcher_background.xml`：紫色到红色的渐变背景。
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`：保留日历、人物、双心和锁结构的单色版本，锁孔使用透明挖空。
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`：Android 8.0+ 自适应图标。
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`：圆形启动器的自适应图标。

`mipmap-anydpi` 下同时保留兼容声明，两个 adaptive XML 都引用同一套前景、背景和单色资源。

## 营销导出

- `daily-record-icon-1024.png`：1024×1024 方形营销主图，作为设计资产保存，不打进 APK。

背景的渐变和轻微霓虹质感适合用位图表现，因此营销图使用高分辨率 PNG；运行时前景使用 VectorDrawable，避免桌面缩放时出现锯齿和透明安全区漂移。

## 本轮落地优化

1. 将关键图形收进 adaptive icon 安全区，避免不同启动器遮掉挂钩、双心或锁孔。
2. 减少发光和噪点，使用干净的高对比线条，让小尺寸仍能辨认日历与锁。
3. 单色版本保留核心轮廓，并把锁孔做成透明挖空，适配 Android 主题化图标。

后续优先打磨：桌面不同形状遮罩的实机对比、单色图标在浅色/深色壁纸上的对比度、以及 48dp/32dp 小尺寸下双心与锁孔的间距。
