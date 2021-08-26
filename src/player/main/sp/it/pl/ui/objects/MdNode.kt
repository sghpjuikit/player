package sp.it.pl.ui.objects

import com.sandec.mdfx.MarkdownView
import java.io.File
import java.net.URI
import java.util.concurrent.atomic.AtomicLong
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.StackPane
import oshi.annotation.concurrent.ThreadSafe
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.dev.printIt
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
   private var dataId = AtomicLong()
   /** Source of the displayed markdown. Relative links resolve against it. */
   private var file: File? = null
   /** Source of the displayed markdown. */
   var text: String = ""
     set(value) {
        dataId.getAndIncrement()
        field = value
        children[0].asIs<ScrollPane>().content.asIs<MarkdownView>().mdString = text
     }

   init {
      lay += scrollPane {
         hbarPolicy = NEVER
         isFitToWidth = true

         content = object: MarkdownView() {
            init {
               stylesheets.clear()
            }
            override fun setLink(node: Node?, link: String?, description: String?) {
               node?.onEventDown(MOUSE_CLICKED, PRIMARY) {
                  link?.trim()?.let { runTry { URI(it).resolveAsLink().printIt() }.orNull() }?.browse()
               }
            }
         }
      }
   }

   private fun URI.resolveAsLink() = file?.net { it.toURI().resolve(this) } ?: this

   @ThreadSafe
   fun readFile(f: File) {
      val id = dataId.incrementAndGet()
      runIO {
         f.readTextTry().printIt().orNull().orEmpty()
      } ui {
         if (dataId.get()==id) {
            text = it
            file = f
         }
      }
   }

   @ThreadSafe
   fun readText(text: String) {
      runFX {
         file = null
         this.text = text
      }
   }

}