package dev.jdtech.jellyfin.cgflix.maryanne

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.User
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jellyfin.sdk.api.client.util.AuthorizationHeaderBuilder
import org.json.JSONObject
import timber.log.Timber

/** Classificação máxima do Modo Maryanne (o servidor aceita "livre" e "10"). */
enum class CgflixMaryanneTeto(val api: String, val rotulo: String) {
    LIVRE("livre", "Livre"),
    DEZ("10", "Até 10 anos"),
}

/** Cobertura "Modo Maryanne ativado" enquanto a home nova carrega. */
data class CgflixMaryanneCobertura(
    val texto: String,
    val desde: Long = System.currentTimeMillis(),
    /** A home do usuário novo já carregou: a cobertura pode sair (depois do tempo mínimo). */
    val homePronta: Boolean = false,
)

/** Falha ao ativar, já com a mensagem em PT para a pessoa. */
class CgflixMaryanneErro(mensagem: String) : Exception(mensagem)

/**
 * CGFLIX (Modo Maryanne): o app vira um app para crianças. O servidor (`POST /__kids/ativar`) cria
 * ou acha o usuário-sombra infantil do pai, com a classificação escolhida, e devolve um token dele.
 * O app troca a sessão para esse usuário e guarda o pai (o token do pai continua no banco local e
 * continua valendo). Sair volta para o pai, encerra a sessão infantil no servidor e apaga o
 * usuário-sombra do banco local.
 *
 * As preferências ficam num arquivo próprio (`cgflix_maryanne`) para não mexer no AppPreferences.
 */
@Singleton
class CgflixMaryanneRepository
@Inject
constructor(
    @ApplicationContext context: Context,
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao,
    private val appPreferences: AppPreferences,
) {
    private val prefs = context.getSharedPreferences("cgflix_maryanne", Context.MODE_PRIVATE)

    private val okHttp =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    private val _ativo = MutableStateFlow(prefs.getBoolean(PREF_ATIVO, false))
    val ativo: StateFlow<Boolean> = _ativo.asStateFlow()

    private val _cobertura = MutableStateFlow<CgflixMaryanneCobertura?>(null)
    val cobertura: StateFlow<CgflixMaryanneCobertura?> = _cobertura.asStateFlow()

    /** Ativo agora (para quem não observa o fluxo, como o ícone do app). */
    val isAtivo: Boolean
        get() = _ativo.value

    var teto: CgflixMaryanneTeto
        get() =
            CgflixMaryanneTeto.entries.firstOrNull { it.api == prefs.getString(PREF_TETO, null) }
                ?: CgflixMaryanneTeto.LIVRE
        set(value) = prefs.edit { putString(PREF_TETO, value.api) }

    /** O pai marcou "Não mostrar de novo" na apresentação. */
    var introOk: Boolean
        get() = prefs.getBoolean(PREF_INTRO_OK, false)
        set(value) = prefs.edit { putBoolean(PREF_INTRO_OK, value) }

    private val baseUrl: String
        get() = jellyfinApi.api.baseUrl.orEmpty().trimEnd('/')

    private fun autorizacao(token: String?): String {
        val api = jellyfinApi.api
        return AuthorizationHeaderBuilder.buildHeader(
            clientName = api.clientInfo.name,
            clientVersion = api.clientInfo.version,
            deviceId = api.deviceInfo.id,
            deviceName = api.deviceInfo.name,
            accessToken = token,
        )
    }

    /** Troca a sessão para o usuário infantil. Lança [CgflixMaryanneErro] com a mensagem em PT. */
    suspend fun ativar(teto: CgflixMaryanneTeto) {
        withContext(Dispatchers.IO) {
            if (_ativo.value) return@withContext
            val serverId =
                appPreferences.getValue(appPreferences.currentServer)
                    ?: throw CgflixMaryanneErro("Entre na sua conta primeiro.")
            val pai = jellyfinApi.userId ?: throw CgflixMaryanneErro("Entre na sua conta primeiro.")
            val corpo = JSONObject().put("teto", teto.api).toString()
            val request =
                Request.Builder()
                    .url("$baseUrl/__kids/ativar")
                    .header("Authorization", autorizacao(jellyfinApi.api.accessToken))
                    .post(corpo.toRequestBody("application/json".toMediaType()))
                    .build()
            val resposta =
                try {
                    okHttp.newCall(request).execute().use { it.code to it.body?.string().orEmpty() }
                } catch (e: Exception) {
                    Timber.w("CGFLIX: Modo Maryanne sem conexão (${e.javaClass.simpleName})")
                    throw CgflixMaryanneErro("Sem conexão com o servidor. Tente de novo.")
                }
            val (codigo, texto) = resposta
            if (codigo != 200) {
                Timber.i("CGFLIX: Modo Maryanne recusado ($codigo)")
                throw CgflixMaryanneErro(mensagemDoCodigo(codigo))
            }
            // Sem o token no log
            val json =
                try {
                    JSONObject(texto)
                } catch (_: Exception) {
                    throw CgflixMaryanneErro(mensagemDoCodigo(502))
                }
            val token = json.optString("token")
            val id = guidParaUuid(json.optString("usuarioId"))
            if (token.isBlank() || id == null) throw CgflixMaryanneErro(mensagemDoCodigo(502))
            val nome = json.optString("nome").ifBlank { "Modo Maryanne" }

            _cobertura.value = CgflixMaryanneCobertura("Modo Maryanne ativado")
            database.insertUser(
                User(id = id, name = nome, serverId = serverId, accessToken = token)
            )
            database.updateServerCurrentUser(serverId, id)
            jellyfinApi.apply {
                api.update(accessToken = token)
                userId = id
            }
            prefs.edit {
                putBoolean(PREF_ATIVO, true)
                putString(PREF_PAI, pai.toString())
                putString(PREF_KID, id.toString())
                putString(PREF_TETO, teto.api)
            }
            _ativo.value = true
        }
    }

    /**
     * Volta para o pai. Devolve `false` quando o pai não está mais no aparelho (o app manda para a
     * tela de usuários).
     */
    suspend fun sair(): Boolean =
        withContext(Dispatchers.IO) {
            val serverId = appPreferences.getValue(appPreferences.currentServer)
            val kid = prefs.getString(PREF_KID, null)?.let(::uuidOuNulo)
            val pai =
                prefs.getString(PREF_PAI, null)?.let(::uuidOuNulo)?.let { database.getUser(it) }
            val tokenKid = jellyfinApi.api.accessToken

            _cobertura.value = CgflixMaryanneCobertura("Saindo do Modo Maryanne")
            // Encerra a sessão infantil no servidor (sem conexão, não trava a saída)
            if (tokenKid != null) {
                try {
                    val request =
                        Request.Builder()
                            .url("$baseUrl/Sessions/Logout")
                            .header("Authorization", autorizacao(tokenKid))
                            .post(ByteArray(0).toRequestBody())
                            .build()
                    okHttp.newCall(request).execute().close()
                } catch (e: Exception) {
                    Timber.i("CGFLIX: logout infantil falhou (${e.javaClass.simpleName})")
                }
            }
            if (kid != null) database.deleteUser(kid)
            if (serverId != null && pai != null && pai.serverId == serverId) {
                database.updateServerCurrentUser(serverId, pai.id)
                jellyfinApi.apply {
                    api.update(accessToken = pai.accessToken)
                    userId = pai.id
                }
            } else {
                serverId
                    ?.let { database.getServer(it) }
                    ?.let {
                        it.currentUserId = null
                        database.updateServer(it)
                    }
                jellyfinApi.apply {
                    api.update(accessToken = null)
                    userId = null
                }
            }
            prefs.edit {
                putBoolean(PREF_ATIVO, false)
                remove(PREF_PAI)
                remove(PREF_KID)
            }
            _ativo.value = false
            val voltou = pai != null && pai.serverId == serverId
            if (!voltou) _cobertura.value = null
            voltou
        }

    /** A home terminou de carregar (ou falhou): a cobertura pode sair. */
    fun homePronta() {
        _cobertura.update { it?.copy(homePronta = true) }
    }

    fun fecharCobertura() {
        _cobertura.value = null
    }

    private fun uuidOuNulo(texto: String): UUID? =
        try {
            UUID.fromString(texto)
        } catch (_: IllegalArgumentException) {
            null
        }

    companion object {
        private const val PREF_ATIVO = "pref_cgflix_maryanne"
        private const val PREF_PAI = "pref_cgflix_maryanne_pai"
        private const val PREF_KID = "pref_cgflix_maryanne_kid"
        private const val PREF_TETO = "pref_cgflix_maryanne_teto"
        private const val PREF_INTRO_OK = "pref_cgflix_maryanne_intro_ok"

        /** O servidor manda o id do Jellyfin sem hífens (32 dígitos hexadecimais). */
        fun guidParaUuid(guid: String): UUID? {
            val limpo = guid.replace("-", "").lowercase()
            if (limpo.length != 32 || limpo.any { it !in "0123456789abcdef" }) return null
            return UUID.fromString(
                "${limpo.substring(0, 8)}-${limpo.substring(8, 12)}-${limpo.substring(12, 16)}-" +
                    "${limpo.substring(16, 20)}-${limpo.substring(20)}"
            )
        }

        fun mensagemDoCodigo(codigo: Int): String =
            when (codigo) {
                401 -> "Sua sessão expirou. Entre de novo na sua conta."
                403 -> "O Modo Maryanne já está ativo nesta conta."
                404 -> "Este servidor não tem o Modo Maryanne."
                409 ->
                    "Já existe um usuário com o nome do Modo Maryanne no servidor. Avise quem cuida do servidor."
                429 -> "Muitas tentativas. Espere um minuto e tente de novo."
                else -> "O servidor não conseguiu ativar o Modo Maryanne agora. Tente mais tarde."
            }
    }
}
