# 架构决策记录

最后复核：2026-09-30

## ADR-001：Android 原生

- 状态：Accepted
- 决策：Kotlin + Jetpack Compose，`minSdk 26`。
- 说明：当前只交付 Android；技术包名 `io.github.litaog.dailyrecord` 保持稳定。

## ADR-002：独立垂直模块与未来扩展

- 状态：Accepted，取代旧“通用活动模型”。
- 决策：产品使用独立领域实体，不使用活动表、活动 ID 或计量类型；自慰使用 `HandBrewRecord`，新增类型按独立垂直模块实现。
- 原因：当前专用模型更清楚、更易验证，同时用户希望未来扩展不必重写现有自慰逻辑。预建万能活动模型会把未知需求提前固化，独立切片能隔离数据、统计和同步语义。
- 后果：旧 `Activity`、`MeasurementType`、活动管理全部删除。每个模块拥有自己的实体、表、Repository、云端字段、路径和迁移测试；只有已经证明语义相同的基础能力可以共享。

## ADR-003：本地 Room 单一事实来源

- 状态：Accepted
- 决策：所有记录先写 Room，UI 只从 Repository 暴露的 Flow 读取；Firebase 仅作为可选同步与恢复通道。
- 后果：核心功能完全离线；云端快照必须先合并到 Room，网络失败不会阻塞记录。

## ADR-004：零次与未填写分离

- 状态：Accepted
- 决策：对应模块数据库行且次数为 0 表示明确没有；没有行表示尚未填写。
- 后果：清除记录写墓碑并在 UI 恢复未填写；统计只把当前模块次数大于 0 的日期计入发生天数。

## ADR-005：简化主导航

- 状态：Accepted
- 决策：主导航仅日历和统计；日期记录从日历打开。
- 后果：不保留活动或账户底部入口。2026-08-09 起，设置按 ADR-015 作为主页顶部的次级入口存在，仍不占用底部主导航。

## ADR-006：v1 数据非破坏迁移

- 状态：Accepted
- 决策：v1→v2 提取旧自慰记录，旧通用表改名为 legacy 表。
- 原因：项目虽未发布，仍避免静默丢失已有测试或本地数据。
- 后果：运行时代码不读取 legacy 表；日后移除必须另立迁移并验证。

## ADR-007：公开仓库与 Apache 2.0

- 状态：Accepted
- 决策：继续使用公开仓库与 Apache License 2.0，不提交真实数据库、密钥或签名。

## ADR-008：日历与统计共享浏览日期

- 状态：Accepted
- 决策：应用保存一个可恢复的 `browseDate`，日历年月、日期选择器和周/月/年统计都由它派生。
- 原因：逐月浏览历史成本高；统计固定回到今天会让历史月与周明细产生认知错位。
- 后果：点击年月可在 1970 年 1 月 1 日至今天间直接跳转；切换统计周期不重置日期；所有向未来的周期导航都禁用。

## ADR-009：可选邮箱密码账号

- 状态：Accepted
- 决策：使用 Firebase Authentication 的邮箱密码方式；用户也可选择“暂不登录，先使用本机记录”。普通登录不引入短信验证码。
- 后果：Firebase UID 是云端所有权边界；不同账号的 Room 缓存和 Firestore 路径互相隔离。

## ADR-010：墓碑与确定性冲突合并

- 状态：Accepted
- 决策：清除写墓碑；多设备提交以本地最后确认的 `remoteRevision` 作为乐观并发基线，基线落后时保留服务器版本，远端修订号必须连续递增。
- 原因：设备系统时间可能明显偏快或偏慢，不能作为跨设备覆盖依据；修订基线同时阻止离线旧设备让已清除或较新的记录复活，也防止迟到确认覆盖新编辑。
- 后果：普通记录操作不物理删除；客户端 Rules 不允许物理删除，永久删除本人账号的数据清理由要求最近重新认证的 trusted callable 执行。Room 当前版本的两个业务表分别保存同步状态与远端修订号，逐次详情由对应的独立详情表保存。

## ADR-011：生产云端最小权限

- 状态：Accepted
- 决策：Firestore 使用生产模式，区域 `asia-east1`；规则只允许登录用户读写自己的自慰或做爱文档，并分别验证完整字段形状。
- 后果：`google-services.json` 不进入 Git；账号永久删除由 Functions Admin SDK 分批清理两个集合，普通记录仍必须使用墓碑；新客户端详情写入由 callable 做逐项服务端校验。

## ADR-012：GitHub Releases 与稳定自主管理签名

- 状态：Accepted
- 决策：少量用户只从 GitHub Releases 获取签名 APK；从 `v1.0.0-beta.1` 起使用同一份仓库外 release keystore，tag 必须匹配语义化 `versionName`，每次发布递增 `versionCode` 并附 SHA-256。
- 原因：Android 只允许相同证书的 APK 覆盖升级；Debug APK、未签名 APK 或丢失私钥都会破坏后续升级。
- 后果：签名材料和 `google-services.json` 只通过本机忽略文件或 GitHub Actions Secrets 注入；GitHub 工作流验证签名后创建 prerelease。

## ADR-013：App Check 暂缓强制

- 状态：Accepted
- 决策：当前 GitHub-only 侧载版不加入 App Check SDK，也不对 Authentication/Firestore 强制执行。
- 原因：Play Integrity 虽支持 Play 外分发，但仍需 Play Console 应用与 Cloud 项目关联并正确调整侧载识别条件；当前没有该基础，直接强制会拒绝合法侧载用户。
- 后果：未来先发布只采集指标、不强制的候选版，验证真实 GitHub APK 后再单独决策；生产 APK 不使用 debug provider。

## ADR-014：新增做爱记录作为第二个独立垂直模块

- 状态：Accepted
- 决策：新增 `SexRecord`、`sex_records`、`SexRecordDao`、`SexRecordRepository` 和 `/users/{uid}/sexRecords/{date}`，不向 `HandBrewRecord` 增加类型字段。
- 原因：两类记录在产品语义、隐私边界、存储和云端删除上必须可独立验证；通用活动表会重新引入已废止的复杂度。
- 共享边界：日期导航、次数编辑、统计纯函数和冲突算法已经由两个真实模块证明相同，因此以 `DailyCountEntry` UI 投影和强类型 `DailyCountSyncEngine` 复用；领域实体、DAO、Repository、远端映射和集合仍独立。
- 数据迁移：Room v3→v4 只创建空的 `sex_records`，原自慰数据不变；这条历史边界由当前 v1→v6、v2→v6、v3→v6、v4→v6、v5→v6 迁移测试链继续覆盖。
- 隐私后果：云同步新增用户主动填写的做爱日期与次数；不增加伴侣、具体时间、地点、时长、备注、图片或医疗数据。永久删除账号必须覆盖两个集合。

## ADR-015：设置中心保持为次级页面

- 状态：Accepted。
- 决策：主页顶部增加设置按钮；设置使用带返回的全屏次级页面，底部主导航仍只有日历和统计。
- 首版内容：账号与同步、本机优先与隐私事实、版本和开源许可。账号管理复用既有弹窗；自慰 / 做爱模块切换继续只放在主页。
- 原因：账号和应用信息使用频率低于记录和统计，但分散在主页会挤压标题与同步状态；集中后可保持核心页面安静，并让用户在一个位置理解数据边界。模块切换已经是主页核心操作，不在设置重复。
- 后果：设置页不成为万能功能容器。没有真实实现的主题、提醒、目标、导出、应用锁或自定义活动不能以占位项出现；本决策不引入 Room、Firestore 或统计变化。

## ADR-016：逐次时间段与感受继续保持模块独立

- 状态：Accepted
- 决策：为自慰和做爱分别新增 `HandBrewRecordDetail`/`SexRecordDetail`、Room 详情表、DAO、Repository 映射和云端 `details` 数组。日期聚合记录仍是同步版本边界；详情不参与统计。
- 原因：用户需要在次数大于 0 后按需补充每一次的时间段与短感受，但两类数据的隐私语义和删除边界不能通过通用活动类型复用。
- 交互后果：记录页先显示次数，用户主动点击“记录时间和感受”后才展开逐次表单；每次感受最多 100 个可见字符，所有详情可留空。
- 安全后果：聚合次数继续使用完整非负 `Int` 范围；记录页详情编辑器最多物化 512 行，超出时保持折叠并只编辑/保存聚合次数，避免远端异常次数触发海量 UI 分配。
- 数据后果：Room v4→v5 只创建两套空详情表，不生成历史详情；整条记录清除时删除对应本地详情，详情页清除则保留聚合次数并以空列表替换详情；账号同步和永久删除覆盖两个模块的详情字段。

## ADR-017：云端文档消失时本机 pending 编辑优先重建

- 状态：Accepted
- 决策：当本地记录 `remoteRevision > 0` 而云端同日文档已不存在时，提交改为以本机 pending 内容重建文档（新 revision 从 1 重新开始），并为这次物理重建生成新的 `id` 作为云端记录代际标记，不再抛出“Cloud record disappeared”数据错误。普通清除仍写墓碑，继续参与乐观并发检查；只有文档物理消失（如账号数据被清理）才触发重建。自慰与做爱模块行为一致。
- 原因：云端文档消失后，本机保留的待同步编辑若一直无法提交，会长期停留在 `Failed(Data)` 且 WorkManager 放弃重试，既不能自愈也无法区分“远端有意删除”与“远端文档异常消失”。正常清除路径使用墓碑文档，不会被本决策影响；因此“本机 pending 编辑优先”不会无提示地复活用户明确删除的记录。
- 后果：云端删除语义收紧为“墓碑是删除的唯一权威表达”；物理文档消失（账号删除重建等）后，本机离线编辑可以重新进入账号空间。相同 `id` 代际内继续按 `remoteRevision` 做乐观并发检查；发现新的 `id` 时，即使修订号较低也视为新代际并收敛到该远端快照。设备时钟不参与胜负。该策略写入同步契约，禁止再依赖“文档不存在即拒绝提交”的旧行为。

## ADR-018：客户端、Functions 与 CI 运行时保持可复现

- 状态：Accepted
- 决策：Android 构建使用 `compileSdk 37`、`targetSdk 36`；Functions、Firebase Emulator 与 CI 统一使用 Node.js 22，Functions 统一使用 2nd gen 并部署到 `asia-east1`。PR、`main` 和 Release tag 都必须通过 API 34 的 `connectedDebugAndroidTest`。
- 原因：SDK、Node、Functions 代际、区域和设备回归不一致时，规则、同步、生命周期或 Android 行为可能只在某个环境通过，无法把发布物当作可复核结果。
- 后果：`scripts/check-node-runtime.mjs` 在本地测试启动阶段拒绝错误 Node 主版本；Release 工作流先对被打 tag 的源码执行 connected 回归，再构建签名 APK。`targetSdk` 暂不跟随 `compileSdk` 自动升级，运行期行为变化必须另立版本评估。

## ADR-019：双语机制保持 AppCopy 唯一文案事实源

- 状态：Accepted
- 决策：应用支持中文（默认）与英语，设置页“通用 → 语言”切换并持久化，不提供“跟随系统”模式；`AppCopy` 继续是用户可见文案的唯一事实源，不引入 Android 系统字符串资源方案替代。`AppStrings` 接口声明全部文案签名，`ZhStrings`/`EnStrings` 分别实现（漏译编译失败），`AppCopy` 变为语言委托门面；`AppLanguageState.current` 同时服务 UI 与同步/统计等非 UI 层。类加载期不得固化文案（`RecordModule.uiSpec()`、`StatisticsPeriod.label` 按当前语言取值）；切换语言由主 Activity `recreate()` 全量重组，浏览日期等 `rememberSaveable` 状态保留。
- 原因：全部用户可见文案已集中在 AppCopy，改造为语言维度的门面比搬迁数百条字符串到 resources 成本更低、更可审计；英文单词普遍长于中文，固定宽度容器必须使用可见短标签，TalkBack 语义用全称，保证 200% 字体与窄屏不截断。
- 后果：新增文案必须同时实现两个语言文件；测试用 `AppLanguageState` 注入语言；`app_name` 由 `values-en/strings.xml` 提供英文桌面名，应用内文案不依赖系统 locale。

## ADR-020：完整服务端快照对“缺失”具有权威性

- 状态：Accepted
- 决策：当一次**完整**的服务端快照（非缓存、且没有任何文档被判定为格式异常）不再包含某个日期时，本地该日期中 `SYNCED` 且 `remoteRevision > 0` 的镜像行会被移除（连同详情），界面回到“未填写”。其他行永远不受影响。
- 原因：物理消失（账号数据被 trusted callable 清理、坏文档被写入触发器清理）后，云端文档已经不存在；仅靠“用户再编辑才收敛”会让另一台设备长期显示已删除数据。ADR-017 只定义了“本地 pending 编辑优先重建”，没有定义“无 pending 编辑时的收敛方向”，本决策补齐这一环。普通清除仍写墓碑并继续存在于快照中，因此不会被本决策误删；rejected 文档仍存在于云端，携带 rejected 的快照不触发收集，避免把“解析失败”误当“已消失”。
- 后果：`RoomDailyCountSyncStoreBase.applyRemoteRecords` 在 `completeServerSnapshot` 时执行收集；缓存快照、带 rejected 的快照、`PENDING` 行和从未上传（`remoteRevision = 0`）的行一律保留。`applySnapshot` 路径已经位于删除写闸门之内（2026-09 加固），账号删除中的迟到快照不会借本决策复活或误删行。

## ADR-021：本地 Firebase 模拟器占位 Key 采用符合 SDK 格式且避开扫描特征的合成值

- 状态：Accepted
- 决策：在未配置真实 `app/google-services.json` 时，本地 Firebase 模拟器回退配置 `demoOptions()` 采用格式满足 `FirebaseInstanceId` 内部要求（正则 `^A[\w-]{38}$`，即以大写字母 `A` 开头且总长度精确为 39 个字符）、但以前缀 `A_` 开头的可见合成字符串（`A_EMULATOR_DUMMY_KEY_000000000000000000`）。
- 原因：Firebase Android SDK 内部的 `FirebaseInstanceId.checkRequiredFirebaseOptions` 在向 Callable 等云端服务请求上下文时会强制调用 `isValidApiKeyFormat` 校验 API Key 格式（必须匹配 `\AA[\w-]{38}\z`）；若传入普通的短字符串（如 `demo-api-key`），会导致其抛出 `IllegalArgumentException` 阻断测试。而若写成类似真实 Google API Key 的 `AIza...` 前缀，又会命中 GitHub Secret Scanning 等安全工具的静态扫描正则（`\bAIza[0-9A-Za-z-_]{35}\b`）产生误报告警。
- 后果：`demoOptions()` 使用 `A_EMULATOR_DUMMY_KEY_000000000000000000` 既严格满足 Firebase SDK 内部的 39 字符校验规则，保证本地与 CI 的全部自动化设备集成测试正常通过；又完全避开以 `AIza` 开头的通用扫描规则，消除代码库中的假阳性误报。

## ADR-022：流体玻璃视觉、即时记录与 Google Play 就绪（b1 目标书）

- 状态：Accepted（2026-11-05）。产品负责人用仓库外的 b1 目标书指定方向，并要求整体风格为 Material 3 Expressive 加 iOS 式玻璃拟态；本仓库只记录结论与边界，不复制目标书全文。阶段进度见[流体玻璃与 Google Play 就绪 Goal](product/LIQUID_GLASS_PLAY_READINESS_GOAL.md)。
- 视觉决策：
  1. 玻璃分两层。只有不含内容的背景光晕层可以模糊，API 31+ 使用 `RenderEffect`，更低版本用径向渐变近似；卡片层只画奶白填充（不低于 84% 白）、顶部高光描边和柔和弥散阴影，从不模糊自身，文字与控件保持清晰。
  2. Material 3 Expressive 体现为弹簧动效、更大的圆角尺度、按压回弹和更醒目的数字字重；不为此升级 Compose 依赖。
  3. 提供 9 套可选主题（默认“极光星云”，另有 4 套扩展渐变和 4 套用户 Pantone 灵感方案），在设置“通用 → 主题配色”切换并持久化。每套主题为自慰、做爱分别给出独立色板；两模块不共用颜色，也不互相读取。
  4. 运行时图标改为 Compose 矢量绘制，删除 `drawable-nodpi` 下按颜色预渲染的 PNG；启动图标改为矢量自适应图标。
  5. 颜色克制（产品负责人 2026-11-05 补充）：模块色一律以纯色填充选中态、按钮和次数色阶；渐变只用于白色玻璃高光、一处淡色背景光晕和单色淡出，不在任何表面混合多种色相，避免渐变带来的审美疲劳。
- 交互决策：主页“今日速记”浮岛、日历格长按就地加减、月历左右滑动切月（保留箭头与年月跳转）、加减与切换的系统触感反馈、统计图表手指滑动查看单点数值。这些入口只调用既有 Repository 保存路径，不新增数据表。
- 隐私与发布决策：可选应用锁（系统生物识别或设备凭据）和最近任务遮挡、可选桌面伪装（切换启动器图标与名称）；以 Google Play 上架为目标准备 Data Safety、内容分级说明和 AAB 构建。现有 GitHub Releases 渠道（ADR-012）在另立发布决策前保持不变。
- 不变的边界：ADR-002/014 两模块独立、ADR-003 本地优先、ADR-004 零次与未填写分离、ADR-005 两项主导航、ADR-016 详情语义和 Room v6 全部保持。新增持久化只有 SharedPreferences 中的主题 id、应用锁设置和伪装选择，不进入 Room、Firestore 或统计。
- 对比度后果：所有主题由 `DesignTokensTest` 按 WCAG AA 逐套验证。目标书中达不到 AA 的个别色值以 AA 为准调整并在代码注释说明，例如紫色 4+ 档 `#A855F7`（白字 3.95:1、深字 3.70:1）改为 `#C084FC`，注释灰 `#94A3B8`（2.57:1）改为 `#586880`，玫瑰主色 `#E11D48` 在画布上 4.49:1 改为 `#DE1B46`。
- 取代：UI_UX.md 中“模块切换器严格二等分且无内部胶囊”“切月只用箭头或年月标题”的旧规则，以及“设置不出现主题或应用锁”的限制；ADR-015 “没有真实实现不得以占位项出现”仍然有效。新增依赖（如 `androidx.biometric`）在引入时单独记录版本依据并完成对应验证。

## ADR-023：应用锁使用平台认证接口，不引入 androidx.biometric

- 状态：Accepted（2026-11-06）。实现 ADR-022 的可选应用锁，进度见[流体玻璃与 Google Play 就绪 Goal](product/LIQUID_GLASS_PLAY_READINESS_GOAL.md)。
- 决策：Android 10 及以上（API 29+）使用平台 `BiometricPrompt`，同时允许生物识别和设备锁屏密码（API 30+ 用 `BIOMETRIC_WEAK | DEVICE_CREDENTIAL`，API 29 用 `setDeviceCredentialAllowed(true)`）；Android 8–9（API 26–28）使用 `KeyguardManager.createConfirmDeviceCredentialIntent` 确认锁屏密码。不提供应用自己的备用 PIN，设备锁屏密码就是备用方式。
- 原因：截至 2026-11-04，androidx.biometric 的最新稳定版仍是 2021 年的 1.1.0，它要求把 `MainActivity` 改为 `FragmentActivity` 并引入 Fragment；支持 `ComponentActivity` 与 Compose 的 1.4.0 仍是 beta。平台接口覆盖 `minSdk 26` 到 `targetSdk 36` 的全部版本，不新增依赖，也不改变 ADR-018 的构建基线。应用自管 PIN 需要安全存储、找回和防暴力破解，收益小于风险。
- 锁定规则：开启后冷启动即锁定；离开前台超过所选时长（立即、1 分钟、5 分钟，默认立即）后返回时锁定；配置变化和应用自身切换语言或主题引起的 `recreate()` 不锁定；认证界面本身造成的停止与恢复不会再次锁定。锁定状态只在内存中，进程被回收后冷启动重新锁定。
- 界面后果：锁屏层是盖在所有应用弹窗之上的全屏窗口；底层内容保持组合，进行中的保存、同步和账号删除不会因锁定被取消，锁定期间底层内容不进入 TalkBack 焦点，输入法焦点被清除。
- 安全后果：只有设备已设置安全锁屏时才能开启；设备之后移除了锁屏密码时，应用锁自动关闭并提示，避免用户被永久挡在自己的数据之外。开启后，Android 13+ 用 `setRecentsScreenshotEnabled(false)` 隐藏最近任务预览，更低版本使用 `FLAG_SECURE`，后者同时禁止截屏，设置页需要说明。应用锁只保护界面入口，不加密数据库；数据仍由系统应用沙箱保护。
- 测试后果：锁定判断写成纯函数，由 JVM 测试覆盖；三条认证路径需要在 API 34 模拟器和 Android 8/9 设备上手工回归，列入 ADR-022 小版本末尾的设备验证。
- Lint 后果：Firebase Auth 经 `play-services-auth` 20.7.0 传递引入 `androidx.fragment` 1.1.0，lint 因此对 Android 8–9 路径的 `registerForActivityResult` 报 `InvalidFragmentVersionForActivityResult`。该检查针对 1.3.0 以前的 `FragmentActivity`；`MainActivity` 是 `ComponentActivity`，启动器由它自己的 `ActivityResultRegistry` 处理，所以只在注册处局部抑制，不为此新增或升级 Fragment 依赖。

## 已废止方向

旧版通用活动表、三种预设计量方式、活动身份色、活动归档、短信验证码、端到端加密和通用日历标记均由本次产品收缩废止，只存在于 Git 历史，不再指导实现。健身等其他记录不属于当前范围；若未来立项，应采用 ADR-002 的独立垂直模块方式重新定义，而不是恢复旧模型。
