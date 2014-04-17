/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PseudoObjects;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation type indicating a task needs to be done.
 * @author uranium
 */
@Retention(RetentionPolicy.SOURCE)
public @interface TODO {
    String value();
}