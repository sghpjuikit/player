/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates code that depends on some other code or is relied upon by some other
 * code. Marks and documents inflexible code, that should not be changed arbitrarily.
 *
 * @author Plutonium_
 */
@Retention(RetentionPolicy.SOURCE)
@Repeatable(Dependencies.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
public @interface Dependency {
    String value() default "";
}
