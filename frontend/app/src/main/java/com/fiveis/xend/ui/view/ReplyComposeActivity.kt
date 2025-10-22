package com.fiveis.xend.ui.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fiveis.xend.ui.theme.XendTheme

class ReplyComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Intent에서 메일 정보 가져오기
        val senderEmail = intent.getStringExtra("sender_email") ?: ""
        val date = intent.getStringExtra("date") ?: ""
        val subject = intent.getStringExtra("subject") ?: ""
        val body = intent.getStringExtra("body") ?: ""

        setContent {
            XendTheme {
                ReplyComposeScreen(
                    senderEmail = senderEmail,
                    date = date,
                    subject = subject,
                    body = body,
                    // TODO: 첨부파일 API 연동
                    attachments = emptyList(),
                    // TODO: 답장 옵션 API 연동
                    replyOptions = emptyList(),
                    onBack = { finish() },
                    onTemplate = { /* TODO: 템플릿 선택 */ },
                    onAttach = { /* TODO: 첨부파일 추가 */ },
                    onSend = { /* TODO: 답장 전송 */ },
                    onDirectCompose = {
                        val intent = Intent(this@ReplyComposeActivity, ReplyDirectComposeActivity::class.java).apply {
                            putExtra("recipient_email", senderEmail)
                            putExtra("recipient_name", senderEmail) // 전체 문자열 전달 (파싱은 Screen에서)
                            putExtra("subject", subject)
                            putStringArrayListExtra("groups", ArrayList<String>())
                            putExtra("sender_email", senderEmail)
                            putExtra("date", date)
                            putExtra("original_body", body)
                        }
                        startActivity(intent)
                    },
                    onGenerateMore = { /* TODO: 추가 생성 */ }
                )
            }
        }
    }
}
