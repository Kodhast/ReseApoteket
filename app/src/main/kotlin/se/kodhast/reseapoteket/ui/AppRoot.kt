package se.kodhast.reseapoteket.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.navigation.AppNavigation
import se.kodhast.reseapoteket.ui.auth.AuthEvent
import se.kodhast.reseapoteket.ui.auth.AuthUiState
import se.kodhast.reseapoteket.ui.auth.AuthViewModel
import se.kodhast.ui.components.AuthGateHost
import se.kodhast.ui.components.SignInScreen
import se.kodhast.ui.components.UpdateRequiredScreen

/**
 * Appens rot: inloggningen styr vad som visas (AUTH-1). [openTrip] är en resa som en påminnelse
 * öppnar (PÅM-7) – den visas när appen är inloggad, och [onTripOpened] släpper den.
 */
@Composable
fun AppRoot(
    modifier: Modifier = Modifier,
    openTrip: String? = null,
    onTripOpened: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppRootContent(state, viewModel::onEvent, modifier) { AppNavigation(account = state.account, openTrip = openTrip, onTripOpened = onTripOpened) }
}

/** Bibliotekets ram med appens texter: resväskan på inloggningen (NFR-22) och hushållets "Uppdatera appen" (HUS-3). */
@Composable
fun AppRootContent(
    state: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    modifier: Modifier = Modifier,
    main: @Composable () -> Unit = { AppNavigation() }
) {
    val context = LocalContext.current
    AuthGateHost(
        gate = state.gate,
        signIn = {
            SignInScreen(
                appIcon = R.drawable.ic_luggage,
                appName = stringResource(R.string.app_name),
                welcome = stringResource(R.string.sign_in_welcome),
                note = stringResource(R.string.sign_in_note),
                busy = state.busy,
                error = state.error,
                onErrorShown = { onEvent(AuthEvent.ErrorShown) },
                // Kontoväljaren kräver aktivitetens kontext (skill android:firebase-auth).
                onSignIn = { onEvent(AuthEvent.SignIn(context)) }
            )
        },
        updateRequired = {
            UpdateRequiredScreen(
                message = stringResource(R.string.update_required_message),
                note = stringResource(R.string.update_required_note),
                releasesUrl = stringResource(R.string.releases_url)
            )
        },
        modifier = modifier,
        content = main
    )
}
