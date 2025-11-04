package com.fiveis.xend.data.source

import android.content.Context
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TokenManagerTest {

    private lateinit var context: Context
    private lateinit var tokenManager: TokenManager

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        tokenManager = TokenManager(context)
    }

    @Test
    fun `saveTokens should store tokens`() {
        tokenManager.saveTokens("access123", "refresh456", "test@example.com")
        val accessToken = tokenManager.getAccessToken()
        val refreshToken = tokenManager.getRefreshToken()
        val email = tokenManager.getUserEmail()

        assertEquals("access123", accessToken)
        assertEquals("refresh456", refreshToken)
        assertEquals("test@example.com", email)
    }

    @Test
    fun `clearTokens should remove all tokens`() {
        tokenManager.saveTokens("access123", "refresh456", "test@example.com")
        tokenManager.clearTokens()

        val accessToken = tokenManager.getAccessToken()
        val refreshToken = tokenManager.getRefreshToken()
        val email = tokenManager.getUserEmail()

        assertNull(accessToken)
        assertNull(refreshToken)
        assertNull(email)
    }

    @Test
    fun `getAccessToken returns null when not set`() {
        tokenManager.clearTokens()
        val token = tokenManager.getAccessToken()
        assertNull(token)
    }

    @Test
    fun `getRefreshToken returns null when not set`() {
        tokenManager.clearTokens()
        val token = tokenManager.getRefreshToken()
        assertNull(token)
    }

    @Test
    fun `saveAccessToken should only update access token`() {
        tokenManager.saveTokens("access123", "refresh456", "test@example.com")
        tokenManager.saveAccessToken("newAccess789")

        assertEquals("newAccess789", tokenManager.getAccessToken())
        assertEquals("refresh456", tokenManager.getRefreshToken())
    }

    @Test
    fun `isLoggedIn returns true when tokens exist`() {
        tokenManager.saveTokens("access123", "refresh456", "test@example.com")
        assertEquals(true, tokenManager.isLoggedIn())
    }

    @Test
    fun `isLoggedIn returns false when tokens are cleared`() {
        tokenManager.clearTokens()
        assertEquals(false, tokenManager.isLoggedIn())
    }

    @Test
    fun `saveRefreshToken should only update refresh token`() {
        tokenManager.saveTokens("access123", "refresh456", "test@example.com")
        tokenManager.saveRefreshToken("newRefresh789")

        assertEquals("access123", tokenManager.getAccessToken())
        assertEquals("newRefresh789", tokenManager.getRefreshToken())
    }
}
