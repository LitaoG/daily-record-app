# 参与贡献

感谢参与 Daily Record App。当前项目优先保证数据正确、离线可用、隐私安全和未来可扩展。

## 工作流程

1. 先在 Issue 中说明需求或问题。
2. 从最新 `main` 创建功能分支。
3. 保持提交聚焦，避免混入无关格式化或重构。
4. 新功能补充测试和相关文档。
5. 创建 Pull Request，并填写模板中的验证结果。

## 分支与提交

- 功能：`feature/<name>`
- 修复：`fix/<name>`
- 文档：`docs/<name>`
- 自动化代理：`agent/<name>`

提交信息使用简短祈使句，例如：

- `Add calendar activity markers`
- `Define monthly statistics queries`
- `Fix week boundary calculation`

## 代码要求

- Kotlin 代码遵循官方 Kotlin 风格。
- Compose UI 使用状态提升和单向数据流。
- ViewModel 不持有 Activity、Fragment 或 View 引用。
- Repository 是 UI/领域层访问数据的唯一入口。
- 日期计算显式使用 `LocalDate`、时区和用户的周起始设置。
- 不允许用数据库清空重建代替正式迁移。

## 测试要求

- 统计公式、日期边界和同步冲突逻辑需要单元测试。
- Room schema 变化需要迁移测试。
- 关键用户流程需要 Compose UI 测试。
- PR 中列出已运行的命令和结果。

## 隐私与示例数据

测试和截图只能使用虚构数据。请勿提交：

- 真实行为记录、备注或账号信息。
- `local.properties`、`.env`、服务端密钥。
- 签名文件、密码、访问令牌。
- 用户数据库、备份文件或包含隐私的日志。

