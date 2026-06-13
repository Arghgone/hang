package com.argh.hang

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.view.ActionMode
import android.view.DragEvent
import android.view.Menu
import android.view.MenuItem
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.inputmethod.InputContentInfo
import androidx.appcompat.widget.AppCompatEditText

/**
 * An EditText hardened against bypassing the manual-typing requirement:
 *  - paste / cut / copy / select-all / share context actions are blocked
 *  - autofill is disabled
 *  - keyboard suggestions and predictive text are disabled
 *  - personalized IME learning is disabled (the passage is never learned)
 *  - drag-and-drop text insertion is swallowed
 *  - rich content commits from IMEs (stickers, clipboard chips) are rejected
 *  - bulk insertions (clipboard managers, voice dictation sentence commits,
 *    programmatic setText from IMEs) are rejected both at the
 *    InputConnection level and by an InputFilter that only accepts
 *    keystroke-sized increments
 *  - accessibility SET_TEXT / PASTE actions are swallowed so automation
 *    services cannot inject the passage
 */
class SecureEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatEditText(context, attrs) {

    companion object {
        /** Maximum characters accepted in a single input event (keystroke-sized). */
        private const val MAX_CHARS_PER_EVENT = 2
    }

    private val blockAllActionMode = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = false
        override fun onDestroyActionMode(mode: ActionMode?) = Unit
    }

    init {
        importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE
        imeOptions = imeOptions or
            EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or
            EditorInfo.IME_FLAG_NO_EXTRACT_UI
        isLongClickable = false
        setTextIsSelectable(false)
        customSelectionActionModeCallback = blockAllActionMode
        customInsertionActionModeCallback = blockAllActionMode
        filters = arrayOf(InputFilter { source, start, end, _, _, _ ->
            // Reject anything larger than a keystroke-sized insertion.
            if (end - start > MAX_CHARS_PER_EVENT) "" else null
        })
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val base = super.onCreateInputConnection(outAttrs) ?: return null
        outAttrs.imeOptions = outAttrs.imeOptions or
            EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        return object : InputConnectionWrapper(base, true) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                if (text != null && text.length > MAX_CHARS_PER_EVENT) return false
                return super.commitText(text, newCursorPosition)
            }

            override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
                if (text != null && text.length > MAX_CHARS_PER_EVENT) return false
                return super.setComposingText(text, newCursorPosition)
            }

            override fun commitContent(
                inputContentInfo: InputContentInfo,
                flags: Int,
                opts: Bundle?,
            ): Boolean = false // no rich content, ever
        }
    }

    override fun onTextContextMenuItem(id: Int): Boolean = when (id) {
        android.R.id.paste,
        android.R.id.pasteAsPlainText,
        android.R.id.cut,
        android.R.id.copy,
        android.R.id.selectAll,
        android.R.id.shareText,
        android.R.id.autofill,
        -> true // consume without acting (also covers Ctrl+V hardware shortcuts)
        else -> super.onTextContextMenuItem(id)
    }

    /** Swallow accessibility-driven text injection. */
    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean =
        when (action) {
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            AccessibilityNodeInfo.ACTION_PASTE,
            -> true // consume without acting
            else -> super.performAccessibilityAction(action, arguments)
        }

    /** Swallow all drag events so text cannot be dropped into the field. */
    override fun onDragEvent(event: DragEvent): Boolean = true

    override fun isSuggestionsEnabled(): Boolean = false
}
