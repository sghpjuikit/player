package sp.it.pl.gui.objects.picker

import sp.it.pl.layout.widget.ComponentFactory
import sp.it.pl.layout.widget.WidgetFactory
import sp.it.pl.main.APP
import sp.it.pl.main.isUsableByUser
import sp.it.pl.util.functional.asIf
import sp.it.pl.util.functional.net

/** Widget factory picker. */
class WidgetPicker: Picker<ComponentFactory<*>>() {
    init {
        itemSupply = { APP.widgetManager.factories.getComponentFactories().filter { it.isUsableByUser() } }
        textConverter = { it.nameGui() }
        infoConverter = { it.asIf<WidgetFactory<*>>()?.net { it.description() } ?: "" }
    }
}