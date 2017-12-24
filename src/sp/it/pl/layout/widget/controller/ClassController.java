package sp.it.pl.layout.widget.controller;

import java.util.HashMap;
import java.util.Map;
import javafx.scene.layout.AnchorPane;
import org.reactfx.Subscription;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.io.Input;
import sp.it.pl.layout.widget.controller.io.Inputs;
import sp.it.pl.layout.widget.controller.io.Outputs;
import sp.it.pl.util.conf.Config;
import sp.it.pl.util.dev.Dependency;
import sp.it.pl.util.reactive.Disposer;

abstract public class ClassController extends AnchorPane implements Controller<Widget<?>> {

	@Dependency("DO NOT RENAME - accessed using reflection")
	public final Widget<?> widget = null;
	public final Outputs outputs = new Outputs();
	public final Inputs inputs = new Inputs();
	private final HashMap<String,Config<Object>> configs = new HashMap<>();
	public final Disposer onClose = new Disposer();

	@Override
	public Widget<?> getWidget() {
		return widget;
	}

	@Override
	public void refresh() {}


	@Override
	public final void close() {
		onClose.invoke();
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
		onClose.plusAssign(d);
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