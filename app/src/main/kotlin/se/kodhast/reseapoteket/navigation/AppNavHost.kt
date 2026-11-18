package se.kodhast.reseapoteket.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavEntry
import se.kodhast.reseapoteket.R
import se.kodhast.ui.common.SyncUiState
import se.kodhast.ui.components.AppMenuItem
import se.kodhast.ui.components.ToolbarAction
import se.kodhast.ui.components.ToolbarItem
import se.kodhast.ui.navigation.AppNavHost as LibraryNavHost

/**
 * Appens navigationsram: bibliotekets `AppNavHost` med appens tre flikar och plusknappens meny. Skärmarna kommer från
 * [entryProvider] (`appEntries` i appen). [sync] får prickarna i flikarnas sidhuvud att hoppa (NFR-6, NFR-22) och ger
 * meddelandet om en nekad skrivning; [onSyncErrorShown] släpper det. [onAdd] är plusknappens meny (NAV-6) och
 * [account] avataren i sidhuvudet (NAV-5).
 */
@Composable
fun AppNavHost(
    backStack: AppBackStack,
    entryProvider: (AppKey) -> NavEntry<AppKey>,
    modifier: Modifier = Modifier,
    sync: SyncUiState = SyncUiState(),
    onSyncErrorShown: () -> Unit = {},
    onAdd: ((AddChoice) -> Unit)? = null,
    account: @Composable () -> Unit = {}
) {
    val addLabel = stringResource(R.string.add)
    LibraryNavHost(
        backStack = backStack,
        tabs = toolbarItems(),
        entryProvider = entryProvider,
        modifier = modifier,
        sync = sync,
        onSyncErrorShown = onSyncErrorShown,
        action = onAdd?.let { ToolbarAction(addLabel, R.drawable.ic_add, addMenu(it)) },
        account = account
    )
}

/** Plusknappens meny (NAV-6): de tre valen i flikarnas ordning, med flikarnas ikoner, som android-ui:s FAB-meny. */
@Composable
private fun addMenu(onPick: (AddChoice) -> Unit): List<AppMenuItem> = AddChoice.entries.map { choice -> AppMenuItem(stringResource(choice.label), { onPick(choice) }, icon = choice.icon) }

/** Verktygsradens flikar, i samma ordning som [TOP_LEVEL]. */
@Composable
private fun toolbarItems(): List<ToolbarItem> = listOf(
    ToolbarItem(stringResource(R.string.tab_trips), R.drawable.ic_luggage),
    ToolbarItem(stringResource(R.string.tab_persons), R.drawable.ic_person),
    ToolbarItem(stringResource(R.string.tab_family_items), R.drawable.ic_family_items)
)
