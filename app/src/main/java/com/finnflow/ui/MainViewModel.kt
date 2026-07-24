package com.finnflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.profile.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    profileRepository: UserProfileRepository
) : ViewModel() {

    // null = still loading, true/false = resolved
    val hasCompletedOnboarding = profileRepository.profile
        .map { it.hasCompletedOnboarding as Boolean? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val appLockEnabled = profileRepository.profile
        .map { it.appLockEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun onUnlocked() {
        _isUnlocked.value = true
    }
}
