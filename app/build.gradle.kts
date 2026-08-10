import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val releaseSigningProperties = Properties().apply {
    rootProject.file("keystore.properties")
        .takeIf { it.isFile }
        ?.inputStream()
        ?.use(::load)
}

fun releaseSigningValue(propertyName: String, environmentName: String): String? =
    providers.environmentVariable(environmentName).orNull
        ?.takeIf(String::isNotBlank)
        ?: releaseSigningProperties.getProperty(propertyName)?.takeIf(String::isNotBlank)

val releaseStoreFile = releaseSigningValue("storeFile", "DAILY_RECORD_KEYSTORE_FILE")
val releaseStorePassword = releaseSigningValue("storePassword", "DAILY_RECORD_KEYSTORE_PASSWORD")
val releaseKeyAlias = releaseSigningValue("keyAlias", "DAILY_RECORD_KEY_ALIAS")
val releaseKeyPassword = releaseSigningValue("keyPassword", "DAILY_RECORD_KEY_PASSWORD")
val releaseSigningConfigured = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

val configuredVersionCode = providers.gradleProperty("dailyRecord.versionCode")
    .map(String::toInt)
val configuredVersionName = providers.gradleProperty("dailyRecord.versionName")
val effectiveVersionCode = providers.gradleProperty("dailyRecord.versionCodeOverride")
    .map(String::toInt)
    .orElse(configuredVersionCode)
val effectiveVersionName = providers.gradleProperty("dailyRecord.versionNameOverride")
    .orElse(configuredVersionName)

android {
    namespace = "io.github.litaog.dailyrecord"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "io.github.litaog.dailyrecord"
        minSdk = 26
        targetSdk = 36
        versionCode = effectiveVersionCode.get()
        versionName = effectiveVersionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

val validateReleaseSigning by tasks.registering {
    group = "verification"
    description = "Fails before packaging when stable release signing is not configured."
    doLast {
        check(releaseSigningConfigured) {
            "Release signing is not configured. Set DAILY_RECORD_KEYSTORE_FILE, " +
                "DAILY_RECORD_KEYSTORE_PASSWORD, DAILY_RECORD_KEY_ALIAS, and " +
                "DAILY_RECORD_KEY_PASSWORD, or create ignored keystore.properties."
        }
        check(requireNotNull(releaseStoreFile).let(rootProject::file).isFile) {
            "Configured release keystore does not exist."
        }
    }
}

tasks.matching { it.name == "packageRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn(validateReleaseSigning)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
