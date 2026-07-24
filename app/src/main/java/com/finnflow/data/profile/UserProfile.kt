package com.finnflow.data.profile

data class UserProfile(
    val displayName: String = "",
    val initials: String = "",
    val hasCompletedOnboarding: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val appLockEnabled: Boolean = false
)