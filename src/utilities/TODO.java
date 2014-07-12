/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

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
@Retention(RetentionPolicy.SOURCE)
public @interface TODO {
    String value();
}