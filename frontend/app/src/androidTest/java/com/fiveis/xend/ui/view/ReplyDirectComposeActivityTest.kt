package com.fiveis.xend.ui.view

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReplyDirectComposeActivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun activity_launches_with_intent_data() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ReplyDirectComposeActivity::class.java).apply {
            putExtra("recipient_email", "test@test.com")
            putExtra("recipient_name", "Test User")
            putExtra("subject", "Test Subject")
            putExtra("group_names", ArrayList<String>())
        }
        
        ActivityScenario.launch<ReplyDirectComposeActivity>(intent).use {
            // We wait for the UI to settle. Since we can't easily use onNodeWithText 
            // with createComposeRule for an Activity launched this way (unless we use createAndroidComposeRule which launches it for us),
            // we will just rely on the Activity launching without crashing.
            // However, to be more useful, we can try to assert something if the rule attaches.
            
            // Note: createComposeRule() does not automatically attach to the Activity's window 
            // unless setContent is called on the rule. But since the Activity calls setContent,
            // we might need createAndroidComposeRule. 
            // Since we want to customize the intent, passing a rule that doesn't launch immediately is tricky in standard Compose testing 
            // without a custom rule.
            
            // Thus, this test primarily verifies that the Activity can be instantiated and 
            // handles the Intent extras without crashing in onCreate.
            Thread.sleep(1000)
        }
    }
    
    @Test
    fun activity_launches_with_missing_extras_safely() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ReplyDirectComposeActivity::class.java)
        
        ActivityScenario.launch<ReplyDirectComposeActivity>(intent).use {
            // Verifies that the activity handles missing extras gracefully (e.g., empty strings)
            Thread.sleep(1000)
        }
    }
}
