package dev.jdtech.jellyfin.core.presentation.theme

/**
 * CGFLIX: tema escolhido em Configurações > Interface > Aparência (preferência
 * `pref_cgflix_theme`). Isis é o padrão (e o que vale para qualquer valor desconhecido).
 */
enum class CgflixThemeChoice(val value: String) {
    /** Escuro, roxo (preto OLED). */
    ISIS("isis"),
    /** Claro, verde. */
    HEITOR("heitor"),
    /** Segue o modo escuro do aparelho: escuro = Isis, claro = Heitor. */
    AUTO("auto");

    /** `true` quando o app deve usar o tema escuro (Isis). */
    fun isDark(systemIsDark: Boolean): Boolean =
        when (this) {
            ISIS -> true
            HEITOR -> false
            AUTO -> systemIsDark
        }

    companion object {
        fun from(value: String?): CgflixThemeChoice =
            entries.firstOrNull { it.value == value } ?: ISIS
    }
}
