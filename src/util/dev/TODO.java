/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.dev;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;

/** Annotation type indicating a programming task needs to be done. */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ANNOTATION_TYPE,CONSTRUCTOR,FIELD,LOCAL_VARIABLE,METHOD,PACKAGE,PARAMETER,TYPE,TYPE_PARAMETER,TYPE_USE})
@Repeatable(TODOs.class)
public @interface TODO {
    
    Purpose[] purpose() default Purpose.UNSPECIFIED;
    Severity severity() default Severity.UNSPECIFIED;
    String note() default "";


    enum Purpose {
        PERFORMANCE_OPTIMIZATION,
        BUG,
        DOCUMENTATION,
        READABILITY,
        UNTESTED,
        ILL_DEPENDENCY,
        API,
        FUNCTIONALITY,
        UNIMPLEMENTED,
        UNSPECIFIED;
    }

    enum Severity {
        CRITICAL,
        SEVERE,
        MEDIUM,
        LOW,
        INSIGNIFICANT,
        UNSPECIFIED;
    }
}