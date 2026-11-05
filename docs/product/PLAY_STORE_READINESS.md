# Google Play 上架就绪清单

状态：`current reference — ADR-022 Phase 6 准备材料，尚未提交 Play Console`

最后复核：2026-11-06

本文把当前代码事实对应到 Google Play 的官方要求，给出表单的建议答案和剩余工作。Play Console 里的最终答案由产品负责人确认，并对其准确性负责。决策依据见 [ADR-022](../DECISIONS.md)，阶段进度见[流体玻璃与 Google Play 就绪 Goal](LIQUID_GLASS_PLAY_READINESS_GOAL.md)。

## 1. 技术基线

| 项目 | 当前事实 | Play 要求 | 结论 |
| --- | --- | --- | --- |
| 目标 API | `targetSdk 36`（ADR-018） | 新应用和更新需要 API 36 或更高 | 满足 |
| 发布格式 | GitHub Release 签名 APK（ADR-012） | 新应用上传 AAB，并使用 Play App Signing | 待做：负责人在本机用签名材料执行 `bundleRelease` |
| 权限 | 只有 `INTERNET` 与 `ACCESS_NETWORK_STATE` | — | 不需要敏感权限声明 |
| 系统备份 | `allowBackup="false"` | — | 与隐私说明一致 |
| 明文流量 | Release `usesCleartextTraffic="false"` | — | 满足 |
| 原生库 | 未核实 | 含原生库的应用需要满足 Play 的内存页大小要求 | 待核实：检查 AAB 内是否有 `lib/` 目录 |

## 2. 内容政策定位

- Play 不允许包含或宣传色情内容的应用，并把商店页或应用内的露骨文字、成人或性相关关键词列为常见违规示例。
- 本应用没有图片、视频、插画或露骨描述。它只保存用户自己填写的日期、次数、可选的开始/结束时间和 100 字以内的感受。
- 风险：中文模块名“自慰 / 做爱”和英文无障碍全称是直接的性相关词汇。目标书要求中文保持直观，英文与商店元数据改用中性词；Play 版是否也把中文界面改成中性词由负责人决定（决策 D1）。
- 商店截图只用空数据或中性示例，不出现模块名以外的性相关文字。
- 目标受众只选 18 岁及以上；应用不面向儿童，不参与家庭计划。

商店文案草稿（中性表述）：

| 字段 | 中文 | English |
| --- | --- | --- |
| 应用名 | 私密日历 | Daily Record |
| 简短说明 | 本机优先的私密习惯日记：日历记录、周月年统计，可选账号同步。 | Private, offline-first habit journal with calendar, stats and optional sync. |
| 完整说明要点 | 两个独立的私密记录模块；离线可用；周、月、年与全部历史统计；可选邮箱账号换机恢复；无广告、无分析或崩溃上报 SDK。 | Two separate private journals; works offline; weekly, monthly, yearly and all-time stats; optional email account for restoring on a new phone; no ads, analytics or crash-reporting SDKs. |

## 3. Data safety 表单建议答案

数据只有在用户登录并开启同步后才离开设备；未登录时全部记录只在本机 Room 数据库中。Play 只把被传出设备的数据算作“收集”，并要求同时申报第三方 SDK 的收集行为。

| 数据类型（Play 分类） | 何时传出设备 | 用途 | 是否可选 | 说明 |
| --- | --- | --- | --- | --- |
| 个人信息 → 电子邮件地址 | 注册或登录 | 账号管理 | 可选 | Firebase Authentication |
| 个人信息 → 用户 ID | 登录后每次同步 | 账号管理、应用功能 | 可选 | Firebase UID，作为云端所有权路径 |
| 健康与健身 → 健康信息（保守归类） | 登录并同步后 | 应用功能 | 可选 | 两个模块的日期、次数和可选的开始/结束时间 |
| 应用活动 → 其他用户生成的内容 | 登录并同步后 | 应用功能 | 可选 | 每次记录的“感受”短文本 |
| 设备或其他 ID（保守申报） | 调用云函数时 | 应用功能 | 可选 | Firebase 官方披露称 Cloud Functions 客户端会收集 FCM token |

其他答案：

- 共享：无。Firebase 代表开发者处理数据，按 Play 的服务提供方说明不算共享。
- 加密传输：是。Firebase SDK 使用 HTTPS，Release 构建禁止明文流量。
- 删除：用户可以申请删除，应用内已有入口，另需网页入口（见第 4 节）。
- IP 地址与用户代理：Firebase Authentication 和 Cloud Functions 用它们做安全与防滥用，本应用不用它们定位。是否需要单独申报由负责人按 Play 指引判断（决策 D3）。
- 不收集：位置、联系人、照片、通话、短信、广告 ID、崩溃日志、分析或使用行为数据。

## 4. 账号删除要求

- Play 要求允许在应用内创建账号的应用提供两条删除路径：应用内删除，以及一个网页链接，让用户不重装应用也能申请删除账号和数据。
- 应用内路径已经满足：账号弹窗中的“删除账号与云端数据”要求二次确认和密码重验，先删除本人两个云端集合，再删除认证账号，本机记录默认保留，详见[同步与隐私](../SYNC_AND_PRIVACY.md)。
- 缺口是网页入口。建议发布一页删除说明，并用一个私人邮箱接收申请；不要用公开 GitHub Issue 接收，否则会暴露申请人的邮箱（决策 D4）。

## 5. 内容分级问卷

- 按事实回答问卷。目标书中“争取全年龄评级”的写法不作为答题依据；最终评级由问卷系统决定，可能高于全年龄。
- 应用没有暴力、粗俗用语、赌博、违禁品、用户之间的互动或位置分享。
- 应用含有对性行为的文字提及（模块名和统计），没有任何图像。涉及性内容的问题应如实说明“只有文字提及”。

## 6. 健康应用声明

- 所有在 Play 上架的应用都必须填写健康应用声明；没有健康功能时也要声明“我的应用不提供任何健康功能”。
- 最接近的类别是“生殖与性健康”，它指管理生殖健康问题、性健康疾病或提供性健康教育。本应用只是个人记录，不提供医疗建议、诊断或教育内容。
- 决策 D2：商店分类选“健康与健身”并声明该类别时，需要同时满足 Play 的健康内容政策；选“生活时尚”时可以声明不提供健康功能，第 3 节的数据申报仍按保守口径填写。

## 7. 隐私政策

- Play Console 需要填写隐私政策链接，可以使用仓库中的[隐私说明](../../PRIVACY.md)。
- 上架前需要补充：Firebase 自动收集项、网页删除入口，以及应用锁和桌面伪装实现后的说明。
- 建议在设置“关于”中加入隐私政策入口（待做）。

## 8. 待办与决策

| 项目 | 负责方 | 状态 |
| --- | --- | --- |
| 签名 AAB（`bundleRelease`） | 负责人（需要签名材料） | 待做 |
| Play App Signing 方式 | 负责人 | 决策 D5 |
| 网页删除入口与接收邮箱 | 负责人 | 决策 D4 |
| 填写 Data safety、健康应用声明、内容分级、目标受众 | 负责人 | 按本文建议 |
| 英文模块名与商店文案中性化 | 代理 | 待做 |
| 设置页隐私政策入口 | 代理 | 待做 |
| 按新矢量图标导出 512px 商店图标 | 代理 | 待做 |
| 核实 AAB 是否含原生库 | 代理 | 待做 |
| 空数据商店截图 | 代理（需要模拟器内存） | 待做 |

决策项：

- D1：Play 版中文模块名是否改为中性词。
- D2：商店分类与健康应用声明的类别。
- D3：IP 地址是否单独申报。
- D4：网页删除入口的发布位置和接收邮箱。
- D5：Play App Signing 让 Google 生成新密钥，还是上传现有签名密钥。只有上传现有密钥，GitHub 版和 Play 版才能互相覆盖升级；签名边界见[签名与 GitHub Release](../RELEASE.md)。

## 来源

- [Target API level requirements for Google Play apps](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Provide information for Google Play's Data safety section](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Understanding Google Play's app account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111)
- [Inappropriate Content: Sexual Content and Profanity](https://support.google.com/googleplay/android-developer/answer/9878810)
- [Provide information for the Health apps declaration form](https://support.google.com/googleplay/android-developer/answer/14738291)
- [Firebase: data disclosure for Google Play's Data safety section](https://firebase.google.com/docs/android/play-data-disclosure)

以上 Google Play 与 Firebase 的要求均为转述，以官方页面的最新版本为准（Content was rephrased for compliance with licensing restrictions）。
