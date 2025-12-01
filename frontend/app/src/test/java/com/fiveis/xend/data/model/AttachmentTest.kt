package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AttachmentTest {

    @Test
    fun test_construction_setsAllFields() {
        val attachment = Attachment(
            attachmentId = "att123",
            filename = "document.pdf",
            mimeType = "application/pdf",
            size = 1024000L
        )

        assertEquals("att123", attachment.attachmentId)
        assertEquals("document.pdf", attachment.filename)
        assertEquals("application/pdf", attachment.mimeType)
        assertEquals(1024000L, attachment.size)
    }

    @Test
    fun test_copy_changesFilename() {
        val original = Attachment("att1", "file1.pdf", "application/pdf", 1000L)
        val modified = original.copy(filename = "file2.pdf")

        assertEquals("file2.pdf", modified.filename)
        assertEquals("att1", modified.attachmentId)
        assertEquals("application/pdf", modified.mimeType)
        assertEquals(1000L, modified.size)
    }

    @Test
    fun test_copy_changesSize() {
        val original = Attachment("att1", "file.pdf", "application/pdf", 1000L)
        val modified = original.copy(size = 2000L)

        assertEquals(2000L, modified.size)
        assertEquals("att1", modified.attachmentId)
    }

    @Test
    fun test_equals_sameValues_returnsTrue() {
        val attachment1 = Attachment("att1", "file.pdf", "application/pdf", 1000L)
        val attachment2 = Attachment("att1", "file.pdf", "application/pdf", 1000L)

        assertEquals(attachment1, attachment2)
    }

    @Test
    fun test_differentMimeTypes() {
        val pdf = Attachment("att1", "doc.pdf", "application/pdf", 1000L)
        val image = Attachment("att2", "pic.jpg", "image/jpeg", 2000L)
        val text = Attachment("att3", "note.txt", "text/plain", 500L)

        assertEquals("application/pdf", pdf.mimeType)
        assertEquals("image/jpeg", image.mimeType)
        assertEquals("text/plain", text.mimeType)
    }
}
