package dev.jdtech.jellyfin.cgflix.apoio

import android.content.Context
import androidx.compose.runtime.Composable

/**
 * CGFLIX: "Apoiar o CGFLIX", gorjeta opcional pelo app. Não destrava nada, sem anúncio e sem
 * lembrete. Cada sabor tem a sua implementação em `apoioDoSabor` (mesmo nome nos dois):
 * - `libre` (APK da VPS/GitHub): só Pix (`src/libre`);
 * - `play` (AAB da Play Store): só Google Play Billing (`src/play`), como manda a política de
 *   Pagamentos da Play. Nada de Pix nesse sabor.
 */
interface Apoio {
    /** Se a entrada "Apoiar o CGFLIX" aparece no Sobre. */
    fun disponivel(): Boolean

    /** Ao abrir o app. No `play`, confirma e consome compras que chegaram depois (pendentes). */
    fun aoAbrirApp(context: Context) {}

    /**
     * Abre o conteúdo do sabor na tela "Apoiar o CGFLIX", abaixo do texto comum. `demonstracao` =
     * dados falsos, só para as capturas do CI (tela de demonstração do build de debug).
     */
    @Composable fun Abrir(demonstracao: Boolean)
}

const val CGFLIX_APOIO_TITULO = "Apoiar o CGFLIX"
const val CGFLIX_APOIO_TEXTO =
    "O CGFLIX Lite é grátis e continua grátis. Se ele te ajuda, você pode apoiar o desenvolvimento."
