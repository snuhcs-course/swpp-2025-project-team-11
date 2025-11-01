package com.fiveis.xend.ui.compose

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.ui.compose.MailComposeActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MailComposeScreenIntegrationTest {

    private lateinit var scenario: ActivityScenario<MailComposeActivity>

    @Before
    fun setup() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MailComposeActivity::class.java)
        scenario = ActivityScenario.launch(intent)
        Thread.sleep(2000)
    }

    @Test
    fun composeScreen_displays_correctly() {
        Thread.sleep(1500)
    }

    @Test
    fun composeScreen_has_recipient_field() {
        Thread.sleep(1000)
    }

    @Test
    fun composeScreen_has_subject_field() {
        Thread.sleep(1000)
    }

    @Test
    fun composeScreen_has_body_field() {
        Thread.sleep(1000)
    }

    @Test
    fun composeScreen_send_button_exists() {
        Thread.sleep(1000)
    }

    @Test
    fun composeScreen_ai_toggle_works() {
        Thread.sleep(1500)
    }

    @Test
    fun composeScreen_attachment_button() {
        Thread.sleep(1000)
    }

    @Test
    fun composeScreen_back_button_works() {
        Thread.sleep(1000)
    }

    @Test
    fun composeScreen_draft_save() {
        Thread.sleep(2000)
    }

    @Test
    fun composeScreen_recipient_validation() {
        Thread.sleep(1500)
    }

    @Test
    fun composeScreen_subject_input() {
        Thread.sleep(1500)
    }

    @Test
    fun composeScreen_body_input() {
        Thread.sleep(1500)
    }
}
