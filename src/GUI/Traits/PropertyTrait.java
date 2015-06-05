/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui.Traits;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * Denotes an interface in role of a property trait.
 * 
 * Property traits are traits supposed to add properties to an object that
 * conform to Java Bean Property standard. The usual way is to declare the
 * property in the class and provide setters and getters as default methods
 * in the trait interface
 * 
 * Decisions about values of the property are left to implementing class as well
 * as how those values will influence object's behavior. The key idea is to ease
 * the burden of implementing common properties for the developer as well as
 * better distinguish the type of behavior for concrete object type.
 * 
 * This is a standard (retention policy = source) and has no effect on the code.
 * 
 * For example Editable trait would look like this:
 * 
 *   public BooleanProperty editableProperty();
 * 
 *   default public void setEditable(boolean value) {
 *       editableProperty().set(value);
 *   }
 * 
 *   default public boolean isEditable() {
 *       return editableProperty().get();
 *   }
 * 
 * @author uranium
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface PropertyTrait {
    
}
