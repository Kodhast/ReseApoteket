package se.kodhast.reseapoteket.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import se.kodhast.reseapoteket.BuildConfig
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.data.medicines.MedicineRepository
import se.kodhast.ui.common.DateFormat
import se.kodhast.ui.common.screenSharing
import se.kodhast.ui.components.AboutCard
import se.kodhast.ui.components.AppDivider
import se.kodhast.ui.components.ItemRow
import se.kodhast.ui.components.SettingsPage

/** Om appen (SET-6), en underskärm som avatarmenyns "Om ReseApoteket" öppnar (NAV-5). */
@HiltViewModel
class AboutViewModel @Inject constructor(medicines: MedicineRepository) : ViewModel() {

    /** Datumet för den inbyggda läkemedelslistan (REC-6), eller `null` om den saknas. */
    val medicinesUpdated: StateFlow<LocalDate?> = flow { emit(medicines.catalog().updated) }
        .stateIn(viewModelScope, screenSharing(forgetWhenStopped = false), null)
}

@Composable
fun AboutScreen(onBack: () -> Unit, viewModel: AboutViewModel = hiltViewModel()) {
    val medicinesUpdated by viewModel.medicinesUpdated.collectAsStateWithLifecycle()
    AboutContent(BuildConfig.VERSION_NAME, onBack = onBack, medicinesUpdated = medicinesUpdated)
}

/**
 * @param medicinesUpdated datumet för den inbyggda läkemedelslistan (REC-6), under versionen.
 */
@Composable
fun AboutContent(versionName: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, medicinesUpdated: LocalDate? = null) {
    val uriHandler = LocalUriHandler.current
    val sourceUrl = stringResource(R.string.settings_source_url)
    SettingsPage(stringResource(R.string.about_title), modifier, onBack) {
        AboutCard(stringResource(R.string.app_name), versionName) {
            medicinesUpdated?.let {
                AppDivider()
                ItemRow(stringResource(R.string.settings_medicines), subtitle = stringResource(R.string.settings_medicines_note, DateFormat.display(it)))
            }
            AppDivider()
            ItemRow(
                title = stringResource(R.string.settings_source),
                subtitle = stringResource(R.string.settings_source_note),
                onClick = { uriHandler.openUri(sourceUrl) },
                navigates = true
            )
        }
    }
}
