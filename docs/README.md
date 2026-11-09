# 私密日历文档中心

最后复核：2026-11-05

仓库级协作规则另见：[AI 强制入口](../AGENTS.md)、[贡献指南](../CONTRIBUTING.md)。每次开始工作前先看[文档目录与 AI 阅读索引](DOCUMENTATION_CATALOG.md)，按任务范围读[端到端工作流](WORKFLOW.md)与[工程规范](ENGINEERING_STANDARDS.md)。

本页用于区分当前有效规则、工程运维说明和历史审计证据。发生冲突时，优先级依次为：当前代码与测试、架构决策记录、当前事实文档、历史记录。

## 不确定先查哪里

对任何事实不确定时先查文档，不要先猜。查证顺序：本页与[文档目录](DOCUMENTATION_CATALOG.md) → 下表命中的那一份权威文档 → 代码、测试和实际运行结果 → 仍不确定就停下来问人。本表与 [`WORKFLOW.md`](WORKFLOW.md) §0.3 保持一致。

| 我不确定的事 | 去查的权威文档 |
|---|---|
| 自慰与做爱两个模块的边界、能不能加新功能 | [`PRODUCT.md`](PRODUCT.md) |
| 已接受的长期技术与产品决策（ADR） | [`DECISIONS.md`](DECISIONS.md) |
| Room 表结构、字段、迁移、聚合记录与详情的关系 | [`DATA_MODEL.md`](DATA_MODEL.md) |
| 周、月、年、全部历史的统计口径和图表规则 | [`STATISTICS.md`](STATISTICS.md) |
| 页面交互、颜色令牌、色阶、玻璃材质、无障碍 | [`UI_UX.md`](UI_UX.md) |
| 文案集中化、命名、中文标点禁令、注释写法 | [`ENGINEERING_STANDARDS.md`](ENGINEERING_STANDARDS.md) |
| 分层、ViewModel 与 Repository 的调用关系 | [`ARCHITECTURE.md`](ARCHITECTURE.md) |
| 云同步、离线行为、账号六阶段删除 | [`SYNC_AND_PRIVACY.md`](SYNC_AND_PRIVACY.md) |
| 改动后该跑哪些测试、完整门禁何时跑 | [`TESTING.md`](TESTING.md) |
| Gradle 命令、本机环境、SDK 版本 | [`DEVELOPMENT.md`](DEVELOPMENT.md) |
| 版本号、签名、tag 与 GitHub Release | [`RELEASE.md`](RELEASE.md) |
| Firebase 资源、Rules 与 Functions 部署 | [`FIREBASE_SETUP.md`](FIREBASE_SETUP.md) |
| 云端费用、配额、防刷与止损 | [`firebase-cost-and-quota-inventory.md`](operations/firebase-cost-and-quota-inventory.md)、[`firebase-abuse-monitoring-and-emergency-runbook.md`](operations/firebase-abuse-monitoring-and-emergency-runbook.md)、[`firebase-cost-safety-test-matrix.md`](operations/firebase-cost-safety-test-matrix.md) |
| 这个方向以前栽过什么坑（动手前必查） | [`AI-PITFALLS.md`](AI-PITFALLS.md) |
| 证据等级、汇报格式和停止条件（动手前必查） | [`AI-EVIDENCE-STANDARD.md`](AI-EVIDENCE-STANDARD.md) |
| 改码流程：分支、提交、PR、合并、清理（动手前必查） | [`WORKFLOW.md`](WORKFLOW.md) |
| Issue、PR 和贡献者需要遵守的约定 | [`CONTRIBUTING.md`](../CONTRIBUTING.md) |
| AI 总规则、红线和高风险操作 | [`AGENTS.md`](../AGENTS.md) |
| 某个文件算不算权威、归谁管 | [`DOCUMENTATION_CATALOG.md`](DOCUMENTATION_CATALOG.md)、[`README.md`](README.md) |
| 公共/私有边界、文档生命周期、生成物 | [`REPOSITORY_HYGIENE.md`](REPOSITORY_HYGIENE.md) |
| 面向使用者的隐私说明、漏洞报告 | [`PRIVACY.md`](../PRIVACY.md)（[English](../PRIVACY.en.md)）、[`SECURITY.md`](../SECURITY.md) |

当前公共事实来源是 `LitaoG/daily-record-app` 的 `main`；Stage 0–6 和 `v1.0.0-beta.3` 已完成，历史阶段文档只用于追溯当时的截图与验证，不构成新的待办。2026-08-15 曾对 `main` 做过一次代码、安全、性能、生命周期与文档审计，结论已进入当前文档和[踩坑清单](AI-PITFALLS.md)；其后的公共维护还包括 compileSdk 37、Node.js 22/Functions 2nd gen、`asia-east1` 区域固定、生命周期感知的 Flow 收集、双语适配，以及 PR/`main`/Release tag 的 API 34 connected gate。后续合并后仍以 GitHub `main` 的最新提交为准。

## 当前事实文档

| 文档 | 负责回答 |
|---|---|
| [文档目录与 AI 阅读索引](DOCUMENTATION_CATALOG.md) | 每个文档的状态、优先级、必要程度和阅读时机 |
| [端到端工程工作流](WORKFLOW.md) | 从查事实、定界到合并清理的唯一流程 |
| [工程规范](ENGINEERING_STANDARDS.md) | Kotlin/Compose 写法、分层、异步、Room、隐私与测试规范 |
| [产品契约](PRODUCT.md) | 当前产品做什么、不做什么、核心验收规则 |
| [界面与交互](UI_UX.md) | 当前页面、状态、反馈和无障碍规则 |
| [架构](ARCHITECTURE.md) | 数据流、包边界、同步和未来模块演进方式 |
| [数据模型](DATA_MODEL.md) | Room v6 双模块字段、详情、状态推导和迁移 |
| [统计口径](STATISTICS.md) | 周/月/年/全部历史的唯一计算规则 |
| [同步与隐私](SYNC_AND_PRIVACY.md) | 本地优先、冲突、删除和云端数据边界 |
| [架构决策](DECISIONS.md) | 已接受的长期技术与产品决策 |
| [路线图](ROADMAP.md) | 已完成阶段和持续维护方向 |
| [踩坑清单](AI-PITFALLS.md) | 已付出代价的问题、根因和防复发门禁 |
| [AI 证据标准](AI-EVIDENCE-STANDARD.md) | 三档主张、证据合格形式与冲突裁决、测试五态、评审收敛、停止条件与 A~E 交付分类 |

当前运行画面见[README 运行截图](product/assets/readme/README.md)。这些图片来自 API 34 测试模拟器的本机空数据，只作为当前运行时展示。

当前公开发布：[`v1.0.0-beta.3`](releases/v1.0.0-beta.3.md)，GitHub 下载入口见 [`RELEASE.md`](RELEASE.md)。beta.1、beta.2 发布说明只作为不可改写的历史记录。

## 开发与运维

| 文档 | 负责回答 |
|---|---|
| [开发与测试](DEVELOPMENT.md) | 环境、命令、设备安全和完成门槛 |
| [测试策略与执行矩阵](TESTING.md) | 每类代码改动何时跑定向测试、何时在小版本末尾跑一次全量验证 |
| [Firebase 配置与运维](FIREBASE_SETUP.md) | 生产资源、本地配置、规则发布和网络限制 |
| [签名与 GitHub Release](RELEASE.md) | 版本、稳定签名、Secrets、发布和灾难恢复 |
| [隐私说明](../PRIVACY.md)（[English](../PRIVACY.en.md)） | 面向使用者的数据处理说明 |
| [安全策略](../SECURITY.md) | 漏洞报告、敏感材料和安全基线 |
| [参与贡献](../CONTRIBUTING.md) | 分支、代码、测试和隐私要求 |
| [仓库维护与文档生命周期](REPOSITORY_HYGIENE.md) | 公共/私有边界、资源分层、分支清理、文档分类和生成物策略 |

## 产品协作资料

- [产品交付索引](product/README.md)
- [设计资产索引](design/README.md)
- [UI v2 分阶段执行计划](product/QUIET_PRIVATE_JOURNAL_GOALS.md)
- [月统计改版契约：每日次数、次数分布与单日极值](product/MONTH_STATISTICS_REDESIGN.md)
- [UI 视觉重构竞品研究与决策基线](product/UI_REDESIGN_RESEARCH_BASELINE.md)
- [流体玻璃与 Google Play 就绪 Goal（ADR-022）](product/LIQUID_GLASS_PLAY_READINESS_GOAL.md)
- [Google Play 上架就绪清单](product/PLAY_STORE_READINESS.md)
- [设置中心 Goal](product/SETTINGS_HUB_GOAL.md)
- [双语适配 Goal（i18n：中文默认 + English）](product/I18N_GOAL.md)
- [当前 Backlog](product/PRODUCT_BACKLOG.md)
- [用户故事与验收](product/USER_STORIES_AND_ACCEPTANCE.md)
- [实现与发布准备度](product/IMPLEMENTATION_READINESS.md)
- [开源借鉴边界](product/RESEARCH_OPEN_SOURCE.md)
- [每日记录图标资源说明](product/assets/daily-record-icon/README.md)
- [当前 README 运行截图与复现边界](product/assets/readme/README.md)

## 历史证据

以下内容用于解释某次迭代当时验证了什么，不应覆盖上面的当前规则：

- [2026-08 性能与测试加固日志](product/OPTIMIZATION_2026-08_LOG.md)
- [2026-08-11 运行时 UX 与统计验收](product/RUNTIME_UX_AUDIT.md)
- [重构与交付日志](product/HAND_BREW_REFACTOR_LOG.md)
- [同步、隐私与统计一致性加固 Goal](product/GOAL_SYNC_PRIVACY_HARDENING_AUDIT.md)
- [v1.0.0-beta.1 发布说明](releases/v1.0.0-beta.1.md)
- [v1.0.0-beta.2 发布说明](releases/v1.0.0-beta.2.md)

2026-07 至 2026-09 的阶段审计目录（`docs/product/audit/`）、UI v2 设计稿（`docs/product/design/`）、明亮玻璃主题草案、日常使用反馈政策和旧 Figma 设计系统已于 2026-11 从仓库移除；仍然有效的规则已并入 [`UI_UX.md`](UI_UX.md)、[`STATISTICS.md`](STATISTICS.md)、[`ENGINEERING_STANDARDS.md`](ENGINEERING_STANDARDS.md) 和 [`AI-PITFALLS.md`](AI-PITFALLS.md)。需要追溯原始证据时查 Git 历史，恢复方法见 [`REPOSITORY_HYGIENE.md`](REPOSITORY_HYGIENE.md)。

历史文档中的测试数量、截图日期、分支名和“下一步”只描述当时状态。当前版本、发布物和 CI 状态以仓库首页、GitHub Releases 与 `main` 为准。
