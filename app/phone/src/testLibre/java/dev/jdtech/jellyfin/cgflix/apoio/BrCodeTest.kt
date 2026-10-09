package dev.jdtech.jellyfin.cgflix.apoio

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrCodeTest {
    @Test
    fun `CRC confere com o exemplo do manual do Banco Central`() {
        val exemplo =
            "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-426655440000" +
                "5204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***6304"
        assertEquals("1D3D", BrCode.crc16(exemplo))
    }

    @Test
    fun `payload do CGFLIX campo a campo`() {
        val esperado =
            "000201" +
                "2636" +
                "0014br.gov.bcb.pix" +
                "0114+5522981599647" +
                "52040000" +
                "5303986" +
                "5802BR" +
                "5906CGFLIX" +
                "6014RIO DE JANEIRO" +
                "62070503***" +
                "6304030A"
        assertEquals(esperado, BrCode.estatico(ApoioPix.CHAVE, ApoioPix.NOME, ApoioPix.CIDADE))
    }

    @Test
    fun `CRC do payload gerado confere`() {
        val payload = BrCode.estatico(ApoioPix.CHAVE, ApoioPix.NOME, ApoioPix.CIDADE)
        assertTrue(payload.substring(payload.length - 8).startsWith("6304"))
        assertEquals(
            payload.takeLast(4),
            BrCode.crc16(payload.dropLast(4)),
        )
    }

    @Test
    fun `sem valor fixo (sem campo 54)`() {
        val payload = BrCode.estatico(ApoioPix.CHAVE, ApoioPix.NOME, ApoioPix.CIDADE)
        // Depois do 53 (moeda) vem direto o 58 (país)
        assertTrue(payload.contains("53039865802BR"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `cidade longa demais e recusada`() {
        BrCode.estatico(ApoioPix.CHAVE, ApoioPix.NOME, "SAO JOAO DA BARRA DO NORTE")
    }

    @Test
    fun `QR gerado e lido de volta com o mesmo payload`() {
        val payload = BrCode.estatico(ApoioPix.CHAVE, ApoioPix.NOME, ApoioPix.CIDADE)
        val modulos = qrModulos(payload)
        val escala = 4
        val margem = 4
        val lado = (modulos.size + margem * 2) * escala
        val pixels = IntArray(lado * lado) { 0xFFFFFF }
        for (y in modulos.indices) {
            for (x in modulos[y].indices) {
                if (!modulos[y][x]) continue
                for (dy in 0 until escala) {
                    for (dx in 0 until escala) {
                        val py = (y + margem) * escala + dy
                        val px = (x + margem) * escala + dx
                        pixels[py * lado + px] = 0x000000
                    }
                }
            }
        }
        val imagem = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(lado, lado, pixels)))
        assertEquals(payload, QRCodeReader().decode(imagem).text)
    }
}
