package com.fiveis.xend.ui.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.ui.compose.SendMailViewModel
import com.fiveis.xend.ui.compose.TemplateSelectionScreen
import com.fiveis.xend.ui.theme.XendTheme
import com.mohamedrejeb.richeditor.model.rememberRichTextState

class ReplyDirectComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Intent에서 메일 정보 가져오기
        val recipientEmail = intent.getStringExtra("recipient_email") ?: ""
        val recipientName = intent.getStringExtra("recipient_name") ?: ""
        val initialSubject = intent.getStringExtra("subject") ?: ""
        val groups = intent.getStringArrayListExtra("groups") ?: emptyList()
        val senderEmail = intent.getStringExtra("sender_email") ?: ""
        val date = intent.getStringExtra("date") ?: ""
        val originalBody = intent.getStringExtra("original_body") ?: ""

        setContent {
            XendTheme {
                // SendMailViewModel 초기화
                val sendVm: SendMailViewModel = viewModel(
                    factory = SendMailViewModel.Factory(application)
                )
                val sendUiState by sendVm.ui.collectAsState()

                // 템플릿 화면 상태
                var showTemplateScreen by remember { mutableStateOf(false) }

                // subject와 body 상태 관리
                var currentSubject by rememberSaveable { mutableStateOf(initialSubject) }
                val richTextState = rememberRichTextState()

                if (showTemplateScreen) {
                    // 템플릿 선택 화면
                    TemplateSelectionScreen(
                        onBack = { showTemplateScreen = false },
                        onTemplateSelected = { template ->
                            currentSubject = template.subject
                            richTextState.setHtml(template.body)
                            showTemplateScreen = false
                        }
                    )
                } else {
                    // 답장 작성 화면
                    ReplyDirectComposeScreen(
                        recipientEmail = recipientEmail,
                        recipientName = recipientName,
                        subject = currentSubject,
                        groups = groups,
                        onBack = { finish() },
                        onSend = { bodyText ->
                            // 이메일 전송
                            sendVm.sendEmail(
                                to = recipientEmail,
                                subject = currentSubject,
                                body = bodyText
                            )
                        },
                        onTemplateClick = { showTemplateScreen = true },
                        onSubjectChange = { currentSubject = it },
                        richTextState = richTextState,
                        senderEmail = senderEmail,
                        date = date,
                        originalBody = originalBody,
                        sendUiState = sendUiState
                    )
                }
            }
        }
    }
}
