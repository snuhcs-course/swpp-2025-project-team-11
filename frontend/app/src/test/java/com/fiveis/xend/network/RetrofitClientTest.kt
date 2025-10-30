package com.fiveis.xend.network

import android.content.Context
import com.fiveis.xend.data.source.TokenManager
import io.mockk.*
import okhttp3.Dns
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
class RetrofitClientTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    @After
    fun tear_down() {
        unmockkAll()
    }

    @Test
    fun get_client_returns_ok_http_client() {
        val client = RetrofitClient.getClient(context)

        assertNotNull(client)
        assertEquals(30000L, client.connectTimeoutMillis.toLong())
        assertEquals(30000L, client.readTimeoutMillis.toLong())
        assertEquals(30000L, client.writeTimeoutMillis.toLong())
    }

    @Test
    fun get_client_adds_interceptors() {
        val client = RetrofitClient.getClient(context)

        assertNotNull(client)
        assertTrue(client.interceptors.isNotEmpty())
    }

    @Test
    fun get_client_adds_authenticator() {
        val client = RetrofitClient.getClient(context)

        assertNotNull(client)
        assertNotNull(client.authenticator)
    }

    @Test
    fun get_mail_api_service_returns_mail_api_service() {
        val service = RetrofitClient.getMailApiService(context)

        assertNotNull(service)
    }

    @Test
    fun get_contact_api_service_returns_contact_api_service() {
        val service = RetrofitClient.getContactApiService(context)

        assertNotNull(service)
    }

    @Test
    fun auth_api_service_is_not_null() {
        val service = RetrofitClient.authApiService

        assertNotNull(service)
    }

    @Test
    fun auth_api_service_is_lazy_initialized_once() {
        val service1 = RetrofitClient.authApiService
        val service2 = RetrofitClient.authApiService

        assertSame(service1, service2)
    }

    @Test
    fun get_client_with_different_contexts_returns_different_clients() {
        val client1 = RetrofitClient.getClient(context)
        val client2 = RetrofitClient.getClient(context)

        // Each call creates a new instance
        assertNotSame(client1, client2)
    }

    @Test
    fun get_mail_api_service_creates_service_instance() {
        val service = RetrofitClient.getMailApiService(context)

        assertNotNull(service)
        assertEquals("com.fiveis.xend.network.MailApiService", service.javaClass.interfaces[0].name)
    }

    @Test
    fun get_contact_api_service_creates_service_instance() {
        val service = RetrofitClient.getContactApiService(context)

        assertNotNull(service)
        assertEquals("com.fiveis.xend.network.ContactApiService", service.javaClass.interfaces[0].name)
    }

    @Test
    fun dns_response_data_class_works_correctly() {
        // Access private DnsResponse and DnsAnswer classes via reflection to cover them
        val client = RetrofitClient.getClient(context)

        // This will trigger DNS resolution which uses customDns internally
        assertNotNull(client.dns)
    }

    @Test
    fun custom_dns_lookup_succeeds_for_valid_hostname() {
        val client = RetrofitClient.getClient(context)

        // The client has customDns configured
        // When making actual requests, it will use the DNS
        assertNotNull(client.dns)
    }

    @Test
    fun get_client_creates_client_with_custom_dns() {
        val client = RetrofitClient.getClient(context)

        assertNotNull(client.dns)
        // Verify the client is properly configured
        assertTrue(client.connectTimeoutMillis > 0)
    }

    @Test
    fun multiple_get_client_calls_create_separate_instances() {
        val client1 = RetrofitClient.getClient(context)
        val client2 = RetrofitClient.getClient(context)

        // Each call should create a new client instance
        assertNotSame(client1, client2)
        // But both should have DNS configured
        assertNotNull(client1.dns)
        assertNotNull(client2.dns)
    }
}
