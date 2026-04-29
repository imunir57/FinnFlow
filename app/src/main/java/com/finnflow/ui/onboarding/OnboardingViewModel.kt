package com.finnflow.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.profile.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository
) : ViewModel() {

    private val _navigateHome = Channel<Unit>(Channel.BUFFERED)
    val navigateHome = _navigateHome.receiveAsFlow()

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
}
