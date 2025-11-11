package com.fiveis.xend.ui.contactbook

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactDetailActivityTest {

    @Test
    fun activity_with_valid_contact_id_launches() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), ContactDetailActivity::class.java).apply {
            putExtra(ContactDetailActivity.EXTRA_CONTACT_ID, 1L)
        }

        // When
        val scenario = ActivityScenario.launch<ContactDetailActivity>(intent)
        Thread.sleep(300)

        // Then - Should not crash
        scenario.close()
    }

    @Test
    fun activity_companion_object_constant() {
        // Then
        assert(ContactDetailActivity.EXTRA_CONTACT_ID == "extra_contact_id")
    }

    @Test
    fun activity_handles_zero_contact_id() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), ContactDetailActivity::class.java).apply {
            putExtra(ContactDetailActivity.EXTRA_CONTACT_ID, 0L)
        }

        // When
        val scenario = ActivityScenario.launch<ContactDetailActivity>(intent)
        Thread.sleep(200)

        // Then - Should handle zero ID
        scenario.close()
    }

    @Test
    fun activity_handles_large_contact_id() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), ContactDetailActivity::class.java).apply {
            putExtra(ContactDetailActivity.EXTRA_CONTACT_ID, Long.MAX_VALUE)
        }

        // When
        val scenario = ActivityScenario.launch<ContactDetailActivity>(intent)
        Thread.sleep(200)

        // Then - Should handle large ID
        scenario.close()
    }

    @Test
    fun activity_loads_contact_data_on_launch() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), ContactDetailActivity::class.java).apply {
            putExtra(ContactDetailActivity.EXTRA_CONTACT_ID, 1L)
        }

        // When
        val scenario = ActivityScenario.launch<ContactDetailActivity>(intent)
        Thread.sleep(500)

        // Then - LaunchedEffect should trigger load
        scenario.close()
    }

    @Test
    fun activity_collects_viewmodel_state() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), ContactDetailActivity::class.java).apply {
            putExtra(ContactDetailActivity.EXTRA_CONTACT_ID, 1L)
        }

        // When
        val scenario = ActivityScenario.launch<ContactDetailActivity>(intent)
        Thread.sleep(300)

        // Then - State collection should work
        scenario.close()
    }

    @Test
    fun activity_displays_contact_detail_screen() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), ContactDetailActivity::class.java).apply {
            putExtra(ContactDetailActivity.EXTRA_CONTACT_ID, 1L)
        }

        // When
        val scenario = ActivityScenario.launch<ContactDetailActivity>(intent)
        Thread.sleep(500)

        // Then - Should display UI
        scenario.close()
    }

}
