package sp.it.demo

import java.io.File
import javafx.application.Application
import javafx.geometry.Pos.CENTER
import javafx.scene.Scene
import javafx.scene.control.ListCell
import javafx.scene.image.ImageView
import javafx.scene.input.DragEvent.DRAG_DROPPED
import javafx.scene.input.DragEvent.DRAG_OVER
import javafx.scene.input.TransferMode
import javafx.scene.layout.Priority.ALWAYS
import javafx.stage.Stage
import javafx.util.Callback
import sp.it.util.ui.IconExtractor.getFileIcon
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.listView
import sp.it.util.ui.vBox

/** Showcases the icon extraction for various file types and files */
class IconExtractorDemo: Application() {

   private val list = listView<String> {
      items += listOf(
         "a.msg", "a1.msg", "b.txt", "c.pdf", "d.html", "e.png", "f.zip", "g.docx", "h.xlsx", "i.pptx"
      )
      cellFactory = Callback {
         object: ListCell<String>() {
            override fun updateItem(item: String?, empty: Boolean) {
               super.updateItem(item, empty)
               if (empty || item==null) {
                  graphic = null
                  text = null
               } else {
                  graphic = ImageView(getFileIcon(File(item)))
                  text = item
               }
            }
         }
      }
   }

   override fun start(stage: Stage) {
      val box = vBox {
         lay += label("Drag & drop files (such as .exe) to display icon") { alignment = CENTER }
         lay(ALWAYS) += list

         addEventHandler(DRAG_OVER) {
            if (it.dragboard.hasFiles()) {
               it.acceptTransferModes(*TransferMode.ANY)
               it.consume()
            }
         }
         addEventHandler(DRAG_DROPPED) {
            if (it.dragboard.hasFiles()) {
               list.items += it.dragboard.files.map { it.absolutePath }
               it.isDropCompleted = true
               it.consume()
            }
         }
      }

      stage.scene = Scene(box, 200.0, 200.0)
      stage.title = "ListViewSample"
      stage.show()
   }
}

fun main(args: Array<String>) {
   Application.launch(IconExtractorDemo::class.java, *args)
}