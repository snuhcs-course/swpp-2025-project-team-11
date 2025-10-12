package com.fiveis.xend.ui.compose

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.data.source.TokenManager
import com.fiveis.xend.ui.login.MainActivity
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val TAG = "GmailAPI"

private const val SERVER_BASE_URL = "http://10.0.2.2:8008"
private const val REFRESH_ENDPOINT = "/user/refresh/"
private const val SEND_MAIL_ENDPOINT = "/mail/emails/send/"
// private const val LOGIN_CALLBACK_ENDPOINT = "/user/google/callback/"

class MailSendActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MailSendScreen(accessToken = "") }
    }
}

// --------------------------- UI ---------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailSendScreen(accessToken: String, viewModel: MailSendViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }

    var recipientEmail by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("메일 작성") },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            // (선택) 단순 로컬 로그아웃만 수행. 서버 로그아웃은 MainActivity 쪽 구현 참고.
                            tokenManager.clearTokens()
                            Toast.makeText(context, "로그아웃되었습니다", Toast.LENGTH_SHORT).show()
                            context.startActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                            (context as? Activity)?.finish()
                        }
                    ) { Text("로그아웃") }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (recipientEmail.isBlank() || subject.isBlank() || body.isBlank()) {
                                statusMessage = "모든 필드를 입력해주세요"
                                return@IconButton
                            }

                            val tokenInStore = tokenManager.getAccessToken()
                            val token = tokenInStore ?: accessToken
                            if (token.isBlank()) {
                                Toast.makeText(context, "로그인 필요", Toast.LENGTH_SHORT).show()
                                context.startActivity(
                                    Intent(context, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                                (context as? Activity)?.finish()
                                return@IconButton
                            }

                            isSending = true
                            statusMessage = "전송 중..."

                            scope.launch {
                                when (
                                    val result = viewModel.sendEmailViaBackend(
                                        tokenManager = tokenManager,
                                        accessToken = token,
                                        to = recipientEmail,
                                        subject = subject,
                                        body = body
                                    )
                                ) {
                                    is SendResult.Success -> {
                                        isSending = false
                                        statusMessage = "✅ 메일 전송 성공!"
                                    }
                                    is SendResult.TokenRefreshed -> {
                                        isSending = false
                                        statusMessage = "✅ 토큰 갱신 후 재시도 성공"
                                    }
                                    is SendResult.RequiresLogin -> {
                                        isSending = false
                                        statusMessage = "❌ 인증 만료. 다시 로그인해주세요."
                                    }
                                    is SendResult.Failure -> {
                                        isSending = false
                                        statusMessage = "❌ 메일 전송 실패: ${result.message}"
                                    }
                                }
                            }
                        },
                        enabled = !isSending
                    ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "전송") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = recipientEmail,
                onValueChange = { recipientEmail = it },
                label = { Text("받는 사람") },
                placeholder = { Text("example@gmail.com") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSending,
                singleLine = true
            )
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("제목") },
                placeholder = { Text("메일 제목을 입력하세요") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSending,
                singleLine = true
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("내용") },
                placeholder = { Text("메일 내용을 입력하세요") },
                modifier = Modifier.fillMaxWidth().height(300.dp),
                enabled = !isSending,
                maxLines = 15
            )

            if (statusMessage.isNotEmpty()) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = statusMessage,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (isSending) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// --------------------------- ViewModel ---------------------------

sealed class SendResult {
    data object Success : SendResult()
    data object TokenRefreshed : SendResult() // 리프레시 후 재시도까지 성공
    data object RequiresLogin : SendResult() // 리프레시 실패
    data class Failure(val message: String) : SendResult()
}

class MailSendViewModel : androidx.lifecycle.ViewModel() {

    suspend fun sendEmailViaBackend(
        tokenManager: TokenManager,
        accessToken: String,
        to: String,
        subject: String,
        body: String
    ): SendResult = withContext(Dispatchers.IO) {
        // 1) 1차 전송
        val first = postSend(accessToken, to, subject, body)
        when {
            first.code in 200..299 || first.code == 201 -> return@withContext SendResult.Success
            first.code == 401 -> {
                // 2) 리프레시
                val refreshed = refreshAccessToken(tokenManager)
                if (!refreshed) return@withContext SendResult.RequiresLogin

                // 3) 새로운 토큰으로 1회 자동 재시도
                val newAccess = tokenManager.getAccessToken() ?: return@withContext SendResult.RequiresLogin
                val second = postSend(newAccess, to, subject, body)
                return@withContext when {
                    second.code in 200..299 || second.code == 201 -> SendResult.TokenRefreshed
                    second.code == 401 -> SendResult.RequiresLogin
                    else -> SendResult.Failure(parseErrorMessage(second.body))
                }
            }
            else -> return@withContext SendResult.Failure(parseErrorMessage(first.body))
        }
    }

    private data class HttpResp(val code: Int, val body: String)

    private fun postSend(accessToken: String, to: String, subject: String, body: String): HttpResp {
        return try {
            val url = URL("$SERVER_BASE_URL$SEND_MAIL_ENDPOINT")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            val payload = JSONObject()
                .put("to", to)
                .put("subject", subject)
                .put("body", body)
                .toString()

            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val text = try {
                if (code in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText().orEmpty()
                }
            } finally {
                conn.disconnect()
            }

            Log.d(TAG, "Send resp ($code): $text") // ✅ 정상 응답 로그
            HttpResp(code, text)
        } catch (e: Exception) {
            Log.e(TAG, "Send exception: ${e.localizedMessage}", e) // ✅ 상세 예외 로그
            HttpResp(500, "{\"error\":\"${e.localizedMessage ?: e.javaClass.simpleName}\"}")
        }
    }

    private fun parseErrorMessage(body: String?): String {
        return try {
            if (body.isNullOrBlank()) return "알 수 없는 오류"
            val root = JSONObject(body)
            when {
                root.has("error") -> {
                    val err = root.getJSONObject("error")
                    val code = err.optInt("code", -1)
                    val msg = err.optString("message", "오류")
                    "code=$code, $msg"
                }
                root.has("detail") -> root.optString("detail")
                else -> body.take(200)
            }
        } catch (_: Exception) {
            body?.take(200) ?: "알 수 없는 오류"
        }
    }

    private suspend fun refreshAccessToken(tokenManager: TokenManager): Boolean = withContext(Dispatchers.IO) {
        try {
            val refreshToken = tokenManager.getRefreshToken() ?: return@withContext false

            val url = URL("$SERVER_BASE_URL$REFRESH_ENDPOINT")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val body = JSONObject().put("refresh", refreshToken).toString()
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val text = try {
                if (code in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText().orEmpty()
                }
            } finally {
                conn.disconnect()
            }

            Log.d(TAG, "Refresh response ($code): $text") // ✅ 응답 로그

            if (code !in 200..299) {
                return@withContext false
            }

            val json = JSONObject(text)
            val newAccess = json.optString("access", "")
            if (newAccess.isBlank()) {
                Log.e(TAG, "Refresh response missing 'access'")
                return@withContext false
            }

            tokenManager.saveAccessToken(newAccess)
            val newRefresh = json.optString("refresh", "")
            if (newRefresh.isNotBlank()) {
                tokenManager.saveRefreshToken(newRefresh)
            }
            Log.d(TAG, "Access token refreshed ✅")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Exception during token refresh: ${e.localizedMessage}", e) // ✅ 상세 예외 로그
            false
        }
    }
}
