/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects;

import static java.util.Objects.requireNonNull;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.MenuItem;

/** 
Simple {@link MenuItem} implementation with check icon. The icon selection
changes on/off on mouse click.
<p>
Features:
<li> check icon reflecting selection state
<li> observable selection state
<li> selection change handler

@author Plutonium_
*/
public class CheckMenuItem extends MenuItem {
    private final CheckIcon icon = new CheckIcon(false);
    /** Selection state reflected by the icon. Changes on click. Default false.*/
    public final BooleanProperty selected = icon.selected;
    
    /**
    @param text text of this menu item
    @param s selection state
    @param selection change handler
    */
    public CheckMenuItem(String text, boolean s, Consumer<Boolean> sel_han) {
        this(text, s);
        
        requireNonNull(sel_han);
        selected.addListener((o,ov,nv) -> sel_han.accept(nv));
    }
    
    /**
    @param text text of this menu item
    @param s selection state
    */
    public CheckMenuItem(String text, boolean s) {
        super(text);
        setGraphic(icon);
        selected.set(s);
        setOnAction(e -> selected.set(!selected.get()));
    }
    
    /**
    @param text text of this menu item
    */
    public CheckMenuItem(String text) {
        this(text, false);
    }
    
    public CheckMenuItem() {
        this("");
    }
    
    
}
