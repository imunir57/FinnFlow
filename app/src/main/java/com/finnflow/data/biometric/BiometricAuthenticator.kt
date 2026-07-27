package com.finnflow.data.biometric

import androidx.fragment.app.FragmentActivity

/**
 * Wraps BiometricManager / BiometricPrompt behind an interface so it can be
 * mocked in ViewModel tests.
 *
 * Logging: All operations are logged at INFO level via SecureLogger, including:
 * - Device biometric capability checks (STRONG/WEAK biometric, device credential availability)
 * - Authentication attempts and outcomes (success/error/failure)
 * - Error details without exposing biometric data
 * - API level compatibility considerations (API 30+ vs earlier)
 */
interface BiometricAuthenticator {
    fun canAuthenticate(): Boolean
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
}
