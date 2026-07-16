# 开发、测试与发布

## 开发环境

当前工程基线：

- Android Studio 稳定版。
- IDE 内置 JDK 与 Android Gradle Plugin 兼容。
- Android SDK、Platform Tools 和 Build Tools。
- 至少一个模拟器或 Android 真机。
- Git 和 GitHub 访问。
- 确认 BIOS 虚拟化；资源不足时优先使用真机。

`minSdk 26`、`namespace` 和 `applicationId` 已由 ADR-009 接受。`compileSdk` 和 `targetSdk` 的升级通过独立工程 PR 验证，不由产品文档临时改变。

## 本地命令基线

README 与 CI 应持续维护以下准确命令：

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
```

Windows 可使用 `gradlew.bat`。

## 测试层级

### 单元测试

- 日期范围和周起始。
- 总次数、总天数和周期聚合。
- ViewModel 状态变化。
- 同步冲突和退避策略。

### 数据库测试

- DAO 查询。
- 唯一约束。
- 每个 Room 版本迁移到最新版本。
- 软删除和归档后的统计。

### UI 测试

- 创建活动。
- 选择日期并增加次数。
- 完成/未完成/跳过。
- 月份切换和筛选。
- 统计表和空状态。

### 端到端测试

- 游客使用后登录合并。
- 两台设备离线修改后同步。
- 卸载/重装和登录恢复。
- 网络中断、令牌过期和服务端拒绝。

## CI

第一阶段 GitHub Actions 执行：

- Gradle Wrapper 校验。
- Debug 编译。
- 单元测试。
- Android lint。
- 格式或静态分析。

发布阶段增加签名构建、依赖安全检查和测试报告。Fork PR 不应获得生产 Secrets。

## 发布

- Debug、staging 和 release 环境分离。
- 包名、后端 URL 和功能开关通过构建配置注入。
- Release 使用受保护的签名凭据。
- 生成 AAB 前记录版本号和变更日志。
- 发布候选必须从干净 checkout 可复现构建。

## Definition of Done

- 验收条件满足。
- 自动化测试覆盖关键规则。
- lint 和构建通过。
- Room 变化具备迁移测试。
- 文档更新。
- 无密钥、真实数据或无关文件。
- PR 描述包含验证结果和剩余风险。

