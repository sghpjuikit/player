/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Labeled;
import static javafx.scene.input.DragEvent.DRAG_ENTERED;
import static javafx.scene.input.DragEvent.DRAG_EXITED;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import util.TODO;
import static util.TODO.Purpose.FUNCTIONALITY;

/**
 <p>
 @author Plutonium_
 */
@TODO(purpose = FUNCTIONALITY, note = "decide best functionality, complete + clean up")
public class ActionChooser extends StackPane {

    public final Text description = new Text();
    public final HBox actionBox;

    private int icon_size = 80;

    public ActionChooser() {
        setAlignment(Pos.CENTER);

        actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER);

        description.setTextAlignment(TextAlignment.CENTER);
//        description.wrappingWidthProperty().bind(actPane.widthProperty());
//        description.setWrappingWidthNatural(true);

//        StackPane h2 = new StackPane(descL);
//                  h2.setAlignment(TOP_CENTER);
        description.setWrappingWidth(150);
//        descL.wrappingWidthProperty().bind(actPane.widthProperty());
        getChildren().addAll(actionBox, description);
        StackPane.setMargin(description, new Insets(20));
        StackPane.setAlignment(description, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(actionBox, Pos.CENTER);
    }

    public void setIconSize(int v) {
        icon_size = v;
        actionBox.getChildren().forEach(icon -> {
//            AwesomeDude.setIcon((Labeled)icon, AwesomeIcon.STAR, ContentDisplay.TOP);
        });
    }

    public Labeled addIcon(AwesomeIcon icon, String descriptn) {
        Labeled l = AwesomeDude.createIconLabel(icon, descriptn, String.valueOf(icon_size), "13", ContentDisplay.TOP);
//        Labeled l = createIcon(icon, icon_size, descriptn, null);
        l.scaleYProperty().bind(l.scaleXProperty());
        l.addEventHandler(DRAG_ENTERED, e -> description.setText(descriptn));
        l.addEventHandler(DRAG_EXITED, e -> description.setText(""));
        l.addEventHandler(DRAG_ENTERED, e -> l.setScaleX(1.1));
        l.addEventHandler(DRAG_EXITED, e -> l.setScaleX(1));
        actionBox.getChildren().add(l);
        return l;
    }
}
