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
    level = HttpLoggingInterceptor.Level.BODY
}

/**
 * 답장 옵션 추천 SSE 클라이언트
 * 엔드포인트: /api/ai/mail/reply/stream/
 *
 * 이벤트 순서:
 * 1. ready - 연결 준비 완료
 * 2. options - 옵션 목록 정보 (count, items=[{id,type,title}])
 * 3. option.delta - 각 옵션별 본문 델타 (id, seq, text)
 * 4. option.done - 각 옵션 완료 (id, total_seq)
 * 5. done - 전체 완료 (reason)
 * 중간에 ping, option.error 가능
 */
class MailReplySseClient(
    context: Context,
    val tokenManager: TokenManager = TokenManager(context),
    private val endpointUrl: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
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
) {
    private var call: Call? = null
    private var readerJob: Job? = null
    private val stopped = AtomicBoolean(false)

    fun start(
        subject: String,
        body: String,
        toEmail: String,
        onReady: () -> Unit = {},
        onOptions: (List<ReplyOptionInfo>) -> Unit,
        onOptionDelta: (optionId: Int, seq: Int, text: String) -> Unit,
        onOptionDone: (optionId: Int, totalSeq: Int) -> Unit,
        onOptionError: (optionId: Int, message: String) -> Unit,
        onDone: (reason: String) -> Unit,
        onError: (String) -> Unit
    ) {
        stop()

        val payload = JSONObject().apply {
            put("subject", subject)
            put("body", body)
            put("to_email", toEmail)
        }

        val json = payload.toString()
        val reqBody = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(endpointUrl)
            .post(reqBody)
            .build()

        Log.d("REPLY_SSE_REQ", "POST $endpointUrl\nBody: $json")

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
                                Log.d("SSE_EVENT", "event='$event', data='${data.take(100)}'")
                                try {
                                    val obj = JSONObject(data)
                                    when (event) {
                                        "ready" -> {
                                            Log.d("SSE_READY", "Ready event received")
                                            onReady()
                                        }
                                        "options" -> {
                                            Log.d("SSE_OPTIONS", "Options event: $data")
                                            val count = obj.getInt("count")
                                            val items = obj.getJSONArray("items")
                                            val options = mutableListOf<ReplyOptionInfo>()
                                            for (i in 0 until items.length()) {
                                                val item = items.getJSONObject(i)
                                                options.add(
                                                    ReplyOptionInfo(
                                                        id = item.getInt("id"),
                                                        type = item.getString("type"),
                                                        title = item.getString("title")
                                                    )
                                                )
                                            }
                                            onOptions(options)
                                        }
                                        "option.delta" -> {
                                            val id = obj.getInt("id")
                                            val seq = obj.getInt("seq")
                                            val text = obj.getString("text")
                                            Log.d("SSE_DELTA", "id=$id, seq=$seq, text='${text.take(20)}'")
                                            onOptionDelta(id, seq, text)
                                        }
                                        "option.done" -> {
                                            val id = obj.getInt("id")
                                            val totalSeq = obj.getInt("total_seq")
                                            Log.d("SSE_DONE", "Option $id done, totalSeq=$totalSeq")
                                            onOptionDone(id, totalSeq)
                                        }
                                        "option.error" -> {
                                            val id = obj.getInt("id")
                                            val message = obj.optString("message", "Unknown error")
                                            Log.e("SSE_ERROR", "Option $id error: $message")
                                            onOptionError(id, message)
                                        }
                                        "done" -> {
                                            val reason = obj.optString("reason", "finished")
                                            Log.d("SSE_DONE_ALL", "All done: $reason")
                                            onDone(reason)
                                        }
                                        "ping" -> {
                                            Log.d("SSE_PING", "Ping received")
                                            // Heartbeat - 무시
                                        }
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

    fun stop() {
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

/**
 * 답장 옵션 정보 (options 이벤트에서 수신)
 */
data class ReplyOptionInfo(
    val id: Int,
    val type: String,
    val title: String
)
