package se.kodhast.reseapoteket.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import se.kodhast.reseapoteket.data.auth.AuthRepository
import se.kodhast.reseapoteket.data.auth.GoogleAuthRepository
import se.kodhast.ui.account.CoilProfilePhotoCache
import se.kodhast.ui.account.ConnectivityMonitor
import se.kodhast.ui.account.NetworkStatus
import se.kodhast.ui.account.ProfilePhotoCache

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds abstract fun auth(repository: GoogleAuthRepository): AuthRepository

    companion object {
        /** Profilfotots cache på enheten (AUTH-5), töms vid utloggning (AUTH-4) – android-ui:s `CoilProfilePhotoCache`. */
        @Provides
        fun profilePhotos(@ApplicationContext context: Context, @IoDispatcher io: CoroutineDispatcher): ProfilePhotoCache = CoilProfilePhotoCache(context, io)

        /** Om appen har internet – pluppen på avataren (NAV-5) och när profilen hämtas om (AUTH-5). En per app. */
        @Provides
        @Singleton
        fun networkStatus(@ApplicationContext context: Context, @ApplicationScope scope: CoroutineScope): NetworkStatus = ConnectivityMonitor(context, scope)
    }
}
