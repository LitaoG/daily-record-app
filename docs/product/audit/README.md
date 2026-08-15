# 审计与验证证据索引

最后复核：2026-08-15

本目录只保存某次实现、截图、设备验证或发布链的可追溯证据。它不是当前需求列表；发生冲突时，以代码、`docs/` 当前事实文档和最新公共 `main` 为准。

## 证据目录

| 日期/目录 | 主题 | 当前状态 |
|---|---|---|
| [2026-07-19 deep UX](2026-07-19-deep-ux/README.md) | 首轮深度 UX 与仅自慰基线 | 历史 |
| [2026-07-22 calendar/statistics](2026-07-22-calendar-statistics/README.md) | 日历与统计边界 | 历史 |
| [2026-07-22 native UI](2026-07-22-native-ui/README.md) | 应用内弹窗与系统 UI 边界 | 历史 |
| [2026-07-22 password reset/sync](2026-07-22-password-reset-sync/README.md) | 密码重置和同步回归 | 历史 |
| [2026-07-27 quick runtime](2026-07-27-quick-runtime/README.md) | 启动、返回、记录和统计快速运行 | 历史 |
| [2026-07-28 consistency hardening](2026-07-28-consistency-hardening/README.md) | 双模块一致性与安全加固 | 历史 |
| [2026-07-30 Stage 1](2026-07-30-ui-v2-stage1/README.md) | Token 与共享外壳 | 已纳入 beta.2 |
| [2026-07-30 Stage 2](2026-07-30-ui-v2-stage2/README.md) | 方形次数热力日历 | 已纳入 beta.2 |
| [2026-07-30 Stage 2 follow-up](2026-07-30-ui-v2-stage2-follow-up/README.md) | 日历反馈修正 | 已纳入 beta.2 |
| [2026-08-01 Stage 3](2026-08-01-ui-v2-stage3/README.md) | 日期记录页 | 已纳入 beta.2 |
| [2026-08-01 Stage 4](2026-08-01-ui-v2-stage4/README.md) | 早期月度/年度统计方案 | 历史，已被最终契约取代 |
| [2026-08-01 Stage 5](2026-08-01-ui-v2-stage5/README.md) | 候选 APK 集成 QA | 已纳入 beta.2 |
| [2026-08-02 boundary fixes](2026-08-02-boundary-fixes/README.md) | 日期、颜色和中间统计边界 | 历史，中间实现 |
| [2026-08-02 month statistics](2026-08-02-month-statistics/README.md) | 逐日脉冲图、分布和极值 | 已纳入 beta.2 |
| [2026-08-02 period glass tabs](2026-08-02-period-glass-tabs/README.md) | 统计周期玻璃控件 | 已纳入 beta.2 |
| [2026-08-02 year line chart](2026-08-02-year-line-chart/README.md) | 年度直线折线和一次性动画 | 已纳入 beta.2 |
| [2026-08-03 copy audit](2026-08-03-copy-audit/README.md) | 集中文案和中文字符串审查 | 已纳入 beta.2 |
| [2026-08-03 Stage 5/6 release](2026-08-03-stage5-stage6-release/README.md) | beta.2 发布、签名和私有镜像 | 发布完成 |
| [2026-08-09 Issue #105 Stage 4–5](2026-08-09-record-details-stage4-stage5/README.md) | 记录页无障碍、窄屏、恢复、跨设备与候选 APK | 待用户验收，PR #119 Draft |
| [2026-08-15 main code audit](2026-08-15-main-code-audit/README.md) | `main` 全面代码、安全、性能、生命周期与文档审计 | 以 `main` `82d4111` 为基线；本分支含认证状态修复 |

## 阅读规则

- “已纳入 beta.2”表示证据对应的实现已进入发布版本，不表示该目录仍有待验收任务。
- “历史，中间实现”表示后来被更好的实现或用户最终契约替代；不应从截图恢复旧布局或旧统计口径。
- 设备截图和 XML 只使用虚构数据；不要把真实账号、真实日期或个人记录追加到这里。
- 2026-08-15 的代码级审计以公共 `main` `82d4111` 为基线；更早的 PR/截图目录只描述当时状态，当前结论以最新审计、代码和 CI 为准。
