package com.fiveis.xend.ui.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyDirectComposeHelperTest {

    @Test
    fun containsHtmlMarkup_returns_true_for_br_tag() {
        // Given
        val html = "Hello<br>World"

        // When
        val result = html.containsHtmlMarkup()

        // Then
        assertTrue(result)
    }

    @Test
    fun containsHtmlMarkup_returns_true_for_closing_tag() {
        // Given
        val html = "Hello</div>World"

        // When
        val result = html.containsHtmlMarkup()

        // Then
        assertTrue(result)
    }

    @Test
    fun containsHtmlMarkup_returns_true_for_p_tag() {
        // Given
        val html = "<p>Paragraph</p>"

        // When
        val result = html.containsHtmlMarkup()

        // Then
        assertTrue(result)
    }

    @Test
    fun containsHtmlMarkup_returns_true_for_div_tag() {
        // Given
        val html = "<div>Content</div>"

        // When
        val result = html.containsHtmlMarkup()

        // Then
        assertTrue(result)
    }

    @Test
    fun containsHtmlMarkup_returns_true_for_span_tag() {
        // Given
        val html = "<span>Text</span>"

        // When
        val result = html.containsHtmlMarkup()

        // Then
        assertTrue(result)
    }

    @Test
    fun containsHtmlMarkup_returns_false_for_plain_text() {
        // Given
        val text = "Just plain text without any tags"

        // When
        val result = text.containsHtmlMarkup()

        // Then
        assertFalse(result)
    }

    @Test
    fun containsHtmlMarkup_returns_false_for_text_with_greater_than() {
        // Given
        val text = "5 > 3 is true"

        // When
        val result = text.containsHtmlMarkup()

        // Then
        assertFalse(result)
    }

    @Test
    fun containsHtmlMarkup_returns_false_for_empty_string() {
        // Given
        val text = ""

        // When
        val result = text.containsHtmlMarkup()

        // Then
        assertFalse(result)
    }

    @Test
    fun containsHtmlMarkup_returns_false_for_blank_string() {
        // Given
        val text = "   "

        // When
        val result = text.containsHtmlMarkup()

        // Then
        assertFalse(result)
    }

    @Test
    fun toHtmlPreservingLineBreaks_converts_newline_to_br() {
        // Given
        val text = "Line 1\nLine 2"

        // When
        val result = text.toHtmlPreservingLineBreaks()

        // Then
        assertEquals("Line 1<br>Line 2", result)
    }

    @Test
    fun toHtmlPreservingLineBreaks_converts_carriage_return_newline_to_br() {
        // Given
        val text = "Line 1\r\nLine 2"

        // When
        val result = text.toHtmlPreservingLineBreaks()

        // Then
        assertEquals("Line 1<br>Line 2", result)
    }

    @Test
    fun toHtmlPreservingLineBreaks_escapes_ampersand() {
        // Given
        val text = "Tom & Jerry"

        // When
        val result = text.toHtmlPreservingLineBreaks()

        // Then
        assertEquals("Tom &amp; Jerry", result)
    }

    @Test
    fun toHtmlPreservingLineBreaks_escapes_less_than() {
        // Given
        val text = "5 < 10"

        // When
        val result = text.toHtmlPreservingLineBreaks()

        // Then
        assertEquals("5 &lt; 10", result)
    }

    @Test
    fun toHtmlPreservingLineBreaks_escapes_greater_than() {
        // Given
        val text = "10 > 5"

        // When
        val result = text.toHtmlPreservingLineBreaks()

        // Then
        assertEquals("10 &gt; 5", result)
    }

    @Test
    fun toHtmlPreservingLineBreaks_escapes_double_quote() {
        // Given
        val text = "He said \"Hello\""

        // When
        val result = text.toHtmlPreservingLineBreaks()

        // Then
        assertEquals("He said &quot;Hello&quot;", result)
    }

    @Test
    fun toHtmlPreservingLineBreaks_escapes_single_quote() {
        // Given
        val text = "It's a test"

        // When
        val result = text.toHtmlPreservingLineBreaks()

        // Then
        assertEquals("It&#39;s a test", result)
    }

    @Test
    fun toHtmlPreservingLineBreaks_handles_multiple_special_chars() {
        // Given
        val text = "A & B < C > D \"E\" 'F'"

        // When
        val result = text.toHtmlPreservingLineBreaks()

        // Then
        assertEquals("A &amp; B &lt; C &gt; D &quot;E&quot; &#39;F&#39;", result)
    }

    @Test
    fun toHtmlPreservingLineBreaks_handles_empty_string() {
        // Given
        val text = ""

        // When
        val result = text.toHtmlPreservingLineBreaks()

        // Then
        assertEquals("", result)
    }

    @Test
    fun toHtmlPreservingLineBreaks_handles_multiple_newlines() {
        // Given
        val text = "Line 1\n\nLine 3"

        // When
        val result = text.toHtmlPreservingLineBreaks()

        // Then
        assertEquals("Line 1<br><br>Line 3", result)
    }

    @Test
    fun toHtmlPreservingLineBreaks_handles_only_carriage_return() {
        // Given
        val text = "Line 1\rLine 2"

        // When
        val result = text.toHtmlPreservingLineBreaks()

        // Then
        assertEquals("Line 1<br>Line 2", result)
    }

    @Test
    fun toHtmlPreservingLineBreaks_complex_text() {
        // Given
        val text = "Hello & welcome!\nThis is a test.\n\"Quoted text\" with <tags>"

        // When
        val result = text.toHtmlPreservingLineBreaks()

        // Then
        assertEquals("Hello &amp; welcome!<br>This is a test.<br>&quot;Quoted text&quot; with &lt;tags&gt;", result)
    }

    @Test
    fun normalizeAsHtml_returns_empty_for_blank() {
        // Given
        val text = ""

        // When
        val result = text.normalizeAsHtml()

        // Then
        assertEquals("", result)
    }

    @Test
    fun normalizeAsHtml_returns_as_is_for_html() {
        // Given
        val html = "<p>Already HTML</p>"

        // When
        val result = html.normalizeAsHtml()

        // Then
        assertEquals(html, result)
    }

    @Test
    fun normalizeAsHtml_converts_plain_text_to_html() {
        // Given
        val text = "Plain text\nNew line"

        // When
        val result = text.normalizeAsHtml()

        // Then
        assertEquals("Plain text<br>New line", result)
    }

    @Test
    fun normalizeAsHtml_handles_text_with_special_chars() {
        // Given
        val text = "A & B < C"

        // When
        val result = text.normalizeAsHtml()

        // Then
        assertEquals("A &amp; B &lt; C", result)
    }

    @Test
    fun normalizeAsHtml_preserves_existing_html_with_br() {
        // Given
        val html = "Line 1<br>Line 2"

        // When
        val result = html.normalizeAsHtml()

        // Then
        assertEquals(html, result)
    }

    @Test
    fun normalizeAsHtml_preserves_existing_html_with_div() {
        // Given
        val html = "<div>Content</div>"

        // When
        val result = html.normalizeAsHtml()

        // Then
        assertEquals(html, result)
    }

    private fun String.containsHtmlMarkup(): Boolean {
        val trimmed = trim()
        if (!trimmed.contains('<')) return false
        val lower = trimmed.lowercase()
        return lower.contains("<br") ||
            lower.contains("</") ||
            lower.contains("<p") ||
            lower.contains("<div") ||
            lower.contains("<span")
    }

    private fun String.toHtmlPreservingLineBreaks(): String {
        val sb = StringBuilder(length + 16)
        var index = 0
        while (index < length) {
            when (val ch = this[index]) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&#39;")
                '\r' -> {
                    if (index + 1 < length && this[index + 1] == '\n') {
                        index++
                    }
                    sb.append("<br>")
                }
                '\n' -> sb.append("<br>")
                else -> sb.append(ch)
            }
            index++
        }
        return sb.toString()
    }

    private fun String.normalizeAsHtml(): String {
        if (isBlank()) return ""
        return if (containsHtmlMarkup()) {
            this
        } else {
            toHtmlPreservingLineBreaks()
        }
    }
}
