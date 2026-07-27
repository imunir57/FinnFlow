package com.finnflow.data.biometric

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.finnflow.data.logger.SecureLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class BiometricAuthenticatorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BiometricAuthenticator {
    companion object {
        private const val TAG = "BiometricAuthenticator"
    }

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
        val apiLevel = Build.VERSION.SDK_INT
        val supportedAuthenticators = if (apiLevel >= Build.VERSION_CODES.R) {
            "BIOMETRIC_STRONG or DEVICE_CREDENTIAL"
        } else {
            "BIOMETRIC_WEAK or DEVICE_CREDENTIAL"
        }

        val canAuth = manager.canAuthenticate(allowedAuthenticators) ==
            BiometricManager.BIOMETRIC_SUCCESS

        SecureLogger.i(TAG, "Biometric capability check [API $apiLevel, supporting $supportedAuthenticators]: $canAuth")

        if (!canAuth) {
            val status = manager.canAuthenticate(allowedAuthenticators)
            val statusName = when (status) {
                BiometricManager.BIOMETRIC_SUCCESS -> "SUCCESS"
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "NO_HARDWARE"
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "HW_UNAVAILABLE"
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "NONE_ENROLLED"
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "SECURITY_UPDATE_REQUIRED"
                else -> "UNKNOWN($status)"
            }
            SecureLogger.i(TAG, "Biometric unavailable: $statusName")
        }

        return canAuth
    }

    override fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        SecureLogger.i(TAG, "Starting biometric authentication prompt")

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                SecureLogger.i(TAG, "Biometric authentication succeeded")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val errorMsg = errString.toString()
                SecureLogger.w(TAG, "Biometric authentication error [$errorCode]: $errorMsg")
                onError(errorMsg)
            }

            override fun onAuthenticationFailed() {
                SecureLogger.i(TAG, "Biometric authentication attempt failed (user can retry)")
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
