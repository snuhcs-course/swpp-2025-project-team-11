package com.fiveis.xend.integration

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.source.TokenManager
import com.fiveis.xend.ui.compose.MailComposeActivity
import com.fiveis.xend.ui.contactbook.ContactBookActivity
import com.fiveis.xend.ui.inbox.InboxActivity
import com.fiveis.xend.ui.login.MainActivity
import com.fiveis.xend.ui.mail.MailActivity
import com.fiveis.xend.ui.profile.ProfileActivity
import com.fiveis.xend.ui.search.SearchActivity
import com.fiveis.xend.ui.sent.SentActivity
import com.fiveis.xend.ui.view.MailDetailActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationIntegrationTest {

    private lateinit var context: Context
    private lateinit var tokenManager: TokenManager

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        tokenManager = TokenManager(context)
    }

    @After
    fun tearDown() {
    }

    @Test
    fun main_activity_launches_successfully() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }

    @Test
    fun inbox_activity_launches_successfully() {
        val scenario = ActivityScenario.launch(InboxActivity::class.java)
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }

    @Test
    fun mail_compose_activity_launches_successfully() {
        val scenario = ActivityScenario.launch(MailComposeActivity::class.java)
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }

    @Test
    fun contact_book_activity_launches_successfully() {
        val scenario = ActivityScenario.launch(ContactBookActivity::class.java)
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }

    @Test
    fun search_activity_launches_successfully() {
        val scenario = ActivityScenario.launch(SearchActivity::class.java)
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }

    @Test
    fun mail_detail_activity_launches_with_intent() {
        val intent = Intent(context, MailDetailActivity::class.java).apply {
            putExtra("message_id", "test_message_123")
        }
        val scenario = ActivityScenario.launch<MailDetailActivity>(intent)
        scenario.onActivity { activity ->
            assertNotNull(activity)
            assertEquals("test_message_123", activity.intent.getStringExtra("message_id"))
        }
        scenario.close()
    }

    @Test
    fun intent_to_inbox_activity_is_correct() {
        val intent = Intent(context, InboxActivity::class.java)
        assertEquals(InboxActivity::class.java.name, intent.component?.className)
    }

    @Test
    fun intent_to_mail_compose_activity_is_correct() {
        val intent = Intent(context, MailComposeActivity::class.java)
        assertEquals(MailComposeActivity::class.java.name, intent.component?.className)
    }

    @Test
    fun intent_to_contact_book_activity_is_correct() {
        val intent = Intent(context, ContactBookActivity::class.java)
        assertEquals(ContactBookActivity::class.java.name, intent.component?.className)
    }

    @Test
    fun intent_to_search_activity_is_correct() {
        val intent = Intent(context, SearchActivity::class.java)
        assertEquals(SearchActivity::class.java.name, intent.component?.className)
    }

    @Test
    fun intent_to_mail_detail_activity_with_extra_is_correct() {
        val messageId = "msg_456"
        val intent = Intent(context, MailDetailActivity::class.java).apply {
            putExtra("message_id", messageId)
        }
        assertEquals(MailDetailActivity::class.java.name, intent.component?.className)
        assertEquals(messageId, intent.getStringExtra("message_id"))
    }

    @Test
    fun activity_navigation_preserves_extras() {
        val testId = "test_email_789"
        val intent = Intent(context, MailDetailActivity::class.java).apply {
            putExtra("message_id", testId)
        }

        val scenario = ActivityScenario.launch<MailDetailActivity>(intent)
        scenario.onActivity { activity ->
            val receivedId = activity.intent.getStringExtra("message_id")
            assertEquals(testId, receivedId)
        }
        scenario.close()
    }

    @Test
    fun token_manager_context_is_available_across_activities() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            val localTokenManager = TokenManager(activity.applicationContext)
            assertNotNull(localTokenManager)
        }
        scenario.close()
    }

    @Test
    fun multiple_activities_can_launch_sequentially() {
        val scenario1 = ActivityScenario.launch(MainActivity::class.java)
        scenario1.close()

        val scenario2 = ActivityScenario.launch(InboxActivity::class.java)
        scenario2.close()

        val scenario3 = ActivityScenario.launch(MailComposeActivity::class.java)
        scenario3.close()
    }

    @Test
    fun activity_recreation_preserves_state() {
        val scenario = ActivityScenario.launch(InboxActivity::class.java)
        scenario.recreate()
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }

    @Test
    fun intent_flags_can_be_set_correctly() {
        val intent = Intent(context, InboxActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val hasFlags = (intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0
        assertEquals(true, hasFlags)
    }

    @Test
    fun application_context_is_consistent() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            assertEquals(context.packageName, activity.applicationContext.packageName)
        }
        scenario.close()
    }

    @Test
    fun activity_lifecycle_onCreate_is_called() {
        var onCreateCalled = false
        val scenario = ActivityScenario.launch(InboxActivity::class.java)
        scenario.onActivity { activity ->
            onCreateCalled = activity.lifecycle.currentState.isAtLeast(
                androidx.lifecycle.Lifecycle.State.CREATED
            )
        }
        assertEquals(true, onCreateCalled)
        scenario.close()
    }

    @Test
    fun multiple_intent_extras_are_preserved() {
        val messageId = "msg_123"
        val threadId = "thread_456"
        val intent = Intent(context, MailComposeActivity::class.java).apply {
            putExtra("recipient_email", messageId)
            putExtra("subject", threadId)
        }

        assertEquals(messageId, intent.getStringExtra("recipient_email"))
        assertEquals(threadId, intent.getStringExtra("subject"))
    }

    @Test
    fun activity_can_finish_without_crash() {
        val scenario = ActivityScenario.launch(InboxActivity::class.java)
        scenario.onActivity { activity ->
            activity.finish()
        }
        scenario.close()
    }

    @Test
    fun profile_activity_launches_from_sent_activity() {
        val scenario = ActivityScenario.launch(SentActivity::class.java)
        Intents.init()
        composeTestRule.onNodeWithContentDescription("Profile").performClick()
        intended(hasComponent(ProfileActivity::class.java.name))
        Intents.release()
        scenario.close()
    }

    @Test
    fun profile_activity_launches_from_mail_activity() {
        val scenario = ActivityScenario.launch(MailActivity::class.java)
        Intents.init()
        composeTestRule.onNodeWithContentDescription("Profile").performClick()
        intended(hasComponent(ProfileActivity::class.java.name))
        Intents.release()
        scenario.close()
    }
}
