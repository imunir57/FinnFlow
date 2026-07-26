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
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import javax.inject.Inject

class GoogleAuthClientImpl @Inject constructor() : GoogleAuthClient {

    override suspend fun signIn(context: Context): GoogleAuthResult {
        // An empty server client ID means local.properties is missing GOOGLE_WEB_CLIENT_ID.
        // Credential Manager would fail with an opaque provider error; say so plainly instead.
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
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
                GoogleAuthResult.Success(toIdentity(googleIdTokenCredential))
            } else {
                GoogleAuthResult.Error("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleAuthResult.Cancelled
        } catch (e: GetCredentialException) {
            GoogleAuthResult.Error(e.message ?: "Sign-in failed")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Token parsing (createFrom / decodeJwtPayload) throws types that are not
            // GetCredentialException — GoogleIdTokenParsingException, IllegalArgumentException,
            // JSONException. Callers launch this in viewModelScope with no handler, so anything
            // escaping here would crash the app instead of surfacing a sign-in error.
            GoogleAuthResult.Error("Couldn't read the Google account details")
        }
    }

    override suspend fun signOut(context: Context) {
        // Clears Credential Manager's cached sign-in state so the next signIn() re-prompts
        // the account picker instead of silently auto-selecting the same account. Best-effort:
        // a provider failure must not stop the caller from clearing the local profile.
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClearCredentialException) {
            // Ignored — the local sign-out still proceeds.
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
