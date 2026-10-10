package dev.jdtech.jellyfin.cgflix.apoio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.spacings

/** CGFLIX: tela "Apoie o CGFLIX" (Configurações → Sobre, e o último item das Configurações). */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CgflixApoioScreen(navigateBack: () -> Unit, demonstracao: Boolean = false) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = CGFLIX_APOIO_TITULO) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = "Voltar",
                        )
                    }
                },
                windowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(MaterialTheme.spacings.default),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        ) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_cgflix_heart),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = CGFLIX_APOIO_TEXTO,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            apoioDoSabor.Abrir(demonstracao)
        }
    }
}
