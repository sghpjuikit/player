package objectInfo

import javafx.scene.image.Image
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import mu.KLogging
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.APP
import sp.it.pl.layout.widget.WidgetCompanion
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.Opener
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconUN
import sp.it.pl.main.computeDataInfo
import sp.it.pl.main.emScaled
import sp.it.pl.main.getAny
import sp.it.pl.main.installDrag
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.pl.ui.pane.ImageFlowPane
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.async.FX
import sp.it.util.file.toFileOrNull
import sp.it.util.functional.getOr
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.sync
import sp.it.util.ui.image.toFX
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.stackPane
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicLong

class ObjectInfo(widget: Widget): SimpleController(widget), Opener {
   private val inputItems = io.i.create<Any>("To display", null, ::open)
   private val info = Text()
   private val thumb = Thumbnail()
   private val openId = AtomicLong(1L)

   init {
      root.prefSize = 600.emScaled x 300.emScaled
      root.consumeScrolling()
      root.lay += ImageFlowPane(null, null).apply {
         thumb.image sync { setImageVisible(it!=null) }
         setGap(15.emScaled)
         setImage(thumb)
         setContent(
            stackPane {
               lay += scrollPane {
                  content = TextFlow().apply {
                     lay += info
                  }
               }
            }
         )
      }

      root.installDrag(
         IconFA.INFO,
         "Display information about the object",
         { true },
         { open(it.dragboard.getAny()) }
      )
   }

   override fun open(data: Any?) {
      val id = openId.incrementAndGet()
      computeDataInfo(data).onDone(FX) {
         if (id==openId.get()) {
            info.text = it.toTry().getOr("Failed to obtain data information.")
         }
      }

      when (data) {
         is Image -> thumb.loadImage(data)
         is BufferedImage -> thumb.loadImage(data.toFX())
         else -> thumb.loadFile(
            when (data) {
               is File -> data
               is URI -> data.toFileOrNull()
               is URL -> data.toFileOrNull()
               is Path -> data.toFile()
               else -> null
            }
         )
      }

   }

   override fun focus() = Unit

   companion object: WidgetCompanion, KLogging() {
      override val name = "Object Info"
      override val description = "Displays information about or preview of an object"
      override val descriptionLong = "$description."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 1, 1)
      override val isSupported = true
      override val year = year(2020)
      override val author = "spit"
      override val contributor = ""
      override val summaryActions = listOf(
         ShortcutPane.Entry("Data", "Set data", "Drag & drop object"),
      )
      override val group = APP
   }
}