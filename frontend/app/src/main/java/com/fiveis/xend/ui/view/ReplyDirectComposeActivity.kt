package com.fiveis.xend.ui.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fiveis.xend.ui.theme.XendTheme

class ReplyDirectComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Intent에서 메일 정보 가져오기
        val recipientEmail = intent.getStringExtra("recipient_email") ?: ""
        val recipientName = intent.getStringExtra("recipient_name") ?: ""
        val subject = intent.getStringExtra("subject") ?: ""
        val groups = intent.getStringArrayListExtra("groups") ?: emptyList()
        val senderEmail = intent.getStringExtra("sender_email") ?: ""
        val date = intent.getStringExtra("date") ?: ""
        val originalBody = intent.getStringExtra("original_body") ?: ""

        setContent {
            XendTheme {
                ReplyDirectComposeScreen(
                    recipientEmail = recipientEmail,
                    recipientName = recipientName,
                    subject = subject,
                    groups = groups,
                    onBack = { finish() },
                    onSend = { /* TODO: 전송 로직 */ },
                    senderEmail = senderEmail,
                    date = date,
                    originalBody = originalBody
                )
            }
        }
    }
}
