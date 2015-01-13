/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects;

import de.jensd.fx.fontawesome.AwesomeDude;
import static de.jensd.fx.fontawesome.AwesomeIcon.TOGGLE_ALTN;
import static de.jensd.fx.fontawesome.AwesomeIcon.TOGGLE_OFF;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Label;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;

/**
 * Very simple alternative CheckBox control.
 */
public class CheckIcon extends Label {
    
    private final BooleanProperty s = new SimpleBooleanProperty();
    
    public CheckIcon() {
        s.addListener((o,ov,nv)->AwesomeDude.setIcon(this, nv ? TOGGLE_ALTN : TOGGLE_OFF, "12"));
        s.set(true);
        addEventHandler(MOUSE_CLICKED, e -> {
            s.set(!s.get());
            e.consume();
        });
    }
    
    public CheckIcon(boolean selected) {
        this();
        this.s.set(selected);
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
