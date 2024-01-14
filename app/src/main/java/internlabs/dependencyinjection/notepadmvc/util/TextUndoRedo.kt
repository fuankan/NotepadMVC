package internlabs.dependencyinjection.notepadmvc.util

import android.text.Editable
import android.text.Selection
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import android.widget.TextView
import java.util.*

class TextUndoRedo(private val myTextView: TextView) {
    private var isUndoOrRedo = false
    private val editHistory: EditHistory
    private val changeListener: EditTextChangeListener

    val canUndo: Boolean
        get() = (editHistory.tPosition > 0)

    val canRedo: Boolean
        get() = (editHistory.tPosition < editHistory.tHistory.size)

    init {
        editHistory = EditHistory()
        changeListener = EditTextChangeListener()
        myTextView.addTextChangedListener(changeListener)
    }

    fun disconnect() {
        myTextView.removeTextChangedListener(changeListener)
    }

    fun setMaxHistorySize(maxHistorySize: Int) {
        editHistory.setMaxHistorySize(maxHistorySize)
    }

    fun clearHistory() {
        editHistory.clear()
    }

    fun undo() {
        val edit: EditItem = editHistory.getPrevious() ?: return
        val text = myTextView.editableText
        val start = edit.tStart
        val end = start + if (edit.tAfter != null) edit.tAfter!!.length else 0
        isUndoOrRedo = true
        text.replace(start, end, edit.tBefore)
        isUndoOrRedo = false
        for (i in text.getSpans(0, text.length, UnderlineSpan::class.java)) {
            text.removeSpan(i)
        }
        Selection.setSelection(
            text, if (edit.tBefore == null) start else start + edit.tBefore!!.length
        )
    }

    fun redo() {
        val edit: EditItem =
            editHistory.getNext()
                ?: return
        val text = myTextView.editableText
        val start = edit.tStart
        val end = start + if (edit.tBefore != null) edit.tBefore!!.length else 0
        isUndoOrRedo = true
        text.replace(start, end, edit.tAfter)
        isUndoOrRedo = false

        for (i in text.getSpans(0, text.length, UnderlineSpan::class.java)) {
            text.removeSpan(i)
        }
        Selection.setSelection(
            text, if (edit.tAfter == null) start else start + edit.tAfter!!.length
        )
    }

    private inner class EditHistory {
        var tPosition = 0
        var tMaxHistorySize = -1
        val tHistory = LinkedList<EditItem>()

        fun clear() {
            tPosition = 0
            tHistory.clear()
        }

        fun add(item: EditItem) {
            while (tHistory.size > tPosition) {
                tHistory.removeLast()
            }
            tHistory.add(item)
            tPosition++
            if (tMaxHistorySize >= 0) {
                trimHistory()
            }
        }

        fun setMaxHistorySize(maxHistorySize: Int) {
            tMaxHistorySize = maxHistorySize
            if (tMaxHistorySize >= 0) {
                trimHistory()
            }
        }

        fun trimHistory() {
            while (tHistory.size > tMaxHistorySize) {
                tHistory.removeFirst()
                tPosition--
            }
            if (tPosition < 0) {
                tPosition = 0
            }
        }

        fun getCurrent(): EditItem? {
            return if (tPosition == 0) {
                null
            } else tHistory[tPosition - 1]
        }

        fun getPrevious(): EditItem? {
            if (tPosition == 0) {
                return null
            }
            tPosition--
            return tHistory[tPosition]
        }

        fun getNext(): EditItem? {
            if (tPosition >= tHistory.size) {
                return null
            }
            val item = tHistory[tPosition]
            tPosition++
            return item
        }
    }

    private inner class EditItem(
        var tStart: Int,
        var tBefore: CharSequence?,
        var tAfter: CharSequence?
    )

    internal enum class ActionType {
        INSERT, DELETE, PASTE, NOT_DEF
    }

    private inner class EditTextChangeListener : TextWatcher {

        private var beforeChange: CharSequence? = null
        private var afterChange: CharSequence? = null
        private var lastActionType = ActionType.NOT_DEF
        private var lastActionTime: Long = 0

        private val actionType: ActionType
            get() {
                return if (!TextUtils.isEmpty(beforeChange) && TextUtils.isEmpty(afterChange)) {
                    ActionType.DELETE
                } else if (TextUtils.isEmpty(beforeChange) && !TextUtils.isEmpty(afterChange)) {
                    ActionType.INSERT
                } else {
                    ActionType.PASTE
                }
            }


        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            if (isUndoOrRedo) {
                return
            }
            beforeChange = s.subSequence(start, start + count)
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (isUndoOrRedo) {
                return
            }
            afterChange = s.subSequence(start, start + count)
            makeBatch(start)
        }

        override fun afterTextChanged(s: Editable) {}

        private fun makeBatch(start: Int) {
            val at = actionType
            val editItem: EditItem? = editHistory.getCurrent()
            if (lastActionType != at || ActionType.PASTE == at
                || System.currentTimeMillis() - lastActionTime > 1000
                || editItem == null
            ) {
                editHistory.add(EditItem(start, beforeChange, afterChange))
            } else {
                if (at == ActionType.DELETE) {
                    editItem.tStart = start
                    editItem.tBefore = TextUtils.concat(beforeChange, editItem.tBefore)
                } else {
                    editItem.tAfter = TextUtils.concat(editItem.tAfter, afterChange)
                }
            }
            lastActionType = at
            lastActionTime = System.currentTimeMillis()
        }
    }
}