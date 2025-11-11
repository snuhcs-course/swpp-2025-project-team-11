package com.fiveis.xend.ui.compose.common

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import jp.wasabeef.richeditor.RichEditor
import kotlin.math.abs

/**
 * Extended RichEditor with cursor tracking and text insertion capabilities
 */
class XendRichEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RichEditor(context, attrs, defStyleAttr) {

    private var textChangeListener: ((String) -> Unit)? = null
    private var cursorPosition: Int = 0
    private var onSwipeListener: (() -> Unit)? = null

    private var touchStartX: Float = 0f
    private var touchStartY: Float = 0f
    private var isSwiping: Boolean = false

    init {
        // Enable JavaScript
        settings.javaScriptEnabled = true

        // Add JavaScript interface for cursor tracking
        addJavascriptInterface(CursorTracker(), "CursorTracker")

        // Add JavaScript interface for swipe button
        addJavascriptInterface(AndroidInterface(), "AndroidInterface")

        // Set up text change listener
        setOnTextChangeListener { text ->
            textChangeListener?.invoke(text)
            updateCursorPosition()
        }
    }

    /**
     * Set swipe listener
     */
    fun setOnSwipeListener(listener: () -> Unit) {
        onSwipeListener = listener
    }

    /**
     * JavaScript interface for Android communication
     */
    inner class AndroidInterface {
        @JavascriptInterface
        fun onSwipe() {
            onSwipeListener?.invoke()
        }
    }

    /**
     * Set text change listener
     */
    fun setTextChangedListener(listener: (String) -> Unit) {
        textChangeListener = listener
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
     * Show suggestion text after cursor position (gray, italic style) with swipe button
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
                var existing = document.getElementById('ai-suggestion-container');
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

                // Create container span for suggestion + button
                var container = document.createElement('span');
                container.id = 'ai-suggestion-container';
                container.style.display = 'inline';

                // Create suggestion text span
                var textSpan = document.createElement('span');
                textSpan.id = 'ai-suggestion';
                textSpan.contentEditable = 'false';
                textSpan.style.color = '#9CA3AF';
                textSpan.style.fontStyle = 'italic';
                textSpan.style.fontSize = '14px';
                textSpan.style.userSelect = 'none';
                textSpan.textContent = ' ' + '$escapedText';

                // Create swipe button (simple and small)
                var button = document.createElement('button');
                button.type = 'button';
                button.id = 'ai-swipe-button';
                button.contentEditable = 'false';
                button.style.marginLeft = '4px';
                button.style.padding = '2px 6px';
                button.style.backgroundColor = 'white';
                button.style.color = '#6366F1';
                button.style.border = '1px solid #C7D2FE';
                button.style.borderRadius = '6px';
                button.style.fontSize = '11px';
                button.style.fontWeight = '600';
                button.style.cursor = 'pointer';
                button.style.outline = 'none';
                button.textContent = '→ swipe';

                // Add click event
                button.addEventListener('click', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    AndroidInterface.onSwipe();
                }, true);

                // Assemble
                container.appendChild(textSpan);
                container.appendChild(button);

                // Insert at current cursor position
                range.insertNode(container);

                // Restore cursor position (before the suggestion)
                range.setStartBefore(container);
                range.collapse(true);
                sel.removeAllRanges();
                sel.addRange(range);
            })();
        """.trimIndent()

        evaluateJavascript(js, null)
    }

    /**
     * Remove suggestion text and button
     */
    fun removeSuggestion() {
        val js = """
            (function() {
                var existing = document.getElementById('ai-suggestion-container');
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
                var container = document.getElementById('ai-suggestion-container');
                if (!container) return;

                var suggestion = document.getElementById('ai-suggestion');
                if (!suggestion) return;

                var text = suggestion.textContent;
                var parent = container.parentNode;

                // Create a text node with the suggestion text
                var textNode = document.createTextNode(text);

                // Replace the container with the text node
                parent.replaceChild(textNode, container);

                // Move cursor to after the inserted text
                var sel = window.getSelection();
                var range = document.createRange();
                range.setStartAfter(textNode);
                range.collapse(true);
                sel.removeAllRanges();
                sel.addRange(range);

                // Force focus on editor
                document.body.focus();

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
     * Check if suggestion exists
     */
    private fun hasSuggestion(callback: (Boolean) -> Unit) {
        evaluateJavascript(
            "(function() { return document.getElementById('ai-suggestion-container') !== null; })();"
        ) { result ->
            callback(result == "true")
        }
    }

    /**
     * Handle touch events for swipe gesture detection
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                isSwiping = false
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - touchStartX
                val deltaY = event.y - touchStartY

                // Detect horizontal swipe (right direction)
                if (abs(deltaX) > abs(deltaY) && deltaX > 100 && !isSwiping) {
                    isSwiping = true

                    // Check if suggestion exists before triggering swipe
                    hasSuggestion { exists ->
                        if (exists) {
                            post {
                                onSwipeListener?.invoke()
                            }
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isSwiping = false
            }
        }

        return super.onTouchEvent(event)
    }
}
