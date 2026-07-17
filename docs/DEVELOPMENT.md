# 开发、测试与发布

## 环境

- Android Studio 稳定版与内置 JDK
- Android SDK / Platform Tools
- `minSdk 26`，`compileSdk` 与 `targetSdk` 由工程配置管理
- 至少一台模拟器或真机

GitHub 暂时不可连接时，使用本地分支和提交保留完整历史；连接恢复后直接推送，不复制或压平提交。

## 验证命令

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebugAndroidTest --no-parallel
.\gradlew.bat connectedDebugAndroidTest --no-parallel
```

## 必测范围

- `HandBrewRecord` 非负次数和时间约束。
- 同一本地日期唯一、重复保存沿用 ID。
- 0 次、未填写和清除语义。
- 周/月/年/全部历史统计一致性。
- 128 次、74 天固定数据集。
- Room v1→v2 手冲提取迁移与 legacy 表保留。
- 空数据、闰年、月末、跨年周、未来日期。

## Definition of Done

- 产品契约、代码、Figma 和测试一致。
- 单元测试、Lint、Debug 构建和设备数据库测试通过。
- 全文审计没有活动管理、健身或未来活动扩展的有效承诺。
- 本地提交说明变更、用户影响、验证结果和剩余风险。
- 不包含密钥、真实数据库、APK/AAB 或账号信息。
