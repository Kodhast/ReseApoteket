package se.kodhast.reseapoteket.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.kodhast.reseapoteket.data.auth.AuthRepository
import se.kodhast.reseapoteket.data.common.AppErrors
import se.kodhast.reseapoteket.data.common.dataError
import se.kodhast.ui.account.ProfilePhotoCache
import se.kodhast.ui.common.UiError

/**
 * Utloggningen i avatarmenyn (NAV-5, SET-4, AUTH-4). Profilfotot är ingen data som väntar på servern: det tas bort efter
 * en lyckad utloggning (AUTH-5); ett fel där är inget för användaren. Ett fel från utloggningen står i [error] tills det
 * visats ([onErrorShown]).
 */
@HiltViewModel
class AccountViewModel @Inject constructor(private val auth: AuthRepository, private val photos: ProfilePhotoCache) : ViewModel() {

    private val failure = MutableStateFlow<UiError?>(null)
    val error: StateFlow<UiError?> = failure

    fun signOut() {
        failure.update { null }
        viewModelScope.launch {
            val result = auth.signOut().onSuccess { photos.clear() }
            result.dataError()?.let { error -> failure.update { AppErrors.map(error) } }
        }
    }

    fun onErrorShown() = failure.update { null }
}
