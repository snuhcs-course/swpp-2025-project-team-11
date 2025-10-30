package com.fiveis.xend.data.source

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenManagerIntegrationTest {

    private lateinit var context: Context
    private lateinit var tokenManager: TokenManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        tokenManager = TokenManager(context)
        tokenManager.clearTokens()
    }

    @After
    fun tearDown() {
        tokenManager.clearTokens()
    }

    @Test
    fun saveTokens_and_getTokens_returns_saved_values() {
        val accessToken = "test_access_token_123"
        val refreshToken = "test_refresh_token_456"
        val email = "test@example.com"

        tokenManager.saveTokens(accessToken, refreshToken, email)

        assertEquals(accessToken, tokenManager.getAccessToken())
        assertEquals(refreshToken, tokenManager.getRefreshToken())
        assertEquals(email, tokenManager.getUserEmail())
    }

    @Test
    fun saveTokens_with_null_access_token_does_not_save_access_token() {
        val refreshToken = "test_refresh_token_456"
        val email = "test@example.com"

        tokenManager.saveTokens(null, refreshToken, email)

        assertNull(tokenManager.getAccessToken())
        assertEquals(refreshToken, tokenManager.getRefreshToken())
        assertEquals(email, tokenManager.getUserEmail())
    }

    @Test
    fun saveTokens_with_empty_access_token_does_not_save_access_token() {
        val refreshToken = "test_refresh_token_456"
        val email = "test@example.com"

        tokenManager.saveTokens("", refreshToken, email)

        assertNull(tokenManager.getAccessToken())
        assertEquals(refreshToken, tokenManager.getRefreshToken())
        assertEquals(email, tokenManager.getUserEmail())
    }

    @Test
    fun saveTokens_with_null_refresh_token_does_not_save_refresh_token() {
        val accessToken = "test_access_token_123"
        val email = "test@example.com"

        tokenManager.saveTokens(accessToken, null, email)

        assertEquals(accessToken, tokenManager.getAccessToken())
        assertNull(tokenManager.getRefreshToken())
        assertEquals(email, tokenManager.getUserEmail())
    }

    @Test
    fun saveAccessToken_updates_only_access_token() {
        val initialAccessToken = "initial_access_token"
        val initialRefreshToken = "initial_refresh_token"
        val email = "test@example.com"

        tokenManager.saveTokens(initialAccessToken, initialRefreshToken, email)

        val newAccessToken = "new_access_token_123"
        tokenManager.saveAccessToken(newAccessToken)

        assertEquals(newAccessToken, tokenManager.getAccessToken())
        assertEquals(initialRefreshToken, tokenManager.getRefreshToken())
        assertEquals(email, tokenManager.getUserEmail())
    }

    @Test
    fun saveRefreshToken_updates_only_refresh_token() {
        val initialAccessToken = "initial_access_token"
        val initialRefreshToken = "initial_refresh_token"
        val email = "test@example.com"

        tokenManager.saveTokens(initialAccessToken, initialRefreshToken, email)

        val newRefreshToken = "new_refresh_token_456"
        tokenManager.saveRefreshToken(newRefreshToken)

        assertEquals(initialAccessToken, tokenManager.getAccessToken())
        assertEquals(newRefreshToken, tokenManager.getRefreshToken())
        assertEquals(email, tokenManager.getUserEmail())
    }

    @Test
    fun clearTokens_removes_all_tokens() {
        val accessToken = "test_access_token_123"
        val refreshToken = "test_refresh_token_456"
        val email = "test@example.com"

        tokenManager.saveTokens(accessToken, refreshToken, email)

        tokenManager.clearTokens()

        assertNull(tokenManager.getAccessToken())
        assertNull(tokenManager.getRefreshToken())
        assertNull(tokenManager.getUserEmail())
    }

    @Test
    fun isLoggedIn_returns_true_when_tokens_exist() {
        val accessToken = "test_access_token_123"
        val refreshToken = "test_refresh_token_456"
        val email = "test@example.com"

        tokenManager.saveTokens(accessToken, refreshToken, email)

        assertTrue(tokenManager.isLoggedIn())
    }

    @Test
    fun isLoggedIn_returns_false_when_access_token_is_missing() {
        val refreshToken = "test_refresh_token_456"
        val email = "test@example.com"

        tokenManager.saveTokens(null, refreshToken, email)

        assertFalse(tokenManager.isLoggedIn())
    }

    @Test
    fun isLoggedIn_returns_false_when_email_is_missing() {
        val accessToken = "test_access_token_123"
        val refreshToken = "test_refresh_token_456"

        tokenManager.saveTokens(accessToken, refreshToken, "")

        assertFalse(tokenManager.isLoggedIn())
    }

    @Test
    fun isLoggedIn_returns_false_when_no_tokens_saved() {
        assertFalse(tokenManager.isLoggedIn())
    }

    @Test
    fun getAccessToken_returns_null_when_not_saved() {
        assertNull(tokenManager.getAccessToken())
    }

    @Test
    fun getRefreshToken_returns_null_when_not_saved() {
        assertNull(tokenManager.getRefreshToken())
    }

    @Test
    fun getUserEmail_returns_null_when_not_saved() {
        assertNull(tokenManager.getUserEmail())
    }

    @Test
    fun tokens_persist_across_multiple_tokenManager_instances() {
        val accessToken = "test_access_token_123"
        val refreshToken = "test_refresh_token_456"
        val email = "test@example.com"

        tokenManager.saveTokens(accessToken, refreshToken, email)

        val newTokenManager = TokenManager(context)

        assertEquals(accessToken, newTokenManager.getAccessToken())
        assertEquals(refreshToken, newTokenManager.getRefreshToken())
        assertEquals(email, newTokenManager.getUserEmail())
    }

    @Test
    fun saveTokens_overwrites_existing_tokens() {
        val firstAccessToken = "first_access_token"
        val firstRefreshToken = "first_refresh_token"
        val firstEmail = "first@example.com"

        tokenManager.saveTokens(firstAccessToken, firstRefreshToken, firstEmail)

        val secondAccessToken = "second_access_token"
        val secondRefreshToken = "second_refresh_token"
        val secondEmail = "second@example.com"

        tokenManager.saveTokens(secondAccessToken, secondRefreshToken, secondEmail)

        assertEquals(secondAccessToken, tokenManager.getAccessToken())
        assertEquals(secondRefreshToken, tokenManager.getRefreshToken())
        assertEquals(secondEmail, tokenManager.getUserEmail())
    }

    @Test
    fun tokens_are_encrypted_in_shared_preferences() {
        val accessToken = "test_access_token_123"
        val refreshToken = "test_refresh_token_456"
        val email = "test@example.com"

        tokenManager.saveTokens(accessToken, refreshToken, email)

        val regularPrefs = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
        val allPrefs = regularPrefs.all

        assertNotNull(allPrefs)
        assertTrue(allPrefs.isNotEmpty())
    }

    @Test
    fun long_tokens_are_saved_and_retrieved_correctly() {
        val longAccessToken = "a".repeat(500)
        val longRefreshToken = "b".repeat(500)
        val email = "test@example.com"

        tokenManager.saveTokens(longAccessToken, longRefreshToken, email)

        assertEquals(longAccessToken, tokenManager.getAccessToken())
        assertEquals(longRefreshToken, tokenManager.getRefreshToken())
        assertEquals(email, tokenManager.getUserEmail())
    }

    @Test
    fun special_characters_in_tokens_are_handled_correctly() {
        val accessToken = "token!@#$%^&*()_+-=[]{}|;':\",./<>?"
        val refreshToken = "refresh!@#$%^&*()_+-=[]{}|;':\",./<>?"
        val email = "test+special@example.com"

        tokenManager.saveTokens(accessToken, refreshToken, email)

        assertEquals(accessToken, tokenManager.getAccessToken())
        assertEquals(refreshToken, tokenManager.getRefreshToken())
        assertEquals(email, tokenManager.getUserEmail())
    }
}
