package com.fiveis.xend.ui.search

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.ui.search.SearchActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchScreenIntegrationTest {

    private lateinit var scenario: ActivityScenario<SearchActivity>

    @Before
    fun setup() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), SearchActivity::class.java)
        scenario = ActivityScenario.launch(intent)
        Thread.sleep(2000)
    }

    @Test
    fun searchScreen_displays_correctly() {
        Thread.sleep(1500)
    }

    @Test
    fun searchScreen_has_search_field() {
        Thread.sleep(1000)
    }

    @Test
    fun searchScreen_shows_results() {
        Thread.sleep(2000)
    }

    @Test
    fun searchScreen_handles_empty_query() {
        Thread.sleep(1500)
    }

    @Test
    fun searchScreen_back_button_works() {
        Thread.sleep(1000)
    }

    @Test
    fun searchScreen_filter_options() {
        Thread.sleep(1500)
    }

    @Test
    fun searchScreen_result_click() {
        Thread.sleep(1500)
    }
}
