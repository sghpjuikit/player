package iconBox;

import de.jensd.fx.glyphs.GlyphIcons;
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
import sp.it.util.access.VarAction;
import sp.it.util.access.VarEnum;
import sp.it.util.conf.ConfList;
import sp.it.util.conf.Config;
import sp.it.util.conf.IsConfig;
import sp.it.util.conf.ListConfigurable;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.BUS;
import static sp.it.pl.main.AppExtensionsKt.getEmScaled;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UtilKt.onChange;
import static sp.it.util.reactive.UtilKt.syncC;

@Info(
    author = "Martin Polakovic",
    name = "IconBar",
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

    @SuppressWarnings({"RedundantCast", "unchecked"})
    @IsConfig(name = "Icons", info = "List of icons to show")
    private final ConfList<Icon> icons = new ConfList<>(
        Icon.class,
        () -> {
            Icon i = new Icon(BUS);
            syncC(icon_size, v -> i.size(v.doubleValue()));
            return i;
        },
        i -> new ListConfigurable<Object>(
            (Config) Config.forProperty(GlyphIcons.class, "Icon", new VarEnum<>(i.getGlyph(), Icon.GLYPHS).initAttachC(i::icon)),
            (Config) Config.forProperty(String.class, "Action", new VarAction(i.getOnClickAction(), consumer(i::onClick)))
        ),
        false
    );

    public IconBox(Widget widget) {
        super(widget);
        root.setPrefSize(getEmScaled(400), getEmScaled(50));

        FlowPane box = new FlowPane(5,5);
        root.getChildren().add(new VBox(30,box));

        onChange(icons.list, runnable(() -> box.getChildren().setAll(icons.list)));
    }

}