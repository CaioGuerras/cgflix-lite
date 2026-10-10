package dev.jdtech.jellyfin.settings.presentation.models

import androidx.annotation.StringRes

data class PreferenceGroup(
    @param:StringRes val nameStringResource: Int? = null,
    val preferences: List<Preference>,
    // CGFLIX: texto de ajuda mostrado embaixo do grupo
    @param:StringRes val cgflixNotaRes: Int? = null,
)
