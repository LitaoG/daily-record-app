package io.github.litaog.dailyrecord.core.common

internal interface AppCopySettings {
        val title: String get() = AppLanguageState.current.settings.title
        val open: String get() = AppLanguageState.current.settings.open
        val back: String get() = AppLanguageState.current.settings.back
        val accountSection: String get() = AppLanguageState.current.settings.accountSection
        val localAccountTitle: String get() = AppLanguageState.current.settings.localAccountTitle
        val localAccountSummary: String get() = AppLanguageState.current.settings.localAccountSummary
        val signedInAccountSummary: String get() = AppLanguageState.current.settings.signedInAccountSummary
        val generalSection: String get() = AppLanguageState.current.settings.generalSection
        val terminologyTitle: String get() = AppLanguageState.current.settings.terminologyTitle
        val terminologyDialogTitle: String get() = AppLanguageState.current.settings.terminologyDialogTitle
        val terminologyElegant: String get() = AppLanguageState.current.settings.terminologyElegant
        val terminologyDirect: String get() = AppLanguageState.current.settings.terminologyDirect
        val terminologyElegantOption: String get() = AppLanguageState.current.settings.terminologyElegantOption
        val terminologyDirectOption: String get() = AppLanguageState.current.settings.terminologyDirectOption
        val languageTitle: String get() = AppLanguageState.current.settings.languageTitle
        val languageZh: String get() = AppLanguageState.current.settings.languageZh
        val languageEn: String get() = AppLanguageState.current.settings.languageEn
        val languageDialogTitle: String get() = AppLanguageState.current.settings.languageDialogTitle
        val themeTitle: String get() = AppLanguageState.current.settings.themeTitle
        val themeDialogTitle: String get() = AppLanguageState.current.settings.themeDialogTitle
        val themeAurora: String get() = AppLanguageState.current.settings.themeAurora
        val themeGlacialCoral: String get() = AppLanguageState.current.settings.themeGlacialCoral
        val themeTwilightLilac: String get() = AppLanguageState.current.settings.themeTwilightLilac
        val themeEmeraldBerry: String get() = AppLanguageState.current.settings.themeEmeraldBerry
        val themeTitaniumChampagne: String get() = AppLanguageState.current.settings.themeTitaniumChampagne
        val themeCreamLavender: String get() = AppLanguageState.current.settings.themeCreamLavender
        val themeSpringSky: String get() = AppLanguageState.current.settings.themeSpringSky
        val themeBuffGlacier: String get() = AppLanguageState.current.settings.themeBuffGlacier
        val themeDreamBlush: String get() = AppLanguageState.current.settings.themeDreamBlush
        val dataSection: String get() = AppLanguageState.current.settings.dataSection
        val localFirstTitle: String get() = AppLanguageState.current.settings.localFirstTitle
        val localFirstSummary: String get() = AppLanguageState.current.settings.localFirstSummary
        val privacyTitle: String get() = AppLanguageState.current.settings.privacyTitle
        val privacySummary: String get() = AppLanguageState.current.settings.privacySummary
        val aboutSection: String get() = AppLanguageState.current.settings.aboutSection
        val version: String get() = AppLanguageState.current.settings.version
        val license: String get() = AppLanguageState.current.settings.license
        val licenseValue: String get() = AppLanguageState.current.settings.licenseValue
        val privacyPolicy: String get() = AppLanguageState.current.settings.privacyPolicy
        val privacyPolicyAction: String get() = AppLanguageState.current.settings.privacyPolicyAction

        fun accountDescription(title: String, status: String): String =
            AppLanguageState.current.settings.accountDescription(title, status)
    }

internal interface AppCopyComponent {
        val decrease: String get() = AppLanguageState.current.components.decrease
        val increase: String get() = AppLanguageState.current.components.increase

        fun joinSemantics(vararg parts: String): String =
            AppLanguageState.current.joinSemantics(*parts)

        fun joinSemantics(parts: Iterable<String>): String =
            AppLanguageState.current.joinSemantics(parts)
    }

internal interface AppCopyAppLock {
        val lockedTitle: String get() = AppLanguageState.current.appLock.lockedTitle
        val lockedBody: String get() = AppLanguageState.current.appLock.lockedBody
        val unlock: String get() = AppLanguageState.current.appLock.unlock
        val notCompleted: String get() = AppLanguageState.current.appLock.notCompleted
        val promptTitle: String get() = AppLanguageState.current.appLock.promptTitle
        val promptSubtitle: String get() = AppLanguageState.current.appLock.promptSubtitle
        val settingTitle: String get() = AppLanguageState.current.appLock.settingTitle
        val settingSummary: String get() = AppLanguageState.current.appLock.settingSummary
        val settingOn: String get() = AppLanguageState.current.appLock.settingOn
        val settingOff: String get() = AppLanguageState.current.appLock.settingOff
        val recentsNote: String get() = AppLanguageState.current.appLock.recentsNote
        val recentsAndScreenshotNote: String get() = AppLanguageState.current.appLock.recentsAndScreenshotNote
        val delayTitle: String get() = AppLanguageState.current.appLock.delayTitle
        val delayDialogTitle: String get() = AppLanguageState.current.appLock.delayDialogTitle
        val delayImmediately: String get() = AppLanguageState.current.appLock.delayImmediately
        val delayOneMinute: String get() = AppLanguageState.current.appLock.delayOneMinute
        val delayFiveMinutes: String get() = AppLanguageState.current.appLock.delayFiveMinutes
        val needsScreenLock: String get() = AppLanguageState.current.appLock.needsScreenLock
        val turnedOffNoScreenLock: String get() = AppLanguageState.current.appLock.turnedOffNoScreenLock
    }

internal interface AppCopyDisguise {
        val settingTitle: String get() = AppLanguageState.current.disguise.settingTitle
        val settingSummary: String get() = AppLanguageState.current.disguise.settingSummary
        val settingNote: String get() = AppLanguageState.current.disguise.settingNote
    }

internal interface AppCopyOnboarding {
        val title: String get() = AppLanguageState.current.onboarding.title
        val soloLabel: String get() = AppLanguageState.current.onboarding.soloLabel
        val soloDescription: String get() = AppLanguageState.current.onboarding.soloDescription
        val duoLabel: String get() = AppLanguageState.current.onboarding.duoLabel
        val duoDescription: String get() = AppLanguageState.current.onboarding.duoDescription
        val settingsHint: String get() = AppLanguageState.current.onboarding.settingsHint
        val dismissAction: String get() = AppLanguageState.current.onboarding.dismissAction
    }
