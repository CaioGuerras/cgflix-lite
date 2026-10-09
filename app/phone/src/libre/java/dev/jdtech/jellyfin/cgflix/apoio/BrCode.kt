package dev.jdtech.jellyfin.cgflix.apoio

/**
 * CGFLIX (só no `libre`): BR Code estático do Pix (padrão EMV do Banco Central), sem valor fixo.
 * Campos: 00 (versão), 26 (`br.gov.bcb.pix` + chave), 52 `0000`, 53 `986` (real), 58 `BR`, 59 nome,
 * 60 cidade, 62/05 txid `***` e 63 CRC16-CCITT (polinômio `0x1021`, início `0xFFFF`).
 */
object BrCode {
    fun estatico(chave: String, nome: String, cidade: String, txid: String = "***"): String {
        require(nome.length <= 25) { "Nome do recebedor com mais de 25 caracteres" }
        require(cidade.length <= 15) { "Cidade com mais de 15 caracteres" }
        val conta = campo("00", "br.gov.bcb.pix") + campo("01", chave)
        val semCrc =
            campo("00", "01") +
                campo("26", conta) +
                campo("52", "0000") +
                campo("53", "986") +
                campo("58", "BR") +
                campo("59", nome) +
                campo("60", cidade) +
                campo("62", campo("05", txid)) +
                "6304"
        return semCrc + crc16(semCrc)
    }

    /** Campo EMV: id (2 dígitos) + tamanho (2 dígitos) + valor. Só ASCII. */
    internal fun campo(id: String, valor: String): String {
        require(valor.length in 1..99 && valor.all { it.code in 32..126 }) { "Campo $id inválido" }
        return id + valor.length.toString().padStart(2, '0') + valor
    }

    /** CRC16-CCITT (FALSE): polinômio 0x1021, início 0xFFFF, em 4 dígitos hexadecimais. */
    fun crc16(texto: String): String {
        var crc = 0xFFFF
        for (byte in texto.toByteArray(Charsets.UTF_8)) {
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return "%04X".format(crc)
    }
}
