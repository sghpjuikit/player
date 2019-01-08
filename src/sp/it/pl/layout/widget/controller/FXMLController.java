package sp.it.pl.layout.widget.controller;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.reactfx.Subscription;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.io.Input;
import sp.it.pl.layout.widget.controller.io.Inputs;
import sp.it.pl.layout.widget.controller.io.Outputs;
import sp.it.pl.util.conf.Config;
import sp.it.pl.util.dev.Dependency;
import static sp.it.pl.util.dev.Fail.noNull;

/**
 * Controller for widgets built around fxml files. See {@link javafx.fxml.FXMLLoader}.
 */
abstract public class FXMLController implements Controller {

	@SuppressWarnings("ConstantConditionalExpression")
	@Dependency("DO NOT RENAME - accessed using reflection")
	public final Widget<?> widget = true ? null : null;
	@Dependency("DO NOT RENAME - accessed using reflection")
	public final boolean isInitialized = false;
	private Node root = null;

	public final Outputs outputs = new Outputs();
	public final Inputs inputs = new Inputs();
	private final HashMap<String,Config<Object>> configs = new HashMap<>();
	public final List<Subscription> onClose = new ArrayList<>();

	@Override
	public Widget<?> getOwnerWidget() {
		noNull(widget);
		return widget;
	}

	@Override
	public Node loadFirstTime() throws Exception {
		String name = getClass().getSimpleName();   // TODO: use directory name ?

		FXMLLoader loader = new FXMLLoader();
				   loader.setLocation(getResource(name + ".fxml").toURI().toURL());
				   loader.setController(this);
		root = loader.load();
		if (root instanceof Pane) loadSkin("skin.css", (Pane) root);
		return root;
	}

	@Override
	abstract public void refresh();

	@Override
	public void focus() {
		if (root!=null) root.requestFocus();
	}

	public void loadSkin(String filename, Pane root) {
		try {
			File skin = getResource(filename);
			if (skin.exists())
				root.getStylesheets().add(skin.toURI().toURL().toExternalForm());
		} catch (MalformedURLException ex) {}
	}

	@Override
	public Outputs getOwnedOutputs() {
		return outputs;
	}

	@Override
	public Inputs getOwnedInputs() {
		return inputs;
	}

	@Override
	public Map<String, Config<Object>> getFieldsMap() {
		return configs;
	}

	@Override
	public final void close() {
		onClose.forEach(Subscription::unsubscribe);
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
		onClose.add(d);
	}

}