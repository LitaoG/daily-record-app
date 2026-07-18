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
            val app = FirebaseApp.initializeApp(context) ?: FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:1234567890:android:daily-record-demo")
                    .setApiKey("demo-api-key")
                    .setProjectId(DEMO_PROJECT_ID)
                    .build(),
            )
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
    }
}
