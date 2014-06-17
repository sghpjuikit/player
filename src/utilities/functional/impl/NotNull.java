/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.functional.impl;

import java.util.function.Predicate;

/**
 * 
 * Predicate returning true if and only if the object is not null.
 *
 * @author Plutonium_
 */
public class NotNull implements Predicate<Object> {
    @Override
    public boolean test(Object t) {
        return t!=null;
    }
}
