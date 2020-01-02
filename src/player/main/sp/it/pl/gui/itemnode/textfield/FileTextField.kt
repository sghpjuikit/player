package sp.it.pl.gui.itemnode.textfield

import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.input.DragEvent.DRAG_DROPPED
import javafx.scene.input.DragEvent.DRAG_OVER
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.main.IconFA
import sp.it.util.access.toggleNext
import sp.it.util.access.v
import sp.it.util.conf.Constraint.FileActor
import sp.it.util.file.FilePickerType
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.FileType.FILE
import sp.it.util.file.isAnyParentOrSelfOf
import sp.it.util.reactive.sync
import sp.it.util.system.chooseFile
import sp.it.util.system.saveFile
import sp.it.util.ui.Util.layHorizontally
import sp.it.util.ui.drag.handlerAccepting
import java.io.File

/** Text field for [File] with file/dir constraint, drag & drop and picker. Supports relative files. */
class FileTextField(val constraint: FileActor, val relativeTo: File?, val pickerType: FilePickerType = FilePickerType.IN): ValueTextField<File>() {
   private val type = v(if (constraint==FileActor.FILE) FILE else DIRECTORY)

   init {
      styleClass += STYLECLASS

      if (constraint==FileActor.ANY) {
         val b2 = right.value as ArrowDialogButton
         val b1 = Icon(null, 7.0).onClickDo { type.toggleNext() }.tooltip("Switch mode between file and directory")

         right.value = layHorizontally(5.0, CENTER_RIGHT, b1, b2)

         type sync { b1.icon(if (it==FILE) IconFA.FILE else IconFA.FOLDER) }
      }

      addEventHandler(DRAG_OVER, handlerAccepting { it.dragboard.hasFiles() && it.dragboard.files.any { constraint.isValid(it) } })
      addEventHandler(DRAG_DROPPED) { value = it.dragboard.files.find { constraint.isValid(it) } }
   }

   override fun onDialogAction() {
      if (type.value==FILE && pickerType==FilePickerType.OUT) {
         saveFile("Define file", value, "name.extension", scene.window)
      } else {
         chooseFile(if (type.value==DIRECTORY) "Choose directory" else "Choose file", type.value, value, scene.window)
      }.ifOk {
         value = relativeTo?.takeIf { p -> p.isAnyParentOrSelfOf(it) }?.let { p -> it.relativeTo(p) } ?: it
      }
   }

   companion object {
      const val STYLECLASS = "file-text-field"
   }

}