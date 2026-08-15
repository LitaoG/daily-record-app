package io.github.litaog.dailyrecord.core.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import io.github.litaog.dailyrecord.core.account.AccountRemoteDataStore
import io.github.litaog.dailyrecord.core.account.FirebaseAccountDataDeletionStore
import io.github.litaog.dailyrecord.core.auth.AuthRepository
import io.github.litaog.dailyrecord.core.auth.FirebaseAuthRepository
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.sync.FirebaseHandBrewRemoteDataSource
import io.github.litaog.dailyrecord.core.sync.FirebaseSexRemoteDataSource
import io.github.litaog.dailyrecord.core.sync.HandBrewRemoteDataSource
import io.github.litaog.dailyrecord.core.sync.SexRemoteDataSource

internal const val FIREBASE_EMULATOR_APP_NAME = "daily-record-emulator"

/** Ports match the firebase.json emulator configuration. */
internal const val FIREBASE_AUTH_EMULATOR_PORT = 9099
internal const val FIREBASE_FIRESTORE_EMULATOR_PORT = 8080
internal const val FIREBASE_FUNCTIONS_EMULATOR_PORT = 5001

internal data class FirebaseServices(
    val authRepository: AuthRepository,
    val remoteDataSource: HandBrewRemoteDataSource,
    val sexRemoteDataSource: SexRemoteDataSource,
    val accountDataDeletionStore: AccountRemoteDataStore,
    val productionConfigured: Boolean,
    val currentUserId: () -> String?,
) {
    companion object {
        private const val DEMO_PROJECT_ID = "demo-daily-record-app"

        fun create(
            context: Context,
            emulatorHost: String? = null,
            database: DailyRecordDatabase? = null,
        ): FirebaseServices {
            var emulatorAppCreated = false
            val app = if (emulatorHost == null) {
                FirebaseApp.initializeApp(context) ?: FirebaseApp.initializeApp(context, demoOptions())
            } else {
                FirebaseApp.getApps(context).firstOrNull { it.name == FIREBASE_EMULATOR_APP_NAME }
                    ?: FirebaseApp.initializeApp(context, demoOptions(), FIREBASE_EMULATOR_APP_NAME)
                        .also { emulatorAppCreated = true }
            }
            val auth = FirebaseAuth.getInstance(app)
            val firestore = FirebaseFirestore.getInstance(app)
            val functions = FirebaseFunctions.getInstance(app)
            if (emulatorHost != null && emulatorAppCreated) {
                auth.useEmulator(emulatorHost, FIREBASE_AUTH_EMULATOR_PORT)
                firestore.useEmulator(emulatorHost, FIREBASE_FIRESTORE_EMULATOR_PORT)
                functions.useEmulator(emulatorHost, FIREBASE_FUNCTIONS_EMULATOR_PORT)
            }
            return FirebaseServices(
                authRepository = FirebaseAuthRepository(auth),
                remoteDataSource = FirebaseHandBrewRemoteDataSource(firestore, functions) { ownerId, localDate ->
                    database?.handBrewRecordDetailDao()?.getByDate(ownerId, localDate).orEmpty()
                },
                sexRemoteDataSource = FirebaseSexRemoteDataSource(firestore, functions) { ownerId, localDate ->
                    database?.sexRecordDetailDao()?.getByDate(ownerId, localDate).orEmpty()
                },
                accountDataDeletionStore = FirebaseAccountDataDeletionStore(functions),
                productionConfigured = app.options.projectId != DEMO_PROJECT_ID,
                currentUserId = { auth.currentUser?.uid },
            )
        }

        private fun demoOptions() = FirebaseOptions.Builder()
            .setApplicationId("1:1234567890:android:daily-record-demo")
            // The emulator accepts any non-empty key; keep a visibly synthetic
            // value so a production-looking credential can never be mistaken
            // for a checked-in Firebase secret.
            .setApiKey("AIzaSyDUMMY0000000000000000000000000000")
            .setProjectId(DEMO_PROJECT_ID)
            .build()
    }
}
