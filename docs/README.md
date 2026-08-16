# 私密日历文档中心

最后复核：2026-08-16

仓库级协作规则另见：[开始工作前与完成门槛](../AGENTS.md)、[AI 协作约定](../AI_COLLABORATION.md)、[贡献指南](../CONTRIBUTING.md)。每次开始工作前先看[文档目录与 AI 阅读索引](DOCUMENTATION_CATALOG.md)。

本页用于区分当前有效规则、工程运维说明和历史审计证据。发生冲突时，优先级依次为：当前代码与测试、架构决策记录、当前事实文档、历史记录。

当前公共事实来源是 `LitaoG/daily-record-app` 的 `main`；Stage 0–6 和 `v1.0.0-beta.3` 已完成，历史阶段文档只用于追溯当时的截图与验证，不构成新的待办。2026-08-15 的 `main` 代码、安全、性能、生命周期与文档审计见[审计报告](product/audit/2026-08-15-main-code-audit/README.md)；后续合并后仍以 GitHub `main` 的最新提交为准。

## 当前事实文档

| 文档 | 负责回答 |
|---|---|
| [文档目录与 AI 阅读索引](DOCUMENTATION_CATALOG.md) | 每个文档的状态、优先级、必要程度和阅读时机 |
| [产品契约](PRODUCT.md) | 当前产品做什么、不做什么、核心验收规则 |
| [界面与交互](UI_UX.md) | 当前页面、状态、反馈和无障碍规则 |
| [架构](ARCHITECTURE.md) | 数据流、包边界、同步和未来模块演进方式 |
| [数据模型](DATA_MODEL.md) | Room v5 双模块字段、详情、状态推导和迁移 |
| [统计口径](STATISTICS.md) | 周/月/年/全部历史的唯一计算规则 |
| [同步与隐私](SYNC_AND_PRIVACY.md) | 本地优先、冲突、删除和云端数据边界 |
| [架构决策](DECISIONS.md) | 已接受的长期技术与产品决策 |
| [路线图](ROADMAP.md) | 已完成阶段和持续维护方向 |

当前公开发布：[`v1.0.0-beta.3`](releases/v1.0.0-beta.3.md)，GitHub 下载入口见 [`RELEASE.md`](RELEASE.md)。beta.1、beta.2 及各阶段发布审计只作为不可改写的历史记录。

## 开发与运维

| 文档 | 负责回答 |
|---|---|
| [开发与测试](DEVELOPMENT.md) | 环境、命令、设备安全和完成门槛 |
| [测试策略与执行矩阵](TESTING.md) | 每类代码改动何时跑定向测试、何时在小版本末尾跑一次全量验证 |
| [Firebase 配置与运维](FIREBASE_SETUP.md) | 生产资源、本地配置、规则发布和网络限制 |
| [签名与 GitHub Release](RELEASE.md) | 版本、稳定签名、Secrets、发布和灾难恢复 |
| [隐私说明](../PRIVACY.md) | 面向使用者的数据处理说明 |
| [安全策略](../SECURITY.md) | 漏洞报告、敏感材料和安全基线 |
| [参与贡献](../CONTRIBUTING.md) | 分支、代码、测试和隐私要求 |
| [仓库维护与文档生命周期](REPOSITORY_HYGIENE.md) | 公共/私有边界、分支清理、文档分类和生成物策略 |
| [资源清理记录](RESOURCE_CLEANUP.md) | 当前运行时资源、设计源、历史证据与本次删除范围 |

## 产品协作资料

- [产品交付索引](product/README.md)
- [审计与验证证据索引](product/audit/README.md)
- [设计资产索引](design/README.md)
- [UI v2 分阶段执行计划](product/QUIET_PRIVATE_JOURNAL_GOALS.md)
- [已确认的 UI v2 设计基线与三张高保真图](product/design/quiet-private-journal-v2/README.md)
- [月统计改版契约：每日次数、次数分布与单日极值](product/MONTH_STATISTICS_REDESIGN.md)
- [UI 视觉重构竞品研究与决策基线](product/UI_REDESIGN_RESEARCH_BASELINE.md)
- [明亮渐变玻璃主题 Goal 与当前契约](product/BRIGHT_GLASS_THEME.md)
- [设置中心 Goal](product/SETTINGS_HUB_GOAL.md)
- [当前 Backlog](product/PRODUCT_BACKLOG.md)
- [用户故事与验收](product/USER_STORIES_AND_ACCEPTANCE.md)
- [实现与发布准备度](product/IMPLEMENTATION_READINESS.md)
- [日常使用反馈](product/DAILY_USE_FEEDBACK.md)
- [2026-08-09 记录页逐次详情设计基线](product/design/record-details-v1/README.md)
- [开源借鉴边界](product/RESEARCH_OPEN_SOURCE.md)
- [每日记录图标资源说明](product/assets/daily-record-icon/README.md)

## 历史证据

以下内容用于解释某次迭代当时验证了什么，不应覆盖上面的当前规则：

- [2026-08-15 `main` 全面代码审计](product/audit/2026-08-15-main-code-audit/README.md)
- [2026-08-11 Issues #123–#145 清理审计](product/audit/2026-08-11-issues-123-145/README.md)
- [2026-08-11 运行时 UX 与统计验收](product/RUNTIME_UX_AUDIT.md)
- [2026-08-09 Issue #105 Stage 4–5 记录页验收](product/audit/2026-08-09-record-details-stage4-stage5/README.md)
- [2026-08-03 应用内中文文案审查](product/audit/2026-08-03-copy-audit/README.md)
- [2026-08-03 Stage 5/6 最终审查与 beta.2 发布](product/audit/2026-08-03-stage5-stage6-release/README.md)
- [2026-08-02 统计与日历边界修复](product/audit/2026-08-02-boundary-fixes/README.md)
- [2026-08-02 统计周期玻璃分段控件](product/audit/2026-08-02-period-glass-tabs/README.md)
- [2026-08-02 月统计改版验收](product/audit/2026-08-02-month-statistics/README.md)
- [2026-08-02 年度折线图验收](product/audit/2026-08-02-year-line-chart/README.md)
- [2026-08-01 Stage 4 月度热力图与年度月份分析](product/audit/2026-08-01-ui-v2-stage4/README.md)
- [2026-08-01 Stage 3 统计与日历基础视觉验收](product/audit/2026-08-01-ui-v2-stage3/README.md)
- [2026-08-01 Stage 5 集成 QA 与候选 APK](product/audit/2026-08-01-ui-v2-stage5/README.md)
- [2026-07-30 UI v2 Stage 2 用户反馈修正](product/audit/2026-07-30-ui-v2-stage2-follow-up/README.md)
- [2026-07-30 UI v2 Stage 2 方形次数热力日历](product/audit/2026-07-30-ui-v2-stage2/README.md)
- [2026-07-30 UI v2 Stage 1 设计 Token 与共享外壳](product/audit/2026-07-30-ui-v2-stage1/README.md)
- [2026-07-30 Stage 1 设计 QA 附录](product/audit/2026-07-30-ui-v2-stage1/DESIGN_QA.md)
- [历史 Figma Hand-brew-only v2 设计系统](product/FIGMA_DESIGN_SYSTEM.md)
- [重构与交付日志](product/HAND_BREW_REFACTOR_LOG.md)
- [2026-07-28 局部一致性、扩展边界与安全加固](product/audit/2026-07-28-consistency-hardening/README.md)
- [2026-07-27 快速运行审计](product/audit/2026-07-27-quick-runtime/README.md)
- [2026-07-22 找回密码与同步审计](product/audit/2026-07-22-password-reset-sync/README.md)
- [2026-07-22 应用内 UI 一致性审计](product/audit/2026-07-22-native-ui/README.md)
- [2026-07-22 月历与统计审计](product/audit/2026-07-22-calendar-statistics/README.md)
- [2026-07-19 深度 UX 审计](product/audit/2026-07-19-deep-ux/README.md)
- [同步、隐私与统计一致性加固 Goal](product/GOAL_SYNC_PRIVACY_HARDENING_AUDIT.md)
- [v1.0.0-beta.1 发布说明](releases/v1.0.0-beta.1.md)
- [v1.0.0-beta.2 发布说明](releases/v1.0.0-beta.2.md)

历史文档中的测试数量、截图日期、分支名和“下一步”只描述当时状态。当前版本、发布物和 CI 状态以仓库首页、GitHub Releases 与 `main` 为准。
