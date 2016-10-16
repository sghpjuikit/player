package configurator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

import gui.itemnode.ConfigField;
import gui.objects.icon.Icon;
import gui.objects.tree.TreeItems;
import gui.objects.tree.TreeItems.Name;
import gui.pane.ConfigPane;
import layout.widget.Widget;
import layout.widget.controller.ClassController;
import layout.widget.feature.ConfiguringFeature;
import util.conf.Config;
import util.conf.Configurable;
import util.conf.IsConfig;
import util.graphics.fxml.ConventionFxmlLoader;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static java.util.stream.Collectors.toList;
import static javafx.scene.control.SelectionMode.SINGLE;
import static main.App.APP;
import static util.functional.Util.map;

@Widget.Info(
	author = "Martin Polakovic",
	name = "Settings",
	description = "Provides access to application settings",
	howto = "Available actions:\n"
	+ "    Select category\n"
	+ "    Change setting value: Automatically takes change\n"
	+ "    Default : Set default value for this setting\n",
	notes = "To do: generate active widget settings",
	version = "1",
	year = "2016",
	group = Widget.Group.APP
)
public final class Configurator extends ClassController implements ConfiguringFeature {

	@FXML TreeView<Name> groups;
	@FXML Pane controls;
	@FXML AnchorPane configsRootPane;
	private final ConfigPane<Object> configsPane = new ConfigPane<>();
	private final List<Config> configs = new ArrayList<>();

	@IsConfig(name = "Group titles alignment", info = "Alignment of group names.")
	public final ObjectProperty<Pos> title_align = new SimpleObjectProperty<>(Pos.CENTER);
	// TODO: reuires COnfigPane support for this, easy to do, just boring... someone do this pls
	// @IsConfig(name = "Field names alignment", info = "Alignment of field names.")
	// public final V<HPos> alignment = new V<>(RIGHT, v -> groupsOld.forEach((n, g) -> g.grid.getColumnConstraints().get(0).setHalignment(v)));
	// TODO: requires support for injectable widget factory configs to share configs between widget instances of same type
	// @IsConfig(editable = false)
	// public final V<String> expanded = new V<>("", v -> {});

	public Configurator() {
		// create inputs/outputs
		inputs.create("To configure", Configurable.class, this::configure);

		// load fxml part
		new ConventionFxmlLoader(this).loadNoEx();

		// set up graphics
		configsRootPane.getChildren().setAll(configsPane.getNode());
		groups.getSelectionModel().setSelectionMode(SINGLE);
		groups.setCellFactory(TreeItems::buildTreeCell);
		groups.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> {
			boolean isSelection = nv!=null;
			boolean isValueSelected = isSelection && nv.getValue()!=null;
			if (isValueSelected)
				populateConfigFields(configs.stream().filter(f -> f.getGroup().equals(nv.getValue().pathUp)));
		});

		// header icons
		Icon appI = new Icon(HOME,13,"App settings",() -> configure(APP.configuration.getFields())),
			 reI  = new Icon(REFRESH,13,"Refresh all",this::refresh),
			 defI = new Icon(RECYCLE,13,"Set all to default",this::defaults);
		controls.getChildren().addAll(appI,new Label("    "),reI,defI);

		// consume scroll event to prevent other scroll behavior // optional
		setOnScroll(Event::consume);
	}

	/** Set and apply values and refresh if needed (no need for hard refresh). */
	@FXML
	public void ok() {
		configsPane.getValuesC().forEach(ConfigField::apply);
	}

	/** Set default app settings. */
	@FXML
	public void defaults() {
		configsPane.getValuesC().forEach(ConfigField::setNapplyDefault);
	}

	@Override
	public void refresh() {
//		alignment.applyValue();
//		expanded.applyValue();
		refreshConfigs();
	}

	@Override
	public void configure(Collection<Config> c) {
		if (c==null) return;

		configs.clear();
		configs.addAll(c);
		groups.setRoot(TreeItems.tree(Name.treeOfPaths("Groups", map(c,Config::getGroup))));
		groups.getRoot().setExpanded(true);
	}

	private void populateConfigFields(Stream<Config> visibleConfigs) {
		configsPane.configure(visibleConfigs.collect(toList()));
	}

	public void refreshConfigs() {
		configsPane.getValuesC().forEach(ConfigField::refreshItem);
	}

}