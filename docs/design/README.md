# 设计资产索引

最后复核：2026-11-05

## 当前设计入口

- [界面与交互](../UI_UX.md)：当前日历、记录页和统计页的视觉令牌、状态色阶和交互规则。UI v2 高保真设计稿已于 2026-11 从仓库移除，需要追溯时查 Git 历史。
- [图标源文件](icon-source/README.md)：Android 自适应图标的 background、foreground、monochrome 分层 SVG/PNG，以及运行时轻量 WebP 层。

实现后的真实页面画面不放在设计源目录，统一见[当前 README 运行截图](../product/assets/readme/README.md)。截图只说明当前构建在测试模拟器中的实际页面，不能替代 `UI_UX.md` 的规则或自动化测试。

## 规则

- 设计图用于表达层级、材质和气质；日期拓扑、次数、未来状态、触控范围和 TalkBack 以代码与测试为准。
- 设计源可以保留高分辨率备份，但不把营销图或 Figma 导出大图复制到 APK 资源。
- 旧 Figma/Canva 仅自慰画板已从仓库移除，不是当前双模块产品的需求来源。
