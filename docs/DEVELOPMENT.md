# 开发、测试与发布

## 环境

- Android Studio 稳定版与内置 JDK
- Android SDK / Platform Tools
- `minSdk 26`，`compileSdk` 与 `targetSdk` 由工程配置管理
- 至少一台模拟器或真机
- 生产登录联调需要本机私有的 `app/google-services.json`
- Firestore 规则测试需要 Node.js/pnpm；仓库已锁定依赖版本

GitHub 暂时不可连接时，使用本地分支和提交保留完整历史；连接恢复后直接推送，不复制或压平提交。

## 验证命令

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest assembleRelease --no-parallel
pnpm test:android-connected
pnpm test:firestore-rules
```

`test:android-connected` 会以 demo 项目启动隔离的 Authentication 与 Firestore 模拟器，运行完整设备测试后自动关闭。测试前保持 Android 模拟器的全局 HTTP 代理为空；若本地 Firebase 探针返回 `502` 或 Auth 超时，先运行 `adb shell settings list global | Select-String proxy` 检查是否残留 `global_http_proxy_host` / `global_http_proxy_port`。

## 必测范围

- `HandBrewRecord` 非负次数和时间约束。
- 同一本地日期唯一、重复保存沿用 ID。
- 0 次、未填写和清除语义。
- 周/月/年/全部历史统计一致性。
- 128 次、74 天固定数据集。
- Room v1→v2→v3 和 v2→v3 迁移、legacy 表保留与本机 owner 迁移。
- 空数据、闰年、月末、跨年周、未来日期。
- 年月标题快速跳转、日历/统计共享锚点、月份切换后周明细不串月。
- 邮箱密码校验、重复提交锁、旋转恢复和 200% 字体。
- 本机模式跨冷启动保留；登录入口可显式退出本机模式。
- 离线待同步、网络恢复、实时监听失败重连、迟到确认、多设备编辑/清除、不同账号隔离。
- Firestore 规则的所有权、字段形状、非负次数、修订递增和禁止物理删除。

## Definition of Done

- 产品契约、代码、Figma 和测试一致。
- 单元测试、Lint、Debug/Release 构建、设备数据库/Compose 测试和规则测试通过。
- 全文审计没有活动管理、健身或未来活动扩展的有效承诺。
- 本地提交说明变更、用户影响、验证结果和剩余风险。
- 不包含 `google-services.json`、服务账号、真实数据库、APK/AAB 或账号口令。
