package com.conest.app.di

import com.conest.app.data.auth.AuthRepository
import com.conest.app.data.auth.FirebaseAuthRepository
import com.conest.app.data.profile.FirestoreProfileRepository
import com.conest.app.data.profile.ProfileRepository
import com.conest.app.data.social.DiscoveryRepository
import com.conest.app.data.social.FirestoreDiscoveryRepository
import com.conest.app.data.social.FirestoreMatchRepository
import com.conest.app.data.social.FirestoreSafetyRepository
import com.conest.app.data.social.MatchRepository
import com.conest.app.data.social.SafetyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds @Singleton
    abstract fun bindAuth(impl: FirebaseAuthRepository): AuthRepository

    @Binds @Singleton
    abstract fun bindProfiles(impl: FirestoreProfileRepository): ProfileRepository

    @Binds @Singleton
    abstract fun bindDiscovery(impl: FirestoreDiscoveryRepository): DiscoveryRepository

    @Binds @Singleton
    abstract fun bindMatches(impl: FirestoreMatchRepository): MatchRepository

    @Binds @Singleton
    abstract fun bindSafety(impl: FirestoreSafetyRepository): SafetyRepository
}
