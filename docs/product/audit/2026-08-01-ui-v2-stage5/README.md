# 2026-08-01 Stage 5：集成 QA 与候选 APK

状态：`Historical — completed; superseded by the beta.2 release audit and carried into beta.3`

当时状态（2026-08-01）：`implementation-verified`，等待公共 PR/CI 合并和用户候选 APK 验收。本页保留候选 APK 阶段的测试边界；最终合并、发布和镜像证据见 2026-08-03 Stage 5/6 记录。

对应阶段：[UI v2 分阶段执行计划](../../QUIET_PRIVATE_JOURNAL_GOALS.md) · [GitHub Issue #40](https://github.com/LitaoG/daily-record-app/issues/40)

## 范围与边界

- 分支：`agent/ui-v2-stage5-integration-qa`
- 基线：Stage 4 统计实现公共 head；本轮不重新设计 UI，也不改变 Room、Firestore 或统计业务规则。
- 回归范围：模块切换、浏览月份/年份、周/月/年/全部周期、日期记录进入、保存、显式 0、清除、未来日期禁用、本机模式、登录/注册/密码重置入口、离线/同步入口。
- 视口与状态：API 34 模拟器、390×844 逻辑视口、正常字号与 200% 字体；未填写、0 次、正次数、未来日期、空统计和两个模块独立数据。
- 测试输出只写入 `build/tmp/stage5/`，解析后清理；不把机器日志放在桌面或仓库文档中。

## 本轮测试宿主加固

本轮没有运行时业务代码改动，只有两个 Android 测试类的稳定性修正：

1. `AuthScreenTest` 和 `RecordModuleIntegrationTest` 使用 `AndroidComposeRule<ComponentActivity>`，让返回键和截图操作绑定到明确的 Activity 宿主。
2. 本机模式登录页返回测试通过 `onBackPressedDispatcher` 触发返回，不依赖没有焦点的 Espresso 根视图。
3. 密码重置测试只验证有效邮箱提交一次和成功反馈，不在 Compose 模态仍等待时阻塞回调；重复提交锁定测试仍保留真实异步 gate。
4. 模块切换器像素检查在节点可捕获后再读取 bitmap，避免首次渲染期间的 2 秒截图超时。

## 结果

| 门禁 | 结果 |
|---|---|
| `RecordModuleIntegrationTest` 定向设备测试 | 6/6 通过 |
| `AuthScreenTest` 定向设备测试 | 12/12 通过 |
| `pnpm test:android-connected`（最终功能 head） | 91 个测试完成：90 个通过、1 个按设计跳过、0 个失败 |
| `testDebugUnitTest`、`lintDebug`、Debug/AndroidTest 编译与打包 | 通过；JVM 90/90，0 失败；Gradle `BUILD SUCCESSFUL` |
| Firestore Rules 模拟器 | 通过：所有模块的所有权、字段、修订和删除检查通过 |
| 文档链接与发布元数据 | `pnpm test:docs` 及 `pnpm test:release-metadata` 通过 |
| Crash Buffer | 清空历史缓冲后冷启动应用为空；此前记录仅为模拟器系统/WebView 与测试宿主 teardown 噪声 |
| Release 合并 Manifest | `android:usesCleartextTraffic="false"` |

生产 Firebase 烟雾测试按测试策略跳过；本次连接套件使用本地 Auth/Firestore Emulator。实体手机日常验收属于 Stage 6，不在本审计中冒充完成。

## 手工回归摘要

- 本机模式冷启动后直接进入日历；从登录入口返回仍保留本机偏好。
- 保存自慰记录后，日历、记录页和统计页显示相同日期/次数；切换到做爱模块不会混入自慰数据。
- 月度和年度浏览保持当前模块和所选日期；未来日期不可记录，0 次与未填写在图表和语义上保持区别。
- 200% 字体下主要操作仍可读、可触控；返回、清除确认、同步错误和登录入口没有系统崩溃。

## 候选 APK

候选构建使用仓库内 Debug APK（仅供本阶段安装验收，不是签名 Release）：

`app/build/outputs/apk/debug/app-debug.apk`

SHA-256：`BDADA8277765D3A02C81421E097E9A96B27061989219339A6FF1B0EDD81C5940`

Stage 6 用户验收后才递增版本并按 [`RELEASE.md`](../../../RELEASE.md) 构建稳定签名包。

## Git 与隐私边界

- 所有日常提交、PR、CI 和合并在公共 `LitaoG/daily-record-app` 完成。
- 私有恢复仓库本阶段不同步；只有用户完成候选 APK 日常验收、公共 `main` 形成完整小版本后才执行 Stage 6 镜像同步。
- 本地 Firebase 配置、签名材料、真实账号和真实记录不进入公共提交；敏感信息扫描必须保持为空。

## 停止点

Stage 5 当时在公共 PR 合并、Issue #40 标记 `status:awaiting-acceptance` 后停止。下一步当时等待用户安装候选 APK 进行日常使用；后续 Stage 5/6 已完成并发布，当前复现问题应单独建 Issue，不能以历史暂停点继续无目的重构。
