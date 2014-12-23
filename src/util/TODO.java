/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation type indicating a programming task needs to be done, for example
 * a new feature or a bug fix.
 * <p>
 * Retention is kept to SOURCE.
 * 
 * @author uranium
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface TODO {
    Purpose purpose() default Purpose.UNSPECIFIED;
    Severity severity() default Severity.UNSPECIFIED;
    String note() default "";
    
    
    public static enum Purpose {
        PERFORMANCE_OPTIMIZATION,
        BUG,
        DOCUMENTATION,
        READABILITY,
        ILL_DEPENDENCY,
        COSMETIC,
        API,
        FUNCTIONALITY,
        UNIMPLEMENTED,
        UNSPECIFIED;
    }
    
    public static enum Severity {
        CRITICAL,
        SEVERE,
        MEDIUM,
        LOW,
        INSIGNIFICANT,
        UNSPECIFIED;
    }
}