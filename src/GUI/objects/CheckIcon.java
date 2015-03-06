/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.TOGGLE_OFF;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.TOGGLE_ON;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static util.functional.Util.mapB;
import static util.reactive.Util.maintain;

/**
 * Very simple alternative CheckBox control.
 */
public class CheckIcon extends Icon {
    
    public final BooleanProperty selected = new SimpleBooleanProperty();
    
    public CheckIcon() {
        this(true);
    }
    
    public CheckIcon(boolean s) {
        selected.addListener((o,ov,nv)->icon.setValue(nv ? TOGGLE_ON : TOGGLE_OFF));
        selected.set(s);
        addEventHandler(MOUSE_CLICKED, e -> {
            selected.set(!selected.get());
            e.consume();
        });
        maintain(selected,mapB(TOGGLE_ON,TOGGLE_OFF),icon);
    }
    
}
