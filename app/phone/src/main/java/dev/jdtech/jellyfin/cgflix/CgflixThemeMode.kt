package dev.jdtech.jellyfin.cgflix

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import dev.jdtech.jellyfin.core.presentation.theme.CgflixThemeChoice

/**
 * CGFLIX: modo noturno do app conforme o tema (Isis = escuro, Heitor = claro, automático = segue o
 * aparelho). Vale para o que não é Compose: fundo da janela na abertura, barras do sistema e
 * diálogos. No Android 12+ o sistema guarda a escolha; antes disso, o AppCompat.
 */
fun applyCgflixNightMode(context: Context, choice: CgflixThemeChoice) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val mode =
            when (choice) {
                CgflixThemeChoice.ISIS -> UiModeManager.MODE_NIGHT_YES
                CgflixThemeChoice.HEITOR -> UiModeManager.MODE_NIGHT_NO
                CgflixThemeChoice.AUTO -> UiModeManager.MODE_NIGHT_AUTO
            }
        context.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(mode)
    } else {
        val mode =
            when (choice) {
                CgflixThemeChoice.ISIS -> AppCompatDelegate.MODE_NIGHT_YES
                CgflixThemeChoice.HEITOR -> AppCompatDelegate.MODE_NIGHT_NO
                CgflixThemeChoice.AUTO -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
