package com.fiveis.xend

import android.content.Intent // ✅ 추가: MailSendActivity로 이동
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private lateinit var serverClientId: String
    private var messages by mutableStateOf("")
    private var isLoggedIn by mutableStateOf(false)
    private var userEmail by mutableStateOf("")

    // 에뮬레이터용 로컬 엔드포인트
    private val authCallbackEndpoint = "http://10.0.2.2/user/google/callback/"

    // EncryptedSharedPreferences
    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            applicationContext,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Google Sign-In 런처
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data == null) {
            Log.w("GoogleAuth", "Sign-in canceled or no data.")
            Toast.makeText(this, "로그인이 취소되었습니다", Toast.LENGTH_LONG).show()
            messages = "로그인 취소됨"
            return@registerForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            handleSignInResult(account)
        } catch (e: ApiException) {
            Log.e("GoogleAuth", "Sign-in failed: ${e.statusCode}", e)
            Toast.makeText(this, "로그인 실패: ${e.statusCode}", Toast.LENGTH_LONG).show()
            messages = "로그인 실패: ${e.statusCode} - ${e.message ?: "알 수 없음"}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        serverClientId = getString(R.string.server_client_id)

        // 저장된 토큰이 있는지 확인
        checkSavedTokens()

        // ✅ 이미 로그인된 세션이면 바로 MailSendActivity로 이동
        if (isLoggedIn) {
            goToMailSend()
            return
        }

        setContent {
            MaterialTheme {
                LoginScreen(
                    onLoginClick = { signInWithGoogle() },
                    onLogoutClick = { signOutFromGoogle() },
                    messages = messages,
                    isLoggedIn = isLoggedIn,
                    userEmail = userEmail
                )
            }
        }
    }

    private fun checkSavedTokens() {
        val accessToken = encryptedPrefs.getString("access_token", null)
        val savedEmail = encryptedPrefs.getString("user_email", null)
        if (!accessToken.isNullOrEmpty() && !savedEmail.isNullOrEmpty()) {
            isLoggedIn = true
            userEmail = savedEmail
            messages = "저장된 세션으로 로그인됨"
            Log.d("TokenStorage", "Access Token: ${accessToken.take(20)}...")
        } else {
            isLoggedIn = false
            userEmail = ""
        }
    }

    private fun saveTokens(accessToken: String?, refreshToken: String?, email: String) {
        encryptedPrefs.edit().apply {
            if (!accessToken.isNullOrEmpty()) putString("access_token", accessToken)
            if (!refreshToken.isNullOrEmpty()) putString("refresh_token", refreshToken)
            putString("user_email", email)
            apply()
        }
        Log.d("TokenStorage", "✅ 토큰 저장 완료")
        Log.d("TokenStorage", "Access Token: ${accessToken?.take(20) ?: "(없음)"}...")
        Log.d("TokenStorage", "Refresh Token: ${refreshToken?.take(20) ?: "(없음)"}...")
    }

    private fun clearTokens() {
        encryptedPrefs.edit().apply {
            remove("access_token")
            remove("refresh_token")
            remove("user_email")
            apply()
        }
        Log.d("TokenStorage", "🗑️ 모든 토큰 삭제 완료")
    }

    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode(serverClientId, true) // forceCodeForRefreshToken=true
            .requestEmail()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/gmail.readonly"),
                Scope("https://www.googleapis.com/auth/gmail.send")
            )
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        signInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun handleSignInResult(account: GoogleSignInAccount?) {
        if (account == null) {
            Toast.makeText(this, "계정 정보를 가져오지 못했습니다", Toast.LENGTH_LONG).show()
            messages = "계정 정보 없음"
            return
        }

        val authCode = account.serverAuthCode
        val email = account.email ?: "unknown@example.com"

        Log.d("GoogleAuth", "=== 로그인 성공 ===")
        Log.d("GoogleAuth", "Email: ${account.email}")
        Log.d("GoogleAuth", "Display Name: ${account.displayName}")
        Log.d("GoogleAuth", "ID Token: ${account.idToken?.take(30)}...")
        Log.d("GoogleAuth", "Server Auth Code: ${authCode?.take(30)}...")
        Log.d("GoogleAuth", "Granted Scopes: ${account.grantedScopes.joinToString { it.scopeUri }}")

        // UI 상태 업데이트 (바로 화면 전환은 하지 않음 — 서버 교환 성공시 이동)
        isLoggedIn = true
        userEmail = email

        if (authCode.isNullOrBlank()) {
            val errorMsg = "Authorization Code를 받지 못했습니다.\n\n" +
                "확인사항:\n" +
                "1) server_client_id가 Web 클라이언트 ID인지\n" +
                "2) GCP에서 Gmail API 활성화\n" +
                "3) OAuth 동의 화면에 Gmail 스코프 추가"
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            messages = errorMsg
            return
        }

        // Authorization Code를 서버로 전송
        sendAuthCodeToServer(authCode, email)
        Toast.makeText(this, "Auth Code 수신 성공!", Toast.LENGTH_SHORT).show()
        messages = "✅ Auth Code 받음\n(Gmail 스코프 포함)\n${authCode.take(30)}..."
    }

    private fun signOutFromGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode(serverClientId)
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInClient.signOut().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                clearTokens()
                isLoggedIn = false
                userEmail = ""
                messages = "로그아웃되었습니다\n모든 토큰이 삭제되었습니다"
                Toast.makeText(this, "로그아웃 완료", Toast.LENGTH_SHORT).show()
                Log.d("GoogleAuth", "로그아웃 성공")
            } else {
                Toast.makeText(this, "로그아웃 실패", Toast.LENGTH_SHORT).show()
                Log.e("GoogleAuth", "로그아웃 실패", task.exception)
            }
        }
    }

    private fun sendAuthCodeToServer(authCode: String, email: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val endpoint = authCallbackEndpoint
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

                val body = JSONObject().put("auth_code", authCode).toString()
                conn.outputStream.use { os ->
                    os.write(body.toByteArray(Charsets.UTF_8))
                    os.flush()
                }

                val code = conn.responseCode
                val text: String = try {
                    if (code in 200..299) {
                        conn.inputStream?.bufferedReader()?.readText().orEmpty()
                    } else {
                        conn.errorStream?.bufferedReader()?.readText().takeUnless { it.isNullOrBlank() }
                            ?: """{"result":"error","message":"HTTP $code"}"""
                    }
                } finally {
                    conn.disconnect()
                }

                withContext(Dispatchers.Main) {
                    if (code in 200..299) {
                        try {
                            val json = JSONObject(text)

                            val accessToken = json.optString("access_token", "")
                            val refreshToken = json.optString("refresh_token", "")
                            val result = json.optString("result", "")
                            val message = json.optString("message", "")

                            when {
                                // ✅ 토큰을 받았으면 저장하고 바로 MailSendActivity로 이동
                                accessToken.isNotEmpty() || refreshToken.isNotEmpty() -> {
                                    saveTokens(
                                        accessToken.ifEmpty { null },
                                        refreshToken.ifEmpty { null },
                                        email
                                    )
                                    Toast.makeText(
                                        this@MainActivity,
                                        "✅ 서버 통신 성공 & 토큰 저장 완료",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    messages = "✅ 서버 응답 (HTTP $code): 토큰 저장 완료"
                                    goToMailSend() // ✅ 이동
                                }
                                // ✅ 토큰 없이 { "result": "success" }만 오는 경우도 지원
                                result.equals("success", ignoreCase = true) -> {
                                    messages = if (message.isNotEmpty()) {
                                        "✅ 서버 응답 (HTTP $code): $message"
                                    } else {
                                        "✅ 서버 응답 (HTTP $code): success"
                                    }
                                    goToMailSend() // ✅ 이동
                                }
                                else -> {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "⚠️ 토큰/결과 키 없음",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    messages = "⚠️ 서버 응답 (HTTP $code):\n$text"
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("TokenStorage", "토큰/결과 파싱 실패", e)
                            Toast.makeText(this@MainActivity, "⚠️ 파싱 실패", Toast.LENGTH_LONG).show()
                            messages = "⚠️ 서버 응답 파싱 실패:\n$text"
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "❌ 서버 통신 실패", Toast.LENGTH_LONG).show()
                        messages = "❌ 서버 오류 (HTTP $code):\n$text"
                    }
                }
            } catch (e: Exception) {
                Log.e("ServerAuth", "서버 연동 실패", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "서버 연동 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    messages = "❌ 서버 연동 실패: ${e.message ?: "알 수 없음"}"
                }
            }
        }
    }

    // ✅ 추가: MailSendActivity로 이동하는 헬퍼
    private fun goToMailSend() {
        try {
            startActivity(Intent(this, MailSendActivity::class.java))
            finish() // 로그인 화면을 백스택에서 제거(원치 않으면 지워도 됨)
        } catch (e: Exception) {
            Log.e("Nav", "MailSendActivity 이동 실패", e)
            Toast.makeText(this, "화면 이동 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    messages: String,
    isLoggedIn: Boolean,
    userEmail: String
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
            if (isLoggedIn) {
                Text(
                    text = "로그인됨: $userEmail",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )

                Button(
                    onClick = onLogoutClick,
                    modifier = Modifier.padding(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("로그아웃")
                }
            } else {
                Button(onClick = onLoginClick, modifier = Modifier.padding(16.dp)) {
                    Text("Gmail API 인증")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = if (messages.isEmpty()) "아직 응답 없음" else messages)
        }
    }
}
