package sp.it.pl.gui.objects.picker

import sp.it.pl.layout.widget.ComponentFactory
import sp.it.pl.layout.widget.WidgetFactory
import sp.it.pl.main.APP
import sp.it.pl.util.functional.asIf
import sp.it.pl.util.functional.net
import java.util.function.Function
import java.util.function.Supplier
import kotlin.streams.asStream

/** Widget factory picker. */
class WidgetPicker: Picker<ComponentFactory<*>>() {
    init {
        itemSupply = Supplier { APP.widgetManager.factories.getComponentFactories().asStream() }
        textConverter = Function { it.nameGui() }
        infoConverter = Function { it.asIf<WidgetFactory<*>>()?.net { it.description() } ?: "" }
    }
}