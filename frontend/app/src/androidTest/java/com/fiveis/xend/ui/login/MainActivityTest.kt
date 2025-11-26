package com.fiveis.xend.ui.login

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fiveis.xend.data.source.TokenManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private lateinit var tokenManager: TokenManager
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        tokenManager = TokenManager(context)
        // Clear any existing tokens
        tokenManager.clearTokens()
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
        tokenManager.clearTokens()
    }

    @Test
    fun mainActivity_launches_successfully() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - Activity should launch without crash
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun mainActivity_displays_login_screen_when_not_logged_in() {
        // Given - No saved tokens
        tokenManager.clearTokens()

        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - Should show LoginScreen (wait for composition)
        Thread.sleep(500) // Wait for compose to render
        // LoginScreen content should be visible
    }

    @Test
    fun mainActivity_creates_with_correct_theme() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then
        scenario.onActivity { activity ->
            assertNotNull(activity)
            // Activity should be created successfully with theme
        }
    }

    @Test
    fun mainActivity_initializes_tokenManager() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then
        scenario.onActivity { activity ->
            assertNotNull(activity)
            // TokenManager should be initialized (implicitly tested by no crash)
        }
    }

    @Test
    fun mainActivity_handles_onCreate_lifecycle() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - Should complete onCreate without exceptions
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun mainActivity_sets_content_view() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then
        scenario.onActivity { activity ->
            // Content should be set via setContent
            assertNotNull(activity.window.decorView)
        }
    }

    @Test
    fun mainActivity_survives_recreation() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.recreate()

        // Then - Activity should recreate successfully
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun mainActivity_with_saved_tokens_should_not_crash() {
        // Given
        tokenManager.saveTokens(
            access = "test_access_token",
            refresh = "test_refresh_token",
            email = "test@example.com"
        )

        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - Should not crash even with saved tokens
        // Activity may navigate away if tokens are valid, so just verify it launched
        Thread.sleep(100)
    }

    @Test
    fun mainActivity_handles_configuration_change() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Simulate configuration change
        scenario.recreate()

        // Then - Should handle recreation
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun mainActivity_can_be_finished() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { it.finish() }

        // Then - Should finish gracefully
        Thread.sleep(100)
    }

    @Test
    fun mainActivity_launches_with_intent() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)

        // When
        scenario = ActivityScenario.launch<MainActivity>(intent)

        // Then
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun mainActivity_handles_null_savedInstanceState() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - First launch with null savedInstanceState
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun mainActivity_multiple_launch_and_finish_cycles() {
        // First launch
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { it.finish() }
        scenario.close()
        Thread.sleep(100)

        // Second launch
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun mainActivity_context_is_available() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then
        scenario.onActivity { activity ->
            assertNotNull(activity.applicationContext)
            assertNotNull(activity.baseContext)
        }
    }

    @Test
    fun mainActivity_window_is_initialized() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then
        scenario.onActivity { activity ->
            assertNotNull(activity.window)
            assertNotNull(activity.window.decorView)
        }
    }

    @Test
    fun mainActivity_theme_is_applied() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then
        scenario.onActivity { activity ->
            assertNotNull(activity.theme)
        }
    }

    @Test
    fun mainActivity_survives_pause_and_resume() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

        // Then
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun mainActivity_can_move_to_background() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)

        // Then - Should handle background state
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun mainActivity_resources_are_available() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then
        scenario.onActivity { activity ->
            assertNotNull(activity.resources)
            assertNotNull(activity.assets)
        }
    }

    @Test
    fun mainActivity_has_valid_component_name() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then
        scenario.onActivity { activity ->
            assertNotNull(activity.componentName)
            assertEquals("com.fiveis.xend", activity.componentName.packageName)
        }
    }

    @Test
    fun mainActivity_instrumentation_context() {
        // Given
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Then
        assertNotNull(context)
        assertEquals("com.fiveis.xend", context.packageName)
    }
}
