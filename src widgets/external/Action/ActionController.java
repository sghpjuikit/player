
package Action;

import Configuration.IsConfig;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Widget;
import Layout.Widgets.Widget.Info;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import static java.util.stream.Collectors.toList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import util.access.Accessor;
import util.access.AccessorEnum;

/**
 *
 * @author Plutonium_
 */
@Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Action",
    description = "Provides button with customizable action.",
    howto = "Available actions:\n" +
            "    Button click : execute action",
    notes = "",
    version = "0.7",
    year = "2014",
    group = Widget.Group.OTHER
)
public class ActionController extends FXMLController {
    
    @FXML private StackPane root;
    private final Label button = new Label("");
    
    int size = 12;
    
    // configurables
    @IsConfig(name = "Icon", info = "Icon for the action button")
    public final Accessor<AwesomeIcon> icon = new Accessor<>(AwesomeIcon.GAMEPAD, v -> AwesomeDude.setIcon(button, v, String.valueOf(size)));
    
    @IsConfig(name = "Action", info = "Action for the button.")
    // not sure if string instead of action is good idea, but it is safer
    public final AccessorEnum<String> action = new AccessorEnum<String>(Action.EMPTY.getName(), v -> {},
        () -> Action.getActions().stream().map(Action::getName).collect(toList())
    );
    
    @IsConfig(name = "Alignment", info = "Alignment of the button")
    public final Accessor<Pos> align = new Accessor<>(Pos.CENTER, v -> StackPane.setAlignment(button, v));

    
    @Override
    public void init() {
        root.getChildren().add(button);
        root.setOnScroll(Event::consume);
        button.setOnMouseClicked(e-> Action.getAction(action.getValue()).run());
        button.setOnScroll(e -> {
            size += (int)e.getTextDeltaY();
            AwesomeDude.setIcon(button, icon.getValue(), String.valueOf(size));
        });
    }

    @Override
    public void refresh() {
       icon.applyValue();
       action.applyValue();
       align.applyValue();
    }

    @Override
    public void close() { }
    
}
