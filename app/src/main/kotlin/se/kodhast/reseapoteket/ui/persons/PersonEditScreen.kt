package se.kodhast.reseapoteket.ui.persons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.core.model.Person
import se.kodhast.reseapoteket.data.common.AppErrors
import se.kodhast.reseapoteket.data.common.upsertPlaced
import se.kodhast.reseapoteket.data.repository.PersonRepository
import se.kodhast.reseapoteket.ui.components.AvatarSize
import se.kodhast.reseapoteket.ui.components.PERSON_EMOJIS
import se.kodhast.reseapoteket.ui.components.PersonAvatar
import se.kodhast.reseapoteket.ui.components.PersonColorMarker
import se.kodhast.reseapoteket.ui.components.personColorLabel
import se.kodhast.reseapoteket.ui.theme.AppColors
import se.kodhast.ui.common.EditorEvents
import se.kodhast.ui.common.EditorLoader
import se.kodhast.ui.common.EditorState
import se.kodhast.ui.common.EditorUiState
import se.kodhast.ui.common.Validator
import se.kodhast.ui.components.AppTextField
import se.kodhast.ui.components.ColorSwatchPicker
import se.kodhast.ui.components.EmojiPicker
import se.kodhast.ui.components.EntityEditScreen
import se.kodhast.ui.components.LabeledGroup

/** PER-2: namnet är obligatoriskt; emoji och färg har alltid ett värde. */
object PersonValidator : Validator<Person> {
    const val NAME = "name"

    override fun validate(value: Person): Map<String, Int> = if (value.name.isBlank()) mapOf(NAME to R.string.name_required) else emptyMap()
}

sealed interface PersonEditEvent {
    data class Name(val name: String) : PersonEditEvent

    data class Emoji(val emoji: String) : PersonEditEvent

    data class PersonColor(val hex: String) : PersonEditEvent

    data object Save : PersonEditEvent

    data object Retry : PersonEditEvent
}

/** Skapa ([personId] = `null`) eller redigera en person (PER-2). */
@HiltViewModel(assistedFactory = PersonEditViewModel.Factory::class)
class PersonEditViewModel @AssistedInject constructor(
    @Assisted private val personId: String?,
    private val persons: PersonRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(personId: String?): PersonEditViewModel
    }

    val isNew: Boolean = personId == null

    val editor: EditorState<Person> = EditorState(
        Person(id = personId ?: persons.newId(), emoji = PERSON_EMOJIS.first(), colorHex = AppColors.PERSON_HEX.first()),
        PersonValidator,
        AppErrors,
        loading = !isNew
    )
    val state = editor.state
    private val loader = EditorLoader(editor, viewModelScope, personId?.let { id -> { persons.get(id) } }, prepare = { it.withDefaults() })

    fun onEvent(event: PersonEditEvent) {
        when (event) {
            is PersonEditEvent.Name -> editor.update(PersonValidator.NAME) { it.copy(name = event.name) }
            is PersonEditEvent.Emoji -> editor.update { it.copy(emoji = event.emoji) }
            is PersonEditEvent.PersonColor -> editor.update { it.copy(colorHex = event.hex) }
            PersonEditEvent.Save -> viewModelScope.launch { editor.save(::write) }
            PersonEditEvent.Retry -> loader.retry()
        }
    }

    /**
     * Saknas emoji eller färg (äldre eller trasigt dokument) får formuläret förvalen, så att
     * avataren aldrig blir tom; ett värde som appen inte känner till (från en nyare app) står kvar.
     */
    private fun Person.withDefaults(): Person = copy(
        emoji = emoji.ifBlank { PERSON_EMOJIS.first() },
        colorHex = colorHex.ifBlank { AppColors.PERSON_HEX.first() }
    )

    /** En ny person hamnar sist i listan. */
    private suspend fun write(person: Person): Result<Unit> = persons.upsertPlaced(person.copy(name = person.name.trim()), isNew) { copy(sortOrder = it) }
}

@Composable
fun PersonEditScreen(onClose: () -> Unit, viewModel: PersonEditViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PersonEditContent(viewModel.isNew, state, viewModel.editor, viewModel::onEvent, onClose)
}

@Composable
fun PersonEditContent(
    isNew: Boolean,
    state: EditorUiState<Person>,
    events: EditorEvents,
    onEvent: (PersonEditEvent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    EntityEditScreen(
        title = stringResource(if (isNew) R.string.person_new else R.string.person_edit),
        state = state,
        events = events,
        onSave = { onEvent(PersonEditEvent.Save) },
        onClose = onClose,
        modifier = modifier,
        onRetry = { onEvent(PersonEditEvent.Retry) },
        header = { PersonAvatar(state.value.emoji, AppColors.person(state.value.colorHex), size = AvatarSize.Large) }
    ) {
        val person = state.value
        AppTextField(
            person.name,
            { onEvent(PersonEditEvent.Name(it)) },
            stringResource(R.string.name),
            error = state.errorFor(PersonValidator.NAME)?.let { stringResource(it) }
        )
        LabeledGroup(stringResource(R.string.person_emoji)) {
            EmojiPicker(PERSON_EMOJIS, person.emoji, { onEvent(PersonEditEvent.Emoji(it)) })
        }
        LabeledGroup(stringResource(R.string.person_color)) {
            ColorSwatchPicker(AppColors.PERSON_HEX, person.colorHex, { onEvent(PersonEditEvent.PersonColor(it)) }, AppColors::person, { personColorLabel(it) }, PersonColorMarker)
        }
    }
}
