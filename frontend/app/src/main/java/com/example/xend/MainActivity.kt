package com.example.xend

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {

    private lateinit var credentialManager: CredentialManager
    private lateinit var serverClientId: String
    private var messages by mutableStateOf("")






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //초기화
        serverClientId = getString(R.string.server_client_id)
        credentialManager = CredentialManager.create(this)
        //화면
        setContent {
            MaterialTheme {
                LoginScreen(
                    onLoginClick = { signInWithGoogle() },
                    messages = messages
                )
            }
        }
    }

    /** Credential Manager + Google Sign-In */
    private fun signInWithGoogle() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false) // 계정 선택 UI 표시
                    .setServerClientId(serverClientId)    // OAuth 2.0 Web client ID
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val response: GetCredentialResponse =
                    credentialManager.getCredential(
                        context = this@MainActivity,
                        request = request
                    )

                val credential = response.credential
                val googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                if (idToken.isNullOrBlank()) {
                    Toast.makeText(this@MainActivity, "ID Token 없음", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // 서버로 로그인 콜백 호출 (결과만 확인)
                sendIdTokenToServer(idToken)

            } catch (e: GetCredentialException) {
                Log.e("GoogleAuth", "로그인 실패", e)
                Toast.makeText(this@MainActivity, "로그인 실패: ${e.message}", Toast.LENGTH_LONG).show()
                messages = "로그인 실패: ${e.message}"
            } catch (e: Exception) {
                Log.e("GoogleAuth", "예외 발생", e)
                Toast.makeText(this@MainActivity, "오류: ${e.message}", Toast.LENGTH_LONG).show()
                messages = "오류: ${e.message}"
            }
        }
    }

    // 일단 { "result": "success" }만 확인
    private fun sendIdTokenToServer(idToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val endpoint = getString(R.string.google_auth_callback_endpoint)
            try {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "application/json")
                }

                val body = JSONObject().put("id_token", idToken).toString()
                conn.outputStream.use { os ->
                    os.write(body.toByteArray(Charsets.UTF_8))
                    os.flush()
                }

                val code = conn.responseCode
                val text = try {
                    if (code in 200..299) {
                        conn.inputStream.bufferedReader().readText()
                    } else {
                        conn.errorStream?.bufferedReader()?.readText()
                            ?: """{"result":"error","message":"HTTP $code"}"""
                    }
                } finally {
                    conn.disconnect()
                }

                val json = try { JSONObject(text) } catch (_: Exception) { JSONObject() }
                val result = json.optString("result", "error")

                withContext(Dispatchers.Main) {
                    if (code in 200..299 && result.equals("success", ignoreCase = true)) {
                        Toast.makeText(this@MainActivity, "로그인 성공(서버)", Toast.LENGTH_LONG).show()
                        messages = """서버 응답: $result"""
                    } else {
                        Toast.makeText(this@MainActivity, "로그인 실패(서버)", Toast.LENGTH_LONG).show()
                        messages = """서버 오류: HTTP $code, 응답: $text"""
                    }
                }
            } catch (e: Exception) {
                Log.e("ServerAuth", "서버 연동 실패", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "서버 연동 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    messages = "서버 연동 실패: ${e.message}"
                }
            }
        }
    }
}











@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    messages: String
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Button(onClick = onLoginClick, modifier = Modifier.padding(16.dp)) {
                Text("백엔드 통신시도")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = if (messages.isEmpty()) "아직 응답 없음" else messages)
        }
    }
}