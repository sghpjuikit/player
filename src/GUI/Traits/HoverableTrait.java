/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.Traits;

import javafx.beans.property.BooleanProperty;

/**
 *
 * @author uranium
 */
@PropertyTrait
public interface HoverableTrait extends NodeTrait {
    
    /**
     * If true the owning Node of this trait will ha enabled hovering
     * functionality. Otherwise disabled.
     */
    public BooleanProperty hoverableProperty();
    
    /**
     * Sets whether {@link #hoverableProperty() hoverable} trait is
     * enabled or not.
     */
    default public void setHoverable(boolean value) {
        hoverableProperty().set(value);
    }
    
    /**
     * Returns whether {@link #hoverableProperty() hoverable} trait is
     * enabled or not.
     */
    default public boolean isHoverable() {
        return hoverableProperty().get();
    }
}
