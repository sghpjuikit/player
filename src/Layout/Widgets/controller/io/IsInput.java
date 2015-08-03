/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.controller.io;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Signals for a method to be turned into a widget {@link Input}. Method must
 * not be static or have more than 1 parameter. Parameterless methods will
 * produce inputs of type {@link Void}, otherwise the type is decided by the
 * parameter type. When the input gets a value passed in, thw annotated method
 * will be invoked with the value as parameter.
 * 
 * @author Plutonium_
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface IsInput {
    /** Name of the input.*/
    String value();
}


