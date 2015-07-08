/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package action;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.atteo.classindex.IndexAnnotated;

/**
 * Marks class containing {@link IsAction} annotated method. If such class is not
 * annotated by this annotation, the action will not be discovered.
 * <p>
 * For that purpose, this annotation itself is annotated by {@link IndexAnnotated}.
 * @author Plutonium_
 */
@Documented
@IndexAnnotated
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface IsActionable {
    
}
