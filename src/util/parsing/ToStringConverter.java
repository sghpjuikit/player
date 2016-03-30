/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.parsing;

/**
 * Object to String converter.
 *
 * @author Martin Polakovic
 */
@FunctionalInterface
public interface ToStringConverter<T> {

    /**
     * Converts object into string.
     *
     * @return String the object has been converted.
     */
    String toS(T object);
}
