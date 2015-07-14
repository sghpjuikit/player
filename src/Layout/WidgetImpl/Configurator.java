package Layout.WidgetImpl;

import Configuration.Config;
import Configuration.Configuration;
import Configuration.IsConfig;
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import static Layout.Widgets.Widget.Group.APP;
import Layout.Widgets.controller.ClassController;
import Layout.Widgets.feature.ConfiguringFeature;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.*;
import gui.itemnode.ConfigField;
import gui.objects.icon.Icon;
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
import static javafx.scene.layout.Priority.ALWAYS;
import static util.Util.setAnchors;
import util.access.Accessor;
import static util.functional.Util.byNC;
import static util.functional.Util.list;
import util.graphics.fxml.ConventionFxmlLoader;
@IsWidget
@Widget.Info(
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
    group = APP
)
public final class Configurator extends ClassController implements ConfiguringFeature {

    // gui & state
    @FXML Pane controls;
    @FXML Accordion accordion;
    private final Map<String, ConfigGroup> groups = new HashMap<>();
    private final List<ConfigField> configFields = new ArrayList<>();
    private final boolean isSimple;

    // auto applied configurables
    @IsConfig(name = "Field names alignment", info = "Alignment of field names.")
    public final Accessor<HPos> alignemnt = new Accessor<>(RIGHT, v -> groups.forEach((n, g) -> g.grid.getColumnConstraints().get(0).setHalignment(v)));
    @IsConfig(name = "Group titles alignment", info = "Alignment of group names.")
    public final ObjectProperty<Pos> title_align = new SimpleObjectProperty<>(CENTER);
    @IsConfig(editable = false)
    public final Accessor<String> expanded = new Accessor<>("", v -> {
        if (groups.containsKey(v))
            accordion.setExpandedPane(groups.get(v).pane);
    });
    private final Icon appI = new Icon(HOME,13,"App settings",() -> configure(Configuration.getFields()));
    private final Icon reI = new Icon(REFRESH,13,"Refresh all",this::refresh);
    private final Icon defI = new Icon(RECYCLE,13,"Set all to default",this::defaults);

    
    public Configurator() {
        // creating widget loads controller's no-arg construstor - this one,
        // and we need widget to be in non-simple mode, hence param==false
        this(false);
    }
    /**
     * @param simple simple mode==true hides home button and categories
     */
    public Configurator(boolean simple) {
        isSimple = simple;
        
        // load fxml part
        new ConventionFxmlLoader(this).loadNoEx();
        controls.getChildren().addAll(appI,new Region(),reI,defI);
        if(simple) controls.getChildren().remove(appI);
        
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
//        configFields.forEachBoth(ConfigField::setNapplyDefault);
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
        groups.values().forEach(ConfigGroup::dispose);
        groups.clear();

        // sort & populate fields
        ConfigGroup oneg = isSimple ? new ConfigGroup("") : null;
        c.stream().sorted(byNC(o -> o.getGuiName())).forEach(f -> {
            // create graphics
            ConfigField cf = ConfigField.create(f);
            configFields.add(cf);

            // get group
            String cat = f.getGroup();
            ConfigGroup g = isSimple ? oneg : groups.containsKey(cat) ? groups.get(cat) : new ConfigGroup(cat);

            // add to grid
            g.grid.getRowConstraints().add(new RowConstraints());
            g.grid.add(cf.getLabel(), 0, g.grid.getRowConstraints().size()-1);
            g.grid.add(cf.getNode(), 2, g.grid.getRowConstraints().size()-1);
        });

        // autoexpand
        boolean single = groups.size()==1;
        accordion.setVisible(!single);
        ((AnchorPane)accordion.getParent()).getChildren().retainAll(accordion);
        if(single) {
            Pane t = list(groups.values()).get(0).grid;
            ((AnchorPane)accordion.getParent()).getChildren().add(t);
            setAnchors(t,0);
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
            grid.setHgap(5);

            ColumnConstraints c1 = new ColumnConstraints(80,-1,-1,ALWAYS,alignemnt.get(),true);
            ColumnConstraints gap = new ColumnConstraints(0);
            ColumnConstraints c2 = new ColumnConstraints(50,-1,-1,ALWAYS,alignemnt.get(),true);
            grid.getColumnConstraints().addAll(c1, gap, c2);

            groups.put(name, this);
        }

        String name() {
            return pane.getText();
        }
        
        void dispose() {
            pane.alignmentProperty().unbind();
        }
        
        void autosize() {
//            grid.getColumnConstraints().stream().mapToDouble(c->c.g)
        }
    }

}
