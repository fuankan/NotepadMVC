package internlabs.dependencyinjection.notepadmvc.controller

import android.view.MenuItem

interface OurTasks {
    fun new()
    fun open()
    fun save()
    fun saveAs()
    fun print()
    fun aboutApp()
    fun exit()

    // edit
    fun redo()
    fun undo()
    fun cut()
    fun copy()
    fun paste(pasteItem: MenuItem)
    fun delete()
    fun find()
    fun selectAll()
    fun dateAndTime()

    // Font
    fun size()
    fun makeBold()
    fun makeItalic()
    fun makeUnderlined()
    fun makeRegularFormat()

    // paragraph
    fun alignLeft()
    fun alignRight()
    fun alignCenter()
}