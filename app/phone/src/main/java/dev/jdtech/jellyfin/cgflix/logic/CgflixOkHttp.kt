package dev.jdtech.jellyfin.cgflix.logic

import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

// CGFLIX (Etapa 1B): transporte HTTP do CGFLIX (Seerr e emalta.json) com OkHttp, que o app já
// carrega (Coil). Sem logging interceptor: cookies nunca vão para o log.
class CgflixOkHttp(private val client: OkHttpClient) : CgflixHttp {
    override suspend fun send(request: CgflixHttpRequest): CgflixHttpResponse {
        val body = request.jsonBody?.toRequestBody("application/json; charset=utf-8".toMediaType())
        val builder =
            Request.Builder()
                .url(request.url)
                .header("Accept", "application/json")
                .method(request.method, body)
        request.cookie?.let { builder.header("Cookie", it) }
        val call = client.newCall(builder.build())
        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (cont.isActive) cont.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val result =
                            try {
                                response.use {
                                    CgflixHttpResponse(
                                        code = it.code,
                                        body = it.body.string(),
                                        setCookies = it.headers("Set-Cookie"),
                                    )
                                }
                            } catch (e: IOException) {
                                if (cont.isActive) cont.resumeWithException(e)
                                return
                            }
                        if (cont.isActive) cont.resume(result)
                    }
                }
            )
        }
    }
}
