package layout.widget.controller;

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

import layout.widget.Widget;
import layout.widget.controller.io.Input;
import layout.widget.controller.io.Inputs;
import layout.widget.controller.io.Outputs;
import util.conf.Config;
import util.dev.Dependency;

import static util.dev.Util.noØ;

/**
 * Controller for widgets built around fxml files. See {@link javafx.fxml.FXMLLoader}.
 */
abstract public class FXMLController implements Controller<Widget<?>> {

	@Dependency("DO NOT RENAME - accessed using reflection")
	public final Widget<?> widget = null;
	public final Outputs outputs = new Outputs();
	public final Inputs inputs = new Inputs();
	private final HashMap<String,Config<Object>> configs = new HashMap<>();
	private final List<Subscription> disposables = new ArrayList<>();

	@Override
	public Widget<?> getWidget() {
		noØ(widget);
		return widget;
	}

	@Override
	public Node loadFirstTime() throws Exception {
//		String name = widget.getName(); // TODO: remove or implement
		String name = getClass().getSimpleName();

		FXMLLoader loader = new FXMLLoader();
				   loader.setLocation(getResource(name + ".fxml").toURI().toURL());
				   loader.setController(this);
		Node root = loader.load();
		if (root instanceof Pane) loadSkin("skin.css",(Pane)root);
		return root;
	}

	@Override
	abstract public void refresh();

	/**
	 * Returns specified resource file from the widget's location.
	 * @param filename of the file with extension. For example: "bgr.jpg"
	 */
	public File getResource(String filename) {
		noØ(widget);
		return new File(widget.factory.location,filename).getAbsoluteFile();
	}

	public void loadSkin(String filename, Pane root) {
		try {
			File skin = getResource(filename);
			if (skin.exists())
				root.getStylesheets().add(skin.toURI().toURL().toExternalForm());
		} catch (MalformedURLException ex) {}
	}

	@Override
	public Outputs getOutputs() {
		return outputs;
	}

	@Override
	public Inputs getInputs() {
		return inputs;
	}

	@Override
	public Map<String, Config<Object>> getFieldsMap() {
		return configs;
	}

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

}