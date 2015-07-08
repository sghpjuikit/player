
package Icon;

import static action.Action.EMPTY;
import Configuration.IsConfig;
import Layout.Widgets.FXMLWidget;
import Layout.Widgets.Widget;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.controller.FXMLController;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.GAMEPAD;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import static javafx.geometry.Pos.CENTER;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import util.access.Accessor;
import util.access.AccessorAction;
import util.graphics.Icons;

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
    
    @FXML private StackPane root;
    private final Label button = new Label("");
    int size = 12;
    
    // configurables
    @IsConfig(name = "Icon", info = "Icon for the action button")
    public final Accessor<FontAwesomeIconName> icon = new Accessor<>(GAMEPAD, v -> Icons.setIcon(button, v, String.valueOf(size)));
    @IsConfig(name = "Action", info = "Action for the button.")
    public final AccessorAction action = new AccessorAction(EMPTY, button::setOnMouseClicked);
    @IsConfig(name = "Alignment", info = "Alignment of the button")
    public final Accessor<Pos> align = new Accessor<>(CENTER, v -> StackPane.setAlignment(button, v));

    
    public IconController(FXMLWidget widget) {
        super(widget);
    }
    
    @Override
    public void init() {
        root.getChildren().add(button);
        root.setOnScroll(Event::consume);
        button.setOnScroll(e -> {
            size += (int)e.getTextDeltaY();
            Icons.setIcon(button, icon.getValue(), String.valueOf(size));
        });
    }

    @Override
    public void refresh() {
       icon.applyValue();
       action.applyValue();
       align.applyValue();
    }

}
