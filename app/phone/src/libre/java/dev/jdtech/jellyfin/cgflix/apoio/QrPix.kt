package dev.jdtech.jellyfin.cgflix.apoio

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

/**
 * CGFLIX (só no `libre`): módulos do QR Code (`true` = escuro), sem a margem. Usa só o codificador
 * do ZXing core (o R8 tira o resto); o desenho é em Compose ([QrCode]).
 */
fun qrModulos(texto: String): Array<BooleanArray> {
    val matriz = Encoder.encode(texto, ErrorCorrectionLevel.M).matrix
    return Array(matriz.height) { y ->
        BooleanArray(matriz.width) { x -> matriz.get(x, y) == 1.toByte() }
    }
}
