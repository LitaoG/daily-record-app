# AGENTS.md

## 开始工作前

1. 阅读 `README.md`、`docs/PRODUCT.md`、`docs/ARCHITECTURE.md`、`docs/DATA_MODEL.md` 和 `docs/DECISIONS.md`。
2. 检查当前分支与工作区，保护用户已有改动。
3. 当前版本只记录手冲和做爱两个固定、独立模块；不得引入活动表、活动类型、健身或自定义活动抽象。未来记录类型必须通过新 ADR 作为独立垂直模块加入。
4. 统计口径以 `docs/STATISTICS.md` 为唯一事实来源。
5. Room schema 变化必须提供显式迁移和设备测试。
6. UI v2 工作必须先读 `docs/product/QUIET_PRIVATE_JOURNAL_GOALS.md` 和 `docs/product/design/quiet-private-journal-v2/README.md`；只执行当前带 `status:in-progress` 的阶段，用户验收前不得提前进入下一阶段。

## 工程原则

- 本地优先：无网络也能完成全部核心功能。
- 单一事实来源：UI 只通过 Repository 读取 Room。
- 专用建模：每个模块每天最多一条 `HandBrewRecord` 或 `SexRecord`，不使用 `Activity` 或 `MeasurementType`；未来模块不得污染现有模型。
- 精确统计：次数和天数必须可从原始记录重算。
- 范围克制：未进入 `docs/PRODUCT.md` 的功能不实现、不提前抽象。
- 可访问：颜色不是唯一状态编码，关键控件具备 TalkBack 描述。

## Git 与完成标准

- 使用短生命周期 `agent/<description>` 分支；不强推，不改写共享历史。
- 每个提交只解决一个明确阶段，并同步更新对应 GitHub Issue、`docs/product/QUIET_PRIVATE_JOURNAL_GOALS.md` 和必要的当前事实文档。`docs/product/HAND_BREW_REFACTOR_LOG.md` 是历史证据，不再作为当前执行日志。
- 测试严格遵循 `docs/TESTING.md`：开发中只运行本次变更影响范围内的定向测试；一个连贯小版本的功能代码冻结后，在最终功能 head 上运行一次完整套件。后续仅文档或截图变化不会使 Room、同步、规则或设备测试结果失效，不得机械重复全量测试。
- 完成前必须有未失效的单元测试、Lint、Android 测试编译、Room 迁移测试和全文范围审计证据。
