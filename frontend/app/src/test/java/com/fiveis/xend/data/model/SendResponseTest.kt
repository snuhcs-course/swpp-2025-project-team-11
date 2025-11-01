package com.fiveis.xend.data.model

import org.junit.Assert.*
import org.junit.Test

class SendResponseTest {

    @Test
    fun create_send_response_with_all_fields() {
        val response = SendResponse(
            id = "msg-123",
            threadId = "thread-456",
            labelIds = listOf("SENT", "INBOX")
        )

        assertEquals("msg-123", response.id)
        assertEquals("thread-456", response.threadId)
        assertEquals(2, response.labelIds.size)
        assertEquals("SENT", response.labelIds[0])
        assertEquals("INBOX", response.labelIds[1])
    }

    @Test
    fun create_send_response_with_null_thread_id() {
        val response = SendResponse(
            id = "msg-789",
            threadId = null,
            labelIds = listOf("SENT")
        )

        assertEquals("msg-789", response.id)
        assertNull(response.threadId)
        assertEquals(1, response.labelIds.size)
    }

    @Test
    fun create_send_response_with_empty_label_ids() {
        val response = SendResponse(
            id = "msg-abc",
            threadId = "thread-def",
            labelIds = emptyList()
        )

        assertTrue(response.labelIds.isEmpty())
    }

    @Test
    fun send_response_copy_updates_id() {
        val original = SendResponse(
            id = "old-id",
            threadId = "thread",
            labelIds = listOf("SENT")
        )
        val updated = original.copy(id = "new-id")

        assertEquals("new-id", updated.id)
        assertEquals("thread", updated.threadId)
    }

    @Test
    fun send_responses_with_same_values_are_equal() {
        val response1 = SendResponse(
            id = "id",
            threadId = "thread",
            labelIds = listOf("SENT")
        )
        val response2 = SendResponse(
            id = "id",
            threadId = "thread",
            labelIds = listOf("SENT")
        )

        assertEquals(response1, response2)
        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun send_responses_with_different_ids_are_not_equal() {
        val response1 = SendResponse(
            id = "id1",
            threadId = "thread",
            labelIds = listOf("SENT")
        )
        val response2 = SendResponse(
            id = "id2",
            threadId = "thread",
            labelIds = listOf("SENT")
        )

        assertNotEquals(response1, response2)
    }

    @Test
    fun send_response_to_string_contains_fields() {
        val response = SendResponse(
            id = "my-id",
            threadId = "my-thread",
            labelIds = listOf("SENT")
        )
        val toString = response.toString()

        assertTrue(toString.contains("my-id"))
        assertTrue(toString.contains("my-thread"))
    }
}
