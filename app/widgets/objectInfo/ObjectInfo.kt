package objectInfo

import javafx.scene.text.Text
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.Opener
import sp.it.pl.main.IconFA
import sp.it.pl.main.computeDataInfo
import sp.it.pl.main.emScaled
import sp.it.pl.main.getAny
import sp.it.pl.main.installDrag
import sp.it.util.async.FX
import sp.it.util.functional.getOr
import sp.it.util.reactive.consumeScrolling
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollText
import sp.it.util.ui.x

@Widget.Info(
   author = "Martin Polakovic",
   name = "Object Info",
   description = "Displays info or preview of an object.",
   version = "1.0.1",
   year = "2020",
   group = Widget.Group.APP
)
class ObjectInfo(widget: Widget): SimpleController(widget), Opener {

   private val inputItems = io.i.create<Any>("To display", null, ::open)
   private val info = Text()

   init {
      root.prefSize = 500.emScaled x 300.emScaled
      root.consumeScrolling()
      root.lay += scrollText { info }

      installDrag(
         root, IconFA.INFO, "Display information about the object",
         { true },
         { open(it.dragboard.getAny()) }
      )
   }

   override fun open(data: Any?) {
      computeDataInfo(data).onDone(FX) {
         info.text = it.toTry().getOr("Failed to obtain data information.")
      }
   }

   override fun focus() = Unit

}