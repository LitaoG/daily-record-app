# Daily Record App

一个面向 Android 的可扩展日历与活动记录应用。第一阶段用于记录手冲次数、手冲天数和健身天数，并提供周、月、年统计；未来可以在不重构核心数据层的前提下扩展到学习、喝水、睡眠、跑步等活动。

> 项目已进入 Android 工程骨架阶段，当前优先完成可扩展的本地 MVP。

## 核心目标

- 在日历上用不同颜色展示不同活动。
- 快速记录某天是否完成以及完成次数。
- 分别计算总次数、总活动天数，以及周、月、年维度统计。
- 支持离线记录，登录后可以跨设备同步。
- 默认重视隐私，不在公开仓库提交用户数据或服务端密钥。
- 用通用活动模型承载新功能，降低未来扩展成本。

## 计划中的主要页面

1. 日历：查看月份、日期颜色和当日记录。
2. 快速记录：完成、未完成、跳过、增加或调整次数。
3. 统计：总计、周表、月表、年表、趋势图和热力图。
4. 活动管理：创建、编辑、排序、归档活动。
5. 账户与设置：登录、同步、隐私锁、导入导出和主题设置。

## 技术方向

- Kotlin
- Jetpack Compose + Material 3
- 单 Activity + Navigation
- ViewModel、Kotlin Coroutines、Flow、单向数据流
- Room 本地数据库
- WorkManager 同步队列
- 后端通过接口隔离，候选为 Supabase/PostgreSQL 或自建服务
- GitHub Actions 自动执行构建、静态检查和测试

## 项目文档

- [产品发现 v1](docs/product/README.md)
- [开源调研](docs/product/RESEARCH_OPEN_SOURCE.md)
- [用户故事与验收](docs/product/USER_STORIES_AND_ACCEPTANCE.md)
- [信息架构](docs/product/INFORMATION_ARCHITECTURE.md)
- [KPI 框架](docs/product/KPI_FRAMEWORK.md)
- [视觉原型 v1](docs/product/PROTOTYPE_V1.md)
- [可扩展日历标记](docs/product/SCALABLE_CALENDAR_MARKERS.md)
- [产品交付 Backlog](docs/product/PRODUCT_BACKLOG.md)
- [开发准备度基线](docs/product/IMPLEMENTATION_READINESS.md)
- [产品需求](docs/PRODUCT.md)
- [版本路线图](docs/ROADMAP.md)
- [系统架构](docs/ARCHITECTURE.md)
- [数据模型](docs/DATA_MODEL.md)
- [统计口径](docs/STATISTICS.md)
- [同步与隐私](docs/SYNC_AND_PRIVACY.md)
- [界面与交互](docs/UI_UX.md)
- [开发与测试](docs/DEVELOPMENT.md)
- [架构决策记录](docs/DECISIONS.md)

## 当前状态

- [x] 建立公开仓库
- [x] 选定 Apache License 2.0
- [x] 建立产品和架构基线文档
- [x] 确认技术项目名、包名和最低 Android 版本
- [x] 检查本机 Android Studio、SDK、JDK 和测试设备
- [x] 创建 Android 工程骨架
- [x] 完成 Product Discovery v1、开源调研、信息架构和 KPI 基线
- [x] 创建 FigJam 产品规划板与核心记录流程
- [x] 选择暖色纸感日志方向并完成五张视觉原型
- [x] 将手冲图标统一为飞机，并定义 2/4/8+ 活动标记规则
- [ ] 将仓库视觉稿迁移为 Figma 原生可编辑组件（待 Starter MCP 额度恢复）
- [ ] 确认正式产品名称和云端部署区域
- [ ] 实现本地 MVP
- [ ] 实现统计
- [ ] 实现账户与同步
- [ ] 内测与发布

## 开源与安全

本项目使用 [Apache License 2.0](LICENSE)。贡献前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。安全问题请遵循 [SECURITY.md](SECURITY.md)，不要在公开 Issue 中提交账号、行为记录、日志、数据库文件或密钥。

