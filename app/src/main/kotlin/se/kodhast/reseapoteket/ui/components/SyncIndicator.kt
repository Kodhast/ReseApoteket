package se.kodhast.reseapoteket.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.ui.theme.Blue
import se.kodhast.ui.components.AppTopBar
import se.kodhast.ui.components.InfoPill
import se.kodhast.ui.components.LocalTopBarStatus
import se.kodhast.ui.theme.Tone

/** Synkindikatorn (NFR-6): [pending] när ändringar väntat på servern; [onClick] förklarar den. */
@Immutable
data class SyncIndicator(val pending: Boolean = false, val onClick: () -> Unit = {})

/**
 * Indikatorn som status i `AppTopBar` (android-ui): sätts som `LocalTopBarStatus` av navigationen;
 * utan den visas ingen indikator (t.ex. i tester och galleriet).
 */
fun SyncIndicator.topBarStatus(): @Composable RowScope.() -> Unit = { SyncPill(this@topBarStatus) }

@Composable
private fun RowScope.SyncPill(sync: SyncIndicator) {
    val description = stringResource(R.string.sync_pending_description)
    AnimatedVisibility(sync.pending, enter = fadeIn(), exit = fadeOut()) {
        InfoPill(
            stringResource(R.string.sync_pending),
            Modifier.semantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
            tone = Tone.Blue,
            icon = R.drawable.ic_cloud_upload,
            onClick = sync.onClick
        )
    }
}
