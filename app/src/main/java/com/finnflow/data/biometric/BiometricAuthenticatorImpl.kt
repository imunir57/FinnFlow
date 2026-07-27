package com.finnflow.data.biometric

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class BiometricAuthenticatorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BiometricAuthenticator {

    // DEVICE_CREDENTIAL keeps the user from being locked out of their own data when
    // biometrics become unavailable (fingerprints deleted, or app_lock_enabled restored
    // via Auto Backup onto a device with no biometrics enrolled).
    // BIOMETRIC_STRONG or DEVICE_CREDENTIAL is unsupported below API 30; the compat
    // path there is BIOMETRIC_WEAK or DEVICE_CREDENTIAL.
    private val allowedAuthenticators: Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

    override fun canAuthenticate(): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(allowedAuthenticators) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    override fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                onError("Authentication failed")
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        // No negative button: setNegativeButtonText is disallowed when DEVICE_CREDENTIAL
        // is among the allowed authenticators — the credential fallback takes its place.
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock FinnFlow")
            .setSubtitle("Confirm your identity to continue")
            .setAllowedAuthenticators(allowedAuthenticators)
            .build()

        prompt.authenticate(promptInfo)
    }
}
