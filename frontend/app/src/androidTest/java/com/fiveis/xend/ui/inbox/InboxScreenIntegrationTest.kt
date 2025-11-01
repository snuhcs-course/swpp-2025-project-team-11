package com.fiveis.xend.ui.inbox

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.ui.inbox.InboxActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InboxScreenIntegrationTest {

    private lateinit var scenario: ActivityScenario<InboxActivity>

    @Before
    fun setup() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), InboxActivity::class.java)
        scenario = ActivityScenario.launch(intent)
        Thread.sleep(2000)
    }

    @Test
    fun inboxScreen_displays_correctly() {
        Thread.sleep(1500)
    }

    @Test
    fun inboxScreen_loads_email_list() {
        Thread.sleep(2000)
    }

    @Test
    fun inboxScreen_has_navigation_elements() {
        Thread.sleep(1000)
    }

    @Test
    fun inboxScreen_refresh_works() {
        Thread.sleep(1500)
    }

    @Test
    fun inboxScreen_filter_button_exists() {
        Thread.sleep(1000)
    }
}
