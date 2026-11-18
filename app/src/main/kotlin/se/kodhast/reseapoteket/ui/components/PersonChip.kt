package se.kodhast.reseapoteket.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import se.kodhast.ui.components.AppFilterChip

/** Personchip ("Vem åker med?"): ett [AppFilterChip] med personens avatar före namnet, också när det är valt. */
@Composable
fun PersonChip(
    name: String,
    emoji: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppFilterChip(
        label = name,
        selected = selected,
        onClick = onClick,
        modifier = modifier.heightIn(min = PERSON_CHIP_HEIGHT),
        leading = { PersonAvatar(emoji, color, size = AvatarSize.Small) }
    )
}

private val PERSON_CHIP_HEIGHT = 40.dp
