package se.kodhast.reseapoteket.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import se.kodhast.reseapoteket.data.common.CollectionFactory
import se.kodhast.reseapoteket.data.common.HouseholdVersionSource
import se.kodhast.reseapoteket.data.common.SyncStatus
import se.kodhast.reseapoteket.data.export.HouseholdExport
import se.kodhast.reseapoteket.data.household.HouseholdDirectory

/** Firestore-instansen och det som bygger på den. Ligger här – `FirebaseFirestore` får inte finnas utanför `data/firestore`. */
@Module
@InstallIn(SingletonComponent::class)
abstract class FirestoreModule {

    @Binds
    abstract fun collections(factory: FirestoreCollectionFactory): CollectionFactory

    @Binds
    abstract fun syncStatus(status: FirestoreSyncStatus): SyncStatus

    @Binds
    abstract fun householdVersions(source: FirestoreHouseholdVersions): HouseholdVersionSource

    @Binds
    abstract fun householdDirectory(directory: FirestoreHouseholdDirectory): HouseholdDirectory

    @Binds
    abstract fun rawDocuments(documents: FirestoreRawDocuments): RawDocuments

    @Binds
    abstract fun householdExport(exporter: HouseholdExporter): HouseholdExport

    companion object {
        /** Offline först: lokal cache på 100 MB (skill firestore-data-layer). */
        private const val CACHE_BYTES = 100L * 1024 * 1024

        @Provides
        @Singleton
        fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance().apply {
            firestoreSettings = firestoreSettings {
                setLocalCacheSettings(persistentCacheSettings { setSizeBytes(CACHE_BYTES) })
            }
        }
    }
}
