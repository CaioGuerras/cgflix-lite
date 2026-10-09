package dev.jdtech.jellyfin

import androidx.core.net.toUri
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.toAndroidUri

/**
 * CGFLIX: pede ao servidor imagens do tamanho da tela (`maxWidth`) e com `quality` reduzida, nunca
 * a original, para poupar dados e memória em celular fraco. Só mexe em URLs de imagem do Jellyfin
 * (`.../Images/...`) que ainda não tenham `maxWidth`.
 */
class CgflixImageSizeInterceptor(private val screenWidthPx: Int) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val uri =
            when (val data = request.data) {
                is android.net.Uri -> data
                is coil3.Uri -> data.toAndroidUri()
                is String -> data.toUri()
                else -> null
            }
        val path = uri?.path
        if (
            uri == null ||
                path == null ||
                !path.contains("/Images/", ignoreCase = true) ||
                uri.getQueryParameter("maxWidth") != null ||
                uri.getQueryParameter("fillWidth") != null
        ) {
            return chain.proceed()
        }

        // Fundos ocupam a tela toda; pôsteres e logos nunca passam de ~60% dela.
        val isBackdrop = path.contains("/Backdrop", ignoreCase = true)
        val width = (if (isBackdrop) screenWidthPx else screenWidthPx * 6 / 10).coerceIn(200, 1080)

        val resized =
            uri.buildUpon()
                .appendQueryParameter("maxWidth", width.toString())
                .appendQueryParameter("quality", "80")
                .build()
        return chain.withRequest(request.newBuilder().data(resized).build()).proceed()
    }
}
