package gui.objects.picker;

import layout.widget.WidgetFactory;
import static main.App.APP;

/** Widget factory picker. */
public class WidgetPicker extends Picker<WidgetFactory<?>>{

    public WidgetPicker() {
        super();
        itemSupply = APP.widgetManager::getFactories;
        textCoverter = WidgetFactory::nameGui;
        infoCoverter = widget -> widget.toStr();
    }

}