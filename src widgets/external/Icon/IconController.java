
package Icon;

import Configuration.Config;
import Configuration.Config.ListAccessor;
import Configuration.IsConfig;
import Configuration.ListConfigurable;
import Layout.Widgets.Widget;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.controller.FXMLController;
import gui.objects.icon.Icon;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
    
    
    @IsConfig(name = "Icon size", info = "Size of each icon")
    private final IntegerProperty icon_size = new SimpleIntegerProperty(13);
    @IsConfig(name = "Icons", info = "List of icons to show")
    private final ListAccessor<Icon> icons = new ListAccessor<>(() -> {
            Icon i = new Icon();
            i.icon_size.set(icon_size.get());
            i.icon_size.bind(icon_size);
            return i;
        }, i ->
        new ListConfigurable(
            Config.forProperty("Icon", i.icon),
            Config.forProperty("Action",new AccessorAction(i.getOnClickAction(),i::onClick))
        )
    );
    @FXML private StackPane root;
    private final FlowPane box = new FlowPane(5,5);
    
    
    private final Label button = new Label("");
    int size = 12;
    
    @Override
    public void init() {
        root.getChildren().add(new VBox(30,box));
        icons.onInvalid(box.getChildren()::setAll);
        icon_size.addListener((o,ov,nv) -> System.out.println("1 " + nv));
        icon_size.addListener((o) -> System.out.println("2 " + icon_size.getValue()));
    }

    @Override
    public void refresh() {}

}