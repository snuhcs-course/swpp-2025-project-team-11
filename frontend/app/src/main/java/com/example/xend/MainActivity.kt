package com.fiveis.xend

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {

    private lateinit var serverClientId: String
    private var messages by mutableStateOf("")
    private var isLoggedIn by mutableStateOf(false)
    private var userEmail by mutableStateOf("")

    @Suppress("DEPRECATION")
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            handleSignInResult(account)
        } catch (e: ApiException) {
            Log.e("GoogleAuth", "Sign-in failed: ${e.statusCode}", e)
            Toast.makeText(this, "로그인 실패: ${e.statusCode}", Toast.LENGTH_LONG).show()
            messages = "로그인 실패: ${e.statusCode} - ${e.message}"
        }
    }

    /**
     * Initializes the activity, loads the server client ID, and sets the Compose UI to the login screen.
     *
     * @param savedInstanceState The saved instance state bundle provided by the system, or `null` if none.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        serverClientId = getString(R.string.server_client_id)

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

    /**
     * Starts the Google Sign-In flow requesting a server auth code, the user's email, and Gmail read/send scopes, then launches the sign-in intent.
     *
     * Builds GoogleSignInOptions using the activity's serverClientId, obtains a GoogleSignInClient, and triggers the registered signInLauncher to open the sign-in UI.
     */
    @Suppress("DEPRECATION")
    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode(serverClientId)
            .requestEmail()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/gmail.readonly"),
                Scope("https://www.googleapis.com/auth/gmail.send")
            )
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        signInLauncher.launch(googleSignInClient.signInIntent)
    }

    /**
     * Handle the result of a Google sign-in and advance the app's authentication flow.
     *
     * If `account` is null, signals the failure by showing a toast and setting `messages` to indicate missing account.
     * If `account` is present, updates login state (`isLoggedIn`, `userEmail`), and:
     * - if a server auth code is available, sends it to the app backend, shows a success toast, and updates `messages` with a short code snippet;
     * - if the auth code is missing, shows an explanatory toast with troubleshooting steps and updates `messages` with that guidance.
     *
     * @param account The nullable GoogleSignInAccount returned by the sign-in intent; may be null on failure.
    private fun handleSignInResult(account: GoogleSignInAccount?) {
        if (account == null) {
            Toast.makeText(this, "계정 정보를 가져오지 못했습니다", Toast.LENGTH_LONG).show()
            messages = "계정 정보 없음"
            return
        }

        val authCode = account.serverAuthCode

        Log.d("GoogleAuth", "=== 로그인 성공 ===")
        Log.d("GoogleAuth", "Email: ${account.email}")
        Log.d("GoogleAuth", "Display Name: ${account.displayName}")
        Log.d("GoogleAuth", "ID Token: ${account.idToken?.take(30)}...")
        Log.d("GoogleAuth", "Server Auth Code: ${authCode?.take(30)}...")
        Log.d("GoogleAuth", "Granted Scopes: ${account.grantedScopes.joinToString { it.scopeUri }}")

        // 로그인 상태 업데이트
        isLoggedIn = true
        userEmail = account.email ?: "Unknown"

        if (authCode.isNullOrBlank()) {
            val errorMsg = "Authorization Code를 받지 못했습니다.\n\n" +
                "확인사항:\n" +
                "1. server_client_id가 Web 클라이언트 ID인지\n" +
                "2. Google Cloud Console에서 Gmail API 활성화\n" +
                "3. OAuth 동의 화면에 Gmail 스코프 추가"
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            messages = errorMsg
            return
        }

        // Authorization Code를 서버로 전송
        sendAuthCodeToServer(authCode)
        Toast.makeText(this, "Auth Code 수신 성공!", Toast.LENGTH_SHORT).show()
        messages = "✅ Auth Code 받음\n(Gmail 스코프 포함)\n${authCode.take(30)}..."
    }

    /**
     * Signs out the current Google account and updates local UI state.
     *
     * Initiates a GoogleSignInClient sign-out; on success it clears login state (isLoggedIn, userEmail),
     * updates the messages text, shows a success Toast, and logs the event. On failure it shows a
     * failure Toast and logs the error.
     */
    @Suppress("DEPRECATION")
    private fun signOutFromGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode(serverClientId)
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInClient.signOut().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                isLoggedIn = false
                userEmail = ""
                messages = "로그아웃되었습니다"
                Toast.makeText(this, "로그아웃 완료", Toast.LENGTH_SHORT).show()
                Log.d("GoogleAuth", "로그아웃 성공")
            } else {
                Toast.makeText(this, "로그아웃 실패", Toast.LENGTH_SHORT).show()
                Log.e("GoogleAuth", "로그아웃 실패", task.exception)
            }
        }
    }

    /**
     * Sends the provided Google authorization code to the backend callback endpoint and surfaces the server response to the UI.
     *
     * Performs a network POST of a JSON body containing the `authCode` to the configured callback path, then updates `messages` and shows a success or failure Toast on the main thread based on the HTTP response. On exception, logs the error and updates `messages` with the exception message.
     *
     * @param authCode The server authorization code obtained from Google sign-in to forward to the backend.
     */
    private fun sendAuthCodeToServer(authCode: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val endpoint = getString(R.string.google_auth_callback_endpoint) + "/auth/google/callback"
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

                withContext(Dispatchers.Main) {
                    if (code in 200..299) {
                        Toast.makeText(this@MainActivity, "✅ 서버 통신 성공", Toast.LENGTH_LONG).show()
                        messages = "✅ 서버 응답 (HTTP $code):\n$text"
                    } else {
                        Toast.makeText(this@MainActivity, "❌ 서버 통신 실패", Toast.LENGTH_LONG).show()
                        messages = "❌ 서버 오류 (HTTP $code):\n$text"
                    }
                }
            } catch (e: Exception) {
                Log.e("ServerAuth", "서버 연동 실패", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "서버 연동 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    messages = "❌ 서버 연동 실패:\n${e.message}"
                }
            }
        }
    }
}

/**
 * Composable UI that shows a Gmail authentication button or a logout area and displays status messages.
 *
 * When `isLoggedIn` is true the composable displays the logged-in `userEmail` and a logout button that invokes `onLogoutClick`.
 * When `isLoggedIn` is false it shows a button that invokes `onLoginClick` to start authentication.
 *
 * @param onLoginClick Callback invoked when the authentication button is pressed.
 * @param onLogoutClick Callback invoked when the logout button is pressed.
 * @param messages Status or response text to display to the user; empty string results in a default placeholder.
 * @param isLoggedIn True if a user is currently signed in; controls which UI (login vs. logout) is shown.
 * @param userEmail Email address of the currently signed-in user (displayed when `isLoggedIn` is true).
 */
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
                // 로그인된 상태
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
                // 로그아웃된 상태
                Button(onClick = onLoginClick, modifier = Modifier.padding(16.dp)) {
                    Text("Gmail API 인증")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = if (messages.isEmpty()) "아직 응답 없음" else messages)
        }
    }
}
