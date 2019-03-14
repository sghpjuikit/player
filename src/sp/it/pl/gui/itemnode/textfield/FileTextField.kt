package sp.it.pl.gui.itemnode.textfield

import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.input.DragEvent.DRAG_DROPPED
import javafx.scene.input.DragEvent.DRAG_OVER
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.util.access.v
import sp.it.pl.util.file.FileType.DIRECTORY
import sp.it.pl.util.file.FileType.FILE
import sp.it.pl.util.graphics.Util.layHorizontally
import sp.it.pl.util.graphics.drag.DragUtil
import sp.it.pl.util.graphics.drag.DragUtil.getFiles
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.system.chooseFile
import sp.it.pl.util.validation.Constraint.FileActor
import java.io.File

/** Text field for [File] with file/dir constraint, drag & drop and picker. */
class FileTextField(constraint: FileActor): ValueTextField<File>({ APP.converter.general.toS(it) }) {
    private val type = v(if (constraint==FileActor.FILE) FILE else DIRECTORY)

    init {
        styleClass += STYLECLASS

        if (constraint==FileActor.ANY) {
            val b2 = right.value as ArrowDialogButton
            val b1 = Icon(null, 7.0).onClickDo { type.setNextValue() }.tooltip("Switch mode between file and directory")

            right.value = layHorizontally(5.0, CENTER_RIGHT, b1, b2)

            type sync { b1.icon(if (it==FILE) IconFA.FILE else IconFA.FOLDER) }
        }
        addEventHandler(DRAG_OVER, DragUtil.accept { e -> getFiles(e).any { constraint.isValid(it) } })
        addEventHandler(DRAG_DROPPED) { value = getFiles(it).find { constraint.isValid(it) } }
    }

    override fun onDialogAction() {
        val title = if (type.get()==DIRECTORY) "Choose directory" else "Choose file"
        chooseFile(title, type.get(), vl, scene.window).ifOk {
            value = it
        }
    }

    companion object {
        const val STYLECLASS = "file-text-field"
    }

}