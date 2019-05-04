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
import sp.it.util.access.FAccessor;
import sp.it.util.access.VarAction;
import sp.it.util.conf.Config;
import sp.it.util.conf.Config.VarList;
import sp.it.util.conf.IsConfig;
import sp.it.util.conf.ListConfigurable;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.BUS;
import static sp.it.pl.main.AppExtensionsKt.scaleEM;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.reactive.UtilKt.syncC;

@Info(
    author = "Martin Polakovic",
    name = "IconBox",
    description = "Provides button with customizable action.",
    howto = "Available actions:\n" +
            "    Button click : execute action",
    version = "0.7.0",
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
            syncC(icon_size, v -> i.size(v.doubleValue()));
            return i;
        },
        i -> new ListConfigurable<>(
            (Config) Config.forProperty(Icon.class, "Icon", new FAccessor<>(i::icon, i::getGlyph)),
            (Config) Config.forProperty(String.class, "Action", new VarAction(i.getOnClickAction(), consumer(i::onClick)))
        )
    );

    public IconBox(Widget widget) {
        super(widget);
        root.setPrefSize(scaleEM(400), scaleEM(50));

        FlowPane box = new FlowPane(5,5);
        root.getChildren().add(new VBox(30,box));

        icons.onListInvalid(box.getChildren()::setAll);
    }

}