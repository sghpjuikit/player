package gui.objects.picker;

import layout.widget.ComponentFactory;
import static main.App.APP;

/** Widget factory picker. */
public class WidgetPicker extends Picker<ComponentFactory<?>> {

	public WidgetPicker() {
		super();
		itemSupply = () -> APP.widgetManager.getComponentFactories();
		textConverter = ComponentFactory::nameGui;
		infoConverter = ComponentFactory::toStr;
	}

}