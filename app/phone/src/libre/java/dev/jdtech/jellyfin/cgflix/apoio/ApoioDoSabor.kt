package dev.jdtech.jellyfin.cgflix.apoio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.presentation.theme.CgflixButton
import dev.jdtech.jellyfin.presentation.theme.CgflixOutlinedButton

/** CGFLIX (sabor `libre`): gorjeta só por Pix. Sem biblioteca de Billing neste APK. */
val apoioDoSabor: Apoio = ApoioLibre

private object ApoioLibre : Apoio {
    override fun disponivel() = true

    @Composable
    override fun Abrir(demonstracao: Boolean) {
        val context = LocalContext.current
        val copiaECola = remember {
            BrCode.estatico(ApoioPix.CHAVE, ApoioPix.NOME, ApoioPix.CIDADE)
        }
        val modulos = remember(copiaECola) { qrModulos(copiaECola) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // QR sempre escuro sobre branco (leitura dos bancos), nos dois temas
            Box(
                modifier =
                    Modifier.clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(12.dp)
                        .semantics { contentDescription = "QR Code do Pix" }
            ) {
                QrCode(modulos, Modifier.size(232.dp))
            }
            Text(
                text =
                    "No app do seu banco, escolha Pix e leia o QR Code ou use o \"copia e " +
                        "cola\". O valor é você quem escolhe.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            CgflixButton(
                onClick = { copiar(context, "Pix copia e cola", copiaECola) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Copiar Pix copia e cola")
            }
            CgflixOutlinedButton(
                onClick = { copiar(context, "Chave Pix", ApoioPix.CHAVE) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Copiar chave")
            }
            Text(
                text = "Chave (${ApoioPix.TIPO_DA_CHAVE}): ${ApoioPix.CHAVE} · ${ApoioPix.NOME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Desenha os módulos do QR com a margem obrigatória de 4 módulos. */
@Composable
private fun QrCode(modulos: Array<BooleanArray>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val margem = 4
        val lado = size.minDimension / (modulos.size + margem * 2)
        for (y in modulos.indices) {
            for (x in modulos[y].indices) {
                if (modulos[y][x]) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset((x + margem) * lado, (y + margem) * lado),
                        // +0,5 px evita frestas entre módulos vizinhos
                        size = Size(lado + 0.5f, lado + 0.5f),
                    )
                }
            }
        }
    }
}

private fun copiar(context: Context, rotulo: String, texto: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(rotulo, texto))
    // Android 13+ já mostra o aviso do sistema ao copiar
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
    }
}
