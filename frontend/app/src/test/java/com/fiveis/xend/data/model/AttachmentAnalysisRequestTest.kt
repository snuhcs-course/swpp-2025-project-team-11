package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AttachmentAnalysisRequestTest {

    @Test
    fun test_construction_setsAllFields() {
        val request = AttachmentAnalysisRequest(
            messageId = "msg123",
            attachmentId = "att456",
            filename = "document.pdf",
            mimeType = "application/pdf"
        )

        assertEquals("msg123", request.messageId)
        assertEquals("att456", request.attachmentId)
        assertEquals("document.pdf", request.filename)
        assertEquals("application/pdf", request.mimeType)
    }

    @Test
    fun test_copy_changesFields() {
        val original = AttachmentAnalysisRequest(
            messageId = "msg1",
            attachmentId = "att1",
            filename = "file1.pdf",
            mimeType = "application/pdf"
        )

        val modified = original.copy(filename = "file2.pdf", mimeType = "image/png")

        assertEquals("msg1", modified.messageId)
        assertEquals("att1", modified.attachmentId)
        assertEquals("file2.pdf", modified.filename)
        assertEquals("image/png", modified.mimeType)
    }

    @Test
    fun test_equals_sameValues_returnsTrue() {
        val request1 = AttachmentAnalysisRequest("msg1", "att1", "file.pdf", "application/pdf")
        val request2 = AttachmentAnalysisRequest("msg1", "att1", "file.pdf", "application/pdf")

        assertEquals(request1, request2)
    }
}
