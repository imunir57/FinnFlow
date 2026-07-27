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
 *
 * Logging: All operations are logged at INFO level via SecureLogger, including:
 * - Sign-in attempts and outcomes (success/cancellation/error)
 * - Error reasons without exposing credentials or tokens
 * - Sign-out operations (best-effort)
 * Any sensitive data (emails, tokens, IDs) is automatically masked by SecureLogger.
 */
interface GoogleAuthClient {
    suspend fun signIn(context: Context): GoogleAuthResult
    suspend fun signOut(context: Context)
}
