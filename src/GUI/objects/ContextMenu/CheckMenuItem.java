/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.ContextMenu;

import gui.objects.CheckIcon;
import static java.util.Objects.requireNonNull;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

/** 
 * Simple {@link Menu} implementation with check icon. Clicking on the icon 
 * always has the same effect as clicking on the menu item text. The icon 
 * selection changes on/off on mouse click but this default behavior can be
 * changed using {@link #setOnMouseClicked(java.lang.Runnable)} which overrides
 * it.
 * <p>
 * Features:
 * <ul>
 * <li> Check icon reflecting selection state
 * <li> Observable selection state
 * <li> Custom click implementation
 * </ul>
 * 
 * @author Plutonium_
 */
public class CheckMenuItem extends Menu {
    
    private static final String STYLECLASS = "checkicon-menu-item";
    private static final String STYLECLASS_ICON = "checkicon-menu-item-icon";
    private static final String NO_CHILDREN_STYLECLASS = "menu-nochildren";
    
    
    private final CheckIcon icon = new CheckIcon(false);
    /** Selection state reflected by the icon. Changes on click. Default false.*/
    public final BooleanProperty selected = icon.selected;
    
    /**
     * Creates menu item and adds selection listener for convenience.
     * 
     * @param text text of this menu item
     * @param s initial selection state
     * @param sh selection listener to add. Equivalent to:
     * {@code selected.addListener((o,oldv,newv) -> sel_han.accept(newv)); }
     */
    public CheckMenuItem(String text, boolean s, Consumer<Boolean> sh) {
        this(text, s);
        
        requireNonNull(sh);
        selected.addListener((o,ov,nv) -> sh.accept(nv));
        
    }
    
    /**
     * Creates menu item with specified text and selection.
     * 
     * @param text text of this menu item
     * @param s initial selection state
     */
    public CheckMenuItem(String text, boolean s) {
        super(text);
        setGraphic(icon);
        getStyleClass().add(STYLECLASS);
        icon.getStyleClass().add(STYLECLASS_ICON);
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
     * Creates menu item with specified text and false selection.
     * 
     * @param text text of this menu item
     */
    public CheckMenuItem(String text) {
        this(text, false);
    }
    
    /**
     * Creates menu item with empty text and false selection.
     */
    public CheckMenuItem() {
        this("");
    }
    
    /**
     * Overrides default click implementation which changes the selection value.
     * After using this method, icon will still reflect the selection, but it it
     * will not change unless changed manually from the handler.
     * <p>
     * This is useful for cases, where the menu lists items to choose from and
     * exactly one must be selected at any time. This requires deselection to
     * be impossible.
     * 
     * @param h click handler
     */
    public void setOnMouseClicked(Runnable h) {
        setOnAction(e -> h.run());
        icon.setOnMouseClicked(e -> h.run());
    }
    
}
