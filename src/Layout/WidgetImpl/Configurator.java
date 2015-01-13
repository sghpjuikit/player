package Layout.WidgetImpl;

import Configuration.Configuration;
import Configuration.IsConfig;
import GUI.ItemHolders.ConfigField;
import Layout.Widgets.ClassWidget;
import Layout.Widgets.Controller;
import Layout.Widgets.Features.ConfiguringFeature;
import Layout.Widgets.Widget;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import util.Log;
import util.access.Accessor;
import static util.functional.FunctUtil.cmpareNoCase;

@Layout.Widgets.Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Settings",
    description = "Provides access to application settings",
    howto = "Available actions:\n" +
            "    Select category\n" +
            "    Change setting value: Automatically takes change\n" +
            "    OK : Applies any unapplied change\n" +
            "    Default : Set default value for this setting\n",
    notes = "To do: generate active widget settings, allow subcategories.",
    version = "0.9",
    year = "2014",
    group = Widget.Group.APP
)
public final class Configurator extends AnchorPane implements Controller<ClassWidget>, ConfiguringFeature {
    
    // gui & state
    @FXML Accordion accordion;
    private final Map<String,ConfigGroup> groups = new HashMap();
    private final List<ConfigField> configFields = new ArrayList();
    
    // auto applied configurables
    @IsConfig(name = "Field names alignment", info = "Alignment of field names.")
    public final Accessor<HPos> alignemnt = new Accessor<>(HPos.RIGHT, v -> groups.forEach((n,g) -> g.grid.getColumnConstraints().get(1).setHalignment(v)));
    @IsConfig(name = "Group titles alignment", info = "Alignment of group names.")
    public final Accessor<Pos> title_align = new Accessor<>(Pos.CENTER, v -> groups.forEach((n,g) -> g.pane.setAlignment(v)));
    @IsConfig(editable = false)
    public final Accessor<String> expanded = new Accessor<>("", v -> {
        if(groups.containsKey(v))
            accordion.setExpandedPane(groups.get(v).pane);
    });
    
    public Configurator() {
        super();
        FXMLLoader fxmlLoader = new FXMLLoader(Configurator.class.getResource("Configurator.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException ex) {
            Log.err("ConfiguratorComponent source data coudlnt be read.");
        }
        
        // clear previous fields
        configFields.clear();
        accordion.getPanes().clear();
        groups.forEach((n,g) -> g.grid.getChildren().clear());
        groups.forEach((n,g) -> g.grid.getRowConstraints().clear());
        
        // sort & populate fields
        Configuration.getFields().stream().sorted(cmpareNoCase(o->o.getGuiName())).forEach(f-> {
            if (f.isEditable()) {        // ignore noneditabe
                // create graphics
                ConfigField cf = ConfigField.create(f);
                configFields.add(cf);
                
                // get group
                String cat = f.getGroup();
                ConfigGroup g = groups.containsKey(cat) ? groups.get(cat) : new ConfigGroup(cat);
                
                // add to grid
                g.grid.getRowConstraints().add(new RowConstraints());
                g.grid.add(cf.getLabel(), 1, g.grid.getRowConstraints().size());  
                g.grid.add(cf.getControl(), 2, g.grid.getRowConstraints().size());
            }
        });
        
        // sort & populate groups
        groups.values().stream()
                .sorted(cmpareNoCase(ConfigGroup::name))
                .forEach(g -> accordion.getPanes().add(g.pane));
        
        // consume scroll event to prevent other scroll behavior // optional
        setOnScroll(Event::consume);
    }
    
    public void initialize() {
        // do nothing here, we simply follow the contract - fxmlLoader needs this method
    }

/****************************** PUBLIC API ************************************/
    
    /** Set and apply values and refresh if needed (no need for hard refresh) */
    @FXML public void ok() {
        configFields.forEach(ConfigField::applyNsetIfNeed);
    }
    
    /** Set default app settings. */
    @FXML public void defaults() {
        // use this for now
        Configuration.toDefault();
        refresh();
        // bug with empty default shortcut?
//        configFields.forEach(ConfigField::setNapplyDefault);
    }
    
    @Override
    public void refresh() {
        alignemnt.applyValue();
        title_align.applyValue();
        expanded.applyValue();
        // refresh values
        configFields.forEach(ConfigField::refreshItem);
    }
    
/******************************************************************************/
    
    private ClassWidget w;

    @Override public void setWidget(ClassWidget w) {
        this.w = w;
    }

    @Override public ClassWidget getWidget() {
        return w;
    }

/****************************** HELPER METHODS ********************************/
        
    private final class ConfigGroup {
        final TitledPane pane = new TitledPane();
        final GridPane grid = new GridPane();
        
        public ConfigGroup(String name) {
            pane.setText(name);
            pane.setContent(grid);
            pane.expandedProperty().addListener((o,ov,nv) ->
                expanded.setValue(nv ? pane.getText() : ""));
            
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
            grid.getColumnConstraints().addAll(gap,c1,c2);
            
            groups.put(name, this);
        } 
        
        public String name() {
            return pane.getText();
        }
    }
    
}