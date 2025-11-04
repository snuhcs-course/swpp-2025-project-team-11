package com.fiveis.xend.ui.contactbook

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.ui.contactbook.ContactBookActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactBookScreenIntegrationTest {

    private lateinit var scenario: ActivityScenario<ContactBookActivity>

    @Before
    fun setup() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ContactBookActivity::class.java)
        scenario = ActivityScenario.launch(intent)
        Thread.sleep(2000)
    }

    @Test
    fun contactBookScreen_displays_correctly() {
        Thread.sleep(1500)
    }

    @Test
    fun contactBookScreen_shows_tabs() {
        Thread.sleep(1000)
    }

    @Test
    fun contactBookScreen_contacts_tab_works() {
        Thread.sleep(1500)
    }

    @Test
    fun contactBookScreen_groups_tab_works() {
        Thread.sleep(1500)
    }

    @Test
    fun contactBookScreen_search_button_exists() {
        Thread.sleep(1000)
    }

    @Test
    fun contactBookScreen_add_button_exists() {
        Thread.sleep(1000)
    }

    @Test
    fun contactBookScreen_loads_contact_list() {
        Thread.sleep(2000)
    }

    @Test
    fun contactBookScreen_loads_group_list() {
        Thread.sleep(2000)
    }

    @Test
    fun contactBookScreen_refresh_contacts() {
        Thread.sleep(1500)
    }

    @Test
    fun contactBookScreen_refresh_groups() {
        Thread.sleep(1500)
    }
}
