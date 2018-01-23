package sp.it.pl.gui.itemnode.textfield

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.geometry.Pos
import javafx.scene.input.DragEvent.DRAG_DROPPED
import javafx.scene.input.DragEvent.DRAG_OVER
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.access.V
import sp.it.pl.util.file.FileType
import sp.it.pl.util.graphics.Util.layHorizontally
import sp.it.pl.util.graphics.drag.DragUtil
import sp.it.pl.util.graphics.drag.DragUtil.getFiles
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.system.chooseFile
import sp.it.pl.util.validation.Constraint.FileActor
import java.io.File

private typealias Type = FileType
private typealias IconFA = FontAwesomeIcon

/** Text field for [File] with file/dir constraint, drag & drop and picker. */
class FileItemNode: TextFieldItemNode<File> {
    private val type: V<FileType>

    constructor(constraint: FileActor): super({ APP.converter.general.toS(it) }) {
        type = V(if (constraint==FileActor.FILE) Type.FILE else Type.DIRECTORY)
        if (constraint==FileActor.ANY) {
            val b2 = right.value as ArrowDialogButton
            val b1 = Icon(null, 7.0).apply {
                tooltip("Switch mode between file and directory")
                setOnMouseClicked { type.setNextValue() }
            }
            right.value = layHorizontally(5.0, Pos.CENTER_RIGHT, b1, b2)

            type sync { b1.icon(if (it==Type.FILE) IconFA.FILE else IconFA.FOLDER) }
        }
        addEventHandler(DRAG_OVER, DragUtil.accept { e -> getFiles(e).any { constraint.isValid(it) } })
        addEventHandler(DRAG_DROPPED) { value = getFiles(it).find { constraint.isValid(it) } }
    }

    override fun onDialogAction() {
        val title = if (type.get()==Type.DIRECTORY) "Choose directory" else "Choose file"
        chooseFile(title, type.get(), v, scene.window)
                .handleOk { value = it }
    }

}