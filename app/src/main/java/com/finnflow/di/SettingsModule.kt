package com.finnflow.di

import com.finnflow.data.biometric.BiometricAuthenticator
import com.finnflow.data.biometric.BiometricAuthenticatorImpl
import com.finnflow.data.notification.ReminderScheduler
import com.finnflow.data.notification.ReminderSchedulerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    abstract fun bindReminderScheduler(impl: ReminderSchedulerImpl): ReminderScheduler

    @Binds
    abstract fun bindBiometricAuthenticator(impl: BiometricAuthenticatorImpl): BiometricAuthenticator
}
