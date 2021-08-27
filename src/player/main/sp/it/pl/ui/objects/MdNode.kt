package sp.it.pl.ui.objects

import java.io.File
import java.net.URI
import java.util.concurrent.atomic.AtomicLong
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.StackPane
import oshi.annotation.concurrent.ThreadSafe
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.file.readTextTry
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.onEventDown
import sp.it.util.system.open
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
        children[0].asIs<ScrollPane>().content.asIs<MdNodeContent>().mdString.value = text
     }

   init {
      lay += scrollPane {
         hbarPolicy = NEVER
         isFitToWidth = true

         content = object: MdNodeContent() {
            init {
               stylesheets.clear()
            }
            override fun setLink(node: Node, link: String, description: String) {
               node.onEventDown(MOUSE_CLICKED, PRIMARY) {
                  runTry { URI(link.trim()).resolveAsLink() }.orNull()?.open()
               }
            }

            override fun generateImage(url: String): Node {
               return if (url.isEmpty()) {
                  Group()
               } else {
                  url.trim().let { runTry { URI(it).resolveAsLink() }.orNull() }
                     ?.let {
                        ImageView(runTry { Image(it.toURL().toExternalForm(), true) }.orNull())

//                        Thumbnail(200.0, 200.0).apply {
//                           loadImage(runTry { Image(it.toURL().toExternalForm(), true) }.orNull())
//                        }.pane
                     }
                     ?: Group()
               }
            }
         }
      }
   }

   private fun URI.asDirectory(): URI = toString().net { if (!it.endsWith("/")) URI.create("$it/") else this }

   private fun URI.resolveAsLink() = when(scheme) {
      null, "", "file" -> file?.parentFile?.absoluteFile?.net { it.toURI().asDirectory().resolve(path.removePrefix("/")) } ?: this
      else -> this
   }

   @ThreadSafe
   fun readFile(f: File?) {
      val id = dataId.incrementAndGet()
      runIO {
         f?.readTextTry()?.orNull().orEmpty()
      } ui {
         if (dataId.get()==id) {
            file = f
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