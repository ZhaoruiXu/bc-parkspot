package ca.bcparkspot.app.base

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

/**
 * Talks to the local Firebase emulators through its own Firebase app on a `demo-` project, which the
 * emulators never forward to a real project. Skips when the emulators are not running.
 * Run with: firebase emulators:exec --only firestore,auth --project demo-no-project "./gradlew testDebugUnitTest"
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
abstract class FirestoreEmulatorTestBase {

    protected lateinit var firestore: FirebaseFirestore
    protected lateinit var auth: FirebaseAuth

    companion object {
        const val PROJECT_ID = "demo-no-project"
        const val APP_NAME = "emulator-tests"
        const val EMULATOR_HOST = "127.0.0.1"
        const val FIRESTORE_PORT = 8085
        const val AUTH_PORT = 9099
        const val CONNECT_TIMEOUT_MS = 400
        const val AUTH_TIMEOUT_MS = 10_000L
        const val TEST_PASSWORD = "emulator-only-password"
        private const val HTTP_OK_MAX = 299
    }

    private fun isListening(port: Int): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(EMULATOR_HOST, port), CONNECT_TIMEOUT_MS) }
        true
    } catch (e: Exception) {
        false
    }

    @Before
    open fun setUpFirebase() {
        Assume.assumeTrue(
            "Firebase emulators not running on $EMULATOR_HOST:$FIRESTORE_PORT and :$AUTH_PORT",
            isListening(FIRESTORE_PORT) && isListening(AUTH_PORT)
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = FirebaseApp.getApps(context).firstOrNull { it.name == APP_NAME } ?: FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApplicationId("ca.bcparkspot.app")
                .setProjectId(PROJECT_ID)
                .setApiKey("emulator-only-key")
                .build(),
            APP_NAME
        ).also { created ->
            FirebaseFirestore.getInstance(created).apply {
                useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
                firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
            }
            FirebaseAuth.getInstance(created).useEmulator(EMULATOR_HOST, AUTH_PORT)
        }
        firestore = FirebaseFirestore.getInstance(app)
        auth = FirebaseAuth.getInstance(app)
        clearFirestore()
    }

    @After
    open fun tearDownFirebase() {
        if (::auth.isInitialized) auth.signOut()
    }

    protected suspend fun signInTestUser(email: String): String = withContext(Dispatchers.IO) {
        withTimeout(AUTH_TIMEOUT_MS) {
            val result = try {
                auth.signInWithEmailAndPassword(email, TEST_PASSWORD).await()
            } catch (e: Exception) {
                auth.createUserWithEmailAndPassword(email, TEST_PASSWORD).await()
            }
            result.user?.uid ?: error("User UID null")
        }
    }

    private fun documentsUrl(path: String = "") =
        "http://$EMULATOR_HOST:$FIRESTORE_PORT/v1/projects/$PROJECT_ID/databases/(default)/documents$path"

    /** The emulator's admin token: bypasses the rules, so tests can seed data no client could write. */
    private fun adminRequest(method: String, url: String, body: String? = null) {
        val conn = URI(url).toURL().openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("Authorization", "Bearer owner")
        if (body != null) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = conn.responseCode
        check(code <= HTTP_OK_MAX) { "$method $url -> $code ${conn.errorStream?.bufferedReader()?.readText()}" }
        conn.disconnect()
    }

    private fun clearFirestore() =
        adminRequest("DELETE", "http://$EMULATOR_HOST:$FIRESTORE_PORT/emulator/v1/projects/$PROJECT_ID/databases/(default)/documents")

    /** Writes a report as the server would hold it, at any age. */
    protected fun seedReport(id: String, userId: String, observedAtIso: String) {
        val body = """{"fields":{
            "userId":{"stringValue":"$userId"},"reporterName":{"stringValue":"BC Hiker"},
            "lotId":{"stringValue":"lot"},"parkName":{"stringValue":"Park"},"status":{"stringValue":"FULL"},
            "comment":{"stringValue":"packed"},"observedAt":{"timestampValue":"$observedAtIso"},
            "createdAt":{"timestampValue":"$observedAtIso"}}}"""
        adminRequest("POST", documentsUrl("/communityReports?documentId=$id"), body)
    }
}
