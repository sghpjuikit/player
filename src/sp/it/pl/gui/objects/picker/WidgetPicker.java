package sp.it.pl.gui.objects.picker;

import sp.it.pl.layout.widget.ComponentFactory;
import static sp.it.pl.main.App.APP;

/** Widget factory picker. */
public class WidgetPicker extends Picker<ComponentFactory<?>> {

	public WidgetPicker() {
		super();
		itemSupply = () -> APP.widgetManager.getComponentFactories();
		textConverter = ComponentFactory::nameGui;
		infoConverter = ComponentFactory::toStr;
	}

}