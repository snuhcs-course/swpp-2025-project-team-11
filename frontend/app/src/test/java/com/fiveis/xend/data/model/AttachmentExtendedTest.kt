package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentExtendedTest {

    @Test
    fun test_attachment_pdf() {
        val attachment = Attachment(
            filename = "document.pdf",
            mimeType = "application/pdf",
            size = 102400,
            attachmentId = "att_123"
        )

        assertEquals("document.pdf", attachment.filename)
        assertEquals("application/pdf", attachment.mimeType)
        assertEquals(102400, attachment.size)
        assertEquals("att_123", attachment.attachmentId)
    }

    @Test
    fun test_attachment_image() {
        val attachment = Attachment(
            filename = "photo.jpg",
            mimeType = "image/jpeg",
            size = 2048000,
            attachmentId = "att_456"
        )

        assertEquals("photo.jpg", attachment.filename)
        assertEquals("image/jpeg", attachment.mimeType)
        assertEquals(2048000, attachment.size)
    }

    @Test
    fun test_attachment_text() {
        val attachment = Attachment(
            filename = "notes.txt",
            mimeType = "text/plain",
            size = 512,
            attachmentId = "att_789"
        )

        assertEquals("notes.txt", attachment.filename)
        assertEquals("text/plain", attachment.mimeType)
        assertEquals(512, attachment.size)
    }

    @Test
    fun test_attachment_zip() {
        val attachment = Attachment(
            filename = "archive.zip",
            mimeType = "application/zip",
            size = 5242880,
            attachmentId = "att_zip"
        )

        assertEquals("archive.zip", attachment.filename)
        assertEquals("application/zip", attachment.mimeType)
    }

    @Test
    fun test_attachment_specialCharactersInFilename() {
        val attachment = Attachment(
            filename = "문서 파일 (1).pdf",
            mimeType = "application/pdf",
            size = 1024,
            attachmentId = "att_kr"
        )

        assertEquals("문서 파일 (1).pdf", attachment.filename)
    }

    @Test
    fun test_attachment_longFilename() {
        val longFilename = "very_long_filename_" + "a".repeat(200) + ".pdf"
        val attachment = Attachment(
            filename = longFilename,
            mimeType = "application/pdf",
            size = 1024,
            attachmentId = "att_long"
        )

        assertTrue(attachment.filename.length > 200)
    }

    @Test
    fun test_attachment_largeSize() {
        val attachment = Attachment(
            filename = "video.mp4",
            mimeType = "video/mp4",
            size = Long.MAX_VALUE,
            attachmentId = "att_video"
        )

        assertEquals(Long.MAX_VALUE, attachment.size)
    }

    @Test
    fun test_attachment_zeroSize() {
        val attachment = Attachment(
            filename = "empty.txt",
            mimeType = "text/plain",
            size = 0,
            attachmentId = "att_empty"
        )

        assertEquals(0, attachment.size)
    }

    @Test
    fun test_attachment_equality() {
        val attachment1 = Attachment("att_1", "file.pdf", "application/pdf", 1024L)
        val attachment2 = Attachment("att_1", "file.pdf", "application/pdf", 1024L)

        assertEquals(attachment1, attachment2)
        assertEquals(attachment1.hashCode(), attachment2.hashCode())
    }

    @Test
    fun test_attachment_inequality_differentFilename() {
        val attachment1 = Attachment("att_1", "file1.pdf", "application/pdf", 1024L)
        val attachment2 = Attachment("att_1", "file2.pdf", "application/pdf", 1024L)

        assertNotEquals(attachment1, attachment2)
    }

    @Test
    fun test_attachment_inequality_differentSize() {
        val attachment1 = Attachment("att_1", "file.pdf", "application/pdf", 1024L)
        val attachment2 = Attachment("att_1", "file.pdf", "application/pdf", 2048L)

        assertNotEquals(attachment1, attachment2)
    }

    @Test
    fun test_attachment_copy() {
        val original = Attachment("att_1", "file.pdf", "application/pdf", 1024L)
        val modified = original.copy(filename = "renamed.pdf", size = 2048)

        assertEquals("file.pdf", original.filename)
        assertEquals(1024, original.size)
        assertEquals("renamed.pdf", modified.filename)
        assertEquals(2048, modified.size)
    }

    @Test
    fun test_attachment_variousMimeTypes() {
        val mimeTypes = mapOf(
            "document.pdf" to "application/pdf",
            "image.png" to "image/png",
            "video.mp4" to "video/mp4",
            "audio.mp3" to "audio/mpeg",
            "spreadsheet.xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )

        mimeTypes.forEach { (filename, mimeType) ->
            val attachment = Attachment("att", filename, mimeType, 1024L)
            assertEquals(mimeType, attachment.mimeType)
        }
    }

    @Test
    fun test_attachment_emojiInFilename() {
        val attachment = Attachment(
            filename = "Important 💌.pdf",
            mimeType = "application/pdf",
            size = 1024,
            attachmentId = "att_emoji"
        )

        assertEquals("Important 💌.pdf", attachment.filename)
    }

    @Test
    fun test_attachment_multipleExtensions() {
        val attachment = Attachment(
            filename = "backup.tar.gz",
            mimeType = "application/gzip",
            size = 10485760,
            attachmentId = "att_tar"
        )

        assertEquals("backup.tar.gz", attachment.filename)
    }
}
