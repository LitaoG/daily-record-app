package io.github.litaog.dailyrecord.core.common

internal object ZhSettingsStrings : SettingsStrings {
        override val title = "设置"
        override val open = "打开设置"
        override val back = "返回主页"
        override val accountSection = "账号与同步"
        override val localAccountTitle = "本机记录"
        override val localAccountSummary = "记录只保存在这台设备；登录后可同步并在换机时恢复。"
        override val signedInAccountSummary = "查看同步状态、手动同步或管理账号"
        override val generalSection = "通用"
        override val terminologyTitle = "用语风格"
        override val terminologyDialogTitle = "选择用语风格"
        override val terminologyElegant = "典雅"
        override val terminologyDirect = "直白"
        override val terminologyElegantOption = "典雅（个人私密 / 伴侣亲密）"
        override val terminologyDirectOption = "直白（自慰 / 做爱）"
        override val languageTitle = "语言"
        override val languageZh = "中文"
        override val languageEn = "English"
        override val languageDialogTitle = "选择语言"
        override val themeTitle = "主题配色"
        override val themeDialogTitle = "选择主题配色"
        override val themeAurora = "极光星云"
        override val themeGlacialCoral = "冰川蜜桃"
        override val themeTwilightLilac = "暮光丁香"
        override val themeEmeraldBerry = "翡翠莓果"
        override val themeTitaniumChampagne = "钛金香槟"
        override val themeCreamLavender = "奶油丁香"
        override val themeSpringSky = "嫩芽晴空"
        override val themeBuffGlacier = "暖阳冰川"
        override val themeDreamBlush = "梦境腮红"
        override val dataSection = "数据与隐私"
        override val localFirstTitle = "本机优先"
        override val localFirstSummary = "所有记录先保存在本机；未登录时不会上传。"
        override val privacyTitle = "隐私保护"
        override val privacySummary = "不含广告、分析或崩溃上报 SDK"
        override val aboutSection = "关于"
        override val version = "版本"
        override val license = "开源许可"
        override val licenseValue = "Apache 2.0"
        override val privacyPolicy = "隐私政策"
        override val privacyPolicyAction = "在浏览器中打开"

        override fun accountDescription(title: String, status: String): String = "$title，$status"
    }

internal object ZhComponentStrings : ComponentStrings {
        override val decrease = "减少一次"
        override val increase = "增加一次"
    }

internal object ZhAppLockStrings : AppLockStrings {
        override val lockedTitle = "已锁定"
        override val lockedBody = "验证身份后继续使用"
        override val unlock = "解锁"
        override val notCompleted = "验证未完成，可以再试一次"
        override val promptTitle = "验证身份"
        override val promptSubtitle = "使用指纹、面容或锁屏密码"
        override val settingTitle = "应用锁"
        override val settingSummary = "打开应用时需要验证身份"
        override val settingOn = "已开启"
        override val settingOff = "已关闭"
        override val recentsNote = "开启后，最近任务中不显示内容"
        override val recentsAndScreenshotNote = "开启后，最近任务中不显示内容，也无法截屏"
        override val delayTitle = "重新锁定"
        override val delayDialogTitle = "何时重新锁定"
        override val delayImmediately = "立即"
        override val delayOneMinute = "离开 1 分钟后"
        override val delayFiveMinutes = "离开 5 分钟后"
        override val needsScreenLock = "请先在系统设置中设置锁屏密码"
        override val turnedOffNoScreenLock = "设备已没有锁屏密码，应用锁已关闭"
    }

internal object ZhDisguiseStrings : DisguiseStrings {
        override val settingTitle = "桌面伪装"
        override val settingSummary = "开启后，桌面图标和名称显示为“日记”"
        override val settingNote = "切换后桌面要几秒才更新；已放到桌面上的图标需要重新添加"
    }

internal object ZhOnboardingStrings : OnboardingStrings {
        override val title = "用语说明"
        override val soloLabel = "个人私密"
        override val soloDescription = "记录个人习惯与节奏"
        override val duoLabel = "伴侣亲密"
        override val duoDescription = "记录与伴侣的亲密"
        override val settingsHint = "可在「设置」中随时切换为您偏好的用语风格（典雅 / 直白）。"
        override val dismissAction = "我知道了"
    }
