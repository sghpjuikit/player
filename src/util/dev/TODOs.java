/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.dev;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Container annotation for {@link TIDO}.
 * <p>
 * Used solely by compiler and has no practical use for developer.
 * 
 * @author Plutonium_
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface TODOs {
    TODO[] value();
}