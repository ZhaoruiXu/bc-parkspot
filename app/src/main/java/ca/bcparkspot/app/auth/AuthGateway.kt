package ca.bcparkspot.app.auth

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class AuthUser(val uid: String, val isAnonymous: Boolean, val email: String?)

enum class LinkOutcome { LINKED, COLLISION }

/** The parts of Firebase Auth the account flows use, so the flows can be tested with a fake. */
interface AuthGateway {
    fun currentUser(): AuthUser?
    fun users(): Flow<AuthUser?>
    suspend fun signInAnonymously()
    suspend fun linkGoogle(idToken: String): LinkOutcome
    suspend fun signInGoogle(idToken: String)
    suspend fun reauthenticateGoogle(idToken: String)
    suspend fun deleteCurrentUser()
    fun signOut()
}

class FirebaseAuthGateway(private val auth: FirebaseAuth) : AuthGateway {
    /** The credential Firebase hands back on a collision; signing in with it avoids reusing a spent token. */
    private var collisionCredential: AuthCredential? = null

    private fun map() = auth.currentUser?.let { AuthUser(it.uid, it.isAnonymous, it.email) }

    override fun currentUser(): AuthUser? = map()

    override fun users(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(map()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInAnonymously() {
        auth.signInAnonymously().await()
    }

    override suspend fun linkGoogle(idToken: String): LinkOutcome {
        val user = auth.currentUser ?: error("No current user to link")
        return try {
            user.linkWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            LinkOutcome.LINKED
        } catch (e: FirebaseAuthUserCollisionException) {
            collisionCredential = e.updatedCredential
            LinkOutcome.COLLISION
        }
    }

    override suspend fun signInGoogle(idToken: String) {
        val credential = collisionCredential ?: GoogleAuthProvider.getCredential(idToken, null)
        collisionCredential = null
        auth.signInWithCredential(credential).await()
    }

    override suspend fun reauthenticateGoogle(idToken: String) {
        val user = auth.currentUser ?: error("No current user to re-authenticate")
        user.reauthenticate(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    override suspend fun deleteCurrentUser() {
        (auth.currentUser ?: error("No current user to delete")).delete().await()
    }

    override fun signOut() = auth.signOut()
}
