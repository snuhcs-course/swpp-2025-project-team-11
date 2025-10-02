package com.fiveis.xend

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
private const val TAG = "GmailAPI"
private const val SERVER_BASE_URL = "https://myserver.com"
private const val REFRESH_ENDPOINT = "/user/refresh"

class MailSendActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MailSendScreen(accessToken = "")
        }
    }
}

// --------------------------- TokenStore ---------------------------

object TokenStore {
    private const val PREF_NAME = "secure_tokens"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREF_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getAccessToken(context: Context): String? = prefs(context).getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(context: Context): String? = prefs(context).getString(KEY_REFRESH_TOKEN, null)

    fun saveTokens(context: Context, accessToken: String, refreshToken: String?) {
        prefs(context).edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            if (!refreshToken.isNullOrBlank()) putString(KEY_REFRESH_TOKEN, refreshToken)
        }.apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}

// --------------------------- UI ---------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailSendScreen(
    // 기존 파라미터는 유지하지만, 저장소 우선 사용. 비어 있으면 fallback.
    accessToken: String,
    viewModel: MailSendViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                            TokenStore.clear(context)
                            Toast.makeText(context, "로그아웃되었습니다", Toast.LENGTH_SHORT).show()
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

                            // 1) 토큰 확인 (저장소 우선, 없으면 파라미터 사용)
                            val tokenInStore = TokenStore.getAccessToken(context)
                            val token = tokenInStore ?: accessToken

                            if (token.isBlank()) {
                                // 3) 토큰 자체가 없으면 메인으로 이동 + 토스트
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
                                    val result = viewModel.sendEmail(
                                        context = context,
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
                                        // 재전송은 요구사항상 하지 않음
                                        Toast.makeText(context, "토큰 재발급 완료", Toast.LENGTH_SHORT).show()
                                        statusMessage = "ℹ️ 토큰이 만료되어 재발급했습니다. 다시 전송을 눌러주세요."
                                    }
                                    is SendResult.Failure -> {
                                        isSending = false
                                        statusMessage = "❌ 메일 전송 실패: ${result.message}"
                                    }
                                }
                            }
                        },
                        enabled = !isSending
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "전송"
                        )
                    }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                enabled = !isSending,
                maxLines = 15
            )

            if (statusMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            statusMessage.contains("성공") -> MaterialTheme.colorScheme.primaryContainer
                            statusMessage.contains("실패") -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
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
    data object TokenRefreshed : SendResult()
    data class Failure(val message: String) : SendResult()
}

class MailSendViewModel : androidx.lifecycle.ViewModel() {

    suspend fun sendEmail(
        context: Context,
        accessToken: String,
        to: String,
        subject: String,
        body: String
    ): SendResult = withContext(Dispatchers.IO) {
        try {
            // RFC 2822 메시지 생성 (간단 텍스트)
            val emailContent = buildString {
                appendLine("To: $to")
                appendLine("Subject: $subject")
                appendLine("Content-Type: text/plain; charset=utf-8")
                appendLine()
                append(body)
            }

            // Gmail은 base64url 인코딩(패딩 제거)을 기대함
            val encodedEmail = Base64.encodeToString(
                emailContent.toByteArray(Charsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_WRAP
            ).replace("=", "") // 패딩 제거

            Log.d(TAG, "Sending email to: $to")
            Log.d(TAG, "Subject: $subject")

            val url = URL("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val requestBody = JSONObject().put("raw", encodedEmail).toString()
            conn.outputStream.use { os ->
                os.write(requestBody.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = conn.responseCode
            val responseText = try {
                if (responseCode in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                }
            } finally {
                conn.disconnect()
            }

            Log.d(TAG, "Response code: $responseCode")
            Log.d(TAG, "Response: $responseText")

            when {
                responseCode in 200..299 -> {
                    Log.d(TAG, "✅ Email sent successfully")
                    SendResult.Success
                }

                responseCode == 401 -> {
                    // 토큰 만료/유효하지 않음 → 서버로 리프레시 요청
                    val refreshed = refreshAccessToken(context)
                    if (refreshed) {
                        // 재전송은 요구사항상 하지 않음
                        SendResult.TokenRefreshed
                    } else {
                        SendResult.Failure("인증 실패 (토큰 재발급 실패)")
                    }
                }

                else -> {
                    Log.e(TAG, "❌ Failed to send email: $responseText")
                    val msg = parseErrorMessage(responseText)
                    SendResult.Failure(msg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while sending email", e)
            SendResult.Failure("예외 발생: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    private fun parseErrorMessage(body: String?): String {
        return try {
            if (body.isNullOrBlank()) return "알 수 없는 오류"
            val root = JSONObject(body)
            if (root.has("error")) {
                val err = root.getJSONObject("error")
                val code = err.optInt("code", -1)
                val msg = err.optString("message", "오류")
                "code=$code, $msg"
            } else {
                body.take(200)
            }
        } catch (_: Exception) {
            body?.take(200) ?: "알 수 없는 오류"
        }
    }

    private suspend fun refreshAccessToken(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val refreshToken = TokenStore.getRefreshToken(context) ?: return@withContext false

            val url = URL("$SERVER_BASE_URL$REFRESH_ENDPOINT")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val body = JSONObject().put("refresh_token", refreshToken).toString()
            conn.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            val text = try {
                if (code in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                }
            } finally {
                conn.disconnect()
            }

            if (code !in 200..299) {
                Log.e(TAG, "Refresh failed: $text")
                return@withContext false
            }

            val json = JSONObject(text)
            val newAccess = json.optString("access_token", "")
            val newRefresh = json.optString("refresh_token", refreshToken) // 서버가 안 주면 기존 유지

            if (newAccess.isBlank()) {
                Log.e(TAG, "Refresh response missing access_token")
                return@withContext false
            }

            TokenStore.saveTokens(context, newAccess, newRefresh)
            Log.d(TAG, "Access token refreshed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Exception during token refresh", e)
            false
        }
    }
}
