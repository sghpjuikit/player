package sp.it.pl.ui.objects.picker

import sp.it.pl.layout.ComponentFactory
import sp.it.pl.layout.WidgetFactory
import sp.it.pl.main.APP
import sp.it.pl.main.IconUN
import sp.it.pl.main.emScaled
import sp.it.pl.main.isUsableByUser
import sp.it.util.functional.asIf
import sp.it.util.functional.net
import sp.it.util.ui.x

/** Widget factory picker. */
class WidgetPicker(private val mode: Mode): Picker<ComponentFactory<*>>() {

   init {
      itemSupply = {
         val factories = when (mode) {
            Mode.WIDGET -> APP.widgetManager.factories.getFactories()
            Mode.LAYOUTS -> APP.widgetManager.factories.getComponentFactories().filter { it !is WidgetFactory<*> }
            Mode.ALL -> APP.widgetManager.factories.getComponentFactories()
         }
         factories.filter { it.isUsableByUser() }
      }
      textConverter = { it.name }
      infoConverter = { it.asIf<WidgetFactory<*>>()?.net { it.description } ?: "" }
      iconConverter = { it.asIf<WidgetFactory<*>>()?.net { it.icon ?: IconUN(0x2b1a) } ?: IconUN(0x2ff4) }
      minCellSize.value = 150.emScaled x 70.emScaled
   }

   enum class Mode {
      WIDGET, LAYOUTS, ALL
   }

}