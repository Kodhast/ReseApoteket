package se.kodhast.reseapoteket.ui.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.kodhast.reseapoteket.core.model.Archivable

/** Arkiverade dokument visas inte i listorna förrän "Visa arkiverade" finns för samlingen. */
fun <T : Archivable> Flow<List<T>>.activeOnly(): Flow<List<T>> = map { items -> items.filterNot { it.archived } }
