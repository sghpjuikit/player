
package Icon;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import Configuration.Config;
import Configuration.Config.ListAccessor;
import Configuration.IsConfig;
import Configuration.ListConfigurable;
import Layout.Widgets.Widget;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.controller.FXMLController;
import gui.objects.icon.Icon;
import util.access.AccessorAction;
import util.access.FunctAccessor;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.BUS;
import static util.reactive.Util.maintain;

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
    private final DoubleProperty icon_size = new SimpleDoubleProperty(13);
    @IsConfig(name = "Icons", info = "List of icons to show")
    private final ListAccessor<Icon> icons = new ListAccessor<>(() -> {
            Icon i = new Icon(BUS);
            maintain(icon_size,v -> i.size(v.doubleValue()));
            return i;
        }, i ->
        new ListConfigurable(
            Config.forProperty("Icon", new FunctAccessor<>(i::icon,i::getIco)),
            Config.forProperty("Action",new AccessorAction(i.getOnClickAction(),i::onClick))
        )
    );
    @FXML private StackPane root;
    private final FlowPane box = new FlowPane(5,5);
    
    
    @Override
    public void init() {
        root.getChildren().add(new VBox(30,box));
        icons.onInvalid(box.getChildren()::setAll);
    }

    @Override
    public void refresh() {}

}