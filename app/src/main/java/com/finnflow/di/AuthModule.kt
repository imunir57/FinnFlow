package com.finnflow.di

import com.finnflow.data.auth.GoogleAuthClient
import com.finnflow.data.auth.GoogleAuthClientImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    abstract fun bindGoogleAuthClient(impl: GoogleAuthClientImpl): GoogleAuthClient
}
