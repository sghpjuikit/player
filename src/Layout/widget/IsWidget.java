/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import org.atteo.classindex.IndexAnnotated;


/**
 Use for autodiscovery of the widget. Can only be used on classes that extend
 {@link javafx.scene.Node} and implement {@link Controller}. These classes will 
 then have widget factories ({@link ClassWidgetFactory})created and registered, 
 with the annotated class being a
 controller for the widget.
 
 @author Plutonium_
 */
@Documented
@IndexAnnotated
@Retention(RUNTIME)
@Target(TYPE)
public @interface IsWidget {
    
}
