# 2026-08-09 Issue #105：Stage 4–5 记录页验收

## 范围与基线

本次审计只收口 Issue #105 的 Stage 4（无障碍、窄屏、字体放大、恢复与跨设备）和 Stage 5（同视口运行验证、候选 APK 与发布前证据）。不改动 `main`，不重新设计页面，也不把时间/感受字段加入统计口径。

当前实现分支：`integration/review-candidate`；PR #119 保持 Draft，等待用户真机验收后再决定是否进入 `main`。

## 本轮实现

| 区域 | 文件 | 变化 |
| --- | --- | --- |
| 记录页布局 | `app/src/main/java/io/github/litaog/dailyrecord/ui/record/RecordScreen.kt` | 记录详情行根据可用宽度（小于 340dp）或字体缩放（≥1.5）改为纵向重排；宽屏仍保持时间字段和“写感受”同一行。所有时间、感受操作补充发生次数上下文、测试标签和触摸语义；感受输入获得焦点后等待 IME inset 稳定并自动滚入可见区域，键盘不再遮住编辑框。 |
| 文案与 TalkBack | `app/src/main/java/io/github/litaog/dailyrecord/core/common/AppCopy.kt` | 集中生成“第 N 次，开始/结束/编辑感受”等描述，避免在组件内散落中文。 |
| Compose 验收 | `app/src/androidTest/java/io/github/litaog/dailyrecord/ui/record/RecordScreenTest.kt` | 覆盖 0 次隐藏入口、展开后逐次语义、260dp 窄屏重排和 200% 字体。 |
| 进程恢复 | `app/src/test/java/io/github/litaog/dailyrecord/ui/record/RecordDetailsDraftTest.kt` | 验证 `Saver` 可恢复展开状态、时间和感受草稿。 |
| 跨设备同步 | `app/src/androidTest/java/io/github/litaog/dailyrecord/core/sync/HandBrewSyncCoordinatorTest.kt` | 新增逐次详情的开始/结束时间和感受跨设备上传、下载、重建断言；同时修正 Firebase Auth 异常参数和可配置超时断言。 |

## Stage 4 验收矩阵

| 要求 | 证据 | 结果 |
| --- | --- | --- |
| 0 次不显示入口 | `RecordScreenTest.countControlsStopAtZeroAndRecoverAfterIncrement`、现有入口条件 | 通过 |
| 1+ 次显示入口，点击后才展开 | `RecordScreenTest.detailsStayHiddenUntilOpenedAndExposeOccurrenceContextToTalkBack` | 通过 |
| 详情条目与次数一致、增加/减少草稿规则 | `RecordDetailsDraftTest` 三个既有规则测试 + 记录页 UI 测试 | 通过 |
| 260dp 窄屏不裁切 | `RecordScreenTest.detailsReflowInsideANarrowViewport` | 通过 |
| 200% 字体仍可读 | `RecordScreenTest.detailsRemainReadableAt200PercentFontScale` | 通过 |
| 进程重建后草稿保留 | `RecordDetailsDraftTest.saverRoundTripsExpandedDraftForProcessRecreation` | 通过（Saver 层证据） |
| 跨设备详情同步 | `HandBrewSyncCoordinatorTest.perOccurrenceDetailsRoundTripAcrossDevices` | 通过 |
| 自慰/做爱模块隔离 | 两模块独立模型、Repository、同步测试；本轮未改变边界 | 通过（回归范围未改变） |
| TalkBack 语义和最小触摸目标 | 详情操作使用 `Role.Button`、集中描述和 `MinimumTouchTarget`；定向 Compose 测试通过 | 通过 |

本轮没有用自定义 Activity 强行模拟进程重建：该方式会绕过正式 Manifest/安全边界，并在当前测试环境触发解析失败。进程恢复以生产使用的 `RecordDetailsDraft.Saver` 单元测试为证据；真实 Activity 进程杀死/恢复仍应由用户在 Android Studio 设备上做最终验收。

## Stage 5 运行证据

所有临时证据只放在仓库 `build/tmp/stage5/`，不写入桌面，也不提交机器日志。当前候选调试 APK：

```text
build/tmp/stage5/daily-record-stage5-debug.apk
```

SHA-256（当前候选 APK）：`B1BBA0A81D427AC4D28B6F1EFB3429E308007AAC63C34A688DB10900278E98BC`

运行截图：

```text
build/tmp/stage5/home.png
build/tmp/stage5/local.png
build/tmp/stage5/local2.png
build/tmp/stage5/local3.png
build/tmp/stage5/record-one.png
build/tmp/stage5/record-expanded.png
build/tmp/stage5/record-feeling.png
```

`home.png` 为真实模拟器截图，确认启动页、登录/注册入口、VPN 提示和“本机记录”入口在 1080×2280 设备上正常显示且无崩溃。`local.png` 为进入本机模式后的月历，`record-one.png` 为 0→1 次后的记录页，`record-expanded.png` 为点击入口后的逐次详情，`record-feeling.png` 为最终 IME 修复前的展开感受编辑器状态；这些非键盘场景的布局在最后一次修复中未改变。当前候选 APK 的键盘可见性由 `bridge-keyboard-delay3.png` 单独复核，避免把修复前截图误报为当前构建证据。详情入口、窄屏和字号证据仍由 `RecordScreenTest` 的真实 Compose 运行断言提供。

## 已执行检查

定向检查：

```text
:app:compileDebugKotlin                         PASS
:app:compileDebugUnitTestKotlin                 PASS
:app:compileDebugAndroidTestKotlin              PASS
:app:testDebugUnitTest --tests RecordDetailsDraftTest PASS
RecordScreenTest connected                       13/13 PASS
HandBrewSyncCoordinatorTest connected            18/18 PASS
```

统一构建与静态检查：

```text
:app:testDebugUnitTest                          PASS
:app:lintDebug                                  PASS
:app:assembleDebug                              PASS
:app:assembleDebugAndroidTest                   PASS
```

完整 connected 套件已运行一次，结果为 **115 项：105 通过、1 项按设计跳过、9 项失败**。失败没有被伪装为通过：

- 2 项 Firebase Auth 集成测试：当前模拟器未启动 Auth emulator（`10.0.2.2:9099`）；
- 2 项日期选择器测试：测试设备日期/预期状态不一致；
- 1 项模块语言测试：既有 content description 断言与当前集中式文案不一致；
- 4 项认证密码重置测试：现有弹窗的无限高度约束问题，位于 Issue #105 范围之外。

这些失败均没有改变本轮记录详情代码的定向结果；它们应分别建/跟进独立修复，不在 Stage 4–5 中悄悄扩大范围。

## 发布门槛与剩余事项

- 候选 APK 已生成，可供 Android Studio/真机验收；安装前请校验本轮提交后的 SHA-256。
- PR #119 只合并到 `integration/review-candidate`，保持 Draft；`main`、Release 和私有恢复仓库均不在本轮自动推进范围内。
- Issue #105 的“用户验收/发布收口”复选框仍保持未勾选。用户应重点验证：正常字号与 200% 字体、窄屏、TalkBack、返回/旋转/进程恢复、自慰与做爱切换、0/1/2/9+ 次、未填写详情仍可保存，以及跨设备登录后的详情恢复。
- 若真机发现问题，按一个可复现问题一个 Issue 记录；不要以截图主观差异为理由继续无目标重构。

## 最后一次输入可见性复核

在 API 34 `emulator-5554`、1080×2280 模拟器上，安装当前候选 APK 后按“日期 → +1 → 记录时间和感受 → 写感受”，键盘弹起后输入框自动滚到键盘上方并保持可见。运行时 UI 树中编辑框为 `[198,1293][1003,1458]`，IME 顶部约为 `1304`；截图证据：`build/tmp/stage5/bridge-keyboard-delay3.png`（SHA-256 `A430970977C0A81C23459BAC49A82A7747FF031403EB2540D7208AF29E7C571C`）。保存按钮仍由 `Scaffold` 固定在底部，未与编辑区重叠。

本轮候选 APK SHA-256 为 `B1BBA0A81D427AC4D28B6F1EFB3429E308007AAC63C34A688DB10900278E98BC`；旧的 `5ECDCDBF…` 校验值不再代表当前候选构建。
