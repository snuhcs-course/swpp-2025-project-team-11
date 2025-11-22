package com.fiveis.xend.ui.contactbook

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.repository.ContactBookTab
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactBookActivityIntegrationTest {

    private var scenario: ActivityScenario<ContactBookActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun activity_launches_successfully() {
        scenario = ActivityScenario.launch(ContactBookActivity::class.java)
        // If activity launches without crashing, test passes
    }

    @Test
    fun activity_launches_with_start_tab_intent() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ContactBookActivity::class.java).apply {
            putExtra(ContactBookActivity.START_TAB, ContactBookTab.Contacts.toString())
        }
        scenario = ActivityScenario.launch(intent)
        // If activity launches without crashing with intent, test passes
    }

    @Test
    fun activity_handles_back_press() {
        scenario = ActivityScenario.launch(ContactBookActivity::class.java)
        scenario?.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        // If activity handles back press without crashing, test passes
    }
}
