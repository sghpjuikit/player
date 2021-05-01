package node

import javafx.scene.Node
import kotlin.reflect.full.createInstance
import mu.KLogging
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.APP
import sp.it.pl.layout.widget.WidgetCompanion
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.IconUN
import sp.it.pl.main.emScaled
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.collections.setTo
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.sync
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year

class Node(widget: Widget): SimpleController(widget) {

   private val node by cvn<String>(null).def(name = "Component class", info = "kotlin.reflect.KClass of the javafx.scene.Node component. Needs public no argument constructor.")

   init {
      root.prefSize = 500.emScaled x 500.emScaled
      root.consumeScrolling()

      node sync {
         root.children setTo listOfNotNull(runTry { Class.forName(it).kotlin.createInstance() as Node }.orNull())
      }
   }

   companion object: WidgetCompanion, KLogging() {
      override val name = "Node"
      override val description = "Displays component specified by class"
      override val descriptionLong = "$description. This avoids the need to create widget wrappers for ui components."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2021)
      override val author = "spit"
      override val contributor = ""
      override val summaryActions = listOf<ShortcutPane.Entry>()
      override val group = APP
   }
}