package com.fiveis.xend

import android.content.Intent
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

    // ✅ 서버 베이스 URL과 엔드포인트(필요시 변경)
    companion object {
        private const val SERVER_BASE_URL = "http://10.0.2.2:8008"
        private const val AUTH_CALLBACK = "/user/google/callback/"
        private const val LOGOUT_ENDPOINT = "/user/logout/"
    }

    // ✅ EncryptedSharedPreferences (다른 곳과 파일명/키 통일: "secure_prefs")
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

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: run {
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

        checkSavedTokens()

        // ✅ 이미 로그인되어 있으면 받은편지함으로
        if (isLoggedIn) {
            goToInbox()
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

    private fun saveTokens(access: String?, refresh: String?, email: String) {
        encryptedPrefs.edit().apply {
            if (!access.isNullOrEmpty()) putString("access_token", access)
            if (!refresh.isNullOrEmpty()) putString("refresh_token", refresh)
            putString("user_email", email)
            apply()
        }
        Log.d("TokenStorage", "✅ 토큰 저장 완료")
        Log.d("TokenStorage", "Access Token: ${access?.take(20) ?: "(없음)"}...")
        Log.d("TokenStorage", "Refresh Token: ${refresh?.take(20) ?: "(없음)"}...")
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
        Log.d("GoogleAuth", "Server Auth Code: ${authCode?.take(30)}...")
        Log.d("GoogleAuth", "Granted Scopes: ${account.grantedScopes.joinToString { it.scopeUri }}")

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

        // 서버로 Code 전달 → 백 JWT(access/refresh) 수령
        sendAuthCodeToServer("$SERVER_BASE_URL$AUTH_CALLBACK", authCode, email)
        Toast.makeText(this, "Auth Code 수신 성공!", Toast.LENGTH_SHORT).show()
        messages = "✅ Auth Code 받음\n${authCode.take(30)}..."
    }

    private fun signOutFromGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode(serverClientId)
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                // (선택) 서버 로그아웃도 호출 가능
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val access = encryptedPrefs.getString("access_token", null) ?: return@launch
                        val refresh = encryptedPrefs.getString("refresh_token", null) ?: ""
                        val url = URL("$SERVER_BASE_URL$LOGOUT_ENDPOINT")
                        val conn = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            connectTimeout = 10_000
                            readTimeout = 10_000
                            doOutput = true
                            setRequestProperty("Authorization", "Bearer $access")
                            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        }
                        val body = JSONObject().put("refresh", refresh).toString()
                        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                        conn.inputStream?.close()
                        conn.disconnect()
                    } catch (_: Exception) { /* ignore */ }
                }

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

    private fun sendAuthCodeToServer(endpoint: String, authCode: String, email: String) {
        lifecycleScope.launch(Dispatchers.IO) {
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
                conn.outputStream.use { os -> os.write(body.toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                val text = try {
                    if (code in 200..299) {
                        conn.inputStream?.bufferedReader()?.readText().orEmpty()
                    } else {
                        conn.errorStream?.bufferedReader()?.readText().orEmpty()
                    }
                } finally {
                    conn.disconnect()
                }

                withContext(Dispatchers.Main) {
                    if (code in 200..299) {
                        try {
                            val json = JSONObject(text)

                            // ✅ 명세에 맞춘 파싱: { user:{}, jwt:{ access:"", refresh:"" } }
                            val jwt = json.optJSONObject("jwt")
                            val access = jwt?.optString("access").orEmpty()
                            val refresh = jwt?.optString("refresh").orEmpty()

                            if (access.isNotEmpty() || refresh.isNotEmpty()) {
                                saveTokens(
                                    access.ifEmpty { null },
                                    refresh.ifEmpty { null },
                                    email
                                )
                                Toast.makeText(
                                    this@MainActivity,
                                    "✅ 서버 통신 성공 & 토큰 저장 완료",
                                    Toast.LENGTH_LONG
                                ).show()
                                messages = "✅ 서버 응답 (HTTP $code): 토큰 저장 완료"
                                goToInbox()
                            } else {
                                // (호환) 혹시 과거 키명을 쓰는 응답도 수용
                                val access2 = json.optString("access_token", "")
                                val refresh2 = json.optString("refresh_token", "")
                                if (access2.isNotEmpty() || refresh2.isNotEmpty()) {
                                    saveTokens(
                                        access2.ifEmpty { null },
                                        refresh2.ifEmpty { null },
                                        email
                                    )
                                    goToInbox()
                                } else {
                                    messages = "⚠️ 예상치 못한 응답 형식:\n$text"
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("TokenStorage", "토큰 파싱 실패", e)
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

    // ✅ 받은편지함(더미 리스트) 화면으로 이동
    private fun goToInbox() {
        try {
            startActivity(Intent(this, InboxActivity::class.java))
            finish()
        } catch (e: Exception) {
            Log.e("Nav", "InboxActivity 이동 실패", e)
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
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("로그아웃") }
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
