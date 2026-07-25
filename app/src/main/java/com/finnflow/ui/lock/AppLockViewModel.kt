package com.finnflow.ui.lock

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.finnflow.data.biometric.BiometricAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val biometricAuthenticator: BiometricAuthenticator
) : ViewModel() {

    fun canAuthenticate(): Boolean = biometricAuthenticator.canAuthenticate()

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        biometricAuthenticator.authenticate(activity, onSuccess, onError)
    }
}
