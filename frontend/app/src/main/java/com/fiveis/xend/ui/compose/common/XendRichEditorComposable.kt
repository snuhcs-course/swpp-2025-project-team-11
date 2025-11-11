package com.fiveis.xend.ui.compose.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * State holder for XendRichEditor
 */
class XendRichEditorState {
    internal var editor: XendRichEditor? = null

    /**
     * Get HTML content from editor
     */
    fun getHtml(): String {
        return editor?.getHtmlContent() ?: ""
    }

    /**
     * Set HTML content to editor
     */
    fun setHtml(html: String) {
        editor?.setHtmlContent(html)
    }

    /**
     * Insert text at current cursor position
     */
    fun insertTextAtCursor(text: String) {
        editor?.insertTextAtCursor(text)
    }

    /**
     * Get current cursor position
     */
    fun getCursorPosition(): Int {
        return editor?.getCursorPosition() ?: 0
    }

    /**
     * Get plain text content (from HTML)
     */
    fun getText(): String {
        val html = getHtml()
        // Simple HTML tag removal for text content
        return html
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace("<br />", "\n")
            .replace(Regex("<[^>]*>"), "")
            .trim()
    }

    /**
     * Focus the editor
     */
    fun focus() {
        editor?.focus()
    }

    /**
     * Clear focus
     */
    fun clearFocus() {
        editor?.clearEditorFocus()
    }

    /**
     * Toggle bold formatting
     */
    fun toggleBold() {
        editor?.toggleBold()
    }

    /**
     * Toggle italic formatting
     */
    fun toggleItalic() {
        editor?.toggleItalic()
    }

    /**
     * Toggle underline formatting
     */
    fun toggleUnderline() {
        editor?.toggleUnderline()
    }

    /**
     * Toggle strikethrough formatting
     */
    fun toggleStrikethrough() {
        editor?.toggleStrikethrough()
    }

    /**
     * Change font size
     */
    fun changeFontSize(size: Int) {
        editor?.changeFontSize(size)
    }

    /**
     * Change text color
     */
    fun changeTextColor(color: Int) {
        editor?.changeTextColor(color)
    }

    /**
     * Show AI suggestion text (gray, italic) at the end
     */
    fun showSuggestion(suggestionText: String) {
        editor?.showSuggestion(suggestionText)
    }

    /**
     * Remove AI suggestion text
     */
    fun removeSuggestion() {
        editor?.removeSuggestion()
    }

    /**
     * Accept AI suggestion and convert to actual text
     */
    fun acceptSuggestion() {
        editor?.acceptSuggestion()
    }
}

/**
 * Remember XendRichEditorState
 */
@Composable
fun rememberXendRichEditorState(): XendRichEditorState {
    return remember { XendRichEditorState() }
}

/**
 * Composable XendRichEditor using AndroidView
 */
@Composable
fun XendRichEditorView(
    state: XendRichEditorState,
    modifier: Modifier = Modifier,
    placeholder: String = "내용을 입력하세요",
    onTextChanged: ((String) -> Unit)? = null,
    enabled: Boolean = true,
    onSwipe: (() -> Unit)? = null
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            XendRichEditor(context).apply {
                // Set placeholder
                setPlaceholder(placeholder)

                // Set editor height
                setEditorHeight(240)

                // Set font size (한글 최적화)
                setEditorFontSize(15)

                // Set padding
                setPadding(16, 16, 16, 16)

                // Enable focus
                isFocusable = true
                isFocusableInTouchMode = true

                // Set initial HTML (empty)
                setHtml("")

                // Request focus on click
                setOnClickListener {
                    focusEditor()
                }

                // Set text change listener
                setTextChangedListener { html ->
                    // Remove suggestion when user types
                    removeSuggestion()
                    onTextChanged?.invoke(html)
                }

                // Set swipe listener
                onSwipe?.let { callback ->
                    setOnSwipeListener {
                        callback()
                    }
                }

                // Store reference in state
                state.editor = this

                // Set enabled state
                isEnabled = enabled
            }
        },
        update = { editor ->
            // Update enabled state
            editor.isEnabled = enabled

            // Ensure state has reference
            if (state.editor != editor) {
                state.editor = editor
            }
        }
    )

    DisposableEffect(state) {
        onDispose {
            state.editor = null
        }
    }
}
