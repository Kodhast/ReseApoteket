package se.kodhast.reseapoteket.ui.sync

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import se.kodhast.reseapoteket.data.common.AppErrors
import se.kodhast.reseapoteket.data.common.SyncStatus
import se.kodhast.ui.account.NetworkStatus
import se.kodhast.ui.common.UiError
import se.kodhast.ui.common.shownAfter

/**
 * [pending]: ändringar har väntat minst [SyncViewModel.PENDING_DELAY] på servern (prickarna hoppar, NFR-6);
 * [writeError]: en nekad skrivning att visa; [online]: appen har internet (pluppen på avataren, NAV-5).
 */
@Immutable
data class SyncUiState(val pending: Boolean = false, val writeError: UiError? = null, val online: Boolean = false)

sealed interface SyncEvent {
    /** Felet har visats och kan släppas. */
    data object WriteErrorShown : SyncEvent
}

/** Appens synk- och nätläge för UI:t (NFR-6, NAV-5): prickarna i sidhuvudet, pluppen på avataren och meddelandet om en nekad skrivning. */
@HiltViewModel
class SyncViewModel @Inject constructor(private val status: SyncStatus, network: NetworkStatus) : ViewModel() {

    val state: StateFlow<SyncUiState> = combine(status.syncing.shownAfter(PENDING_DELAY), status.lastWriteError.map { it?.let(AppErrors::map) }, network.online, ::SyncUiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, SyncUiState(online = network.online.value))

    fun onEvent(event: SyncEvent) {
        when (event) {
            SyncEvent.WriteErrorShown -> status.clearWriteError()
        }
    }

    companion object {
        /** Så länge en skrivning får vänta innan prickarna hoppar – online syns det aldrig. */
        val PENDING_DELAY = 2.seconds
    }
}
