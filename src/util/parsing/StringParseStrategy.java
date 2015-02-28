/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.parsing;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import static util.parsing.StringParseStrategy.From.NONE;
import static util.parsing.StringParseStrategy.To.TO_STRING_METHOD;

/** Indicates that the class has defined strategy for parsing to and from String.
    This gives hint to parser as to how the parsing should be done.
    <p>
    The strategy for parsing from and to String must be consistent, i.e. parsing
    from string the object was parsed into must not fail and be able to return
    non null value.

    @see #from() for more details
    @see #to() for more details
    */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface StringParseStrategy {

    From from() default NONE;
    To to() default TO_STRING_METHOD;
    String constant() default "";
    
    /** Defines strategy for parsing object from String. */
    public static enum From {
        /** Static fromString(String text) method is responsible. */
        FROM_STRING_METHOD,
        /** Static valueOf(String text) method is responsible. */
        VALUE_OF_METHOD,
        /** {@link ParsesFromString} annotation decides responsible method. */
        ANNOTATED_METHOD,
        /** Constructor with exactly one String parameter (and no others)
            is responsible. Advised when state of the instance can be derived
            from the string.
            <p>
            In this case the constructor basically fills the role of a static
            factory method, like valueOf(String text). */
        CONSTRUCTOR_STR,
        /** No argument constructor is responsible. This should only be advised
            for stateless classes, which have 'equal' reusable instances or 
            always the same state.
            <p>
            In case the state of the instance does not need to be recreated from
            String and default state is enough, this strategy is valid as well.
            For example a class might wish to return default, empty or nullary
            value upon parsing. */
        CONSTRUCTOR,
        /** Parsing strategy undefined. */
        NONE;
    }
    
    /** Defines strategy for parsing object to String. */
    public static enum To {
        /** toString() method is responsible. *//** toString() method is responsible. */
        TO_STRING_METHOD,
        /** {@link ParsesToString} annotation decides responsible method. */
        ANNOTATED_METHOD,
        /** Parses constantly into the same string value defined in. {@link StringParseStrategy} */
        CONSTANT,
        /** Parsing strategy undefined. */
        NONE;
    }
}
