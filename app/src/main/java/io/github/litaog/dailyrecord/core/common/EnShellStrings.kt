package io.github.litaog.dailyrecord.core.common

internal object EnSettingsStrings : SettingsStrings {
        override val title = "Settings"
        override val open = "Open settings"
        override val back = "Back to home"
        override val accountSection = "Account & sync"
        override val localAccountTitle = "Local records"
        override val localAccountSummary = "Stored on this device only; sign in to sync and restore."
        override val signedInAccountSummary = "Check sync status, sync manually, or manage your account"
        override val generalSection = "General"
        override val terminologyTitle = "Terminology Style"
        override val terminologyDialogTitle = "Choose terminology style"
        override val terminologyElegant = "Elegant"
        override val terminologyDirect = "Direct"
        // English copy is style-independent (EnStrings.withStyle returns this),
        // so the options only state that the choice affects the Chinese wording
        // instead of inventing a third naming for the modules.
        override val terminologyElegantOption = "Elegant (affects Chinese wording)"
        override val terminologyDirectOption = "Direct (affects Chinese wording)"
        override val languageTitle = "Language"
        override val languageZh = "中文"
        override val languageEn = "English"
        override val languageDialogTitle = "Choose language"
        override val themeTitle = "Color theme"
        override val themeDialogTitle = "Choose color theme"
        override val themeAurora = "Aurora"
        override val themeGlacialCoral = "Glacier Coral"
        override val themeTwilightLilac = "Twilight Lilac"
        override val themeEmeraldBerry = "Emerald Berry"
        override val themeTitaniumChampagne = "Titanium Gold"
        override val themeCreamLavender = "Cream Lavender"
        override val themeSpringSky = "Spring Sky"
        override val themeBuffGlacier = "Buff Glacier"
        override val themeDreamBlush = "Blush Dream"
        override val dataSection = "Data & privacy"
        override val localFirstTitle = "Local first"
        override val localFirstSummary = "All records save locally first; nothing uploads while signed out."
        override val privacyTitle = "Privacy"
        override val privacySummary = "No ads, analytics, or crash-reporting SDKs"
        override val aboutSection = "About"
        override val version = "Version"
        override val license = "License"
        override val licenseValue = "Apache 2.0"
        override val privacyPolicy = "Privacy policy"
        override val privacyPolicyAction = "Opens in browser"

        override fun accountDescription(title: String, status: String): String = "$title, $status"
    }

internal object EnComponentStrings : ComponentStrings {
        override val decrease = "Decrease by 1"
        override val increase = "Increase by 1"
    }

internal object EnAppLockStrings : AppLockStrings {
        override val lockedTitle = "Locked"
        override val lockedBody = "Verify it's you to continue"
        override val unlock = "Unlock"
        override val notCompleted = "Verification wasn't completed. Try again."
        override val promptTitle = "Verify it's you"
        override val promptSubtitle = "Use your fingerprint, face or screen lock"
        override val settingTitle = "App lock"
        override val settingSummary = "Ask for verification when the app opens"
        override val settingOn = "On"
        override val settingOff = "Off"
        override val recentsNote = "When on, recent apps don't show the content"
        override val recentsAndScreenshotNote = "When on, recent apps don't show the content and screenshots are blocked"
        override val delayTitle = "Lock again"
        override val delayDialogTitle = "When to lock again"
        override val delayImmediately = "Immediately"
        override val delayOneMinute = "After 1 minute away"
        override val delayFiveMinutes = "After 5 minutes away"
        override val needsScreenLock = "Set a screen lock in system settings first"
        override val turnedOffNoScreenLock = "This device has no screen lock, so app lock was turned off"
    }

internal object EnDisguiseStrings : DisguiseStrings {
        override val settingTitle = "Home screen disguise"
        override val settingSummary = "When on, the home screen shows the app as \"Journal\""
        override val settingNote = "The home screen takes a few seconds to update. Add the icon to your home screen again."
    }

internal object EnOnboardingStrings : OnboardingStrings {
        override val title = "Terminology Guide"
        override val soloLabel = "Solo Intimacy"
        override val soloDescription = "Track personal rhythm and solo moments"
        override val duoLabel = "Partner Intimacy"
        override val duoDescription = "Track couple intimacy and shared moments"
        override val settingsHint = "You can switch between Elegant and Direct styles anytime in Settings."
        override val dismissAction = "Got it"
    }
