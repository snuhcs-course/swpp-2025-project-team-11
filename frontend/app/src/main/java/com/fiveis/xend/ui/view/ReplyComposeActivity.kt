package com.fiveis.xend.ui.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.ui.compose.ContactLookupViewModel
import com.fiveis.xend.ui.theme.XendTheme

class ReplyComposeActivity : ComponentActivity() {

    // Re: 중복 방지 헬퍼 함수
    private fun addReplyPrefix(subject: String): String {
        return if (subject.trim().startsWith("Re:", ignoreCase = true)) {
            subject
        } else {
            "Re: $subject"
        }
    }

    // 이메일 주소 추출 헬퍼 함수
    // "이름 <email@example.com>" 형식에서 email@example.com만 추출
    private fun extractEmail(fullEmail: String): String {
        val regex = """<(.+?)>""".toRegex()
        val match = regex.find(fullEmail)
        return match?.groupValues?.get(1)?.trim() ?: fullEmail.trim()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Intent에서 메일 정보 가져오기
        val senderEmailRaw = intent.getStringExtra("sender_email") ?: ""
        val date = intent.getStringExtra("date") ?: ""
        val subject = intent.getStringExtra("subject") ?: ""
        val body = intent.getStringExtra("body") ?: ""

        setContent {
            XendTheme {
                val viewModel: ReplyComposeViewModel = viewModel(
                    factory = ReplyComposeViewModel.Factory(application)
                )
                val uiState by viewModel.uiState.collectAsState()
                val contactLookupVm: ContactLookupViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ContactLookupViewModel(application) as T
                        }
                    }
                )
                val knownContacts by contactLookupVm.byEmail.collectAsState()
                val senderEmailAddress = extractEmail(senderEmailRaw)
                val normalizedSender = senderEmailAddress.trim().lowercase()
                val senderContact = knownContacts[normalizedSender]
                val senderDisplay = senderContact?.let { contact ->
                    "${contact.name} <${contact.email}>"
                } ?: senderEmailRaw
                val senderGroupNames = senderContact?.group?.name?.let { arrayListOf(it) } ?: arrayListOf()

                // 답장 옵션 자동 로딩
                LaunchedEffect(Unit) {
                    viewModel.startReplyOptions(
                        subject = subject,
                        body = body,
                        toEmail = senderEmailAddress
                    )
                }

                // 에러 처리
                LaunchedEffect(uiState.error) {
                    uiState.error?.let {
                        Toast.makeText(this@ReplyComposeActivity, it, Toast.LENGTH_SHORT).show()
                    }
                }

                ReplyComposeScreen(
                    senderEmail = senderDisplay,
                    date = date,
                    subject = subject,
                    body = body,
                    attachments = emptyList(),
                    replyOptions = uiState.options,
                    isLoadingOptions = uiState.isLoading,
                    isStreamingOptions = uiState.isStreaming,
                    onBack = {
                        viewModel.stopStreaming()
                        finish()
                    },
                    onTemplate = { /* TODO: 템플릿 선택 */ },
                    onAttach = { /* TODO: 첨부파일 추가 */ },
                    onSend = { /* TODO: 답장 전송 */ },
                    onDirectCompose = {
                        val intent = Intent(this@ReplyComposeActivity, ReplyDirectComposeActivity::class.java).apply {
                            putExtra("recipient_email", senderEmailAddress)
                            putExtra("recipient_name", senderDisplay)
                            putExtra("subject", addReplyPrefix(subject))
                            putStringArrayListExtra("group_names", senderGroupNames)
                            putExtra("sender_email", senderDisplay)
                            putExtra("date", date)
                            putExtra("original_body", body)
                        }
                        startActivity(intent)
                    },
                    onGenerateMore = {
                        viewModel.startReplyOptions(subject, body, senderEmailAddress)
                    },
                    onUseOption = { selectedOption ->
                        // 선택한 옵션으로 직접 작성 화면으로 이동
                        val intent = Intent(this@ReplyComposeActivity, ReplyDirectComposeActivity::class.java).apply {
                            putExtra("recipient_email", senderEmailAddress)
                            putExtra("recipient_name", senderDisplay)
                            putExtra("subject", addReplyPrefix(subject)) // 원본 제목 사용
                            putStringArrayListExtra("group_names", senderGroupNames)
                            putExtra("sender_email", senderDisplay)
                            putExtra("date", date)
                            putExtra("original_body", body)
                            // 생성된 본문을 초기값으로 전달
                            putExtra("generated_body", selectedOption.body)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}
