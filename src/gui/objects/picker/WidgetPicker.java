package gui.objects.picker;

import layout.widget.WidgetFactory;
import layout.widget.WidgetInfo;
import static main.App.APP;

/** Widget factory picker. */
public class WidgetPicker extends Picker<WidgetFactory<?>> {

	public WidgetPicker() {
		super();
		itemSupply = APP.widgetManager::getFactories;
		textConverter = WidgetFactory::nameGui;
		infoConverter = WidgetInfo::toStr;
	}

}