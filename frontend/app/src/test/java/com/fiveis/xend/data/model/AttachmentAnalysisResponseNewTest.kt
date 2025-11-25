package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AttachmentAnalysisResponseNewTest {

    @Test
    fun test_construction_setsAllFields() {
        val response = AttachmentAnalysisResponse(
            summary = "This is a summary",
            insights = "Key insights here",
            mailGuide = "Follow these steps"
        )

        assertEquals("This is a summary", response.summary)
        assertEquals("Key insights here", response.insights)
        assertEquals("Follow these steps", response.mailGuide)
    }

    @Test
    fun test_copy_changesFields() {
        val original = AttachmentAnalysisResponse(
            summary = "Summary 1",
            insights = "Insights 1",
            mailGuide = "Guide 1"
        )

        val modified = original.copy(summary = "Summary 2")

        assertEquals("Summary 2", modified.summary)
        assertEquals("Insights 1", modified.insights)
        assertEquals("Guide 1", modified.mailGuide)
    }

    @Test
    fun test_equals_sameValues_returnsTrue() {
        val response1 = AttachmentAnalysisResponse("Sum", "Ins", "Guide")
        val response2 = AttachmentAnalysisResponse("Sum", "Ins", "Guide")

        assertEquals(response1, response2)
    }
}
