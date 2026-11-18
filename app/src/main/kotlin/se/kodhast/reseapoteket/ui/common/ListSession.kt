package se.kodhast.reseapoteket.ui.common

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import se.kodhast.reseapoteket.core.engine.PackingGroup
import se.kodhast.reseapoteket.core.model.FamilyItemCategory

/**
 * Listornas läge under appsessionen: vilka sektioner som är hopfällda och om packlistans dolda rader
 * visas (PAK-1, PAK-4, FAM-1). Står kvar när man lämnar en lista och kommer tillbaka, och börjar om
 * (allt utfällt, Dolda gömda) när appen startas om – bara i minnet, aldrig på disk. Den enda hållaren:
 * en ny lista med samma behov får ett fält här.
 */
@Singleton
class ListSession @Inject constructor() {
    /** Packlistans hopfällda sektioner, samma för alla resor; `null` är "Dolda". */
    val packingCollapsed = MutableStateFlow<Set<PackingGroup?>>(emptySet())

    /** Packlistans "Visa dolda" är valt. */
    val packingShowHidden = MutableStateFlow(false)

    /** Gemensamts hopfällda kategorier. */
    val familyCollapsed = MutableStateFlow<Set<FamilyItemCategory>>(emptySet())
}
