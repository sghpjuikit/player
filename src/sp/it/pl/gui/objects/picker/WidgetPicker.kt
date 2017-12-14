package sp.it.pl.gui.objects.picker

import sp.it.pl.layout.widget.ComponentFactory
import sp.it.pl.main.AppUtil.APP
import java.util.function.Function
import java.util.function.Supplier

/** Widget factory picker. */
class WidgetPicker: Picker<ComponentFactory<*>>() {
    init {
        itemSupply = Supplier { APP.widgetManager.componentFactories }
        textConverter = Function { it.nameGui() }
        infoConverter = Function { it.toStr() }
    }
}