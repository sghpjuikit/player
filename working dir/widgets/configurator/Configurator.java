package configurator;

import gui.itemnode.ConfigField;
import gui.objects.icon.Icon;
import gui.objects.tree.TreeItems;
import gui.objects.tree.TreeItems.Name;
import gui.pane.ConfigPane;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import layout.widget.Widget;
import layout.widget.controller.ClassController;
import layout.widget.feature.ConfiguringFeature;
import main.App;
import util.conf.Config;
import util.conf.Configurable;
import util.graphics.fxml.ConventionFxmlLoader;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.HOME;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.RECYCLE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.REFRESH;
import static java.util.stream.Collectors.toList;
import static javafx.scene.control.SelectionMode.SINGLE;
import static main.App.APP;
import static util.functional.Util.map;
import static util.functional.Util.stream;
import static util.functional.UtilKt.seqRec;
import static util.graphics.Util.expandAndSelectTreeItem;

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
	private final ConfigPane<?> configsPane = new ConfigPane<>();
	private final List<Config> configs = new ArrayList<>();
	private final String CONFIG_SELECTION_NAME = "app.settings.selected_group";

	public Configurator() {
		// create inputs/outputs
		inputs.create("To configure", Configurable.class, this::configure);

		// load fxml part
		new ConventionFxmlLoader(this).loadNoEx();

		// set up graphics
		configsRootPane.getChildren().setAll(configsPane);
		groups.getSelectionModel().setSelectionMode(SINGLE);
		groups.setCellFactory(TreeItems::buildTreeCell);
		groups.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
			if (storeSelection(nv))
				populateConfigFields(configs.stream().filter(f -> f.getGroup().equals(nv.getValue().pathUp)));
		});

		// header icons
		Icon appI = new Icon(HOME, 13, "App settings", () -> configure(APP.configuration.getFields())),
			 reI = new Icon(REFRESH, 13, "Refresh all", this::refresh),
			 defI = new Icon(RECYCLE, 13, "Set all to default", this::defaults);
		controls.getChildren().addAll(appI, new Label("    "), reI, defI);

		// consume scroll event to prevent other scroll behavior // optional
		setOnScroll(Event::consume);
	}

	/** Set and apply values and refresh if needed (no need for hard refresh). */
	@FXML
	public void ok() {
		configsPane.getConfigFields().forEach(ConfigField::apply);
	}

	/** Set default app settings. */
	@FXML
	public void defaults() {
		configsPane.getConfigFields().forEach(ConfigField::setNapplyDefault);
	}

	@Override
	public void refresh() {
		refreshConfigs();
	}

	@Override
	public void configure(Collection<Config> c) {
		if (c==null) return;

		configs.clear();
		configs.addAll(c);
		groups.setRoot(TreeItems.tree(Name.treeOfPaths("Groups", map(c, Config::getGroup))));
		restoreSelection();		// invokes #populateConfigFields
	}

	private void populateConfigFields(Stream<? extends Config> visibleConfigs) {
		configsPane.configure((Collection<? extends Config<?>>) visibleConfigs.collect(toList()));
	}

	public void refreshConfigs() {
		configsPane.getConfigFields().forEach(cf -> cf.refreshItem());
	}

	private boolean storeSelection(TreeItem<Name> item) {
		boolean isItemSelected = item!=null;
		boolean isValueSelected = isItemSelected && item.getValue()!=null;
		App.APP.configuration.rawAddProperty(CONFIG_SELECTION_NAME, isValueSelected ? item.getValue().pathUp : "");
		return isValueSelected;
	}


	private void restoreSelection() {
		Optional.ofNullable(App.APP.configuration.rawGet().get(CONFIG_SELECTION_NAME))
				.filter(String.class::isInstance)
				.flatMap(restoredSelection -> stream(seqRec(groups.getRoot(), i -> i.getChildren()))
						.filter(item -> item.getValue().pathUp.equals(restoredSelection))
						.findAny()
				)
				.or(() -> Optional.of(groups.getRoot()))
				.ifPresent(item -> expandAndSelectTreeItem(groups, item));		// invokes #populateConfigFields
	}

}