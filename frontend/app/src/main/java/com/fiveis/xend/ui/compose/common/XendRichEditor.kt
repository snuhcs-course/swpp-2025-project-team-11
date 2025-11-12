package com.fiveis.xend.ui.compose.common

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import jp.wasabeef.richeditor.RichEditor

/**
 * Extended RichEditor with cursor tracking and text insertion capabilities
 */
class XendRichEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RichEditor(context, attrs, defStyleAttr) {

    private val textChangeListeners = mutableListOf<(String) -> Unit>()
    private var cursorPosition: Int = 0
    private var baseStylesInjected: Boolean = false

    init {
        // Enable JavaScript
        settings.javaScriptEnabled = true

        // Add JavaScript interface for cursor tracking
        addJavascriptInterface(CursorTracker(), "CursorTracker")

        // Perform initial setup only after the internal HTML document is loaded.
        // This solves timing issues where settings are applied too early.
        setOnInitialLoadListener {
            setEditorFontSize(15)
            setPadding(16, 16, 16, 16)
            injectBaseStyles()
        }
    }

    private fun injectBaseStyles() {
        if (baseStylesInjected) return
        val js = """
            (function() {
                var style = document.getElementById('xend-editor-base-style');
                if (style) return;
                style = document.createElement('style');
                style.id = 'xend-editor-base-style';
                style.innerHTML = "i, em, #ai-suggestion { font-style: normal !important; transform: skewX(-15deg); }";
                document.head.appendChild(style);
            })();
        """.trimIndent()
        evaluateJavascript(js, null)
        baseStylesInjected = true
    }

    /**
     * Set text change listener
     */
    fun setTextChangedListener(listener: (String) -> Unit) {
        textChangeListeners.add(listener)
        // Set up the parent's text change listener to call our hooks
        setOnTextChangeListener { text ->
            textChangeListeners.forEach { it(text) }
            updateCursorPosition()
        }
    }

    /**
     * Update cursor position by executing JavaScript
     */
    private fun updateCursorPosition() {
        val js = """
            (function() {
                var sel = window.getSelection();
                if (sel.rangeCount > 0) {
                    var range = sel.getRangeAt(0);
                    var preCaretRange = range.cloneRange();
                    preCaretRange.selectNodeContents(document.body);
                    preCaretRange.setEnd(range.endContainer, range.endOffset);
                    var position = preCaretRange.toString().length;
                    CursorTracker.updatePosition(position);
                }
            })();
        """.trimIndent()
        evaluateJavascript(js, null)
    }

    /**
     * Get current cursor position
     */
    fun getCursorPosition(): Int = cursorPosition

    /**
     * Insert text at current cursor position
     */
    fun insertTextAtCursor(text: String) {
        val escapedText = text
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")

        val js = """
            (function() {
                var sel = window.getSelection();
                if (sel.rangeCount > 0) {
                    var range = sel.getRangeAt(0);
                    range.deleteContents();
                    var textNode = document.createTextNode('$escapedText');
                    range.insertNode(textNode);
                    range.setStartAfter(textNode);
                    range.setEndAfter(textNode);
                    sel.removeAllRanges();
                    sel.addRange(range);
                }
            })();
        """.trimIndent()
        evaluateJavascript(js, null)
    }

    /**
     * JavaScript interface for cursor tracking
     */
    inner class CursorTracker {
        @JavascriptInterface
        fun updatePosition(position: Int) {
            cursorPosition = position
        }
    }

    /**
     * Get HTML content
     */
    fun getHtmlContent(): String {
        return html ?: ""
    }

    /**
     * Set HTML content
     */
    fun setHtmlContent(html: String) {
        setHtml(html)
    }

    /**
     * Focus the editor
     */
    fun focus() {
        focusEditor()
    }

    /**
     * Clear focus from editor
     */
    fun clearEditorFocus() {
        clearFocusEditor()
    }

    /**
     * Toggle bold formatting
     */
    fun toggleBold() {
        setBold()
    }

    /**
     * Toggle italic formatting
     */
    fun toggleItalic() {
        setItalic()
    }

    /**
     * Toggle underline formatting
     */
    fun toggleUnderline() {
        setUnderline()
    }

    /**
     * Toggle strikethrough formatting
     */
    fun toggleStrikethrough() {
        setStrikeThrough()
    }

    /**
     * Change font size
     */
    fun changeFontSize(size: Int) {
        setEditorFontSize(size)
    }

    /**
     * Change text color
     */
    fun changeTextColor(color: Int) {
        super.setTextColor(color)
    }

    /**
     * Show suggestion text after cursor position (gray, italic style)
     */
    fun showSuggestion(suggestionText: String) {
        val escapedText = suggestionText
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", " ")
            .replace("\r", "")

        val js = """
            (function() {
                // Remove existing suggestion if any
                var existing = document.getElementById('ai-suggestion');
                if (existing) {
                    existing.remove();
                }
                var sel = window.getSelection();
                if (!sel.rangeCount) return;
                var range = sel.getRangeAt(0).cloneRange();
                // Check if there's any text AFTER the cursor
                var afterRange = range.cloneRange();
                afterRange.selectNodeContents(document.body);
                afterRange.setStart(range.endContainer, range.endOffset);
                var textAfterCursor = afterRange.toString().trim();
                // Only show suggestion if cursor is at the END (no text after)
                if (textAfterCursor.length > 0) {
                    return;  // Don't show suggestion if there's text after cursor
                }
                range.collapse(false);  // Collapse to end of selection
                // Create suggestion text span (no button)
                var textSpan = document.createElement('span');
                textSpan.id = 'ai-suggestion';
                textSpan.contentEditable = 'false';
                textSpan.style.color = '#9CA3AF';
                textSpan.style.fontSize = '14px';
                textSpan.style.userSelect = 'none';
                textSpan.textContent = ' ' + '$escapedText';
                // Insert at current cursor position
                range.insertNode(textSpan);
                // Restore cursor position (before the suggestion)
                range.setStartBefore(textSpan);
                range.collapse(true);
                sel.removeAllRanges();
                sel.addRange(range);
            })();
        """.trimIndent()
        evaluateJavascript(js, null)
    }

    /**
     * Remove suggestion text
     */
    fun removeSuggestion() {
        val js = """
            (function() {
                var existing = document.getElementById('ai-suggestion');
                if (existing) {
                    existing.remove();
                }
            })();
        """.trimIndent()
        evaluateJavascript(js, null)
    }

    /**
     * Accept suggestion and convert it to actual text
     */
    fun acceptSuggestion() {
        val js = """
            (function() {
                var suggestion = document.getElementById('ai-suggestion');
                if (!suggestion) return;
                var text = suggestion.textContent;
                var parent = suggestion.parentNode;
                // Create a text node with the suggestion text
                var textNode = document.createTextNode(text);
                // Replace the suggestion with the text node
                parent.replaceChild(textNode, suggestion);
                // Move cursor to after the inserted text
                var sel = window.getSelection();
                var range = document.createRange();
                range.setStartAfter(textNode);
                range.collapse(true);
                sel.removeAllRanges();
                sel.addRange(range);
                // Force focus on the actual editor div to prevent keyboard dismissal
                var editor = document.getElementById('editor');
                if (editor) {
                    editor.focus();
                }
                // Create temporary element at cursor to scroll into view
                var marker = document.createElement('span');
                marker.innerHTML = '&nbsp;';
                range.insertNode(marker);
                marker.scrollIntoView({ behavior: 'smooth', block: 'end', inline: 'nearest' });
                // Remove marker and restore cursor
                range.selectNode(marker);
                range.deleteContents();
                sel.removeAllRanges();
                sel.addRange(range);
            })();
        """.trimIndent()
        evaluateJavascript(js, null)
    }

    /**
     * Force focus on the editor and explicitly show the keyboard.
     * This is a robust way to prevent the keyboard from being dismissed
     * after programmatic UI changes.
     */
    fun requestFocusAndShowKeyboard() {
        // 1. Request focus on the view itself
        this.requestFocus()
        // 2. Explicitly ask the InputMethodManager to show the keyboard
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, 0)
    }
}
