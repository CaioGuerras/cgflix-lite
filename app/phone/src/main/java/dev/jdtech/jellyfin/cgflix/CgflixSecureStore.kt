package dev.jdtech.jellyfin.cgflix

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dev.jdtech.jellyfin.cgflix.logic.CgflixSessionStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import timber.log.Timber

/**
 * CGFLIX (Etapa 1B): guarda a sessão dos Pedidos (cookie `connect.sid` do Seerr) cifrada com uma
 * chave AES-GCM do Android Keystore (a chave nunca sai do aparelho). Sem biblioteca extra (o
 * EncryptedSharedPreferences foi descontinuado e pesaria no APK). Se a chave sumir ou o dado não
 * abrir, devolve nulo e o app só entra de novo pelo Quick Connect. Nada disto vai para o log.
 */
class CgflixSecureStore(context: Context) : CgflixSessionStore {
    private val prefs = context.getSharedPreferences("cgflix_secure", Context.MODE_PRIVATE)

    override fun read(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        return try {
            val raw = Base64.decode(stored, Base64.NO_WRAP)
            val iv = raw.copyOfRange(0, IV_SIZE)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(raw, IV_SIZE, raw.size - IV_SIZE), Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.w("CGFLIX: sessão dos Pedidos não abriu (${e.javaClass.simpleName}); entra de novo")
            delete(key)
            null
        }
    }

    override fun write(key: String, value: String) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val encrypted = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            prefs.edit().putString(key, Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
        } catch (e: Exception) {
            // Sem onde guardar com segurança: não guarda (a sessão fica só na memória)
            Timber.w("CGFLIX: não deu para guardar a sessão dos Pedidos (${e.javaClass.simpleName})")
        }
    }

    override fun delete(key: String) {
        prefs.edit().remove(key).apply()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let {
            return it
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "cgflix_pedidos"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
