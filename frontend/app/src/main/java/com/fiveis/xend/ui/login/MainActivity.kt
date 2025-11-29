package com.fiveis.xend.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.R
import com.fiveis.xend.data.source.TokenManager
import com.fiveis.xend.ui.mail.MailActivity
import com.fiveis.xend.ui.theme.XendTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

class MainActivity : ComponentActivity() {

    private lateinit var serverClientId: String
    private lateinit var tokenManager: TokenManager

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: run {
            Log.w("GoogleAuth", "Sign-in canceled or no data.")
            return@registerForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            handleSignInResult(account)
        } catch (e: ApiException) {
            Log.e("GoogleAuth", "Sign-in failed: ${e.statusCode}", e)
            Log.e("GoogleAuth", "Status message: ${e.statusMessage}")
            Log.e("GoogleAuth", "Error details: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        serverClientId = getString(R.string.server_client_id)
        tokenManager = TokenManager(applicationContext)

        setContent {
            XendTheme {
                val viewModel: LoginViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return LoginViewModel(tokenManager) as T
                        }
                    }
                )

                val uiState by viewModel.uiState.collectAsState()

                // 이미 로그인되어 있으면 메일함으로
                if (uiState.isLoggedIn) {
                    goToMail()
                    return@XendTheme
                }

                LoginScreen(
                    uiState = uiState,
                    onLoginClick = { signInWithGoogle() },
                    onLogoutClick = {
                        viewModel.logout()
                        signOutFromGoogle()
                    }
                )
            }
        }
    }

    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode(serverClientId, true) // forceCodeForRefreshToken=true
            .requestEmail()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/gmail.readonly"),
                Scope("https://www.googleapis.com/auth/gmail.send"),
                Scope("https://www.googleapis.com/auth/gmail.modify")
            )
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        signInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun handleSignInResult(account: GoogleSignInAccount?) {
        if (account == null) {
            Log.e("GoogleAuth", "Account is null")
            return
        }

        val authCode = account.serverAuthCode
        val email = account.email ?: "unknown@example.com"

        Log.d("GoogleAuth", "=== 로그인 성공 ===")
        Log.d("GoogleAuth", "Email: ${account.email}")
        Log.d("GoogleAuth", "Display Name: ${account.displayName}")
        Log.d("GoogleAuth", "Server Auth Code: ${authCode?.take(30)}...")
        Log.d("GoogleAuth", "Granted Scopes: ${account.grantedScopes.joinToString { it.scopeUri }}")

        if (authCode.isNullOrBlank()) {
            Log.e("GoogleAuth", "Authorization Code is null or blank")
            return
        }

        // ViewModel을 통해 서버에 Auth Code 전송
        val viewModel = (this as? ComponentActivity)?.let {
            androidx.lifecycle.ViewModelProvider(
                it,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return LoginViewModel(tokenManager) as T
                    }
                }
            )[LoginViewModel::class.java]
        }

        viewModel?.handleAuthCodeReceived(authCode, email)
    }

    private fun signOutFromGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode(serverClientId)
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.d("GoogleAuth", "로그아웃 성공")
            } else {
                Log.e("GoogleAuth", "로그아웃 실패", task.exception)
            }
        }
    }

    private fun goToMail() {
        try {
            startActivity(Intent(this, MailActivity::class.java))
            finish()
        } catch (e: Exception) {
            Log.e("Nav", "MailActivity 이동 실패", e)
        }
    }
}
