package dev.jdtech.jellyfin

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.cgflix.apoio.apoioDoSabor
import dev.jdtech.jellyfin.cgflix.maryanne.CgflixMaryanneCoberturaOverlay
import dev.jdtech.jellyfin.cgflix.maryanne.CgflixMaryanneRepository
import dev.jdtech.jellyfin.cgflix.maryanne.LocalCgflixMaryanne
import dev.jdtech.jellyfin.cgflix.maryanne.maryanneIntroAberta
import dev.jdtech.jellyfin.cgflix.ui.CgflixOpening
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.viewmodels.MainViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    // CGFLIX (Modo Maryanne): tema infantil e cobertura da troca de sessão
    @Inject lateinit var maryanne: CgflixMaryanneRepository

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // CGFLIX: no sabor `play`, consome gorjetas que se concluíram depois (pendentes)
        if (savedInstanceState == null) apoioDoSabor.aoAbrirApp(this)

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val maryanneAtivo by maryanne.ativo.collectAsStateWithLifecycle()
            val cobertura by maryanne.cobertura.collectAsStateWithLifecycle()

            // CGFLIX: tema escolhido (Isis, Heitor ou automático), sem cores dinâmicas (trocariam
            // a marca); ícones das barras do sistema escuros no tema claro
            val darkTheme = state.cgflixTheme.isDark(isSystemInDarkTheme())
            // Modo Maryanne (e a apresentação dele) é claro
            val barrasEscuras = darkTheme && !maryanneAtivo && !maryanneIntroAberta.value
            LaunchedEffect(barrasEscuras) {
                val style =
                    if (barrasEscuras) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            FindroidTheme(darkTheme = darkTheme, dynamicColor = false, maryanne = maryanneAtivo) {
                val navController = rememberNavController()
                // CGFLIX: testTag vira resource-id (testes de interface e o robô do Test Lab)
                Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                    if (!state.isLoading) {
                        CompositionLocalProvider(
                            LocalOfflineMode provides state.isOfflineMode,
                            LocalCgflixMaryanne provides maryanneAtivo,
                        ) {
                            NavigationRoot(
                                navController = navController,
                                hasServers = state.hasServers,
                                hasCurrentServer = state.hasCurrentServer,
                                hasCurrentUser = state.hasCurrentUser,
                            )
                        }
                    } else {
                        CgflixOpening() // CGFLIX: abertura com o slogan
                    }
                    CgflixMaryanneCoberturaOverlay(cobertura, onFechar = maryanne::fecharCobertura)
                }
            }
        }
    }
}
