/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.icon;

import de.jensd.fx.glyphs.GlyphIcons;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import org.reactfx.Subscription;
import static util.reactive.Util.maintain;

/**
 * Very simple alternative CheckBox control.
 */
public class CheckIcon extends Icon<CheckIcon> {
    private static final PseudoClass selectedPC = getPseudoClass("selected");
    private static final String STYLECLASS = "check-icon";
    
    /** Selection state.*/
    public final BooleanProperty selected;
    
    /** Creates icon with selection true.*/
    public CheckIcon() {
        this(true);
    }
    
    /** Creates icon with selection set to provided value. */
    public CheckIcon(boolean s) {
        this(new SimpleBooleanProperty(s));
    }
    
    /** Creates icon with property as selection value. {@link #selected}==s will always be true. */
    public CheckIcon(BooleanProperty s) {
        selected = s==null ? new SimpleBooleanProperty(true) : s;
        styleclass(STYLECLASS);
        maintain(selected, v -> pseudoClassStateChanged(selectedPC,v));
        addEventHandler(MOUSE_CLICKED, e -> selected.set(!selected.get()));
    }
    
    
    private Subscription s = null;
    
    /** Sets normal and selected icons. Overrides css values. */
    public CheckIcon icons(GlyphIcons normalIcon, GlyphIcons selectedIcon) {
        if(s!=null) s.unsubscribe();
        s = maintain(selected, v -> icon(v ? selectedIcon : normalIcon));
        return this;
    }
}
