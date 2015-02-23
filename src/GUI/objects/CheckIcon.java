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
    
    private final BooleanProperty s = new SimpleBooleanProperty();
    
    public CheckIcon() {
        this(true);
    }
    
    public CheckIcon(boolean selected) {
        s.addListener((o,ov,nv)->icon.setValue(nv ? TOGGLE_ON : TOGGLE_OFF));
        s.set(selected);
        addEventHandler(MOUSE_CLICKED, e -> {
            s.set(!s.get());
            e.consume();
        });
        maintain(s,mapB(TOGGLE_ON,TOGGLE_OFF),icon);
    }
    
    // we want to be code-compatible with CheckBox for easy code change
    
    public boolean isSelected() {
        return s.get();
    }
    public void setSelected(boolean val) {
        s.set(val);
    }
    public BooleanProperty selectedProperty() {
        return s;
    }
}
