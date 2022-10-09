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
import javafx.scene.text.Text
import kotlin.math.sin
import kotlin.math.sqrt
import sp.it.pl.main.IconOC
import sp.it.pl.main.getText
import sp.it.pl.main.hasText
import sp.it.pl.main.installDrag
import sp.it.util.access.v
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.dev.ThreadSafe
import sp.it.util.file.readTextTry
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventDown
import sp.it.util.system.open
import sp.it.util.ui.lay
import sp.it.util.ui.lookupChildAsOrNull
import sp.it.util.ui.scrollPane
import sp.it.util.units.millis

/** Node displaying markdown as native javafx scene-graph */
class MdNode: StackPane() {
   private var dataId = AtomicLong()
   /** Source of the displayed markdown. Relative links resolve against it. */
   private var file: File? = null
   /** Source of the displayed markdown. */
   var text = v("").apply {
      attach {
        dataId.getAndIncrement()
        children[0].asIs<ScrollPane>().content.asIs<MdNodeContent>().mdString.value = it
      }
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
                  when {
                     link.startsWith("#") -> scrollToAnchor(link)
                     else -> runTry { URI(link.trim()).resolveAsLink() }.orNull()?.open()
                  }
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

            override fun scrollToAnchor(anchor: String) {
               val content = children[0].asIs<MdNodeHelper>()
               val anchorText = anchor.trim().removePrefix("#")
               val anchorNode = content.lookupAll(".markdown-heading").find { it.lookupChildAsOrNull<Text>()?.text?.replace(' ', '-').equals(anchorText, true) }
               anchorNode.ifNotNull {
                  val anchorPosition = content.sceneToLocal(it.localToScene(it.layoutBounds)).minY
                  val virtualHeight = this@scrollPane.content.layoutBounds.height-this@scrollPane.height
                  val ov = this@scrollPane.vvalue
                  val nv = anchorPosition/virtualHeight
                  anim(250.millis) { this@scrollPane.vvalue = ov + it*(nv - ov) }.intpl { sqrt(sin(Math.PI/2*it)) }.play()
               }
            }
         }
      }

      installDrag(
         IconOC.MARKDOWN, "Display as markdown",
         { it.dragboard.hasText() },
         { readText(it.dragboard.getText()) }
      )
      installDrag(
         IconOC.MARKDOWN, "Read & display as markdown",
         { it.dragboard.hasFiles() },
         { readFile(it.dragboard.files.first()) }
      )
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
            text.value = it
            file = f
         }
      }
   }

   @ThreadSafe
   fun readText(text: String) {
      runFX {
         file = null
         this.text.value = text
      }
   }

}