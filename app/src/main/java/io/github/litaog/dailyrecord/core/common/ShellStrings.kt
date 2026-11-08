package io.github.litaog.dailyrecord.core.common

internal interface SettingsStrings {
        val title: String
        val open: String
        val back: String
        val accountSection: String
        val localAccountTitle: String
        val localAccountSummary: String
        val signedInAccountSummary: String
        val generalSection: String
        val terminologyTitle: String
        val terminologyDialogTitle: String
        val terminologyElegant: String
        val terminologyDirect: String
        val terminologyElegantOption: String
        val terminologyDirectOption: String
        val languageTitle: String
        val languageZh: String
        val languageEn: String
        val languageDialogTitle: String
        val themeTitle: String
        val themeDialogTitle: String
        val themeAurora: String
        val themeGlacialCoral: String
        val themeTwilightLilac: String
        val themeEmeraldBerry: String
        val themeTitaniumChampagne: String
        val themeCreamLavender: String
        val themeSpringSky: String
        val themeBuffGlacier: String
        val themeDreamBlush: String
        val dataSection: String
        val localFirstTitle: String
        val localFirstSummary: String
        val privacyTitle: String
        val privacySummary: String
        val aboutSection: String
        val version: String
        val license: String
        val licenseValue: String
        val privacyPolicy: String
        /** Spoken after the privacy policy label: the row leaves the app. */
        val privacyPolicyAction: String

        fun accountDescription(title: String, status: String): String
    }

internal interface ComponentStrings {
        val decrease: String
        val increase: String
    }

    /** App lock screen, system prompt and settings rows (ADR-023). */
internal interface AppLockStrings {
        val lockedTitle: String
        val lockedBody: String
        val unlock: String
        val notCompleted: String
        val promptTitle: String
        val promptSubtitle: String
        val settingTitle: String
        val settingSummary: String
        val settingOn: String
        val settingOff: String
        val recentsNote: String
        val recentsAndScreenshotNote: String
        val delayTitle: String
        val delayDialogTitle: String
        val delayImmediately: String
        val delayOneMinute: String
        val delayFiveMinutes: String
        val needsScreenLock: String
        val turnedOffNoScreenLock: String
    }

    /** Optional home screen disguise (ADR-024); the switch reuses the app lock's on/off words. */
internal interface DisguiseStrings {
        val settingTitle: String
        val settingSummary: String
        val settingNote: String
    }

    /** First-launch onboarding explanatory card for intimacy modules (ADR-025). */
internal interface OnboardingStrings {
        val title: String
        val soloLabel: String
        val soloDescription: String
        val duoLabel: String
        val duoDescription: String
        val settingsHint: String
        val dismissAction: String
    }
