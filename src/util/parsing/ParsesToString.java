/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.parsing;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates the method responsible for the parsing to String strategy.
 * <p>
 * The method must be non static and return String.
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface ParsesToString {}