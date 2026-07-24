package com.finnflow.data.biometric

import androidx.fragment.app.FragmentActivity

/**
 * Wraps BiometricManager / BiometricPrompt behind an interface so it can be
 * mocked in ViewModel tests.
 */
interface BiometricAuthenticator {
    fun canAuthenticate(): Boolean
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
}
