package gui.pane;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextAlignment;

import gui.itemnode.ConfigField;
import layout.widget.feature.ConfiguringFeature;
import util.conf.Config;
import util.conf.Configurable;

import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.layout.Priority.ALWAYS;
import static util.functional.Util.byNC;
import static util.functional.Util.stream;

public class ConfigPane<T> implements ConfiguringFeature {
	private final FlowPane root = new FlowPane(5,5);
	private final List<ConfigField<T>> configs = new ArrayList<>();
	public Runnable onChange;

	public ConfigPane() {}

	@SuppressWarnings("unchecked")
	public ConfigPane(Collection<Config<T>> configs) {
		this.configs.clear();
		configure((Collection)configs);
	}

	public ConfigPane(Configurable<T> configs) {
		this(configs.getFields());
	}

	@SuppressWarnings("unchecked")
	@Override
	public final void configure(Collection<Config> configs) {
		if (configs==null) return;

		this.configs.clear();
		stream(configs)
			.sorted(byNC(Config::getGuiName))
			.map(c -> {
				ConfigField cf = ConfigField.create(c);
				cf.onChange = onChange;
				this.configs.add(cf);
				Label l = cf.createLabel();
				l.setMinWidth(250);
				l.setPrefWidth(250);
				l.setMaxWidth(250);
				l.setAlignment(Pos.CENTER_RIGHT);
				l.setTextAlignment(TextAlignment.RIGHT);
				l.setPadding(new Insets(0, 0, 0, 5));
				HBox h = new HBox(20, l,cf.getNode());
				h.setAlignment(CENTER_LEFT);
				HBox.setHgrow(cf.getNode(), ALWAYS);
				return h;
			})
			.toListAndThen(root.getChildren()::setAll);
	}

	public Node getNode() {
		return root;
	}

	public Stream<T> getValues() {
		return configs.stream().map(ConfigField::getValue);
	}

	public List<ConfigField<T>> getValuesC() {
		return configs;
	}

}