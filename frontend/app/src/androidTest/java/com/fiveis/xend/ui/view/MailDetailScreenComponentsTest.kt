package com.fiveis.xend.ui.view

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Attachment
import com.fiveis.xend.data.model.AttachmentAnalysisResponse
// import com.fiveis.xend.data.model.AttachmentPreviewContent (removed as it's in the same package)
import com.fiveis.xend.data.model.EmailItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MailDetailScreenComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Helper to create basic UI State
    private fun createUiState(
        mail: EmailItem? = null,
        showAnalysis: Boolean = false,
        analysisResult: AttachmentAnalysisResponse? = null,
        showPreview: Boolean = false,
        previewContent: AttachmentPreviewContent? = null,
        previewError: String? = null
    ): MailDetailUiState {
        return MailDetailUiState(
            mail = mail ?: EmailItem(
                id = "1", threadId = "t1", subject = "S", fromEmail = "f@e.com",
                date = "2024.01.01", dateRaw = "2024-01-01T00:00:00Z", snippet = "s", isUnread = false,
                labelIds = listOf("INBOX"), cachedAt = System.currentTimeMillis(), body = "b"
            ),
            showAnalysisPopup = showAnalysis,
            analysisResult = analysisResult,
            analysisTarget = if (showAnalysis) Attachment("1", "f.pdf", "pdf", 100) else null,
            showPreviewDialog = showPreview,
            previewContent = previewContent,
            previewTarget = if (showPreview) Attachment("1", "f.pdf", "pdf", 100) else null,
            previewErrorMessage = previewError
        )
    }

    @Test
    fun attachmentSection_renders_different_file_types() {
        val attachments = listOf(
            Attachment("1", "sheet.xlsx", "sheet", 1000),
            Attachment("2", "image.png", "image", 2000),
            Attachment("3", "doc.pdf", "pdf", 3000)
        )
        val mail = EmailItem(
            id = "1", threadId = "t1", subject = "S", fromEmail = "f@e.com",
            date = "d", dateRaw = "d", snippet = "s", isUnread = false, labelIds = listOf("INBOX"),
            cachedAt = System.currentTimeMillis(),
            body = "b", attachments = attachments
        )

        composeTestRule.setContent {
            MailDetailScreen(uiState = MailDetailUiState(mail = mail), onBack = {})
        }

        // Check filenames are displayed
        composeTestRule.onNodeWithText("sheet.xlsx").assertIsDisplayed()
        composeTestRule.onNodeWithText("image.png").assertIsDisplayed()
        composeTestRule.onNodeWithText("doc.pdf").assertIsDisplayed()

        // Check sizes are formatted (no space between number and unit)
        // 1000 bytes = 1000B (less than 1024)
        composeTestRule.onNodeWithText("1000B").assertIsDisplayed()
        // 2000 bytes = 1.95KB (2000/1024 = 1.953125, formatted as 2.0KB)
        composeTestRule.onNodeWithText("2.0KB").assertIsDisplayed()
        // 3000 bytes = 2.9KB (3000/1024 = 2.93)
        composeTestRule.onNodeWithText("2.9KB").assertIsDisplayed()
    }

    @Test
    fun analysisPopup_displays_full_content() {
        val response = AttachmentAnalysisResponse(
            summary = "Summary Line 1\nSummary Line 2",
            insights = "Insight Line 1",
            mailGuide = "Guide Line 1"
        )
        
        composeTestRule.setContent {
            MailDetailScreen(
                uiState = createUiState(showAnalysis = true, analysisResult = response),
                onBack = {} 
            )
        }

        // Header
        composeTestRule.onNodeWithText("파일 분석 결과").assertIsDisplayed()
        composeTestRule.onNodeWithText("f.pdf").assertIsDisplayed()

        // Sections
        composeTestRule.onNodeWithText("주요 내용 요약").assertIsDisplayed()
        composeTestRule.onNodeWithText("Summary Line 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Summary Line 2").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("핵심 시사점").assertIsDisplayed()
        composeTestRule.onNodeWithText("Insight Line 1").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("답장 작성 가이드").assertIsDisplayed()
        composeTestRule.onNodeWithText("Guide Line 1").assertIsDisplayed()
        
        // Copy button
        composeTestRule.onNodeWithText("답장 가이드 복사").assertIsDisplayed()
    }

    @Test
    fun analysisPopup_handles_empty_sections() {
        val response = AttachmentAnalysisResponse(
            summary = "",
            insights = "",
            mailGuide = ""
        )
        
        composeTestRule.setContent {
            MailDetailScreen(
                uiState = createUiState(showAnalysis = true, analysisResult = response),
                onBack = {} 
            )
        }

        // Should show "표시할 내용이 없습니다."
        composeTestRule.onAllNodesWithText("표시할 내용이 없습니다.").onFirst().assertIsDisplayed()
    }

    @Test
    fun previewDialog_shows_text_content() {
        val content = AttachmentPreviewContent.Text("This is preview text")
        
        composeTestRule.setContent {
            MailDetailScreen(
                uiState = createUiState(showPreview = true, previewContent = content),
                onBack = {} 
            )
        }

        composeTestRule.onNodeWithText("This is preview text").assertIsDisplayed()
    }

    @Test
    fun previewDialog_shows_pdf_error_when_file_missing() {
        val content = AttachmentPreviewContent.Pdf("/path/to/missing/file.pdf")
        
        composeTestRule.setContent {
            MailDetailScreen(
                uiState = createUiState(showPreview = true, previewContent = content),
                onBack = {} 
            )
        }

        composeTestRule.onNodeWithText("PDF 파일을 열 수 없습니다.").assertIsDisplayed()
    }

    @Test
    fun previewDialog_shows_unsupported_message_when_content_unknown() {
        composeTestRule.setContent {
            MailDetailScreen(
                uiState = createUiState(showPreview = true, previewContent = null),
                onBack = {} 
            )
        }

        composeTestRule.onNodeWithText("미리볼 수 있는 내용이 없습니다.").assertIsDisplayed()
    }

    @Test
    fun downloadDialog_appears_on_click() {
        val attachment = Attachment("1", "file.txt", "text", 100)
        val mail = EmailItem(
            id = "1", threadId = "t", subject = "S", fromEmail = "f", date = "d", dateRaw = "d", snippet = "s", 
            isUnread = false, labelIds = emptyList(), body = "b", attachments = listOf(attachment)
        )
        
        composeTestRule.setContent {
            MailDetailScreen(uiState = MailDetailUiState(mail = mail), onBack = {}) 
        }
        
        // Click attachment to trigger download dialog
        composeTestRule.onNodeWithText("file.txt").performClick()
        
        composeTestRule.onNodeWithText("첨부파일 다운로드").assertIsDisplayed()
        composeTestRule.onNodeWithText("'file.txt' 파일을 저장할까요?").assertIsDisplayed()
        composeTestRule.onNodeWithText("파일 저장").assertIsDisplayed()
        composeTestRule.onNodeWithText("취소").assertIsDisplayed()
    }

    @Test
    fun previewDialog_renders_pdf_pages_successfully() {
        // 1. Create a dummy PDF file
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val file = java.io.File(context.cacheDir, "test_render.pdf")
        
        try {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(100, 100, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = android.graphics.Paint()
            paint.color = android.graphics.Color.BLACK
            canvas.drawText("Test PDF", 10f, 50f, paint)
            pdfDocument.finishPage(page)
            
            java.io.FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            // 2. Pass the valid file path to the composable
            val content = AttachmentPreviewContent.Pdf(file.absolutePath)

            composeTestRule.setContent {
                MailDetailScreen(
                    uiState = createUiState(showPreview = true, previewContent = content),
                    onBack = {}
                )
            }

            // 3. Verify the page is rendered
            // PdfPagePreview renders an Image with contentDescription "PDF Page 1"
            composeTestRule.waitUntil(3000) {
                try {
                    composeTestRule.onNodeWithContentDescription("PDF Page 1").assertIsDisplayed()
                    true
                } catch (e: Throwable) {
                    false
                }
            }
            
            composeTestRule.onNodeWithText("페이지 1").assertIsDisplayed()

        } finally {
            if (file.exists()) file.delete()
        }
    }
}
