package se.kodhast.reseapoteket.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.ui.common.toMessage
import se.kodhast.ui.components.AppButton
import se.kodhast.ui.components.AppSnackbarHost
import se.kodhast.ui.components.EmptyState
import se.kodhast.ui.components.LocalAppBrandStyle
import se.kodhast.ui.theme.Spacing

/** Inloggningen (AUTH-1–3). Fel visas som snackbar ovanför knappen; avbrott visas inte alls. */
@Composable
fun SignInScreen(state: AuthUiState, onEvent: (AuthEvent) -> Unit, modifier: Modifier = Modifier) {
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    val error = state.error
    val event by rememberUpdatedState(onEvent)
    LaunchedEffect(error) {
        if (error != null) {
            snackbar.showSnackbar(resources.getString(error.toMessage()))
            event(AuthEvent.ErrorShown)
        }
    }
    // Inloggningen visar appens resväska, inte Kodhästs häst (NFR-22).
    CompositionLocalProvider(LocalAppBrandStyle provides LocalAppBrandStyle.current.copy(emptyStateMark = false)) {
        SignInContent(state, onEvent, snackbar, modifier)
    }
}

@Composable
private fun SignInContent(state: AuthUiState, onEvent: (AuthEvent) -> Unit, snackbar: SnackbarHostState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    EmptyState(
        icon = R.drawable.ic_luggage,
        title = stringResource(R.string.app_name),
        message = stringResource(R.string.sign_in_welcome),
        modifier = modifier,
        note = stringResource(R.string.sign_in_note),
        fullScreen = true,
        action = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                AppSnackbarHost(snackbar)
                AppButton(
                    text = stringResource(if (state.busy) R.string.sign_in_busy else R.string.sign_in_google),
                    onClick = { onEvent(AuthEvent.SignIn(context)) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = R.drawable.ic_login,
                    loading = state.busy
                )
            }
        }
    )
}
