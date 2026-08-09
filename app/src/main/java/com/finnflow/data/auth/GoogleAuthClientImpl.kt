package com.finnflow.data.auth

import android.content.Context
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.finnflow.BuildConfig
import com.finnflow.data.logger.SecureLogger
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import javax.inject.Inject

class GoogleAuthClientImpl @Inject constructor() : GoogleAuthClient {
    companion object {
        private const val TAG = "GoogleAuthClient"
    }

    override suspend fun signIn(context: Context): GoogleAuthResult {
        SecureLogger.i(TAG, "Attempting Google sign-in")

        // An empty server client ID means local.properties is missing GOOGLE_WEB_CLIENT_ID.
        // Credential Manager would fail with an opaque provider error; say so plainly instead.
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            SecureLogger.w(TAG, "Google sign-in not configured: GOOGLE_WEB_CLIENT_ID is blank")
            return GoogleAuthResult.Error("Google sign-in isn't configured for this build")
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = CredentialManager.create(context).getCredential(context, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val identity = toIdentity(googleIdTokenCredential)
                SecureLogger.i(TAG, "Google sign-in successful for user")
                GoogleAuthResult.Success(identity)
            } else {
                SecureLogger.w(TAG, "Unexpected credential type received during sign-in: ${credential?.type ?: "null"}")
                GoogleAuthResult.Error("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            SecureLogger.i(TAG, "Google sign-in cancelled by user")
            GoogleAuthResult.Cancelled
        } catch (e: GetCredentialException) {
            SecureLogger.w(TAG, "GetCredentialException during sign-in: ${e.message ?: "unknown error"}")
            GoogleAuthResult.Error(e.message ?: "Sign-in failed")
        } catch (e: CancellationException) {
            SecureLogger.i(TAG, "Google sign-in cancelled")
            throw e
        } catch (e: Exception) {
            // Token parsing (createFrom / decodeJwtPayload) throws types that are not
            // GetCredentialException — GoogleIdTokenParsingException, IllegalArgumentException,
            // JSONException. Callers launch this in viewModelScope with no handler, so anything
            // escaping here would crash the app instead of surfacing a sign-in error.
            SecureLogger.e(TAG, "Exception parsing Google credentials: ${e.javaClass.simpleName}", e)
            GoogleAuthResult.Error("Couldn't read the Google account details")
        }
    }

    override suspend fun signOut(context: Context) {
        SecureLogger.i(TAG, "Attempting Google sign-out")

        // Clears Credential Manager's cached sign-in state so the next signIn() re-prompts
        // the account picker instead of silently auto-selecting the same account. Best-effort:
        // a provider failure must not stop the caller from clearing the local profile. Catching
        // ClearCredentialException alone is not enough for that — a device with no credential
        // provider, or a broken Play services install, throws from outside that hierarchy, and
        // letting it escape used to abort sign-out with the account still on the device.
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            SecureLogger.i(TAG, "Credential Manager cache cleared successfully")
        } catch (e: CancellationException) {
            SecureLogger.i(TAG, "Google sign-out cancelled")
            throw e
        } catch (e: Exception) {
            // Ignored — the local sign-out still proceeds.
            SecureLogger.w(TAG, "${e.javaClass.simpleName} during sign-out (continuing anyway): ${e.message}")
        }
    }

    /**
     * Decodes the ID token's JWT payload for DISPLAY PURPOSES ONLY — it does not verify the
     * JWT signature against Google's public keys, which is normally a server's job before
     * trusting the claims for authorization. FinnFlow has no backend and makes no
     * authorization decision from this data; the claims only ever prefill local profile
     * fields (name/email/avatar) shown back to the same device that just completed the
     * Google-hosted sign-in UI. If FinnFlow gains a backend, this must be replaced with
     * server-side verification before being trusted for anything beyond display.
     */
    private fun toIdentity(credential: GoogleIdTokenCredential): GoogleIdentity {
        val payload = decodeJwtPayload(credential.idToken)
        return GoogleIdentity(
            googleAccountId = credential.id,
            displayName = credential.displayName ?: payload.optString("name", ""),
            email = payload.optString("email", ""),
            avatarUrl = credential.profilePictureUri?.toString()
                ?: payload.optString("picture").takeIf { it.isNotEmpty() }
        )
    }

    private fun decodeJwtPayload(jwt: String): JSONObject {
        val parts = jwt.split(".")
        require(parts.size == 3) { "Malformed ID token" }
        val bytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        return JSONObject(String(bytes, Charsets.UTF_8))
    }
}
