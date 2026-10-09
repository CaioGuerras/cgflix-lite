package dev.jdtech.jellyfin.cgflix.theme

import dev.jdtech.jellyfin.core.presentation.theme.CgflixThemeChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CgflixThemeChoiceTest {
    @Test
    fun `Isis e o padrao, inclusive para valor desconhecido ou antigo`() {
        assertEquals(CgflixThemeChoice.ISIS, CgflixThemeChoice.from(null))
        assertEquals(CgflixThemeChoice.ISIS, CgflixThemeChoice.from("dark"))
        assertEquals(CgflixThemeChoice.ISIS, CgflixThemeChoice.from("isis"))
        assertEquals(CgflixThemeChoice.HEITOR, CgflixThemeChoice.from("heitor"))
        assertEquals(CgflixThemeChoice.AUTO, CgflixThemeChoice.from("auto"))
    }

    @Test
    fun `Isis e sempre escuro, Heitor sempre claro, automatico segue o aparelho`() {
        assertTrue(CgflixThemeChoice.ISIS.isDark(systemIsDark = false))
        assertFalse(CgflixThemeChoice.HEITOR.isDark(systemIsDark = true))
        assertTrue(CgflixThemeChoice.AUTO.isDark(systemIsDark = true))
        assertFalse(CgflixThemeChoice.AUTO.isDark(systemIsDark = false))
    }
}
