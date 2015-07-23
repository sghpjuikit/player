/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.lang.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Plutonium_
 */
public class ClassName {
    private static final Map<Class,String> m = new HashMap();
    
    
    /**
     * Returns name of the class.
     * <p>
     * Class names are computed lazily (when requested) and cached (every name
     * will be computed only once), with access O(1).
     * <p>
     * Name computation is as follows:
     * <ul>
     * <li> value of the {@link Name} annotation on the class
     * <li> if empty, {@link Class#getSimpleName()}
     * <li> if empty, {@link Class#toString()}
     * </ul>
     */
    public static String get(Class c) {
        String n = m.get(c);
        if(n==null){
            n = getName(c);
            m.put(c, n);
        }
        return n;
    }
    
    private static String getName(Class c) {
        String n = "";
        Name a = (Name) c.getAnnotation(Name.class);
        if(a!=null) n = a.value();
        return n.isEmpty() ? c.getSimpleName().isEmpty() ? c.toString()
                                                         : c.getSimpleName()
                              : n;
    }
    
    
    /**
     * Defines human readable name of the class. Inherited annotation.
     * For example for use in user interface when type of the entity 
     * requires sensible name.
     */
    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public static @interface Name {
        String value() default "";
    }

}
