package com.finnflow.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.auth.GoogleAuthClient
import com.finnflow.data.auth.GoogleAuthResult
import com.finnflow.data.profile.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val googleAuthClient: GoogleAuthClient
) : ViewModel() {

    private val _navigateHome = Channel<Unit>(Channel.BUFFERED)
    val navigateHome = _navigateHome.receiveAsFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun onGetStarted(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) profileRepository.saveProfile(name)
            profileRepository.completeOnboarding()
            _navigateHome.send(Unit)
        }
    }

    fun onSkip() {
        viewModelScope.launch {
            profileRepository.completeOnboarding()
            _navigateHome.send(Unit)
        }
    }

    fun onSignInWithGoogle(context: Context) {
        viewModelScope.launch {
            when (val result = googleAuthClient.signIn(context)) {
                is GoogleAuthResult.Success -> {
                    profileRepository.signInWithGoogle(
                        displayName = result.identity.displayName,
                        email = result.identity.email,
                        avatarUrl = result.identity.avatarUrl,
                        googleAccountId = result.identity.googleAccountId
                    )
                    profileRepository.completeOnboarding()
                    _navigateHome.send(Unit)
                }
                is GoogleAuthResult.Cancelled -> Unit
                is GoogleAuthResult.Error -> _messages.send(result.message)
            }
        }
    }
}
