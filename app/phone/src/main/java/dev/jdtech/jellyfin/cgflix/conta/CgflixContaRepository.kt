package dev.jdtech.jellyfin.cgflix.conta

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.api.JellyfinApi
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.util.AuthorizationHeaderBuilder
import org.jellyfin.sdk.model.api.UpdateUserPassword

/** Nome e foto da pessoa logada. `fotoUrl` nulo = sem foto no servidor. */
data class CgflixPerfil(val nome: String, val fotoUrl: String?)

/** Resultado da troca de senha, já pensado para a mensagem em PT. */
enum class CgflixSenhaResultado {
    OK,
    SENHA_ATUAL_ERRADA,
    FALHOU,
}

/**
 * CGFLIX (Minha conta): a própria pessoa troca a foto e a senha no Jellyfin, sem precisar de admin.
 * Vale no servidor inteiro (site, Moonfin, outros apps). Testado no 10.11.11 em 10/10: `POST
 * /UserImage` recebe a imagem em base64 (204), `POST /Users/Password` com a senha atual errada
 * responde 403. Depois da troca de senha o servidor derruba as outras sessões da pessoa e mantém a
 * deste aparelho.
 */
@Singleton
class CgflixContaRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val jellyfinApi: JellyfinApi,
) {
    private val okHttp =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    private val baseUrl: String
        get() = jellyfinApi.api.baseUrl.orEmpty().trimEnd('/')

    suspend fun perfil(): CgflixPerfil =
        withContext(Dispatchers.IO) {
            val user = jellyfinApi.userApi.getCurrentUser().content
            CgflixPerfil(
                nome = user.name.orEmpty(),
                // a etiqueta muda a cada foto nova: o Coil não mostra a antiga do cache
                fotoUrl =
                    user.primaryImageTag?.let {
                        "$baseUrl/UserImage?userId=${user.id}&tag=$it&maxHeight=384"
                    },
            )
        }

    /** Foto da galeria: quadrado central de até 512 px, em JPEG, enviado em base64. */
    suspend fun trocarFoto(uri: Uri) {
        withContext(Dispatchers.IO) {
            val jpeg = prepararFoto(uri)
            val api = jellyfinApi.api
            val auth =
                AuthorizationHeaderBuilder.buildHeader(
                    clientName = api.clientInfo.name,
                    clientVersion = api.clientInfo.version,
                    deviceId = api.deviceInfo.id,
                    deviceName = api.deviceInfo.name,
                    accessToken = api.accessToken,
                )
            val request =
                Request.Builder()
                    .url("$baseUrl/UserImage?userId=${jellyfinApi.userId}")
                    .header("Authorization", auth)
                    .post(
                        Base64.encodeToString(jpeg, Base64.NO_WRAP)
                            .toRequestBody("image/jpeg".toMediaType())
                    )
                    .build()
            okHttp.newCall(request).execute().use {
                if (!it.isSuccessful) error("servidor respondeu ${it.code}")
            }
        }
    }

    suspend fun removerFoto() {
        withContext(Dispatchers.IO) { jellyfinApi.api.imageApi.deleteUserImage(jellyfinApi.userId) }
    }

    suspend fun trocarSenha(atual: String, nova: String): CgflixSenhaResultado =
        withContext(Dispatchers.IO) {
            try {
                jellyfinApi.userApi.updateUserPassword(
                    jellyfinApi.userId,
                    UpdateUserPassword(currentPw = atual, newPw = nova, resetPassword = false),
                )
                CgflixSenhaResultado.OK
            } catch (e: InvalidStatusException) {
                if (e.status == 403 || e.status == 401) CgflixSenhaResultado.SENHA_ATUAL_ERRADA
                else CgflixSenhaResultado.FALHOU
            }
        }

    private fun prepararFoto(uri: Uri): ByteArray {
        val fonte = ImageDecoder.createSource(context.contentResolver, uri)
        // decodifica já reduzida (foto de câmera tem 12 MP+) e com a rotação do EXIF aplicada
        val bitmap =
            ImageDecoder.decodeBitmap(fonte) { decoder, info, _ ->
                val lado = min(info.size.width, info.size.height)
                val escala = max(1, lado / LADO_FOTO)
                decoder.setTargetSize(info.size.width / escala, info.size.height / escala)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        val lado = min(bitmap.width, bitmap.height)
        val recorte =
            Bitmap.createBitmap(
                bitmap,
                (bitmap.width - lado) / 2,
                (bitmap.height - lado) / 2,
                lado,
                lado,
            )
        val final =
            if (lado > LADO_FOTO) Bitmap.createScaledBitmap(recorte, LADO_FOTO, LADO_FOTO, true)
            else recorte
        return ByteArrayOutputStream().use {
            final.compress(Bitmap.CompressFormat.JPEG, 88, it)
            it.toByteArray()
        }
    }

    private companion object {
        const val LADO_FOTO = 512
    }
}
