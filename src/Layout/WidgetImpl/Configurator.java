package Layout.WidgetImpl;

import Configuration.Configuration;
import Configuration.IsConfig;
import GUI.ItemHolders.ConfigField;
import Layout.Widgets.ClassWidget;
import Layout.Widgets.Controller;
import Layout.Widgets.Features.ConfiguringFeature;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import utilities.Log;
import utilities.Util;

@WidgetInfo(
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
    version = "0.8",
    year = "2014",
    group = Widget.Group.APP
)
public final class Configurator extends AnchorPane implements Controller<ClassWidget>, ConfiguringFeature {
        
    @FXML Accordion accordion;
    List<ConfigGroup> groups = new ArrayList();
    List<ConfigField> configFields = new ArrayList();
    
    @IsConfig(name = "Show non editable fields", info = "Include non read-only fields.")
    public boolean show_noneditable = false;
    @IsConfig(name = "Field names alignment", info = "Alignment of field names.")
    public HPos alignemnt = HPos.RIGHT;
    @IsConfig(name = "Group titles alignment", info = "Alignment of group names.")
    public Pos title_align = Pos.CENTER;
    @IsConfig(editable = false)
    public String expanded = "";
    
    public Configurator() {
        FXMLLoader fxmlLoader = new FXMLLoader(Configurator.class.getResource("Configurator.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException ex) {
            Log.err("ConfiguratorComponent source data coudlnt be read.");
        }
        
        // consume scroll event to prevent other scroll behavior // optional
        setOnScroll(Event::consume);
    }
    
    @FXML
    public void initialize() {
        
        // clear previous fields
        configFields.clear();
        accordion.getPanes().clear();
        groups.forEach(g->g.grid.getChildren().clear());
        groups.forEach(g->g.grid.getRowConstraints().clear());
        
        // sort & populate fields
        Configuration.getFields().stream().sorted(Util.cmpareNoCase(o->o.getGuiName())).forEach(f-> {
            ConfigGroup g = getGroup(f.getGroup());                             // find group
            ConfigField cf = ConfigField.create(f);                             // create
            if (show_noneditable || f.isEditable()) {        // ignore noneditabe
                configFields.add(cf);
                g.grid.getRowConstraints().add(new RowConstraints());
                g.grid.add(cf.getLabel(), 1, g.grid.getRowConstraints().size());  
                g.grid.add(cf.getControl(), 2, g.grid.getRowConstraints().size());
            }
        });
        
        // sort & populate groups
        groups.stream().sorted(Util.cmpareNoCase(g -> g.name()))
                       .forEach(g -> accordion.getPanes().add(g.pane));
        
        // expand     
        for(ConfigGroup g: groups) 
            if(g.name().equals(expanded)) {
                accordion.setExpandedPane(g.pane);
                break;
            }
        
    }
    
    @FXML
    private void ok() {
        // set and apply values and refresh if needed (no need for hard refresh)
        configFields.forEach(ConfigField::applyNsetIfAvailable);
    }
    
    @FXML
    private void defaults() {
        // use this for now
        Configuration.toDefault();
        refresh();
        // bug with empty default shortcut?
//        configFields.forEach(ConfigField::setNapplyDefault);
    }

/******************************************************************************/
    
    private ClassWidget w;
    
    @Override public void refresh() {
        // refresh values
        configFields.forEach(ConfigField::refreshItem);
        
        // relayout layout
        groups.forEach(g -> g.grid.getColumnConstraints().get(1).setHalignment(alignemnt));
        groups.forEach(g -> g.pane.setAlignment(title_align));
        
        // expand remembered
        for(ConfigGroup g: groups) 
            if(g.name().equals(expanded)) {
                accordion.setExpandedPane(g.pane);
                break;
            }
    }

    @Override public void setWidget(ClassWidget w) {
        this.w = w;
    }

    @Override public ClassWidget getWidget() {
        return w;
    }

/******************************************************************************/
    
    private final class ConfigGroup {
        final TitledPane pane = new TitledPane();
        final GridPane grid = new GridPane();
        
        public ConfigGroup(String name) {
            pane.setText(name);
            pane.setContent(grid);
            pane.expandedProperty().addListener((o,ov,nv) ->
                expanded = nv ? pane.getText() : "" );
            
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
            
            groups.add(this);
        } 
        
        public String name() {
            return pane.getText();
        }
    }
    
    private ConfigGroup getGroup(String category) {
        for (ConfigGroup g: groups) {
            if(g.name().equals(category))
                return g;
        }
        return new ConfigGroup(category);
    }
    
}