package Layout.WidgetImpl;

import Configuration.Config;
import Configuration.Configuration;
import Configuration.IsConfig;
import gui.ItemNode.ConfigField;
import gui.objects.Icon;
import Layout.Widgets.ClassWidgetController;
import Layout.Widgets.Features.ConfiguringFeature;
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.*;
import java.util.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import static javafx.geometry.HPos.RIGHT;
import javafx.geometry.Pos;
import static javafx.geometry.Pos.CENTER;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.*;
import static util.Util.setAnchors;
import util.access.Accessor;
import static util.functional.Util.byNC;
import static util.functional.Util.list;
import util.graphics.fxml.ConventionFxmlLoader;
@IsWidget
@Layout.Widgets.Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Settings",
    description = "Provides access to application settings",
    howto = "Available actions:\n"
    + "    Select category\n"
    + "    Change setting value: Automatically takes change\n"
    + "    OK : Applies any unapplied change\n"
    + "    Default : Set default value for this setting\n",
    notes = "To do: generate active widget settings, allow subcategories.",
    version = "1",
    year = "2015",
    group = Widget.Group.APP
)
public final class Configurator extends ClassWidgetController implements ConfiguringFeature {

    // gui & state
    @FXML Pane controls;
    @FXML Accordion accordion;
    private final Map<String, ConfigGroup> groups = new HashMap();
    private final List<ConfigField> configFields = new ArrayList();

    // auto applied configurables
    @IsConfig(name = "Field names alignment", info = "Alignment of field names.")
    public final Accessor<HPos> alignemnt = new Accessor<>(RIGHT, v -> groups.forEach((n, g) -> g.grid.getColumnConstraints().get(1).setHalignment(v)));
    @IsConfig(name = "Group titles alignment", info = "Alignment of group names.")
    public final ObjectProperty<Pos> title_align = new SimpleObjectProperty(CENTER);
    @IsConfig(editable = false)
    public final Accessor<String> expanded = new Accessor<>("", v -> {
        if (groups.containsKey(v))
            accordion.setExpandedPane(groups.get(v).pane);
    });
    private final Icon appI = new Icon(HOME,13,"App settings",() -> configure(Configuration.getFields()));
    private final Icon reI = new Icon(REFRESH,13,"Refresh all",this::refresh);
    private final Icon defI = new Icon(RECYCLE,13,"Set all to default",this::defaults);

    public Configurator() {
        // load fxml part
        new ConventionFxmlLoader(this).loadNoEx();
        controls.getChildren().addAll(appI,new Region(),reI,defI);
        
        // init content
        configure(Configuration.getFields());
        
        // consume scroll event to prevent other scroll behavior // optional
        setOnScroll(Event::consume);
    }
    
    /** Set and apply values and refresh if needed (no need for hard refresh) */
    @FXML
    public void ok() {
        configFields.forEach(ConfigField::apply);
    }

    /** Set default app settings. */
    @FXML
    public void defaults() {
        // use this for now
        Configuration.toDefault();
        refresh();
        // bug with empty default shortcut?
//        configFields.forEach(ConfigField::setNapplyDefault);
    }

    @Override
    public void refresh() {
        alignemnt.applyValue();
        expanded.applyValue();
        refreshConfigs();
    }

    @Override
    public void configure(Collection<Config> c) {
        // clear previous fields
        configFields.clear();
        accordion.getPanes().clear();
        groups.clear();

        // sort & populate fields
        c.stream().sorted(byNC(o -> o.getGuiName())).forEach(f -> {
            // create graphics
            ConfigField cf = ConfigField.create(f);
            configFields.add(cf);

            // get group
            String cat = f.getGroup();
            ConfigGroup g = groups.containsKey(cat) ? groups.get(cat) : new ConfigGroup(cat);

            // add to grid
            g.grid.getRowConstraints().add(new RowConstraints());
            g.grid.add(cf.getLabel(), 1, g.grid.getRowConstraints().size());
            g.grid.add(cf.getNode(), 2, g.grid.getRowConstraints().size());
        });


//        // autoexpand group if only one
//        if(groups.size()==1) accordion.setExpandedPane(list(groups.values()).get(0).pane);
        
        boolean single = groups.size()==1;
        accordion.setVisible(!single);
        ((AnchorPane)accordion.getParent()).getChildren().retainAll(accordion);
        if(single) {
            Pane t = list(groups.values()).get(0).grid;
            ((AnchorPane)accordion.getParent()).getChildren().add(t);
            setAnchors(t,0);
//            TitledPane t = list(groups.values()).get(0).pane;
//            ((AnchorPane)accordion.getParent()).getChildren().add(t);
//            setAnchors(t,0);
//            t.setCollapsible(false);
            
        } else {
            groups.values().stream()
                .sorted(byNC(ConfigGroup::name))
                .forEach(g -> accordion.getPanes().add(g.pane));
        }
        alignemnt.applyValue();
    }
    
    public void refreshConfigs() {
        configFields.forEach(ConfigField::refreshItem);
    }

/******************************************************************************/
    
    class ConfigGroup {

        final TitledPane pane = new TitledPane();
        final GridPane grid = new GridPane();

        ConfigGroup(String name) {
            pane.setText(name);
            pane.setContent(grid);
            pane.expandedProperty().addListener((o, ov, nv) -> {
                if(nv) expanded.setValue(pane.getText());
            });
            pane.alignmentProperty().bind(title_align);

            grid.setVgap(3);
            grid.setHgap(10);

            ColumnConstraints gap = new ColumnConstraints(20);
            ColumnConstraints c1 = new ColumnConstraints(200);
            c1.setMaxWidth(-1);
            c1.setMinWidth(-1);
            c1.setPrefWidth(-1);
            c1.setHgrow(Priority.ALWAYS);
            c1.setFillWidth(true);
            ColumnConstraints c2 = new ColumnConstraints(200);
            c2.setMaxWidth(-1);
            c2.setMinWidth(-1);
            c2.setPrefWidth(-1);
            c2.setHgrow(Priority.ALWAYS);
            c2.setFillWidth(true);
            grid.getColumnConstraints().addAll(gap, c1, c2);

            groups.put(name, this);
        }

        String name() {
            return pane.getText();
        }
    }

}
