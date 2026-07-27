package com.finnflow.ui.lock

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.finnflow.data.biometric.BiometricAuthenticator
import com.finnflow.data.logger.SecureLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val biometricAuthenticator: BiometricAuthenticator
) : ViewModel() {
    companion object {
        private const val TAG = "AppLockViewModel"
    }

    fun canAuthenticate(): Boolean {
        val canAuth = biometricAuthenticator.canAuthenticate()
        SecureLogger.i(TAG, "Lock screen: biometric authentication available: $canAuth")
        return canAuth
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        SecureLogger.i(TAG, "Initiating unlock attempt via biometric authentication")
        biometricAuthenticator.authenticate(
            activity,
            onSuccess = {
                SecureLogger.i(TAG, "Unlock successful, session unlocked")
                onSuccess()
            },
            onError = { errorMsg ->
                SecureLogger.w(TAG, "Unlock failed: $errorMsg")
                onError(errorMsg)
            }
        )
    }
}
