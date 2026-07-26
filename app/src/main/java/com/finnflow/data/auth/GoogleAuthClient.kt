package com.finnflow.data.auth

import android.content.Context

data class GoogleIdentity(
    val googleAccountId: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String?
)

sealed interface GoogleAuthResult {
    data class Success(val identity: GoogleIdentity) : GoogleAuthResult
    data object Cancelled : GoogleAuthResult
    data class Error(val message: String) : GoogleAuthResult
}

/**
 * Wraps CredentialManager / Google ID token retrieval behind an interface so it can be
 * mocked in ViewModel tests, mirroring BiometricAuthenticator's pattern. Callers pass a
 * Context obtained via LocalContext.current in Compose — no Context is retained inside a
 * ViewModel.
 */
interface GoogleAuthClient {
    suspend fun signIn(context: Context): GoogleAuthResult
    suspend fun signOut(context: Context)
}
