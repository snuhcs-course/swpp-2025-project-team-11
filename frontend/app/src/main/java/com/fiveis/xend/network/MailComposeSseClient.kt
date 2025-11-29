package com.fiveis.xend.network

import android.content.Context
import android.util.Log
import com.fiveis.xend.data.source.TokenManager
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject

private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BASIC
}

interface MailComposeStreamSubject {
    fun start(
        payload: JSONObject,
        onSubject: (String) -> Unit,
        onBodyDelta: (Int, String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    )

    fun stop()
}

private fun defaultStreamingClient(context: Context, tokenManager: TokenManager): OkHttpClient {
    return OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
//        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val accessToken = tokenManager.getAccessToken()
            val request = if (accessToken != null) {
                chain.request().newBuilder().addHeader("Authorization", "Bearer $accessToken").build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
        .authenticator(TokenRefreshAuthenticator(context, tokenManager))
        .build()
}

class RemoteMailComposeSubject(
    private val endpointUrl: String,
    private val client: OkHttpClient
) : MailComposeStreamSubject {
    private var call: Call? = null
    private var readerJob: Job? = null
    private val stopped = AtomicBoolean(false)

    override fun start(
        payload: JSONObject,
        onSubject: (String) -> Unit,
        onBodyDelta: (Int, String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        stop()

        val json = payload.toString()
        val reqBody = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(endpointUrl)
            .post(reqBody)
            .build()

        Log.d("SSE_REQ", "POST $endpointUrl\nContent-Type: application/json\nBody: $json")

        call = client.newCall(request)
        stopped.set(false)

        call!!.enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                if (!stopped.get()) {
                    onError("SSE connect failed: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    onError("HTTP ${response.code} ${response.message} | body=${body.take(500)}")
                    response.close()
                    return
                }

                val source = response.body?.source()
                if (source == null) {
                    onError("No response body")
                    response.close()
                    return
                }

                readerJob = CoroutineScope(Dispatchers.IO).launch {
                    try {
                        parseSseStream(
                            source,
                            onEvent = { event, data ->
                                try {
                                    val obj = JSONObject(data)
                                    when (event) {
                                        "ready" -> {} // 필요시 사용
                                        "subject" -> onSubject(obj.optString("title", obj.optString("text")))
                                        "body.delta" -> onBodyDelta(obj.optInt("seq"), obj.optString("text"))
                                        "done" -> onDone()
                                        "error" -> onError(obj.optString("message", "server error"))
                                    }
                                } catch (e: Exception) {
                                    onError("Parse error: ${e.message}. raw=${data.take(200)}")
                                }
                            }
                        )
                    } catch (e: Exception) {
                        if (!stopped.get()) onError("SSE read failed: ${e.message}")
                    } finally {
                        response.close()
                    }
                }
            }
        })
    }

    override fun stop() {
        stopped.set(true)
        readerJob?.cancel()
        readerJob = null
        call?.cancel()
        call = null
    }

    private fun parseSseStream(source: okio.BufferedSource, onEvent: (event: String, data: String) -> Unit) {
        var curEvent = "message"
        val dataLines = mutableListOf<String>()

        while (true) {
            val raw = source.readUtf8Line() ?: break
            val line = raw.removePrefix("\uFEFF")

            if (line.isEmpty()) {
                if (dataLines.isNotEmpty()) {
                    onEvent(curEvent, dataLines.joinToString("\n"))
                    dataLines.clear()
                }
                curEvent = "message"
                continue
            }

            when {
                line.startsWith("event:") -> {
                    curEvent = line.substringAfter("event:").trim()
                }
                line.startsWith("data:") -> {
                    dataLines += line.substringAfter("data:").trim()
                }
            }
        }

        if (dataLines.isNotEmpty()) {
            onEvent(curEvent, dataLines.joinToString("\n"))
        }
    }
}

class MailComposeSseClient(
    context: Context,
    tokenManager: TokenManager = TokenManager(context),
    endpointUrl: String,
    client: OkHttpClient = defaultStreamingClient(context, tokenManager)
) : MailComposeStreamSubject {

    private val realSubject: MailComposeStreamSubject = RemoteMailComposeSubject(endpointUrl, client)
    private val active = AtomicBoolean(false)

    override fun start(
        payload: JSONObject,
        onSubject: (String) -> Unit,
        onBodyDelta: (Int, String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!active.compareAndSet(false, true)) {
            realSubject.stop()
            active.set(true)
        }

        realSubject.start(
            payload = payload,
            onSubject = onSubject,
            onBodyDelta = onBodyDelta,
            onDone = {
                active.set(false)
                onDone()
            },
            onError = { message ->
                active.set(false)
                onError(message)
            }
        )
    }

    override fun stop() {
        active.set(false)
        realSubject.stop()
    }
}
