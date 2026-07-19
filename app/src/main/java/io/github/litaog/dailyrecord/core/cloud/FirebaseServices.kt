package io.github.litaog.dailyrecord.core.cloud

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.github.litaog.dailyrecord.core.auth.AuthRepository
import io.github.litaog.dailyrecord.core.auth.FirebaseAuthRepository
import io.github.litaog.dailyrecord.core.sync.FirebaseHandBrewRemoteDataSource
import io.github.litaog.dailyrecord.core.sync.HandBrewRemoteDataSource

internal const val FIREBASE_EMULATOR_APP_NAME = "daily-record-emulator"

internal data class FirebaseServices(
    val authRepository: AuthRepository,
    val remoteDataSource: HandBrewRemoteDataSource,
    val productionConfigured: Boolean,
    val currentUserId: () -> String?,
) {
    companion object {
        private const val DEMO_PROJECT_ID = "demo-daily-record-app"

        fun create(
            context: Context,
            emulatorHost: String? = null,
        ): FirebaseServices {
            val app = if (emulatorHost == null) {
                FirebaseApp.initializeApp(context) ?: FirebaseApp.initializeApp(context, demoOptions())
            } else {
                FirebaseApp.getApps(context).firstOrNull { it.name == FIREBASE_EMULATOR_APP_NAME }
                    ?: FirebaseApp.initializeApp(context, demoOptions(), FIREBASE_EMULATOR_APP_NAME)
            }
            val auth = FirebaseAuth.getInstance(app)
            val firestore = FirebaseFirestore.getInstance(app)
            if (emulatorHost != null) {
                auth.useEmulator(emulatorHost, 9099)
                firestore.useEmulator(emulatorHost, 8080)
            }
            return FirebaseServices(
                authRepository = FirebaseAuthRepository(auth),
                remoteDataSource = FirebaseHandBrewRemoteDataSource(firestore),
                productionConfigured = app.options.projectId != DEMO_PROJECT_ID,
                currentUserId = { auth.currentUser?.uid },
            )
        }

        private fun demoOptions() = FirebaseOptions.Builder()
            .setApplicationId("1:1234567890:android:daily-record-demo")
            .setApiKey("AIzaSyDUMMY0000000000000000000000000000")
            .setProjectId(DEMO_PROJECT_ID)
            .build()
    }
}
