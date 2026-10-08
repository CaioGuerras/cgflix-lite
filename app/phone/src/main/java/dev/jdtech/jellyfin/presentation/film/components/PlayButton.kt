package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovie
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.theme.CgflixButton
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun PlayButton(
    item: FindroidItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    // CGFLIX: texto pronto ("Continuar S01E03"); nulo = "Assistir" ou "Continuar"
    label: String? = null,
) {
    // CGFLIX: botão principal grande (52 dp) com "Assistir" / "Continuar"
    CgflixButton(onClick = onClick, modifier = modifier.heightIn(min = 52.dp), enabled = enabled) {
        Icon(painter = painterResource(CoreR.drawable.ic_play), contentDescription = null)
        Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
        Text(
            text =
                label
                    ?: if (item.playbackPositionTicks > 0) {
                        stringResource(CoreR.string.cgflix_continue)
                    } else {
                        stringResource(CoreR.string.cgflix_watch)
                    },
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayButtonMoviePreview() {
    FindroidTheme { PlayButton(item = dummyMovie, onClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun PlayButtonEpisodePreview() {
    FindroidTheme { PlayButton(item = dummyEpisode, onClick = {}) }
}
