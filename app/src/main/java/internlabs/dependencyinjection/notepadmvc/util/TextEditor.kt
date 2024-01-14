package internlabs.dependencyinjection.notepadmvc.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.view.MenuItem
import android.widget.EditText

class TextEditor {

    companion object {

        fun copy(editText: EditText, clipboardManager: ClipboardManager): Boolean {
            val startSelection: Int = editText.selectionStart
            val endSelection: Int = editText.selectionEnd
            val cTextCopied: String = editText.text.toString()
                .substring(startSelection, endSelection)
            return if (cTextCopied.isEmpty()) false
            else {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("", cTextCopied))
                editText.setSelection(editText.selectionEnd)
                true
            }
        }

        fun cut(editText: EditText, clipboardManager: ClipboardManager): Boolean {
            val startSelection: Int = editText.selectionStart
            val endSelection: Int = editText.selectionEnd
            val selectedText: String = editText.text.toString()
                .substring(startSelection, endSelection)
            return if (selectedText.isEmpty()) false
            else {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("", selectedText))
                editText.text =
                    editText.text.replace(startSelection, endSelection, "")
                editText.setSelection(startSelection)
                true
            }
        }

        fun paste(clipboardManager: ClipboardManager, pasteItem: MenuItem): String {
            var pasteData: String = ""
            pasteItem.isEnabled = when {
                !clipboardManager.hasPrimaryClip() -> {
                    false
                }
                !(clipboardManager.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))!! -> {
                    false
                }
                else -> {
                    val item = clipboardManager.primaryClip?.getItemAt(0)

                    pasteData = item?.text.toString()
                    true
                }
            }
            return pasteData
        }

        fun delete(editText: EditText): Boolean {
            val startSelection: Int = editText.selectionStart
            val endSelection: Int = editText.selectionEnd
            val selectedText: String = editText.text.toString()
                .substring(startSelection, endSelection)
            return if (selectedText.isEmpty()) false
            else {
                editText.text =
                    editText.text.replace(startSelection, endSelection, "")
                editText.setSelection(startSelection)
                true
            }
        }
    }
}
