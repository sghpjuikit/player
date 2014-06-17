/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.atteo.classindex.IndexAnnotated;

/**
 * Marks any class that can be configured. This is different from {@link Configurable}
 * and is part of a framework to create global interclass application wide
 * configuration accessible from the application by both developer and user. It
 * is used in conjunction with {@link IsConfig} and {@link Config}.
 * <p>
 * Any class annotated with this annotation will be autodiscovered and added
 * into configuration class pool. If class is not annotated by this annotation,
 * it will not be discovered and its configurable fields will not be detected.
 * It will not cause any irrecoverable errors, but any configurable field that
 * is not detected will not be accessible and its value will remain static.
 * <p>
 * Also allows to specify default category name for all configuration fields
 * within the annotated class.
 * <p>
 * For that purpose, this annotation itself is annotated by {@link IndexAnnotated}.
 * <p>
 * @author Plutonium_
 */
@IndexAnnotated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IsConfigurable {
   /*
    * Specifies default configuration category name for configuration fields of 
    * given class. If none is specified, name of the class is used to specify default
    * configuration field's category
    * and this annotation makes it possible to customized the name. The name can
    * also be specified individually for each configuration field. See {@link IsConfig}
    */
    String group() default "";
}