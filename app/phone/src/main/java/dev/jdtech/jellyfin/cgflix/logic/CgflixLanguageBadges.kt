package dev.jdtech.jellyfin.cgflix.logic

// CGFLIX (Etapa 1B): selos "Dublado" (tem áudio em português) e "Legendado" (tem legenda em
// português, inclusive a externa `<vídeo>.por.srt`). Códigos aceitos: por, pob, pt, pt-BR.

enum class CgflixStreamKind {
    AUDIO,
    SUBTITLE,
    OTHER,
}

data class CgflixStreamInfo(val kind: CgflixStreamKind, val language: String?, val title: String? = null)

data class CgflixLanguageBadges(val dubbed: Boolean, val subtitled: Boolean) {
    val isEmpty: Boolean
        get() = !dubbed && !subtitled
}

private val portugueseCodes = setOf("por", "pob", "pt", "pt-br", "pt_br", "ptbr", "pt-pt", "portuguese")

fun cgflixIsPortuguese(language: String?, title: String? = null): Boolean {
    val code = language?.trim()?.lowercase().orEmpty()
    if (code in portugueseCodes) return true
    // Sem código de idioma, o título da faixa às vezes diz ("Português", "PT-BR")
    if (code.isEmpty() || code == "und") {
        val t = cgflixNormalize(title.orEmpty())
        return t.contains("portugues") || t.contains("pt-br")
    }
    return false
}

fun cgflixLanguageBadges(streams: List<CgflixStreamInfo>): CgflixLanguageBadges =
    CgflixLanguageBadges(
        dubbed =
            streams.any { it.kind == CgflixStreamKind.AUDIO && cgflixIsPortuguese(it.language, it.title) },
        subtitled =
            streams.any {
                it.kind == CgflixStreamKind.SUBTITLE && cgflixIsPortuguese(it.language, it.title)
            },
    )
