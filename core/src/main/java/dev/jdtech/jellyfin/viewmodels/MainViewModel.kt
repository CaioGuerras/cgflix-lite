package dev.jdtech.jellyfin.viewmodels

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.core.presentation.theme.CgflixThemeChoice
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.User
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel
@Inject
constructor(private val appPreferences: AppPreferences, private val database: ServerDatabaseDao) :
    ViewModel() {
    // CGFLIX: o tema já vem certo desde a abertura (marca Heitor na abertura do tema Heitor)
    private val _state = MutableStateFlow(MainState(cgflixTheme = readCgflixTheme()))
    val state = _state.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(val server: Server?, val user: User?) : UiState()

        data object Loading : UiState()
    }

    // CGFLIX: troca de tema nas Configurações vale na hora, sem reabrir o app
    private val themeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == appPreferences.cgflixTheme.backendName) {
            _state.update { it.copy(cgflixTheme = readCgflixTheme()) }
        }
    }

    init {
        appPreferences.sharedPreferences.registerOnSharedPreferenceChangeListener(themeListener)
        check()
    }

    override fun onCleared() {
        appPreferences.sharedPreferences.unregisterOnSharedPreferenceChangeListener(themeListener)
    }

    private fun readCgflixTheme(): CgflixThemeChoice =
        CgflixThemeChoice.from(appPreferences.getValue(appPreferences.cgflixTheme))

    private fun check() {
        viewModelScope.launch {
            _state.emit(MainState(isLoading = true, cgflixTheme = readCgflixTheme()))
            val mainState =
                MainState(
                    isLoading = false,
                    isDynamicColors = checkIsDynamicColors(),
                    hasServers = checkHasServers(),
                    hasCurrentServer = checkHasCurrentServer(),
                    hasCurrentUser = checkHasCurrentUser(),
                    isOfflineMode = checkIsOfflineMode(),
                    cgflixTheme = readCgflixTheme(),
                )
            _state.emit(mainState)
        }
    }

    fun loadServerAndUser() {
        viewModelScope.launch {
            val serverId = appPreferences.getValue(appPreferences.currentServer)
            serverId?.let { id ->
                database.getServerWithAddressAndUser(id)?.let { data ->
                    _uiState.emit(UiState.Normal(data.server, data.user))
                }
            }
        }
    }

    private suspend fun checkHasServers(): Boolean {
        val nServers = database.getServersCount()
        return nServers > 0
    }

    private suspend fun checkHasCurrentServer(): Boolean {
        return appPreferences.getValue(appPreferences.currentServer)?.let {
            database.getServer(it) != null
        } == true
    }

    private suspend fun checkHasCurrentUser(): Boolean {
        return appPreferences.getValue(appPreferences.currentServer)?.let {
            database.getServerCurrentUser(it) != null
        } == true
    }

    private fun checkIsDynamicColors(): Boolean {
        return appPreferences.getValue(appPreferences.dynamicColors)
    }

    private fun checkIsOfflineMode(): Boolean {
        return appPreferences.getValue(appPreferences.offlineMode)
    }
}

data class MainState(
    val isLoading: Boolean = true,
    val isDynamicColors: Boolean = true,
    val hasServers: Boolean = false,
    val hasCurrentServer: Boolean = false,
    val hasCurrentUser: Boolean = false,
    val isOfflineMode: Boolean = false,
    val cgflixTheme: CgflixThemeChoice = CgflixThemeChoice.ISIS,
)
