package se.kodhast.reseapoteket.navigation

import androidx.compose.runtime.Composable
import se.kodhast.ui.navigation.TabBackStack
import se.kodhast.ui.navigation.rememberTabBackStack

/**
 * Appens back stack: bibliotekets [TabBackStack] med en stack per flik (NAV-1–3) över flikarna i [TOP_LEVEL] –
 * appen startar på Resor, tillbaka i en annan fliks rot går till Resor och tillbaka i Resors rot lämnar appen.
 * Ett sparat läge som inte går att läsa (från en äldre version eller när Inställningar var en flik) ger startläget.
 */
typealias AppBackStack = TabBackStack<AppKey>

/** En ny back stack i startläget. */
fun appBackStack(): AppBackStack = TabBackStack(TOP_LEVEL, AppKey.serializer())

/** Back stacken, sparad över rotation och processdöd. */
@Composable
fun rememberAppBackStack(): AppBackStack = rememberTabBackStack(TOP_LEVEL, AppKey.serializer())
