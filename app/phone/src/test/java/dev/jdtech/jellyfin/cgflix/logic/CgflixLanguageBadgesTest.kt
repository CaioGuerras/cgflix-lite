package dev.jdtech.jellyfin.cgflix.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class CgflixLanguageBadgesTest {
    private fun audio(lang: String?, title: String? = null) =
        CgflixStreamInfo(CgflixStreamKind.AUDIO, lang, title)

    private fun sub(lang: String?, title: String? = null) =
        CgflixStreamInfo(CgflixStreamKind.SUBTITLE, lang, title)

    @Test
    fun `audio por e Dublado e legenda por ou pob e Legendado`() {
        assertEquals(CgflixLanguageBadges(true, false), cgflixLanguageBadges(listOf(audio("por"), sub("eng"))))
        assertEquals(CgflixLanguageBadges(false, true), cgflixLanguageBadges(listOf(audio("jpn"), sub("pob"))))
        assertEquals(CgflixLanguageBadges(true, true), cgflixLanguageBadges(listOf(audio("pt-BR"), sub("por"))))
    }

    @Test
    fun `legenda em portugues nao faz Dublado e vice-versa`() {
        assertEquals(CgflixLanguageBadges(false, true), cgflixLanguageBadges(listOf(audio("eng"), sub("por"))))
        assertEquals(CgflixLanguageBadges(false, false), cgflixLanguageBadges(listOf(audio("eng"), sub("spa"))))
    }

    @Test
    fun `sem codigo usa o titulo da faixa`() {
        assertEquals(CgflixLanguageBadges(true, false), cgflixLanguageBadges(listOf(audio("und", "Português"))))
        assertEquals(CgflixLanguageBadges(false, false), cgflixLanguageBadges(listOf(audio("eng", "Português"))))
    }
}
