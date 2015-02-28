/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.parsing;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/** Makes the method responsible for the parsing from String strategy. 
    <p>
    The method must be static, return correct object type and take String as a 
    parameter.*/
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface ParsesFromString {}
