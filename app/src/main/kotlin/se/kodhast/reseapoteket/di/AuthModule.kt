package se.kodhast.reseapoteket.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import se.kodhast.reseapoteket.data.auth.AuthRepository
import se.kodhast.reseapoteket.data.auth.GoogleAuthRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds abstract fun auth(repository: GoogleAuthRepository): AuthRepository
}
