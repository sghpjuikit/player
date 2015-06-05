/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;

/**
 * Very simple alternative CheckBox control.
 */
public class CheckIcon extends Icon {
    private static final PseudoClass selectedPC = getPseudoClass("selected");
    private static final String STYLECLASS = "check-icon";
    
    /** Selected state. Default true.*/
    public final BooleanProperty selected = new SimpleBooleanProperty();
    
    /** Creates icon with selection true.*/
    public CheckIcon() {
        this(true);
    }
    
    /** Creates icon with specified selection. */
    public CheckIcon(boolean s) {
        getStyleClass().add(STYLECLASS);
        selected.addListener((o,ov,nv)->pseudoClassStateChanged(selectedPC, nv));
        selected.set(s);
        addEventHandler(MOUSE_CLICKED, e -> selected.set(!selected.get()));
    }

    /** 
     * Creates icon with selection true and assigned styleclass. Use
     * styleclass to define custom icon graphics.
     */
    public CheckIcon(String style) {
        this();
        getStyleClass().add(style);
    }
    
    /** 
     * Creates icon with specified selection and assigned styleclass. Use
     * styleclass to define custom icon graphics.
     */
    public CheckIcon(boolean s, String style) {
        this(s);
        getStyleClass().add(style);
    }
}
