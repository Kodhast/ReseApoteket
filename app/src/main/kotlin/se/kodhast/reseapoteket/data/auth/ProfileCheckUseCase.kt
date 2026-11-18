package se.kodhast.reseapoteket.data.auth

import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import se.kodhast.ui.account.NetworkStatus

/**
 * Hämtar om den inloggades profil från Firebase (AUTH-5) – så att en ny fotoadress når avataren – när appen startar
 * ansluten eller blir ansluten, en gång per inloggad användare så länge anroparen lever: efter en lyckad hämtning
 * görs inget mer för samma konto. Utloggad eller utan nät görs ingenting; ett misslyckat försök görs om nästa gång
 * appen blir ansluten. Okontrollerat: om Firebase tar in ett utbytt Google-foto här eller först vid nästa inloggning.
 */
class ProfileCheckUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val network: NetworkStatus
) {
    /** Lyssnar så länge anroparen lever (`AuthViewModel`). */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend operator fun invoke() {
        auth.authState
            .map { it?.uid }
            .distinctUntilChanged()
            .flatMapLatest { uid ->
                if (uid == null) {
                    emptyFlow()
                } else {
                    var done = false
                    network.online.filter { it }.takeWhile { !done }.map { done = auth.reload().isSuccess }
                }
            }
            .collect {}
    }
}
