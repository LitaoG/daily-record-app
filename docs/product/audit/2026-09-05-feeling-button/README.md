# 2026-09-05 写感受按钮对齐修复证据

状态：`current audit — #254`
最后更新：2026-09-05

来源：`agent/fix-feeling-button-align` 分支，API 34 模拟器 `Pixel_4_API_34`，Debug 构建。

## 背景

写感受按钮内笔图标 PNG 自带约 21% 透明边，32dp 框导致图标与文字间距过大、文字贴右。修复：PNG 裁边至笔形（512×512 → 332×192，紫/酒红两版）、图标框 24dp、光学左移 2dp。

## 截图

| 文件 | 说明 |
|---|---|
| `feeling-fixed.png` | 修复后记录详情行：笔贴左、与“写感受” balance |
| `feeling-compare.png` | 上旧（24dp 未裁边）下新（裁边+左移）同视口对比 |

采集：`adb exec-out screencap -p`，本机模式空数据 +1 次后展开详情。
