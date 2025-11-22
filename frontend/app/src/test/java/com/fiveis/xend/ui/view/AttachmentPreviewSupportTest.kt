package com.fiveis.xend.ui.view

import com.fiveis.xend.data.model.Attachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentPreviewSupportTest {

    @Test
    fun test_previewType_pdf_byMimeType() {
        val attachment = Attachment(
            attachmentId = "1",
            filename = "document.file",
            mimeType = "application/pdf",
            size = 1024
        )
        assertEquals(AttachmentPreviewType.PDF, attachment.previewType())
    }

    @Test
    fun test_previewType_pdf_byExtension() {
        val attachment = Attachment(
            attachmentId = "2",
            filename = "document.pdf",
            mimeType = "application/octet-stream",
            size = 1024
        )
        assertEquals(AttachmentPreviewType.PDF, attachment.previewType())
    }

    @Test
    fun test_previewType_text_byMimeType() {
        val attachment = Attachment(
            attachmentId = "3",
            filename = "file.dat",
            mimeType = "text/plain",
            size = 512
        )
        assertEquals(AttachmentPreviewType.TEXT, attachment.previewType())
    }

    @Test
    fun test_previewType_text_json() {
        val attachment = Attachment(
            attachmentId = "4",
            filename = "data.dat",
            mimeType = "application/json",
            size = 256
        )
        assertEquals(AttachmentPreviewType.TEXT, attachment.previewType())
    }

    @Test
    fun test_previewType_text_byTxtExtension() {
        val attachment = Attachment(
            attachmentId = "5",
            filename = "notes.txt",
            mimeType = "application/octet-stream",
            size = 128
        )
        assertEquals(AttachmentPreviewType.TEXT, attachment.previewType())
    }

    @Test
    fun test_previewType_text_byMdExtension() {
        val attachment = Attachment(
            attachmentId = "6",
            filename = "README.md",
            mimeType = "application/octet-stream",
            size = 256
        )
        assertEquals(AttachmentPreviewType.TEXT, attachment.previewType())
    }

    @Test
    fun test_previewType_text_byCsvExtension() {
        val attachment = Attachment(
            attachmentId = "7",
            filename = "data.csv",
            mimeType = "application/octet-stream",
            size = 1024
        )
        assertEquals(AttachmentPreviewType.TEXT, attachment.previewType())
    }

    @Test
    fun test_previewType_unsupported_image() {
        val attachment = Attachment(
            attachmentId = "8",
            filename = "photo.jpg",
            mimeType = "image/jpeg",
            size = 2048
        )
        assertEquals(AttachmentPreviewType.UNSUPPORTED, attachment.previewType())
    }

    @Test
    fun test_previewType_unsupported_binary() {
        val attachment = Attachment(
            attachmentId = "9",
            filename = "archive.zip",
            mimeType = "application/zip",
            size = 4096
        )
        assertEquals(AttachmentPreviewType.UNSUPPORTED, attachment.previewType())
    }

    @Test
    fun test_previewType_caseInsensitive_upperCaseExtension() {
        val attachment = Attachment(
            attachmentId = "10",
            filename = "DOCUMENT.PDF",
            mimeType = "application/octet-stream",
            size = 1024
        )
        assertEquals(AttachmentPreviewType.PDF, attachment.previewType())
    }

    @Test
    fun test_previewType_caseInsensitive_mixedCaseMimeType() {
        val attachment = Attachment(
            attachmentId = "11",
            filename = "file.dat",
            mimeType = "Text/Plain",
            size = 512
        )
        assertEquals(AttachmentPreviewType.TEXT, attachment.previewType())
    }

    @Test
    fun test_supportsInAppPreview_pdf() {
        val attachment = Attachment(
            attachmentId = "12",
            filename = "doc.pdf",
            mimeType = "application/pdf",
            size = 1024
        )
        assertTrue(attachment.supportsInAppPreview())
    }

    @Test
    fun test_supportsInAppPreview_text() {
        val attachment = Attachment(
            attachmentId = "13",
            filename = "file.txt",
            mimeType = "text/plain",
            size = 512
        )
        assertTrue(attachment.supportsInAppPreview())
    }

    @Test
    fun test_supportsInAppPreview_unsupported() {
        val attachment = Attachment(
            attachmentId = "14",
            filename = "image.png",
            mimeType = "image/png",
            size = 2048
        )
        assertFalse(attachment.supportsInAppPreview())
    }

    @Test
    fun test_previewType_textHtml() {
        val attachment = Attachment(
            attachmentId = "15",
            filename = "page.html",
            mimeType = "text/html",
            size = 1024
        )
        assertEquals(AttachmentPreviewType.TEXT, attachment.previewType())
    }
}
