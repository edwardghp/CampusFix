package com.campusfix.di

import com.campusfix.data.repository.AuthRepositoryImpl
import com.campusfix.data.repository.AulaRepositoryImpl
import com.campusfix.data.repository.ProfileRepositoryImpl
import com.campusfix.data.repository.UserRepositoryImpl
import com.campusfix.domain.repository.UserRepository
import com.campusfix.domain.repository.AuthRepository
import com.campusfix.domain.repository.AulaRepository
import com.campusfix.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.campusfix.data.repository.TicketRepositoryImpl
import com.campusfix.domain.repository.TicketRepository

/** Modulo Hilt: enlaza cada interfaz de repositorio con su implementacion. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds @Singleton
    abstract fun bindAulaRepository(impl: AulaRepositoryImpl): AulaRepository
    @Binds @Singleton
    abstract fun bindTicketRepository(impl: TicketRepositoryImpl): TicketRepository
    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
