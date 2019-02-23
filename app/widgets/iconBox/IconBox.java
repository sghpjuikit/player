package iconBox;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.Widget.Info;
import sp.it.pl.layout.widget.controller.LegacyController;
import sp.it.pl.layout.widget.controller.SimpleController;
import sp.it.pl.layout.widget.feature.HorizontalDock;
import sp.it.pl.util.access.FAccessor;
import sp.it.pl.util.access.VarAction;
import sp.it.pl.util.conf.Config;
import sp.it.pl.util.conf.Config.VarList;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.conf.ListConfigurable;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.BUS;
import static sp.it.pl.util.reactive.UtilKt.maintain;

@Info(
    author = "Martin Polakovic",
    name = "IconBox",
    description = "Provides button with customizable action.",
    howto = "Available actions:\n" +
            "    Button click : execute action",
    version = "0.7",
    year = "2014",
    group = Widget.Group.OTHER
)
@LegacyController
public class IconBox extends SimpleController implements HorizontalDock {

    @IsConfig(name = "Icon size", info = "Size of each icon")
    private final DoubleProperty icon_size = new SimpleDoubleProperty(13);

    @SuppressWarnings("unchecked")
    @IsConfig(name = "Icons", info = "List of icons to show")
    private final VarList<Icon> icons = new VarList<>(
        Icon.class,
        () -> {
            Icon i = new Icon(BUS);
            maintain(icon_size, v -> i.size(v.doubleValue()));
            return i;
        },
        i -> new ListConfigurable<>(
            (Config) Config.forProperty(Icon.class, "Icon", new FAccessor<>(i::icon, i::getGlyph)),
            (Config) Config.forProperty(String.class, "Action", new VarAction(i.getOnClickAction(), i::onClick))
        )
    );

    public IconBox(Widget widget) {
        super(widget);

        FlowPane box = new FlowPane(5,5);
        root.getChildren().add(new VBox(30,box));

        icons.onListInvalid(box.getChildren()::setAll);
    }

}