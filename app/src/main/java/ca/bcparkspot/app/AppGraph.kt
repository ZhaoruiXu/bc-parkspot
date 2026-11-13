package ca.bcparkspot.app

import android.content.Context
import androidx.annotation.VisibleForTesting
import ca.bcparkspot.app.auth.AccountManager
import ca.bcparkspot.app.auth.FirebaseAuthGateway
import ca.bcparkspot.app.data.local.AppDatabase
import ca.bcparkspot.app.data.local.LocalStore
import ca.bcparkspot.app.data.local.SharedDevicePrefs
import ca.bcparkspot.app.data.remote.FirestoreRemoteStore
import ca.bcparkspot.app.domain.ReportPolicies
import ca.bcparkspot.app.sync.CloudSync
import ca.bcparkspot.app.sync.NetworkMonitor
import ca.bcparkspot.app.sync.SyncEngine
import ca.bcparkspot.app.sync.WorkManagerSyncControl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Local Firebase emulator ports; tests point the app at them so no test ever touches the real project. */
object FirebaseEmulator {
    const val HOST_FROM_ANDROID_EMULATOR = "10.0.2.2"
    const val AUTH_PORT = 9099
    const val FIRESTORE_PORT = 8085
}

/** The app's long-lived objects, built once per process. */
class AppGraph private constructor(context: Context) {
    private val app = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val policy = ReportPolicies.DEFAULT
    val clock: () -> Long = System::currentTimeMillis

    val database: AppDatabase = AppDatabase.getDatabase(app)
    val local = LocalStore(database)
    val prefs = SharedDevicePrefs(app)
    val network = NetworkMonitor(app)

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = run {
        val id = app.getString(R.string.firestore_database_id)
        if (id.isBlank() || id == DEFAULT_DATABASE_ID) FirebaseFirestore.getInstance() else FirebaseFirestore.getInstance(id)
    }

    init {
        emulatorHost?.let { host -> pointFirebaseAtEmulator(host) }
    }

    /** The default Firebase instances live for the whole process and accept useEmulator() only once. */
    private fun pointFirebaseAtEmulator(host: String) = synchronized(AppGraph) {
        if (emulatorConfigured) return
        firebaseAuth.useEmulator(host, FirebaseEmulator.AUTH_PORT)
        firestore.useEmulator(host, FirebaseEmulator.FIRESTORE_PORT)
        emulatorConfigured = true
    }

    val remote = FirestoreRemoteStore(firestore)
    private val _lastSyncAt = MutableStateFlow(prefs.lastSuccessfulSyncAt)
    val lastSyncAt: StateFlow<Long?> = _lastSyncAt.asStateFlow()

    val syncEngine = SyncEngine(local, remote, policy, clock, onCompleted = {
        prefs.lastSuccessfulSyncAt = it
        _lastSyncAt.value = it
    })
    val syncControl = WorkManagerSyncControl(app) { syncEngine }

    val accounts = AccountManager(
        auth = FirebaseAuthGateway(firebaseAuth),
        local = local,
        remote = remote,
        sync = syncControl,
        prefs = prefs,
        online = network.online,
        policy = policy,
        clock = clock,
        scope = scope
    )

    val cloud = CloudSync(firestore, local, accounts.activeUid, prefs, policy, clock, scope)

    private fun start() {
        network.start()
        accounts.start()
        cloud.start()
    }

    companion object {
        private const val DEFAULT_DATABASE_ID = "(default)"

        @Volatile private var instance: AppGraph? = null

        /** Set by instrumented tests before the graph is first built. */
        @Volatile var emulatorHost: String? = null

        private var emulatorConfigured = false

        /** JVM tests get a new Application per test; this drops the previous test's graph and database. */
        @VisibleForTesting
        fun resetForTests() = synchronized(this) {
            instance?.scope?.cancel()
            instance = null
            AppDatabase.resetForTests()
        }

        fun from(context: Context): AppGraph = instance ?: synchronized(this) {
            instance ?: AppGraph(context).also {
                instance = it
                it.start()
            }
        }
    }
}
