# AGENTS.md

## 开始工作前

1. 阅读 `README.md`、`docs/PRODUCT.md`、`docs/ARCHITECTURE.md`、`docs/DATA_MODEL.md` 和 `docs/DECISIONS.md`。
2. 检查当前分支与工作区，保护用户已有改动。
3. 本产品只记录手冲；不得重新引入活动表、活动类型、健身或自定义活动抽象。
4. 统计口径以 `docs/STATISTICS.md` 为唯一事实来源。
5. Room schema 变化必须提供显式迁移和设备测试。

## 工程原则

- 本地优先：无网络也能完成全部核心功能。
- 单一事实来源：UI 只通过 Repository 读取 Room。
- 专用建模：每天最多一条 `HandBrewRecord`，不使用 `Activity` 或 `MeasurementType`。
- 精确统计：次数和天数必须可从原始记录重算。
- 范围克制：未进入 `docs/PRODUCT.md` 的功能不实现、不提前抽象。
- 可访问：颜色不是唯一状态编码，关键控件具备 TalkBack 描述。

## Git 与完成标准

- 使用短生命周期 `agent/<description>` 分支；不强推，不改写共享历史。
- 每个提交只解决一个明确阶段，并同步更新 `docs/product/HAND_BREW_REFACTOR_LOG.md`。
- 完成前必须通过单元测试、Lint、Android 测试编译、Room 迁移测试和全文范围审计。
