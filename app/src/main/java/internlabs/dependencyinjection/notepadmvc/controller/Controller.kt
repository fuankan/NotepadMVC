package internlabs.dependencyinjection.notepadmvc.controller

import android.app.Dialog
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.navigation.NavigationView
import internlabs.dependencyinjection.notepadmvc.R
import internlabs.dependencyinjection.notepadmvc.util.BMooreMatchText
import internlabs.dependencyinjection.notepadmvc.util.PrintDocument
import internlabs.dependencyinjection.notepadmvc.util.TextEditor
import internlabs.dependencyinjection.notepadmvc.util.TextUndoRedo
import internlabs.dependencyinjection.notepadmvc.viewer.Viewer
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class  Controller(viewer: Viewer) : OurTasks, View.OnClickListener,
    NavigationView.OnNavigationItemSelectedListener, Toolbar.OnMenuItemClickListener {
    private var viewer: Viewer
    private var uri: Uri = Uri.parse("")
    private var pasteItemInNavMenu: MenuItem? = null
    private var pasteItemInBotMenu: MenuItem? = null

    private lateinit var manager: TextUndoRedo

    init {
        this.viewer = viewer
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.openFile -> {
                open()
                clearUndoRedoHistory()
            }
            R.id.newFile -> {
                new()
                clearUndoRedoHistory()
            }
            R.id.save -> {
                save()
                clearUndoRedoHistory()
            }
            R.id.saveAs -> {
                saveAs()
                clearUndoRedoHistory()
            }
            R.id.about_app -> {
                aboutApp()
            }
            R.id.exit -> {
                exit()
            }
            R.id.copy -> {
                copy()
            }
            R.id.paste -> {
                paste(item)
                pasteItemInNavMenu = item
            }
            R.id.cut -> {
                cut()
            }
            R.id.delete -> {
                delete()
            }
            R.id.select_all -> {
                selectAll()
            }
            R.id.undo -> {
                undo()
            }
            R.id.printDocument -> {
                print()
            }
            R.id.redo -> {
                redo()
            }
            R.id.searchText -> {
                find()
            }
            R.id.dateAndTime -> {
                dateAndTime()
            }
        }
        item.isChecked = true
        viewer.getDrawerLayout().close()
        return true
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.newFileBtm -> {
                new()
                clearUndoRedoHistory()
                true
            }
            R.id.openBtm -> {
                open()
                clearUndoRedoHistory()
                true
            }
            R.id.redoBtm -> {
                redo()
                true
            }
            R.id.undoBtm -> {
                undo()
                true
            }
            R.id.pasteBtm -> {
                paste(item)
                pasteItemInBotMenu = item
                true
            }
            R.id.copyBtm -> {
                copy()
                true
            }
            else -> false
        }
    }

    override fun new() {
        viewer.makeEditTextEditable()
        viewer.setTextFromFile("")
    }

    override fun open() {
        openDoc.launch(arrayOf("*/*"))
    }

    private val openDoc = viewer.registerForActivityResult(ActivityResultContracts.OpenDocument())
    { uri1 ->
        if (uri1 != null) {
            if (isOk(uri1)) {
                val byteData = getText(viewer, uri1)
                byteData?.let { String(it) }?.let {
                    viewer.setTextFromFile(it)
                }
                uri = uri1
                viewer.makeEditTextEditable()
            } else {
                viewer.showToast("File is not supported")
            }
        }
    }

    private fun getText(context: Context, uri: Uri): CharArray? {
        val inputStream: InputStream?
        val bfr: BufferedReader?
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            bfr = BufferedReader(InputStreamReader(inputStream))
            val buffer = ArrayList<String>()
            var bfrRead: String? = bfr.readLine()
            while (null != bfrRead) {
                buffer.add(bfrRead)
                bfrRead = bfr.readLine()
            }
            val readData: String = buffer.joinToString("\n")
            buffer.clear()
            inputStream?.close()
            bfr.close()
            readData.toCharArray()
        } catch (ex: Exception) {
            Log.e("Error", ex.message.toString())
            viewer.showToast("getText error: ${ex.message}")
            null
        }
    }

    private fun clearUndoRedoHistory() {
        viewer.getUndoRedoManager().clearHistory()
    }

    override fun redo() {
        manager = viewer.getUndoRedoManager()
        if (manager.canRedo) {
            manager.redo()
        }
    }

    override fun undo() {
        manager = viewer.getUndoRedoManager()
        if (manager.canUndo) {
            manager.undo()
        }
    }

    override fun cut() {
        val clipboardManager =
            viewer.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (TextEditor.cut(viewer.getEditText(), clipboardManager)) {
            viewer.showToast("Cut Out")
            if (pasteItemInNavMenu?.isEnabled == false)
                pasteItemInNavMenu?.isEnabled = true
            if (pasteItemInBotMenu?.isEnabled == false)
                pasteItemInBotMenu?.isEnabled = true
        }
    }

    override fun copy() {
        val clipboardManager =
            viewer.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (TextEditor.copy(viewer.getEditText(), clipboardManager)) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                viewer.showToast("Copied")
            if (pasteItemInNavMenu?.isEnabled == false || pasteItemInBotMenu?.isEnabled == false) {
                pasteItemInNavMenu?.isEnabled = true
                pasteItemInBotMenu?.isEnabled = true
            }
        }
    }

    override fun paste(pasteItem: MenuItem) {
        viewer.makeEditTextEditable()
        val clipboardManager =
            viewer.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val pasteData = TextEditor.paste(clipboardManager, pasteItem)
        if (pasteData.isNotEmpty()) {
            if (!pasteItem.isEnabled) pasteItem.isEnabled = true
            viewer.setTextForEditor(pasteData)
            viewer.showToast("Pasted")
        } else {
            viewer.showToast("Clipboard is empty!")
            Log.e(ContentValues.TAG, "Clipboard contains an invalid data type")
        }
    }

    override fun delete() {
        if (TextEditor.delete(viewer.getEditText()))
            viewer.showToast("Deleted")
    }

    override fun find() {
        val inflater: LayoutInflater = LayoutInflater.from(viewer)
        val view: View = inflater.inflate(R.layout.feature_find, null)
        val mBuilder = AlertDialog.Builder(viewer)
            .setTitle("Find")
            .setIcon(R.drawable.ic_search_in)
            .setView(view)
            .setPositiveButton("Find next", null)
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .show()

        val mPositiveButton = mBuilder.getButton(AlertDialog.BUTTON_POSITIVE)
        val mNegativeButton = mBuilder.getButton(AlertDialog.BUTTON_NEGATIVE)
        mNegativeButton.setTextColor(Color.RED)
        val editTextFind = view.findViewById<EditText>(R.id.findWhat)

        var posD = 0
        mPositiveButton.setOnClickListener {
            if (viewer.getEditText().text.isNotEmpty()) {
                val matchingAnswer = BMooreMatchText.search(
                    viewer.getEditText().text.toString().toCharArray(),
                    editTextFind.text.toString().toCharArray()
                )
                if (matchingAnswer.isNotEmpty()) {
                    if (posD == matchingAnswer.size || posD > matchingAnswer.size) posD = 0
                    val edFind = editTextFind.text.toString()
                    viewer.getEditText().setSelection(
                        matchingAnswer[posD++],
                        matchingAnswer[posD - 1] + edFind.length
                    )
                } else
                    viewer.showToast("'${editTextFind.text}' not found!")
            } else
                viewer.showToast("Text empty!")
        }
    }

    override fun selectAll() {
        viewer.getEditText().selectAll()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun dateAndTime() {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy")
        val formatted = current.format(formatter)
        viewer.setTextForEditor(formatted)
    }

    override fun size() {
        val editText = viewer.getEditText()
        val spinner = viewer.findViewById<Spinner>(R.id.spinner)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                when (spinner.selectedItemPosition) {
                    1 -> editText.textSize = 18f
                    2 -> editText.textSize = 25f
                    3 -> editText.textSize = 30f
                    4 -> editText.textSize = 35f
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    override fun makeBold() {
        val editText = viewer.getEditText()
        val spannableString = SpannableStringBuilder(editText.text)
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            editText.selectionStart,
            editText.selectionEnd,
            0
        )
        editText.text = spannableString
    }

    override fun makeItalic() {
        val editText = viewer.getEditText()
        val spannableString = SpannableStringBuilder(editText.text)
        spannableString.setSpan(
            StyleSpan(Typeface.ITALIC),
            editText.selectionStart,
            editText.selectionEnd,
            0
        )
        editText.text = spannableString
    }

    override fun makeUnderlined() {
        val editText = viewer.getEditText()
        val spannableString = SpannableStringBuilder(editText.text)
        spannableString.setSpan(UnderlineSpan(), editText.selectionStart, editText.selectionEnd, 0)
        editText.text = spannableString
    }

    override fun makeRegularFormat() {
        val editText = viewer.getEditText()
        val stringText: String = editText.text.toString()
        editText.setText(stringText)
    }

    override fun alignLeft() {
        val editText = viewer.getEditText()
        editText.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        val spannableString = SpannableStringBuilder(editText.text)
        editText.text = spannableString
    }

    override fun alignRight() {
        val editText = viewer.getEditText()
        editText.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
        val spannableString = SpannableStringBuilder(editText.text)
        editText.text = spannableString
    }

    override fun alignCenter() {
        val editText = viewer.getEditText()
        editText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        val spannableString = SpannableStringBuilder(editText.text)
        editText.text = spannableString
    }

    override fun save() {
        if (uri == Uri.parse("")) {
            saveAs()
        } else {
            saveToFile(uri)
        }
    }

    override fun saveAs() {
        if (uri == Uri.parse("")) {
            saveAsDoc.launch("Example.ntp")
        } else {
            val u = DocumentFile.fromSingleUri(viewer, uri)?.name.toString()
            saveAsDoc.launch(u)
        }
    }

    override fun print() {
        val content: String = viewer.getEditText().text.toString()
        if (content != "" && contentIsNormal(content)) {
            val printDocument = PrintDocument(content, this.viewer, viewer.getFonts())
            printDocument.doPrint()
        } else {
            viewer.showToast("Document is empty")
        }
    }

    private fun contentIsNormal(content: String): Boolean {
        content.forEach {
            if (it != ' ' && it != '\n') return true
        }
        return false
    }

    override fun aboutApp() {
        val dialog = Dialog(viewer)
        dialog.setContentView(R.layout.dialog_layout)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    override fun exit() {
        val alertDialog = AlertDialog.Builder(viewer)
        alertDialog.setTitle("Exit")
            .setMessage(" Do you want to exit? ")
            .setCancelable(true)
            .setPositiveButton(" Cancel") { dialogInterface, _ -> dialogInterface.cancel() }
            .setNegativeButton(" Yes") { _, _ ->
                clearUndoRedoHistory()
                manager.disconnect()
                viewer.close()
                viewer.finish()
            }
        alertDialog.show()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.editText -> {
                viewer.keyBoardShow()
            }

            R.id.fab -> {
                viewer.animateFab()
            }

            R.id.bolt -> {
                viewer.animateFab()
                makeBold()
            }

            R.id.italic -> {
                viewer.animateFab()
                makeItalic()
            }

            R.id.underline -> {
                viewer.animateFab()
                makeUnderlined()
            }

            R.id.no_format -> {
                viewer.animateFab()
                makeRegularFormat()
            }

            R.id.align_left -> {
                viewer.animateFab()
                alignLeft()
            }

            R.id.align_right -> {
                viewer.animateFab()
                alignRight()
            }

            R.id.align_center -> {
                viewer.animateFab()
                alignCenter()
            }
        }
    }

    private fun isOk(uri: Uri): Boolean {
        val fullFileName = DocumentFile.fromSingleUri(viewer, uri)?.name
        var dotCount = 0
        fullFileName?.forEach {
            if (it == '.') {
                dotCount++
            }
        }
        if (fullFileName != null && dotCount == 1) {
            if (fullFileName.lastIndexOf(".") != -1 && fullFileName.lastIndexOf(".") != 0) {
                var extensionOfFile = fullFileName.substring(fullFileName.lastIndexOf(".") + 1)
                extensionOfFile = extensionOfFile.substringBefore(" ")
                val fileName = fullFileName.substringBefore(".")
                if (isCorrectName(fileName)) {
                    if (extensionOfFile == "ntp"
                        || extensionOfFile == "kt"
                        || extensionOfFile == "swift"
                        || extensionOfFile == "java"
                    ) {
                        val size = DocumentFile.fromSingleUri(viewer, uri)?.length()
                        println("size          $size")
                        val max = 5823577
                        if (size != null) {
                            return size < max
                        }
                    }
                }
            }
        }
        return false
    }

    private fun isCorrectName(fileName: String): Boolean {
        fileName.forEach {
            val int: Int = it.code
            if (int in 65..90 || int in 97..122) {
                println("")
            } else {
                return false
            }
        }
        return true
    }

    private fun saveToFile(uri: Uri) {
        val text = viewer.getEditText().text.toString()
        try {
            viewer.contentResolver.openFileDescriptor(uri, "rw")?.use { content ->
                FileOutputStream(content.fileDescriptor).use { fos ->
                    fos.write(text.toByteArray())
                    fos.flush()
                    fos.close()
                }
            }
            viewer.showToast("File has been saved!")
        } catch (e: FileNotFoundException) {
            viewer.showToast("File not found")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private val saveAsDoc = viewer.registerForActivityResult(
        ActivityResultContracts
            .CreateDocument("application/ntp")
    ) {
        if (it != null) {
            if (isOk(it)) {
                saveToFile(it)
                uri = it
                viewer.setTextFromFile("")
            } else {
                DocumentFile.fromSingleUri(viewer, it)?.delete()
                viewer.showToast("File extension is not supported!")
            }
        }
    }

}