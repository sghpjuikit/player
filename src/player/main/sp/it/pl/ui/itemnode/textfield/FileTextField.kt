package sp.it.pl.ui.itemnode.textfield

import java.io.File
import javafx.scene.input.DragEvent.DRAG_DROPPED
import javafx.scene.input.DragEvent.DRAG_OVER
import sp.it.pl.main.IconUN
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.access.toggleNext
import sp.it.util.access.v
import sp.it.util.conf.Constraint.FileActor
import sp.it.util.file.FilePickerType
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.FileType.FILE
import sp.it.util.file.isAnyParentOrSelfOf
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.system.chooseFile
import sp.it.util.system.saveFile
import sp.it.util.ui.drag.handlerAccepting

/** Text field for [File] with file/dir constraint, drag & drop and picker. Supports relative files. */
class FileTextField(val constraint: FileActor, val relativeTo: File?, val pickerType: FilePickerType = FilePickerType.IN): ValueTextField<File>() {
   private val type = v(if (constraint==FileActor.FILE) FILE else DIRECTORY)

   init {
      styleClass += STYLECLASS

      if (constraint==FileActor.ANY) {
         val b1 = Icon(null, 7.0).onClickDo { type.toggleNext() }.tooltip("Switch mode between file and directory")
         right.add(0, b1)
         type sync { b1.icon(if (it==FILE) IconUN(0x1f4c4) else IconUN(0x1f4c1)) }
      }

      onEventDown(DRAG_OVER, handlerAccepting { it.dragboard.hasFiles() && it.dragboard.files.any { constraint.isValid(it) } }::handle)
      onEventDown(DRAG_DROPPED) { value = it.dragboard.files.find { constraint.isValid(it) } }
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