# 文档目录与 AI 阅读索引

状态：`current — authoritative documentation registry`
最后复核：2026-08-16

本文是公共仓库中全部 Git 跟踪 Markdown 文档的逐文件索引，也是 AI 开始工作前判断“先读什么、哪些只是历史证据”的唯一入口。当前仓库没有被 Git 跟踪的 `.doc`、`.docx` 或 `.markdown` 文件；如果以后新增或删除任意文档，必须在同一提交中更新本文，`scripts/documentation-catalog.test.mjs` 会检查目录是否完整。当前 README 运行截图及其复现边界单独登记在产品资产目录中，历史审计截图不再冒充首页当前画面。

## 字段定义

- **状态**：`当前必读` 表示会约束日常开发；`当前契约` 表示已经实现但仍约束产品/设计；`当前参考` 表示辅助事实；`当前索引` 表示导航入口；`历史证据` 表示只记录过去；`发布历史` 表示不可改写的某个版本记录；`生成物说明` 表示由工具产生的结构快照说明；`协作模板` 表示 GitHub 工作流模板。
- **优先级**：`P0` 会影响所有工作，冲突时必须先处理；`P1` 影响代码、数据、测试、发布或安全；`P2` 只在对应范围内重要；`P3` 只用于追溯历史。
- **必要程度**：`必需`、`建议`、`按范围`、`留档`。`按范围` 不表示文档过时，而是只有进入对应工作范围才需要阅读。
- **必看**：人类参与者是否应在对应工作前阅读；**AI 必看**：AI 是否必须在对应工作前阅读。`是`、`按范围`、`否` 的含义与必要程度一致。

## AI 开工最小阅读顺序

1. 本目录，确认任务涉及的当前文档和历史文档边界。
2. [`AGENTS.md`](../AGENTS.md)、[`AI_COLLABORATION.md`](../AI_COLLABORATION.md)、[`README.md`](../README.md) 和 [`docs/README.md`](README.md)，确认仓库纪律、公共仓库边界和文档权威层级。
3. [`PRODUCT.md`](PRODUCT.md)、[`ARCHITECTURE.md`](ARCHITECTURE.md)、[`DATA_MODEL.md`](DATA_MODEL.md)、[`DECISIONS.md`](DECISIONS.md)；统计、同步、测试或发布任务再分别加入 [`STATISTICS.md`](STATISTICS.md)、[`SYNC_AND_PRIVACY.md`](SYNC_AND_PRIVACY.md)、[`TESTING.md`](TESTING.md)、[`RELEASE.md`](RELEASE.md)。
4. 按任务范围阅读表中标记为“AI 必看 = 按范围”的文档；`历史证据` 默认不读，只有需要核对当时实现、截图、提交或发布链时才读。

## 逐文件目录

| 路径 | 状态 | 优先级 | 必要程度 | 必看 | AI 必看 | 主要用途 |
| --- | --- | --- | --- | --- | --- | --- |
| `docs/DOCUMENTATION_CATALOG.md` | 当前必读 | P0 | 必需 | 是 | 是 | 全部文档的状态、优先级和阅读路由；新增/删除文档时同步更新 |
| `AGENTS.md` | 当前必读 | P0 | 必需 | 是 | 是 | 开工检查、产品边界、测试和分支纪律 |
| `AI_COLLABORATION.md` | 当前必读 | P0 | 必需 | 是 | 是 | AI 分支、提交、公共仓库和保留原始提交历史的约定 |
| `README.md` | 当前索引 | P0 | 必需 | 是 | 是 | 项目定位、当前版本、技术栈和用户入口 |
| `docs/README.md` | 当前索引 | P0 | 必需 | 是 | 是 | 当前事实、工程运维和历史证据的导航与冲突优先级 |
| `CONTRIBUTING.md` | 当前必读 | P1 | 必需 | 是 | 是 | Issue、分支、PR、测试和隐私边界 |
| `.github/pull_request_template.md` | 协作模板 | P1 | 按范围 | 按范围 | 按范围 | 创建 PR 时填写变更原因和验证证据 |
| `PRIVACY.md` | 当前必读 | P1 | 按范围 | 按范围 | 按范围 | 面向使用者的本机、账号、云端和删除隐私说明 |
| `SECURITY.md` | 当前必读 | P1 | 按范围 | 按范围 | 按范围 | 漏洞报告、敏感文件边界和安全基线 |
| `docs/PRODUCT.md` | 当前必读 | P0 | 必需 | 是 | 是 | 当前产品范围、两个模块语义和不做什么 |
| `docs/ARCHITECTURE.md` | 当前必读 | P0 | 必需 | 是 | 是 | 分层、数据流、同步边界和模块隔离 |
| `docs/DATA_MODEL.md` | 当前必读 | P0 | 必需 | 是 | 是 | Room v5、Firestore 字段、状态和迁移事实 |
| `docs/DECISIONS.md` | 当前必读 | P0 | 必需 | 是 | 是 | 已接受 ADR 和不可随意推翻的技术/产品决策 |
| `docs/STATISTICS.md` | 当前必读 | P1 | 必需 | 是 | 是 | 周/月/年/全部历史统计的唯一口径 |
| `docs/SYNC_AND_PRIVACY.md` | 当前必读 | P1 | 按范围 | 按范围 | 按范围 | 本地优先、账号隔离、同步冲突和删除语义 |
| `docs/UI_UX.md` | 当前契约 | P1 | 按范围 | 按范围 | 按范围 | 当前 Compose 页面、状态、文案和无障碍规则 |
| `docs/DEVELOPMENT.md` | 当前必读 | P1 | 必需 | 是 | 是 | 本机环境、命令、配置边界和日常开发流程 |
| `docs/TESTING.md` | 当前必读 | P1 | 必需 | 是 | 是 | 按影响范围选择测试及小版本完整门禁 |
| `docs/FIREBASE_SETUP.md` | 当前参考 | P1 | 按范围 | 按范围 | 按范围 | Firebase 资源、Rules/Functions 发布和本地配置 |
| `docs/RELEASE.md` | 当前必读 | P1 | 按范围 | 按范围 | 按范围 | 版本号、稳定签名、tag、Release 和恢复边界 |
| `docs/ROADMAP.md` | 当前索引 | P1 | 建议 | 按范围 | 按范围 | 已完成交付和当前维护方向；不作为旧阶段执行入口 |
| `docs/REPOSITORY_HYGIENE.md` | 当前必读 | P1 | 必需 | 是 | 是 | 公共/私有边界、文档生命周期、分支和生成物策略 |
| `docs/RESOURCE_CLEANUP.md` | 历史证据 | P2 | 留档 | 否 | 否 | 资源清理范围、保留理由和可恢复证据 |
| `docs/design/README.md` | 当前索引 | P2 | 按范围 | 按范围 | 按范围 | 设计资产入口与运行时资源边界 |
| `docs/design/icon-source/README.md` | 当前参考 | P2 | 按范围 | 按范围 | 按范围 | 自适应图标源文件和保留的 canonical 导出 |
| `app/schemas/README.md` | 生成物说明 | P1 | 按范围 | 按范围 | 按范围 | Room schema 快照的来源、用途和禁止手工修改规则 |
| `docs/product/README.md` | 当前索引 | P1 | 建议 | 按范围 | 按范围 | 产品、设计、Goal 和交付材料导航 |
| `docs/product/QUIET_PRIVATE_JOURNAL_GOALS.md` | 当前契约（已归档执行计划） | P1 | 按范围 | 按范围 | 按范围 | UI v2 已关闭阶段、视觉目标和未来变更入口 |
| `docs/product/IMPLEMENTATION_READINESS.md` | 当前契约 | P1 | 按范围 | 按范围 | 按范围 | 当前实现、发布和质量门槛 |
| `docs/product/PRODUCT_BACKLOG.md` | 当前参考 | P1 | 按范围 | 按范围 | 按范围 | 已完成事项与持续观察项，不是旧阶段启动器 |
| `docs/product/USER_STORIES_AND_ACCEPTANCE.md` | 当前契约 | P1 | 按范围 | 按范围 | 按范围 | 用户行为和可验收条件 |
| `docs/product/BRIGHT_GLASS_THEME.md` | 当前契约 | P1 | 按范围 | 按范围 | 按范围 | 当前双模块玻璃主题的实现基线 |
| `docs/product/MONTH_STATISTICS_REDESIGN.md` | 当前契约 | P1 | 按范围 | 按范围 | 按范围 | 月统计逐日脉冲图、分布和极值规则 |
| `docs/product/SETTINGS_HUB_GOAL.md` | 当前参考 | P2 | 按范围 | 按范围 | 按范围 | 已实现设置中心的目标、范围和验收 |
| `docs/product/I18N_GOAL.md` | 当前契约 | P1 | 按范围 | 按范围 | 按范围 | 双语适配（中文默认 + English）的目标、机制、简写策略与阶段证据 |
| `docs/product/UI_REDESIGN_RESEARCH_BASELINE.md` | 当前参考 | P2 | 按范围 | 按范围 | 按范围 | 已接受的竞品研究和设计取舍 |
| `docs/product/RESEARCH_OPEN_SOURCE.md` | 当前参考 | P2 | 按范围 | 按范围 | 按范围 | 开源借鉴、许可证和不引入依赖的边界 |
| `docs/product/DAILY_USE_FEEDBACK.md` | 当前参考 | P2 | 按范围 | 按范围 | 按范围 | 日常使用反馈格式和隐私脱敏要求 |
| `docs/product/GOAL_SYNC_PRIVACY_HARDENING_AUDIT.md` | 历史证据 | P3 | 留档 | 否 | 否 | 早期同步/隐私风险拆分与历史 Issue 背景 |
| `docs/product/RUNTIME_UX_AUDIT.md` | 历史证据 | P3 | 留档 | 否 | 否 | 2026-07-19 运行时 UX 和统计验收证据 |
| `docs/product/FIGMA_DESIGN_SYSTEM.md` | 历史证据 | P3 | 留档 | 否 | 否 | 已被双模块 UI v2 取代的旧 Figma 设计系统 |
| `docs/product/HAND_BREW_REFACTOR_LOG.md` | 历史证据 | P3 | 留档 | 否 | 否 | 已完成的早期重构、提交和发布过程记录 |
| `docs/product/assets/daily-record-icon/README.md` | 当前参考 | P2 | 按范围 | 按范围 | 按范围 | 图标资源交接入口和 canonical 来源 |
| `docs/product/assets/readme/README.md` | 当前参考 | P1 | 按范围 | 按范围 | 按范围 | 从 API 34 测试模拟器采集的当前 README 运行截图、数据隐私和复现边界 |
| `docs/product/design/quiet-private-journal-v2/README.md` | 当前契约 | P1 | 按范围 | 按范围 | 按范围 | 当前日历、记录页和统计页的视觉目标 |
| `docs/product/design/record-details-v1/README.md` | 当前参考 | P2 | 按范围 | 按范围 | 按范围 | 逐次详情设计与运行时实现边界 |
| `docs/product/audit/README.md` | 当前索引 | P1 | 按范围 | 按范围 | 按范围 | 所有审计目录的历史状态和阅读路由 |
| `docs/product/audit/2026-08-15-main-code-audit/README.md` | 历史证据（当前最新审计） | P1 | 按范围 | 按范围 | 按范围 | 最近一次安全、性能、生命周期和测试审计结果 |
| `docs/product/audit/2026-08-11-issues-123-145/README.md` | 历史证据 | P2 | 按范围 | 按范围 | 按范围 | Issues #123–#145 的第三轮审计和验证 |
| `docs/product/audit/2026-08-09-record-details-stage4-stage5/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | 记录详情 Stage 4–5 当时的验收和 Draft PR 证据 |
| `docs/product/audit/2026-08-03-stage5-stage6-release/README.md` | 发布历史 | P3 | 留档 | 否 | 否 | beta.2 的最终测试、合并、签名和发布链 |
| `docs/product/audit/2026-08-02-month-statistics/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | 月统计最终方案当时的模型、设备和视觉验收 |
| `docs/product/audit/2026-08-02-period-glass-tabs/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | 统计周期玻璃控件当时的设计和运行证据 |
| `docs/product/audit/2026-08-02-year-line-chart/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | 年度折线面积图当时的验收证据 |
| `docs/product/audit/2026-08-02-boundary-fixes/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | 统计与日历边界中间方案的修复证据 |
| `docs/product/audit/2026-08-03-copy-audit/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | 中文文案集中化和运行时文案审计 |
| `docs/product/audit/2026-08-01-ui-v2-stage5/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | Stage 5 候选 APK 集成 QA |
| `docs/product/audit/2026-08-01-ui-v2-stage4/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | Stage 4 中间统计布局和验收暂停点 |
| `docs/product/audit/2026-08-01-ui-v2-stage3/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | Stage 3 记录页实现和验收范围 |
| `docs/product/audit/2026-07-30-ui-v2-stage2-follow-up/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | Stage 2 用户反馈修正和视觉证据 |
| `docs/product/audit/2026-07-30-ui-v2-stage2/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | Stage 2 初次日历验收证据 |
| `docs/product/audit/2026-07-30-ui-v2-stage1/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | Stage 1 Token 和共享外壳验收 |
| `docs/product/audit/2026-07-30-ui-v2-stage1/DESIGN_QA.md` | 历史证据 | P3 | 留档 | 否 | 否 | Stage 1 设计图与运行截图对照附录 |
| `docs/product/audit/2026-07-28-consistency-hardening/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | 局部一致性、扩展边界和安全加固证据 |
| `docs/product/audit/2026-07-27-quick-runtime/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | 快速真实运行和冷启动检查 |
| `docs/product/audit/2026-07-22-password-reset-sync/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | 找回密码、同步和 200% 字体运行证据 |
| `docs/product/audit/2026-07-22-native-ui/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | 应用内 UI 一致性和弹窗反馈审计 |
| `docs/product/audit/2026-07-22-calendar-statistics/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | 月历、统计和日期边界审计 |
| `docs/product/audit/2026-07-19-deep-ux/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | 早期仅自慰版本深度 UX 审计 |
| `docs/product/audit/2026-07-19-deep-ux/after/360x640/README.md` | 历史证据 | P3 | 留档 | 否 | 否 | 360×640dp 视觉证据附录 |
| `docs/releases/v1.0.0-beta.3.md` | 发布历史（当前版本） | P1 | 按范围 | 按范围 | 按范围 | 第三次 Beta 的内容、升级和验证说明 |
| `docs/releases/v1.0.0-beta.2.md` | 发布历史 | P3 | 留档 | 否 | 否 | 第二次 Beta 的不可改写发布说明 |
| `docs/releases/v1.0.0-beta.1.md` | 发布历史 | P3 | 留档 | 否 | 否 | 首次 Beta 的不可改写发布说明 |

## 维护要求

- 当前文档只能有一个事实入口；若代码、测试和当前文档冲突，先按 `docs/README.md` 的优先级处理，不在历史审计页修正当前状态。
- 历史文档可以补充“已归档/被哪个版本取代”的状态标记，但不得改写当时的测试数量、截图、分支、提交和失败事实。
- 删除文档前必须先检查本目录、文档链接、脚本和 Git 历史用途；若它仍是发布、迁移或审计证据，应保留并标为历史，而不是删除。
- 公共仓库不记录 `local.properties`、SDK/cache、`google-services.json`、签名材料、真实数据或机器日志；这些文件的本机存在不等于应进入版本控制。
