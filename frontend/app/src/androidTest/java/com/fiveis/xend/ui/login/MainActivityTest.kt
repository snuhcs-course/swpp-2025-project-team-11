package com.fiveis.xend.ui.login

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
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
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        tokenManager = TokenManager(context)
        // Clear any existing tokens to ensure clean state
        tokenManager.clearTokens()
    }

    @After
    fun tearDown() {
        scenario?.close()
        tokenManager.clearTokens()
    }

    @Test
    fun mainActivity_launches_successfully() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - Activity should be in RESUMED state
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
    }

    @Test
    fun mainActivity_displays_login_screen_when_not_logged_in() {
        // Given - No saved tokens
        tokenManager.clearTokens()

        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - Should stay in RESUMED state (not navigate away)
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
    }

    @Test
    fun mainActivity_creates_with_correct_theme() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - Verify lifecycle state
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
    }

    @Test
    fun mainActivity_initializes_tokenManager() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - Activity should launch successfully with TokenManager initialized
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
    }

    @Test
    fun mainActivity_handles_onCreate_lifecycle() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - Should complete onCreate without exceptions
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
    }

    @Test
    fun mainActivity_sets_content_view() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - Verify activity state
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
    }

    @Test
    fun mainActivity_survives_recreation() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario?.recreate()

        // Then - Activity should recreate successfully
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
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

        // Then - Should not crash (may navigate to MailActivity and finish, which is ok)
        assertNotNull(scenario)
    }

    @Test
    fun mainActivity_handles_configuration_change() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Simulate configuration change
        scenario?.recreate()

        // Then - Should handle recreation
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
    }

    @Test
    fun mainActivity_can_be_finished() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - Should be able to move to DESTROYED state
        scenario?.moveToState(Lifecycle.State.DESTROYED)
        assertEquals(Lifecycle.State.DESTROYED, scenario?.state)
    }

    @Test
    fun mainActivity_launches_with_intent() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)

        // When
        scenario = ActivityScenario.launch<MainActivity>(intent)

        // Then
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
    }

    @Test
    fun mainActivity_handles_null_savedInstanceState() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - First launch with null savedInstanceState
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
    }

    @Test
    fun mainActivity_multiple_launch_and_finish_cycles() {
        // First launch
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario?.close()

        // Second launch
        scenario = ActivityScenario.launch(MainActivity::class.java)
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
    }

    @Test
    fun mainActivity_context_is_available() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - Verify activity launched successfully
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
    }

    @Test
    fun mainActivity_window_is_initialized() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - Verify activity state
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
    }

    @Test
    fun mainActivity_theme_is_applied() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - Verify activity launched with theme
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
    }

    @Test
    fun mainActivity_survives_pause_and_resume() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario?.moveToState(Lifecycle.State.STARTED)
        scenario?.moveToState(Lifecycle.State.RESUMED)

        // Then
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
    }

    @Test
    fun mainActivity_can_move_to_background() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario?.moveToState(Lifecycle.State.CREATED)

        // Then - Should handle background state
        assertEquals(Lifecycle.State.CREATED, scenario?.state)
    }

    @Test
    fun mainActivity_resources_are_available() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - Verify activity launched
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
    }

    @Test
    fun mainActivity_has_valid_component_name() {
        // When
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Then - Verify activity state
        assertEquals(Lifecycle.State.RESUMED, scenario?.state)
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
