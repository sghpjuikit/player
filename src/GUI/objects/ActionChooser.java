/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import static javafx.scene.input.DragEvent.DRAG_ENTERED;
import static javafx.scene.input.DragEvent.DRAG_EXITED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import util.dev.TODO;
import static util.dev.TODO.Purpose.FUNCTIONALITY;

/**
 <p>
 @author Plutonium_
 */
@TODO(purpose = FUNCTIONALITY, note = "decide best functionality, complete + clean up")
public class ActionChooser<T> extends StackPane {

    public final Text description = new Text();
    public final HBox actionBox;

    private int icon_size = 40;
    public T item;

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

    public Icon addIcon(FontAwesomeIconName icon, String descriptn) {
        return addIcon(icon, null, descriptn);
    }
    public Icon addIcon(FontAwesomeIconName icon, String text, String descriptn) {
        Icon l = new Icon(icon, icon_size);
        l.setFont(new Font(l.getFont().getName(), 13));
//        l.setText(text);
        boolean drag_activated = false;
        boolean hover_activated = true;
        
        l.scaleYProperty().bind(l.scaleXProperty());
        if(drag_activated) {
            l.addEventHandler(DRAG_ENTERED, e -> description.setText(descriptn));
            l.addEventHandler(DRAG_EXITED, e -> description.setText(""));
            l.addEventHandler(DRAG_ENTERED, e -> l.setScaleX(1.1));
            l.addEventHandler(DRAG_EXITED, e -> l.setScaleX(1));
        }
        if(hover_activated) {
            l.addEventHandler(MOUSE_ENTERED, e -> description.setText(descriptn));
            l.addEventHandler(MOUSE_EXITED, e -> description.setText(""));
            l.addEventHandler(MOUSE_ENTERED, e -> l.setScaleX(1.1));
            l.addEventHandler(MOUSE_EXITED, e -> l.setScaleX(1));
        }
        actionBox.getChildren().add(l);
        return l;
    }
    
    public T getItem() {
        return item;
    }
}
