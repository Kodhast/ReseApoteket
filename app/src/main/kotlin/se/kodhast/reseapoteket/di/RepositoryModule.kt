package se.kodhast.reseapoteket.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import se.kodhast.reseapoteket.data.medicines.AssetMedicineRepository
import se.kodhast.reseapoteket.data.medicines.MedicineRepository
import se.kodhast.reseapoteket.data.repository.DefaultFamilyItemRepository
import se.kodhast.reseapoteket.data.repository.DefaultHouseholdRepository
import se.kodhast.reseapoteket.data.repository.DefaultPackingRepository
import se.kodhast.reseapoteket.data.repository.DefaultPersonRepository
import se.kodhast.reseapoteket.data.repository.DefaultRegimenRepository
import se.kodhast.reseapoteket.data.repository.DefaultTripRepository
import se.kodhast.reseapoteket.data.repository.FamilyItemRepository
import se.kodhast.reseapoteket.data.repository.HouseholdRepository
import se.kodhast.reseapoteket.data.repository.PackingRepository
import se.kodhast.reseapoteket.data.repository.PersonRepository
import se.kodhast.reseapoteket.data.repository.RegimenRepository
import se.kodhast.reseapoteket.data.repository.TripRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun households(repository: DefaultHouseholdRepository): HouseholdRepository

    @Binds abstract fun persons(repository: DefaultPersonRepository): PersonRepository

    @Binds abstract fun regimens(repository: DefaultRegimenRepository): RegimenRepository

    @Binds abstract fun familyItems(repository: DefaultFamilyItemRepository): FamilyItemRepository

    @Binds abstract fun trips(repository: DefaultTripRepository): TripRepository

    @Binds abstract fun packing(repository: DefaultPackingRepository): PackingRepository

    @Binds abstract fun medicines(repository: AssetMedicineRepository): MedicineRepository
}
