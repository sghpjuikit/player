package IconBox;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import util.conf.Config;
import util.conf.Config.VarList;
import util.conf.IsConfig;
import util.conf.ListConfigurable;
import layout.widget.Widget;
import layout.widget.Widget.Info;
import layout.widget.controller.FXMLController;
import layout.widget.feature.HorizontalDock;
import gui.objects.icon.Icon;
import util.access.FunctAccessor;
import util.access.VarAction;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.BUS;
import static util.reactive.Util.maintain;

@Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "IconBox",
    description = "Provides button with customizable action.",
    howto = "Available actions:\n" +
            "    Button click : execute action",
    notes = "",
    version = "0.7",
    year = "2014",
    group = Widget.Group.OTHER
)
public class IconBox extends FXMLController implements HorizontalDock {

    @IsConfig(name = "Icon size", info = "Size of each icon")
    private final DoubleProperty icon_size = new SimpleDoubleProperty(13);
    @IsConfig(name = "Icons", info = "List of icons to show")
    private final VarList<Icon> icons = new VarList<>(() -> {
            Icon i = new Icon(BUS);
            maintain(icon_size,v -> i.size(v.doubleValue()));
            return i;
        }, i ->
        new ListConfigurable(
            Config.forProperty(Icon.class, "Icon", new FunctAccessor<>(i::icon,i::getGlyph)),
            Config.forProperty(String.class, "Action",new VarAction(i.getOnClickAction(),i::onClick))
        )
    );
    @FXML private StackPane root;
    private final FlowPane box = new FlowPane(5,5);


    @Override
    public void init() {
        root.getChildren().add(new VBox(30,box));
        icons.onListInvalid(box.getChildren()::setAll);
    }

    @Override
    public void refresh() {}

}