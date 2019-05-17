package sp.it.pl.gui.objects.picker

import sp.it.pl.layout.widget.ComponentFactory
import sp.it.pl.layout.widget.DeserializingFactory
import sp.it.pl.layout.widget.WidgetFactory
import sp.it.pl.main.APP
import sp.it.pl.main.isUsableByUser
import sp.it.util.functional.asIf
import sp.it.util.functional.net

/** Widget factory picker. */
class WidgetPicker(private val mode: Mode): Picker<ComponentFactory<*>>() {

    init {
        itemSupply = {
            val factories = when(mode) {
                Mode.WIDGET -> APP.widgetManager.factories.getFactories()
                Mode.LAYOUTS -> APP.widgetManager.factories.getComponentFactories().filterIsInstance<DeserializingFactory>()
                Mode.ALL -> APP.widgetManager.factories.getComponentFactories()
            }
            factories.filter { it.isUsableByUser() }
        }
        textConverter = { it.nameGui() }
        infoConverter = { it.asIf<WidgetFactory<*>>()?.net { it.description() } ?: "" }
    }

    enum class Mode {
        WIDGET, LAYOUTS, ALL
    }

}