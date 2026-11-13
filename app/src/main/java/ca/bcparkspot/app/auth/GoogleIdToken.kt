package ca.bcparkspot.app.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import ca.bcparkspot.app.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.CancellationException

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

sealed interface GoogleTokenResult {
    data class Token(val idToken: String) : GoogleTokenResult
    data object Cancelled : GoogleTokenResult
    data object NoAccount : GoogleTokenResult
    data object NotConfigured : GoogleTokenResult
    data class Failed(val reason: String?) : GoogleTokenResult
}

private const val PLACEHOLDER_PREFIX = "placeholder"

/** Only reached from a button the user tapped; the app never asks Credential Manager on its own. */
suspend fun requestGoogleIdToken(context: Context): GoogleTokenResult {
    val clientId = context.getString(R.string.default_web_client_id)
    if (clientId.isBlank() || clientId.startsWith(PLACEHOLDER_PREFIX)) return GoogleTokenResult.NotConfigured
    val activity = context.findActivity() ?: return GoogleTokenResult.Failed(null)
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId = clientId).build())
        .build()
    return try {
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            GoogleTokenResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            GoogleTokenResult.Failed(null)
        }
    } catch (e: GetCredentialCancellationException) {
        GoogleTokenResult.Cancelled
    } catch (e: NoCredentialException) {
        GoogleTokenResult.NoAccount
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        GoogleTokenResult.Failed(e.message)
    }
}

/** After sign-out, forget the chosen Google account so the next sign-in shows the account picker. */
suspend fun clearGoogleCredentialState(context: Context) {
    try {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Nothing to clear is fine.
    }
}
