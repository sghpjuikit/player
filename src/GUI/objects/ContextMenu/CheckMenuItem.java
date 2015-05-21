/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.ContextMenu;

import GUI.objects.CheckIcon;
import static java.util.Objects.requireNonNull;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

/** 
Simple {@link Menu} implementation with check icon. The icon selection
changes on/off on mouse click.
<p>
Features:
<li> check icon reflecting selection state
<li> observable selection state
<li> selection change handler

@author Plutonium_
*/
public class CheckMenuItem extends Menu {
    
    private static final String NO_CHILDREN_STYLECLASS = "menu-nochildren";
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
        
        // action = toggle selection
        setOnAction(e -> selected.set(!selected.get()));            
        icon.setOnMouseClicked(e -> getOnAction().handle(null));
        
        // hide open submenu arrow if no children
        getStyleClass().add(NO_CHILDREN_STYLECLASS);
        getItems().addListener(new ListChangeListener<MenuItem>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends MenuItem> c) {
                if(c.getList().isEmpty() && !getStyleClass().contains(NO_CHILDREN_STYLECLASS)) getStyleClass().add(NO_CHILDREN_STYLECLASS);
                else getStyleClass().remove(NO_CHILDREN_STYLECLASS);
            }
        });
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
