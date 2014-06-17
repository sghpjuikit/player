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
public interface EditableTrait {
    
    /**
     * If true this allows for the supporting value to be edited. It is
     * up to the objects implementation as what that mean exactly. Generally
     * non graphical object implements this as a switch for mutability of its
     * values. Graphical object should retain its mutability, but restrict it to
     * programmatical way - forbid the user from setting new value manually - 
     * a read-only control.
     */
    public BooleanProperty editableProperty();
    
    /**
     * Sets whether {@link #editableProperty() editable} support is
     * enabled or not.
     */
    default public void setEditable(boolean value) {
        editableProperty().set(value);
    }
    
    /**
     * Returns whether {@link #editableProperty()editable} support is
     * enabled or not.
     */
    default public boolean isEditable() {
        return editableProperty().get();
    }
}
