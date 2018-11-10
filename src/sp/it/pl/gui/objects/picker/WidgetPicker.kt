package sp.it.pl.gui.objects.picker

import sp.it.pl.layout.widget.ComponentFactory
import sp.it.pl.main.APP
import java.util.function.Function
import java.util.function.Supplier
import kotlin.streams.asStream

/** Widget factory picker. */
class WidgetPicker: Picker<ComponentFactory<*>>() {
    init {
        itemSupply = Supplier { APP.widgetManager.factories.getComponentFactories().asStream() }
        textConverter = Function { it.nameGui() }
        infoConverter = Function { it.toStr() }
    }
}