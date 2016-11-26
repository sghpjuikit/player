package layout.widget.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.layout.AnchorPane;

import org.reactfx.Subscription;

import util.conf.Config;
import layout.widget.Widget;
import layout.widget.controller.io.Input;
import layout.widget.controller.io.Inputs;
import layout.widget.controller.io.Outputs;
import util.dev.Dependency;

/**
 * @author Martin Polakovic
 */
abstract public class ClassController extends AnchorPane implements Controller<Widget<?>> {

	@Dependency("DO NOT RENAME - accessed using reflection")
	public final Widget<?> widget = null;
	public final Outputs outputs = new Outputs();
	public final Inputs inputs = new Inputs();
	private final HashMap<String,Config<Object>> configs = new HashMap<>();
	private final List<Subscription> disposables = new ArrayList<>();

	@Override
	public Widget<?> getWidget() {
		return widget;
	}

	@Override
	public void refresh() {}


	@Override
	public final void close() {
		disposables.forEach(Subscription::unsubscribe);
		onClose();
		inputs.getInputs().forEach(Input::unbindAll);
	}

	public void onClose() {}

	/**
	 * Adds the subscription to the list of subscriptions that unsubscribe when
	 * this controller's widget is closed.
	 * <p/>
	 * Anything that needs to be disposed can be passed here as runnable at any
	 * time.
	 */
	public void d(Subscription d) {
		disposables.add(d);
	}

	/** {@inheritDoc} */
	@Override
	public Outputs getOutputs() {
		return outputs;
	}

	/** {@inheritDoc} */
	@Override
	public Inputs getInputs() {
		return inputs;
	}

	@Override
	public Map<String, Config<Object>> getFieldsMap() {
		return configs;
	}

}