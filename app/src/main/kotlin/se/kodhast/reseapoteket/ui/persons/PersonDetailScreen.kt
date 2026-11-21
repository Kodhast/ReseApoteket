package se.kodhast.reseapoteket.ui.persons

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.core.model.Person
import se.kodhast.reseapoteket.core.model.Regimen
import se.kodhast.reseapoteket.core.model.RegimenKind
import se.kodhast.reseapoteket.core.model.displayName
import se.kodhast.reseapoteket.core.model.listed
import se.kodhast.reseapoteket.data.common.AppErrors
import se.kodhast.reseapoteket.data.repository.PersonRepository
import se.kodhast.reseapoteket.data.repository.RegimenRepository
import se.kodhast.reseapoteket.ui.components.AvatarSize
import se.kodhast.reseapoteket.ui.components.PersonAvatar
import se.kodhast.reseapoteket.ui.regimens.RegimenKindConfig
import se.kodhast.reseapoteket.ui.regimens.RegimenRow
import se.kodhast.reseapoteket.ui.theme.AppColors
import se.kodhast.ui.common.DetailLoader
import se.kodhast.ui.common.DetailUiState
import se.kodhast.ui.common.UiError
import se.kodhast.ui.common.WriteFailures
import se.kodhast.ui.components.AppCard
import se.kodhast.ui.components.AppDivider
import se.kodhast.ui.components.AppIconButton
import se.kodhast.ui.components.AppMenuItem
import se.kodhast.ui.components.DeleteAction
import se.kodhast.ui.components.DeleteConfirmDialog
import se.kodhast.ui.components.DetailHeader
import se.kodhast.ui.components.EntityDetailScreen
import se.kodhast.ui.components.IconButtonVariant
import se.kodhast.ui.components.SectionFoldout
import se.kodhast.ui.components.SwipeToDelete
import se.kodhast.ui.theme.AppTheme

/** En person med sina recept och kosttillskott, utan arkiverade (PER-4). */
data class PersonDetail(val person: Person, val medicines: List<Regimen>, val supplements: List<Regimen>)

sealed interface PersonDetailEvent {
    data object Retry : PersonDetailEvent

    data class SetArchived(val archived: Boolean) : PersonDetailEvent

    /** Radera ett recept eller kosttillskott efter bekräftelse (svep på raden, REC-5). */
    data class DeleteRegimen(val regimen: Regimen) : PersonDetailEvent

    data object ErrorShown : PersonDetailEvent
}

@HiltViewModel(assistedFactory = PersonDetailViewModel.Factory::class)
class PersonDetailViewModel @AssistedInject constructor(
    @Assisted private val personId: String,
    private val persons: PersonRepository,
    private val regimens: RegimenRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(personId: String): PersonDetailViewModel
    }

    private val loader = DetailLoader(
        combine(persons.observe(personId), regimens.forPerson(personId).observe()) { person, all ->
            person?.let {
                PersonDetail(it, all.listed(RegimenKind.MEDICINE), all.listed(RegimenKind.SUPPLEMENT))
            }
        },
        viewModelScope,
        AppErrors
    )
    val state = loader.state

    private val failures = WriteFailures(viewModelScope, AppErrors)

    /** Ett misslyckat arkivera/återställ eller radera, tills skärmen visat det. */
    val error: StateFlow<UiError?> = failures.failure

    fun onEvent(event: PersonDetailEvent) {
        when (event) {
            PersonDetailEvent.Retry -> loader.retry()
            is PersonDetailEvent.SetArchived -> failures.write { persons.setArchived(personId, event.archived) }
            is PersonDetailEvent.DeleteRegimen -> failures.write { regimens.forPerson(personId).delete(event.regimen.id) }
            PersonDetailEvent.ErrorShown -> failures.clear()
        }
    }
}

@Composable
fun PersonDetailScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddRegimen: (RegimenKind) -> Unit,
    onOpenRegimen: (Regimen) -> Unit,
    viewModel: PersonDetailViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    PersonDetailContent(state, viewModel::onEvent, onBack, onEdit, onAddRegimen, onOpenRegimen, error = error)
}

@Composable
fun PersonDetailContent(
    state: DetailUiState<PersonDetail>,
    onEvent: (PersonDetailEvent) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddRegimen: (RegimenKind) -> Unit,
    onOpenRegimen: (Regimen) -> Unit,
    modifier: Modifier = Modifier,
    error: UiError? = null
) {
    val archive = stringResource(R.string.archive)
    val restore = stringResource(R.string.restore)
    val archived = stringResource(R.string.archived_state)
    val summary = (state as? DetailUiState.Content)?.value?.let { personSummary(it.medicines.size, it.supplements.size) }
    EntityDetailScreen(
        state = state,
        header = { DetailHeader(it.person.name, if (it.person.archived) archived else summary) },
        onBack = onBack,
        modifier = modifier,
        onEdit = onEdit,
        menu = {
            // Arkiverad står personen kvar här med "Återställ" – inget behöver ångras (PER-3).
            if (it.person.archived) {
                listOf(AppMenuItem(restore, { onEvent(PersonDetailEvent.SetArchived(false)) }, R.drawable.ic_unarchive))
            } else {
                listOf(AppMenuItem(archive, { onEvent(PersonDetailEvent.SetArchived(true)) }, R.drawable.ic_archive))
            }
        },
        onRetry = { onEvent(PersonDetailEvent.Retry) },
        error = error,
        onErrorShown = { onEvent(PersonDetailEvent.ErrorShown) },
        leading = { PersonAvatar(it.person.emoji, AppColors.person(it.person.colorHex), size = AvatarSize.Large) }
    ) { detail ->
        val delete = { regimen: Regimen -> onEvent(PersonDetailEvent.DeleteRegimen(regimen)) }
        RegimenSection(RegimenKindConfig.of(RegimenKind.MEDICINE), detail.medicines, { onAddRegimen(RegimenKind.MEDICINE) }, onOpenRegimen, delete)
        RegimenSection(RegimenKindConfig.of(RegimenKind.SUPPLEMENT), detail.supplements, { onAddRegimen(RegimenKind.SUPPLEMENT) }, onOpenRegimen, delete)
    }
}

/**
 * Recept eller kosttillskott i ett hopfällbart kort (PER-4, REC-3, REC-5): rubriken med antal fäller ut och ihop, och den
 * runda plusknappen i rubriken lägger till utan att fälla – TalkBack "Lägg till recept"/"Lägg till kosttillskott".
 * Utfällt när detaljen öppnas; läget sparas medan skärmen lever. Svep åt vänster på en rad begär radering (REC-5,
 * android-ui #118): raden fjädrar tillbaka och samma dialog som formulärets Radera frågar.
 */
@Composable
private fun RegimenSection(config: RegimenKindConfig, regimens: List<Regimen>, onAdd: () -> Unit, onOpen: (Regimen) -> Unit, onDelete: (Regimen) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    var deleting by remember { mutableStateOf<Regimen?>(null) }
    AppCard {
        SectionFoldout(
            stringResource(config.title),
            expanded,
            onToggle = { expanded = !expanded },
            icon = config.icon,
            count = regimens.size.toString(),
            tone = config.tone,
            trailing = { AppIconButton(R.drawable.ic_add, stringResource(config.add), onAdd, variant = IconButtonVariant.Tonal, tone = config.tone) },
            emptyText = stringResource(config.none).takeIf { regimens.isEmpty() }
        ) {
            regimens.forEachIndexed { index, regimen ->
                if (index > 0) AppDivider()
                SwipeToDelete({ deleting = regimen }, shape = AppTheme.shapes.row) {
                    // Ogenomskinlig, så att svepets bakgrund bara syns där raden dragits undan.
                    RegimenRow(regimen, onClick = { onOpen(regimen) }, Modifier.clip(AppTheme.shapes.row).background(AppTheme.extendedColors.card))
                }
            }
        }
    }
    deleting?.let { regimen ->
        val action = DeleteAction(stringResource(R.string.delete_title_format, regimen.displayName), stringResource(config.editor.deleteMessage)) { onDelete(regimen) }
        DeleteConfirmDialog(action, onDismiss = { deleting = null })
    }
}
