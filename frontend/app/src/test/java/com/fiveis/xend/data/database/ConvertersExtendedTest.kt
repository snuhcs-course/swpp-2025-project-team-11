package com.fiveis.xend.data.database

import com.fiveis.xend.data.model.Attachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConvertersExtendedTest {

    private lateinit var converters: Converters

    @Before
    fun setup() {
        converters = Converters()
    }

    @Test
    fun test_stringList_roundTrip_emptyList() {
        val original = emptyList<String>()
        val json = converters.fromStringList(original)
        val result = converters.toStringList(json)

        assertEquals(original, result)
    }

    @Test
    fun test_stringList_roundTrip_singleItem() {
        val original = listOf("INBOX")
        val json = converters.fromStringList(original)
        val result = converters.toStringList(json)

        assertEquals(1, result.size)
        assertEquals("INBOX", result[0])
    }

    @Test
    fun test_stringList_roundTrip_multipleItems() {
        val original = listOf("INBOX", "IMPORTANT", "STARRED", "CATEGORY_PERSONAL")
        val json = converters.fromStringList(original)
        val result = converters.toStringList(json)

        assertEquals(4, result.size)
        assertEquals(original, result)
    }

    @Test
    fun test_stringList_roundTrip_specialCharacters() {
        val original = listOf("한글", "日本語", "Emoji: 🎉", "Special: @#$%")
        val json = converters.fromStringList(original)
        val result = converters.toStringList(json)

        assertEquals(4, result.size)
        assertEquals(original, result)
    }

    @Test
    fun test_stringList_roundTrip_longStrings() {
        val longString = "A".repeat(1000)
        val original = listOf(longString, "short", longString)
        val json = converters.fromStringList(original)
        val result = converters.toStringList(json)

        assertEquals(3, result.size)
        assertEquals(original, result)
    }

    @Test
    fun test_attachmentList_roundTrip_emptyList() {
        val original = emptyList<Attachment>()
        val json = converters.fromAttachmentList(original)
        val result = converters.toAttachmentList(json)

        assertEquals(0, result.size)
    }

    @Test
    fun test_attachmentList_roundTrip_null() {
        val json = converters.fromAttachmentList(null)
        val result = converters.toAttachmentList(json)

        assertEquals(0, result.size)
    }

    @Test
    fun test_attachmentList_roundTrip_singleAttachment() {
        val attachment = Attachment(
            filename = "document.pdf",
            mimeType = "application/pdf",
            size = 1024,
            attachmentId = "att123"
        )
        val original = listOf(attachment)
        val json = converters.fromAttachmentList(original)
        val result = converters.toAttachmentList(json)

        assertEquals(1, result.size)
        assertEquals("document.pdf", result[0].filename)
        assertEquals("application/pdf", result[0].mimeType)
        assertEquals(1024, result[0].size)
        assertEquals("att123", result[0].attachmentId)
    }

    @Test
    fun test_attachmentList_roundTrip_multipleAttachments() {
        val attachments = listOf(
            Attachment("att1", "file1.pdf", "application/pdf", 1024L),
            Attachment("att2", "file2.jpg", "image/jpeg", 2048L),
            Attachment("att3", "file3.txt", "text/plain", 512L)
        )
        val json = converters.fromAttachmentList(attachments)
        val result = converters.toAttachmentList(json)

        assertEquals(3, result.size)
        assertEquals("file1.pdf", result[0].filename)
        assertEquals("file2.jpg", result[1].filename)
        assertEquals("file3.txt", result[2].filename)
    }

    @Test
    fun test_attachmentList_roundTrip_specialCharactersInFilename() {
        val attachment = Attachment(
            filename = "한글파일.pdf",
            mimeType = "application/pdf",
            size = 1024,
            attachmentId = "att123"
        )
        val original = listOf(attachment)
        val json = converters.fromAttachmentList(original)
        val result = converters.toAttachmentList(json)

        assertEquals(1, result.size)
        assertEquals("한글파일.pdf", result[0].filename)
    }

    @Test
    fun test_attachmentList_roundTrip_emojiInFilename() {
        val attachment = Attachment(
            filename = "Important 💌.pdf",
            mimeType = "application/pdf",
            size = 1024,
            attachmentId = "att123"
        )
        val original = listOf(attachment)
        val json = converters.fromAttachmentList(original)
        val result = converters.toAttachmentList(json)

        assertEquals(1, result.size)
        assertEquals("Important 💌.pdf", result[0].filename)
    }

    @Test
    fun test_attachmentList_toAttachmentList_blankString() {
        val result = converters.toAttachmentList("")
        assertEquals(0, result.size)
    }

    @Test
    fun test_attachmentList_toAttachmentList_whitespaceString() {
        val result = converters.toAttachmentList("   ")
        assertEquals(0, result.size)
    }

    @Test
    fun test_stringList_fromStringList_generatesValidJson() {
        val original = listOf("A", "B", "C")
        val json = converters.fromStringList(original)

        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
        assertTrue(json.contains("\"A\""))
        assertTrue(json.contains("\"B\""))
        assertTrue(json.contains("\"C\""))
    }

    @Test
    fun test_attachmentList_fromAttachmentList_generatesValidJson() {
        val attachment = Attachment("att1", "test.pdf", "application/pdf", 1024L)
        val json = converters.fromAttachmentList(listOf(attachment))

        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
        assertTrue(json.contains("\"filename\""))
        assertTrue(json.contains("\"mime_type\""))
        assertTrue(json.contains("\"size\""))
        assertTrue(json.contains("\"attachment_id\""))
    }

    @Test
    fun test_attachmentList_largeSize() {
        val attachment = Attachment(
            filename = "large.bin",
            mimeType = "application/octet-stream",
            size = Long.MAX_VALUE,
            attachmentId = "att_large"
        )
        val original = listOf(attachment)
        val json = converters.fromAttachmentList(original)
        val result = converters.toAttachmentList(json)

        assertEquals(1, result.size)
        assertEquals(Long.MAX_VALUE, result[0].size)
    }

    @Test
    fun test_stringList_manyItems() {
        val original = (1..100).map { "Label_$it" }
        val json = converters.fromStringList(original)
        val result = converters.toStringList(json)

        assertEquals(100, result.size)
        assertEquals(original, result)
    }

    @Test
    fun test_attachmentList_manyAttachments() {
        val original = (1..50).map {
            Attachment("att$it", "file$it.txt", "text/plain", (it * 100).toLong())
        }
        val json = converters.fromAttachmentList(original)
        val result = converters.toAttachmentList(json)

        assertEquals(50, result.size)
        assertEquals("file1.txt", result[0].filename)
        assertEquals("file50.txt", result[49].filename)
    }
}
