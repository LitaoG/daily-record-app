# 私密日历产品交付索引

最后复核：2026-11-05

完整工程文档请先看[文档中心](../README.md)。本页只索引产品、设计和交付资料。

公开 Release 仍为 `v1.0.0-beta.3`；当前公共 `main` 在该版本之后继续维护 SDK、生命周期、Functions 运行时和 CI 门禁。首页使用的最新运行时画面集中在[README 运行截图](assets/readme/README.md)。

## 当前产品资料

- [产品契约](../PRODUCT.md)
- [界面与交互](../UI_UX.md)（含视觉材质与令牌）
- [统计口径](../STATISTICS.md)
- [UI v2 分阶段执行计划](QUIET_PRIVATE_JOURNAL_GOALS.md)
- [UI 视觉重构竞品研究与决策基线](UI_REDESIGN_RESEARCH_BASELINE.md)
- [设置中心 Goal](SETTINGS_HUB_GOAL.md)
- [双语适配 Goal（i18n：中文默认 + English）](I18N_GOAL.md)
- [月统计改版契约：每日次数、次数分布与单日极值](MONTH_STATISTICS_REDESIGN.md)
- [实现与发布准备度](IMPLEMENTATION_READINESS.md)
- [当前 README 运行截图与复现边界](assets/readme/README.md)
- [每日记录图标资源说明](assets/daily-record-icon/README.md)
- [产品 Backlog](PRODUCT_BACKLOG.md)
- [用户故事与验收](USER_STORIES_AND_ACCEPTANCE.md)
- [仓库维护与文档生命周期](../REPOSITORY_HYGIENE.md)
- [开源借鉴边界](RESEARCH_OPEN_SOURCE.md)
- [隐私说明](../../PRIVACY.md)
- [签名与 GitHub Release](../RELEASE.md)
- [当前发布说明：v1.0.0-beta.3](../releases/v1.0.0-beta.3.md)

后续实现只能从新的 Issue/Goal 和当前事实文档进入。Stage 1 至 Stage 6 已完成，当前公开发布为 `v1.0.0-beta.3`；当前运行行为仍以代码与事实文档为准，真人日常反馈只通过后续 Issue 进入定向修复。旧 beta.1、beta.2 发布说明只用于追溯，不是当前执行入口。

## 历史交付证据

- [2026-08 性能与测试加固日志](OPTIMIZATION_2026-08_LOG.md)
- [交付与验证日志](HAND_BREW_REFACTOR_LOG.md)
- [早期运行时 UX 与统计验收](RUNTIME_UX_AUDIT.md)
- [同步、隐私与统计一致性加固 Goal](GOAL_SYNC_PRIVACY_HARDENING_AUDIT.md)
- [历史 Figma 设计文件](https://www.figma.com/design/PMtsNNL81BHl9HyJYhjbdw)
- [历史 Canva 产品概览](https://www.canva.com/d/In7LZUcRTdAXbFU)

2026-07 至 2026-09 的阶段审计目录、UI v2 设计稿与截图、明亮玻璃主题草案、日常使用反馈草案和旧 Figma 设计系统说明已于 2026-11 从仓库移除；仅自慰 v2 截图包更早已在 2026-08-11 移除。仍然有效的规则已并入当前事实文档，原始材料可从 Git 历史恢复（见[仓库维护与文档生命周期](../REPOSITORY_HYGIENE.md)）。

这些材料记录特定日期的截图、测试数量和阶段结论，不代表当前待办。旧“通用活动记录器”产品包已从当前文件树移除；未来记录模块也不会恢复旧通用活动模型。
