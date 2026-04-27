package com.finnflow.data.profile

data class UserProfile(
    val displayName: String = "",
    val initials: String = "",
    val hasCompletedOnboarding: Boolean = false
)