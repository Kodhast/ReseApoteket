package se.kodhast.reseapoteket.ui.packing

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import se.kodhast.reseapoteket.core.engine.PackingGroup

/**
 * Packlistans läge under appsessionen (PAK-1, PAK-4): vilka sektioner som är hopfällda och om dolda
 * rader visas. Samma för alla resor, står kvar när man lämnar packlistan och kommer tillbaka, och
 * börjar om (allt utfällt, Dolda gömda) när appen startas om – bara i minnet, aldrig på disk.
 */
@Singleton
class PackingListSession @Inject constructor() {
    /** Hopfällda sektioner; `null` är "Dolda". */
    val collapsed = MutableStateFlow<Set<PackingGroup?>>(emptySet())

    /** "Visa dolda" är valt. */
    val showHidden = MutableStateFlow(false)
}
