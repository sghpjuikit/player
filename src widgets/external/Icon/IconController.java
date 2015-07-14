
package Icon;

import Configuration.Config;
import Configuration.Config.ListAccessor;
import Configuration.IsConfig;
import Configuration.ListConfigurable;
import Layout.Widgets.Widget;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.controller.FXMLController;
import gui.objects.icon.Icon;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import util.access.AccessorAction;

@Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Icon",
    description = "Provides button with customizable action.",
    howto = "Available actions:\n" +
            "    Button click : execute action",
    notes = "",
    version = "0.7",
    year = "2014",
    group = Widget.Group.OTHER
)
public class IconController extends FXMLController {
    
    @IsConfig(name = "Icons", info="List of icons to show")
    private final ListAccessor<Icon> icons = new ListAccessor<>(Icon::new, i ->
        new ListConfigurable(
            Config.fromProperty("Icon", i.icon),
            Config.fromProperty("Action",new AccessorAction(i.getOnClickAction(),a->i.onClick((Runnable)a)))
        )
    );
    @FXML private StackPane root;
    private final FlowPane box = new FlowPane(5,5);
    
    
    private final Label button = new Label("");
    int size = 12;
    
    @Override
    public void init() {
        root.getChildren().add(new javafx.scene.layout.VBox(30,box));
        icons.list.addListener((ListChangeListener.Change<? extends Icon> c) -> box.getChildren().setAll(icons.list));
    }

    @Override
    public void refresh() {}

}