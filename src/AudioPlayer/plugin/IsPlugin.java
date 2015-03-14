/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.plugin;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import org.atteo.classindex.IndexAnnotated;

/**
 <p>
 @author Plutonium_
 */
@Documented
@IndexAnnotated
@Retention(RUNTIME)
@Target(TYPE)
public @interface IsPlugin {
    
}
