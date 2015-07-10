
package Icon;

import Configuration.Config;
import static action.Action.EMPTY;
import Layout.Widgets.FXMLWidget;
import Layout.Widgets.Widget;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.controller.FXMLController;
import action.Action;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import gui.itemnode.ChainConfigField.ListConfigField;
import gui.itemnode.ConfigField;
import gui.itemnode.ItemNode.ValueNode;
import gui.objects.icon.Icon;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import util.access.Accessor;
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
    
    @FXML private StackPane root;
    FlowPane box = new FlowPane(5,5);
    List<Icon> icons = new ArrayList();
    ListConfigField<IconData, IconConfig> icons_config = new ListConfigField<>(IconConfig::new);
    
    private final Label button = new Label("");
    int size = 12;
    
//    // configurables
//    @IsConfig(name = "Icon", info = "Icon for the action button")
//    public final Accessor<FontAwesomeIconName> icon = new Accessor<>(GAMEPAD, v -> Icons.setIcon(button, v, String.valueOf(size)));
//    @IsConfig(name = "Action", info = "Action for the button.")
//    public final AccessorAction action = new AccessorAction(EMPTY, button::setOnMouseClicked);
//    @IsConfig(name = "Alignment", info = "Alignment of the button")
//    public final Accessor<Pos> align = new Accessor<>(CENTER, v -> StackPane.setAlignment(button, v));

    
    public IconController(FXMLWidget widget) {
        super(widget);
    }
    
    @Override
    public void init() {
        root.getChildren().add(new javafx.scene.layout.VBox(30,box,icons_config.getNode()));
//        StackPane.setAlignment(box, CENTER);

//        root.setOnScroll(Event::consume);
//        root.setOnScroll(e -> {
//            size += (int)e.getTextDeltaY();
//            Icons.setIcon(button, icon.getValue(), String.valueOf(size));
//        });

        root.addEventFilter(MOUSE_CLICKED, e -> {
            if(e.getClickCount()==2) {
                box.getChildren().clear();
                icons_config.getValues()
                        .map(id -> new Icon(id.icon,13,id.action.getInfo(),(Runnable)id.action))
                        .forEach(box.getChildren()::add);
            }
        });
    }

    @Override
    public void refresh() {
//       icon.applyValue();
//       action.applyValue();
//       align.applyValue();
    }
    
    
    class IconData {
        Action action;
        FontAwesomeIconName icon;

        public IconData(Action action, FontAwesomeIconName icon) {
            this.action = action;
            this.icon = icon;
        }
    }
    
    class IconConfig extends ValueNode<IconData> {
        AccessorAction nameA;
        Accessor<FontAwesomeIconName> iconA;
        ConfigField<String> configfieldA;
        ConfigField<FontAwesomeIconName> configfieldB;
        HBox root;
        
        public IconConfig() {
            nameA = new AccessorAction(EMPTY, this::generateValue);
            iconA = new Accessor<>(FontAwesomeIconName.ADJUST, this::generateValue);
            configfieldA = ConfigField.create(Config.fromProperty("", nameA));
            configfieldB = ConfigField.create(Config.fromProperty("", iconA));
            root = new HBox(5, configfieldA.getNode(),configfieldB.getNode());
        }
        
        void generateValue(Object o) {
            changeValue(getValue());
        }
        
        @Override
        public IconData getValue() {
            return new IconData(Action.getAction(configfieldA.getValue()), configfieldB.getValue());
        }

        @Override
        public Node getNode() {
            return root;
        }
    }
}




//
//package Icon;
//
//import static action.Action.EMPTY;
//import Configuration.IsConfig;
//import Layout.Widgets.FXMLWidget;
//import Layout.Widgets.Widget;
//import Layout.Widgets.Widget.Info;
//import Layout.Widgets.controller.FXMLController;
//import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
//import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.GAMEPAD;
//import javafx.event.Event;
//import javafx.fxml.FXML;
//import javafx.geometry.Pos;
//import static javafx.geometry.Pos.CENTER;
//import javafx.scene.control.Label;
//import javafx.scene.layout.StackPane;
//import util.access.Accessor;
//import util.access.AccessorAction;
//import util.graphics.Icons;
//
//@Info(
//    author = "Martin Polakovic",
//    programmer = "Martin Polakovic",
//    name = "Icon",
//    description = "Provides button with customizable action.",
//    howto = "Available actions:\n" +
//            "    Button click : execute action",
//    notes = "",
//    version = "0.7",
//    year = "2014",
//    group = Widget.Group.OTHER
//)
//public class IconController extends FXMLController {
//    
//    @FXML private StackPane root;
//    private final Label button = new Label("");
//    int size = 12;
//    
//    // configurables
//    @IsConfig(name = "Icon", info = "Icon for the action button")
//    public final Accessor<FontAwesomeIconName> icon = new Accessor<>(GAMEPAD, v -> Icons.setIcon(button, v, String.valueOf(size)));
//    @IsConfig(name = "Action", info = "Action for the button.")
//    public final AccessorAction action = new AccessorAction(EMPTY, button::setOnMouseClicked);
//    @IsConfig(name = "Alignment", info = "Alignment of the button")
//    public final Accessor<Pos> align = new Accessor<>(CENTER, v -> StackPane.setAlignment(button, v));
//
//    
//    public IconController(FXMLWidget widget) {
//        super(widget);
//    }
//    
//    @Override
//    public void init() {
//        root.getChildren().add(button);
//        root.setOnScroll(Event::consume);
//        button.setOnScroll(e -> {
//            size += (int)e.getTextDeltaY();
//            Icons.setIcon(button, icon.getValue(), String.valueOf(size));
//        });
//    }
//
//    @Override
//    public void refresh() {
//       icon.applyValue();
//       action.applyValue();
//       align.applyValue();
//    }
//
//}
