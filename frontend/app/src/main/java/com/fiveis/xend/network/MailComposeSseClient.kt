package com.fiveis.xend.network

import android.util.Log
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
import org.json.JSONObject

class MailComposeSseClient(
    private val endpointUrl: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.SECONDS) // SSE는 무한 읽기
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private var call: Call? = null
    private var readerJob: Job? = null
    private val stopped = AtomicBoolean(false)

    fun start(
        payload: JSONObject,
        onSubject: (String) -> Unit,
        onBodyDelta: (Int, String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        stop() // 이전 연결 정리

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

                // 백그라운드에서 SSE 라인 파서 가동
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

    fun stop() {
        stopped.set(true)
        readerJob?.cancel()
        readerJob = null
        call?.cancel()
        call = null
    }

    /**
     * 단순 SSE 파서:
     * - 'event: xxx' / 'data: yyy' 라인들을 모아 빈 줄 만나면 1 프레임으로 처리
     * - CRLF/LF 모두 처리
     */
    private fun parseSseStream(source: okio.BufferedSource, onEvent: (event: String, data: String) -> Unit) {
        var curEvent = "message"
        val dataLines = mutableListOf<String>()

        while (true) {
            // UTF-8로 한 줄씩 읽기 (LF/CRLF 모두 처리, EOF면 null)
            val raw = source.readUtf8Line() ?: break
            val line = raw.removePrefix("\uFEFF") // 혹시 있을지 모르는 BOM 제거

            if (line.isEmpty()) {
                // 프레임 종료: 누적된 data를 한 번에 전달
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

        // 스트림이 개행 없이 끝났을 때 마지막 프레임 처리
        if (dataLines.isNotEmpty()) {
            onEvent(curEvent, dataLines.joinToString("\n"))
        }
    }
}
