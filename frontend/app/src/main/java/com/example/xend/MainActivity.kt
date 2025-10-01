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
import androidx.lifecycle.lifecycleScope
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
     * Initializes activity state and composes the login UI.
     *
     * Initializes the serverClientId from resources and sets the Compose content to show the
     * LoginScreen wired to the activity's sign-in and sign-out handlers.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *     shut down, this contains the data it most recently supplied; otherwise null.
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
     * Starts the Google Sign-In flow requesting a server auth code, the user's email, and Gmail scopes, then launches the sign-in intent.
     *
     * Requests the server auth code using the activity's `serverClientId` and asks for
     * the Gmail readonly and send scopes before launching the sign-in UI.
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
     * Processes a Google Sign-In account result: updates local login state, shows user feedback, and forwards the server auth code to the backend when available.
     *
     * If `account` is null the function records the missing account, shows a toast, and stops. When an auth code is present it triggers sending that code to the server and updates `messages`, `isLoggedIn`, and `userEmail` accordingly; when the auth code is missing it shows an explanatory error toast and updates `messages`.
     *
     * @param account The GoogleSignInAccount returned from the sign-in intent (may be null).
     */
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
     * Signs the user out of Google and clears local authentication state.
     *
     * Performs a Google sign-out via a GoogleSignInClient. On success, resets `isLoggedIn` and `userEmail`,
     * updates `messages` to indicate logout, shows a success toast, and logs the event. On failure, shows
     * a failure toast and logs the error.
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
     * Send the Google server auth code to the configured backend and update the UI with the result.
     *
     * Performs an HTTP POST to the app's configured Google auth callback endpoint with a JSON body
     * containing the `auth_code`. On a 2xx response, updates `messages` with the server response and
     * shows a success toast; on a non-2xx response, updates `messages` with the error body and shows a
     * failure toast. On exception, logs the error and updates `messages` with the exception message.
     *
     * @param authCode The server auth code obtained from Google Sign-In.
     */
    private fun sendAuthCodeToServer(authCode: String) {
        lifecycleScope.launch(Dispatchers.IO){
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
 * Composable that shows Google sign-in UI, displays current login status and messages, and provides sign-in or sign-out controls.
 *
 * @param onLoginClick Callback invoked when the sign-in button is pressed.
 * @param onLogoutClick Callback invoked when the logout button is pressed.
 * @param messages Current status or server response text displayed below the buttons; if empty, a placeholder is shown.
 * @param isLoggedIn Whether the UI should present the logged-in state (shows email and logout button) or the logged-out state (shows sign-in button).
 * @param userEmail The email address to display when `isLoggedIn` is true.
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
