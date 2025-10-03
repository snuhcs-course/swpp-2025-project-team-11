package com.fiveis.xend

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

class InboxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                InboxScreen(
                    onFabClick = {
                        startActivity(Intent(this, MailSendActivity::class.java))
                    }
                )
            }
        }
    }
}
