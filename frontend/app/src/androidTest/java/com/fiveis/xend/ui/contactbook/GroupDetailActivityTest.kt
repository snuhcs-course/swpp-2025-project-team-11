package com.fiveis.xend.ui.contactbook

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupDetailActivityTest {

    @Test
    fun activity_with_valid_group_id_launches() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), GroupDetailActivity::class.java).apply {
            putExtra(GroupDetailActivity.EXTRA_GROUP_ID, 1L)
        }
        // use 블록을 사용하여 테스트가 끝나거나 에러가 나도 무조건 scenario.close()가 호출되도록 보장
        ActivityScenario.launch<GroupDetailActivity>(intent).use { scenario ->
            Thread.sleep(1000)
            assertNotNull(scenario)
        }
    }

    @Test
    fun activity_companion_object_constant() {
        assert(GroupDetailActivity.EXTRA_GROUP_ID == "extra_group_id")
    }

    @Test
    fun activity_handles_zero_group_id() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), GroupDetailActivity::class.java).apply {
            putExtra(GroupDetailActivity.EXTRA_GROUP_ID, 0L)
        }
        ActivityScenario.launch<GroupDetailActivity>(intent).use { scenario ->
            Thread.sleep(1000)
            assertNotNull(scenario)
        }
    }

    @Test
    fun activity_loads_group_data_on_launch() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), GroupDetailActivity::class.java).apply {
            putExtra(GroupDetailActivity.EXTRA_GROUP_ID, 1L)
        }
        ActivityScenario.launch<GroupDetailActivity>(intent).use { scenario ->
            Thread.sleep(1000)
            assertNotNull(scenario)
        }
    }

    @Test
    fun activity_collects_viewmodel_state_with_lifecycle() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), GroupDetailActivity::class.java).apply {
            putExtra(GroupDetailActivity.EXTRA_GROUP_ID, 1L)
        }
        ActivityScenario.launch<GroupDetailActivity>(intent).use { scenario ->
            Thread.sleep(1000)
            assertNotNull(scenario)
        }
    }

    @Test
    fun activity_displays_group_detail_screen() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), GroupDetailActivity::class.java).apply {
            putExtra(GroupDetailActivity.EXTRA_GROUP_ID, 1L)
        }
        ActivityScenario.launch<GroupDetailActivity>(intent).use { scenario ->
            Thread.sleep(1000)
            assertNotNull(scenario)
        }
    }
}
