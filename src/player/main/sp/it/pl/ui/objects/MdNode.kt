package sp.it.pl.ui.objects

import com.sandec.mdfx.MarkdownView
import java.io.File
import java.net.URI
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.StackPane
import sp.it.util.file.readTextTry
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.onEventDown
import sp.it.util.system.browse
import sp.it.util.ui.lay
import sp.it.util.ui.scrollPane

/** Node displaying markdown as native javafx scene-graph */
class MdNode: StackPane() {

   init {
      lay += scrollPane {
         hbarPolicy = NEVER
         isFitToWidth = true

         content = object: MarkdownView() {
            override fun setLink(node: Node?, link: String?, description: String?) {
               node?.onEventDown(MOUSE_CLICKED, PRIMARY) {
                  link?.trim()?.let { runTry { URI(it) }.orNull() }?.browse()
               }
            }
         }
      }
   }

   fun readFile(f: File) {
      f.readTextTry().orNull().orEmpty().net(::readText)
   }

   fun readText(text: String) {
      children[0].asIs<ScrollPane>().content.asIs<MarkdownView>().mdString = text
   }

}