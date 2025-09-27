package com.example.xend

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Firebase 인증 및 CredentialManager 초기화
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        setContent {
            // Composable 안에서 코루틴을 사용하기 위해 CoroutineScope 생성
            val scope = rememberCoroutineScope()

            LoginScreen(
                onLoginClick = {
                    // 2. 코루틴 내에서 로그인 함수 실행
                    scope.launch {
                        signInWithGoogle()
                    }
                }
            )
        }
    }

    private suspend fun signInWithGoogle() {
        // 3. R.string에서 웹 클라이언트 ID 가져오기
        val webClientId = getString(R.string.default_web_client_id)

        // 4. Google 로그인 옵션 설정
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // 기존 로그인 계정 외에도 모든 구글 계정을 보여줌
            .setServerClientId(webClientId)
            .build()

        // 5. Credential Manager에 요청 객체 생성
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            // 6. Credential Manager를 통해 로그인 UI를 띄우고 결과 받기
            val result = credentialManager.getCredential(
                context = this,
                request = request
            )
            val credential = result.credential

            // 7. 결과에서 Google ID 토큰 추출
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val googleIdToken = googleIdTokenCredential.idToken

            // 8. Firebase에 인증할 Credential 생성
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)

            // 9. Firebase에 로그인
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user

            Toast.makeText(this, "환영합니다! ${user?.displayName}", Toast.LENGTH_SHORT).show()

        } catch (e: GetCredentialException) {
            // 로그인 실패 또는 사용자가 취소한 경우
            e.printStackTrace()
            Toast.makeText(this, "로그인에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("Auth", "Firebase error", e)
            Toast.makeText(this, "로그인 중 에러 발생: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun LoginScreen(onLoginClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onLoginClick,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Google 로그인")
            }
        }
    }
}