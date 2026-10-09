package dev.jdtech.jellyfin.settings.presentation.settings

import android.content.Intent

sealed interface SettingsEvent {
    data object NavigateToUsers : SettingsEvent

    data object NavigateToServers : SettingsEvent

    data object NavigateToAbout : SettingsEvent

    // CGFLIX: "Apoiar o CGFLIX" (só no celular)
    data object NavigateToApoio : SettingsEvent

    data class NavigateToSettings(val indexes: IntArray) : SettingsEvent

    data class NavigateToSettingsFileEdit(val filePath: String) : SettingsEvent

    data class UpdateTheme(val theme: String) : SettingsEvent

    data class LaunchIntent(val intent: Intent) : SettingsEvent

    data object RestartActivity : SettingsEvent
}
