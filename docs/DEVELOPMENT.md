# 开发、测试与发布

最后复核：2026-08-16

## 环境

- Android Studio 稳定版与内置 JDK 21
- Android SDK / Platform Tools
- 当前配置为 `minSdk 26`、`compileSdk 37`、`targetSdk 36`，Room schema 为 v5；版本号以 `gradle.properties` 为准，当前公开版本为 `v1.0.0-beta.3` / `versionCode 4`
- SDK 版本说明：`compileSdk 37` 是升级 core-ktx 1.19.0 / lifecycle 2.11.0 后的强制要求（AAR 元数据校验），本轮已随库升级同步完成。`targetSdk` 刻意冻结在 36：升级到 37 是运行期行为变化（影响 Android 16+ 设备），需要新版本规划中的 API 34 与 minSdk 26 设备回归后一并评估。compileSdk 37 的新 Lint 版本库还新报告 AGP 9.3.1、Compose BOM 2026.08.00、Firebase BOM 34.17.0：其中 Firebase BOM 升级会改变客户端同步/认证 SDK 行为，需设备回归，统一归入 `v1.0.0-beta.4` 的协调升级矩阵，本轮不追版本。下次复查时间为 `v1.0.0-beta.4` 规划时
- 至少一台专用 Android 测试模拟器；完整自动化设备套件不得连接日常使用的真机
- 生产登录联调需要本机私有的 `app/google-services.json`
- Firestore 规则测试需要 Node.js 22 与 pnpm；Cloud Functions 为 2nd gen（Node 22），与 CI 和本地模拟器保持一致，版本不符时测试在启动阶段直接失败（`scripts/check-node-runtime.mjs`），避免运行时漂移让业务断言失真

GitHub `main` 是当前事实来源；从最新 `main` 建立短生命周期分支，通过 Pull Request、自动化检查和审查后使用普通 merge commit 合并，保留分支中的每一笔原始提交；禁止 squash merge、rebase merge 和改写共享历史。

## 验证命令

测试不按“每次修改都全量运行”执行。日常修改先根据 [`TESTING.md`](TESTING.md) 选择能够覆盖当前影响面的最小测试集；只有一个连贯小版本的功能代码全部完成、准备合并或交付时，才在最终功能 head 上运行一次下面的完整套件。未受后续修改影响的测试结果可以沿用，不机械重复。

```powershell
pnpm test:docs
pnpm test:copy
pnpm test:release-metadata
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --no-parallel
pnpm test:android-connected
pnpm test:firestore-rules
```

`test:docs` 会检查全部 Markdown 的本地链接与图片、文档目录是否覆盖所有 Git 跟踪 Markdown/文档文件，并核对 README/产品契约中的发布版本、Android SDK、Room schema 和当前 README 运行截图入口。修改版本、数据库、截图资产或文档目录时必须同步通过。

首页当前运行截图的采集边界见 [`docs/product/assets/readme/README.md`](product/assets/readme/README.md)。它们来自测试模拟器的 Debug 构建，不用于证明 Release 签名；视觉目标仍以产品设计稿、运行时语义树和自动化测试共同判断。

`assembleRelease` 需要仓库外的稳定签名配置和本机私有 `app/google-services.json`：

```powershell
.\gradlew.bat assembleRelease --no-configuration-cache --no-parallel
```

缺少签名时任务必须失败，不能把未签名或 Debug APK 当成发布物。完整配置、版本、tag、SHA-256 与覆盖升级流程见 [`RELEASE.md`](RELEASE.md)。

`test:android-connected` 会以 demo 项目启动隔离的 Authentication 与 Firestore 模拟器，运行完整设备测试后自动关闭。测试前保持 Android 模拟器的全局 HTTP 代理为空；若本地 Firebase 探针返回 `502` 或 Auth 超时，先运行 `adb shell settings list global | Select-String proxy` 检查是否残留 `global_http_proxy_host` / `global_http_proxy_port`。

Windows 默认使用 `pnpm test:android-connected`；Linux/macOS 使用 `pnpm test:android-connected:unix`。

`test:android-connected` 会安装测试 APK、修改应用数据并执行账号/数据库流程，只能在可重置的模拟器上运行。运行前用 `adb devices -l` 确认列表中没有日常使用的实体手机；生产 Firebase 烟雾测试继续默认跳过。

## 必测范围

- `HandBrewRecord` 非负次数和时间约束。
- 同一模块同一本地日期唯一、重复保存沿用 ID；两个模块互不覆盖。
- 0 次、未填写和清除语义。
- 周/月/年/全部历史统计一致性。
- 128 次、74 天固定数据集。
- Room v1→v2、v2→v3、v3→v4、v4→v5 迁移链，覆盖 v1→v5、v2→v5、v3→v5、v4→v5 四条路径；legacy 表保留、本机 owner 迁移、迁移筛选只依赖冻结的机器键（不引用用户可见文案常量）。
- 空数据、闰年、月末、跨年周、未来日期。
- 年月标题快速跳转、日历/统计共享锚点、月份切换后周明细不串月。
- 当前月网格不暴露相邻月份日期；年月和统计周期标题隐藏辅助副标题后仍可点击并具备 TalkBack 动作语义。
- 周/月分布卡对空值、明确 0 次、正次数和未来状态使用文字与形状共同编码，并在 200% 字体下可滚动、无裁切。
- 邮箱密码校验、重复提交锁、旋转恢复和 200% 字体。
- 找回密码的邮箱预填/规范化、隐私统一提示、重复发送锁、断网/限频/额度错误和模拟器一次性码改密登录。
- 本机模式跨冷启动保留；登录入口可显式退出本机模式。
- 登录前本机记录无网络迁入、离线待同步、系统网络状态不变时的 Firebase 恢复、实时监听失败重连（含认证恢复后重新订阅）、迟到确认、多设备编辑/清除、不同账号隔离。
- 持久化数据库关闭后重开，PENDING 待同步记录仍保留（进程重启后 WorkManager 可继续补偿上传）。
- Firestore Rules/Functions 的所有权、字段形状、非负次数、修订递增、墓碑、详情逐项校验和删除权限；普通记录清除走墓碑，客户端物理删除应被 Rules 拒绝，账号删除走 `deleteAccountData` callable，详情见[2026-08-15 main 代码审计](product/audit/2026-08-15-main-code-audit/README.md)。

## Definition of Done

- 已按 [`TESTING.md`](TESTING.md) 完成修改中的定向验证，并在小版本最终功能 head 上完成一次完整验证；未受影响的套件没有无意义地重复执行。
- 产品契约、代码、Figma 和测试一致。
- 单元测试、Lint、Debug/签名 Release 构建、设备数据库/Compose 测试、覆盖升级和规则测试通过。
- 全文审计没有把活动管理、健身或未来记录类型误写成当前功能；未来扩展只遵循 ADR-002 的独立垂直模块边界。
- 本地提交说明变更、用户影响、验证结果和剩余风险。
- 不包含 `google-services.json`、服务账号、真实数据库、APK/AAB 或账号口令。
- 完成由自动化、模拟器运行证据和 GitHub 审查判定；不要求用户执行重复的人工验收清单。
