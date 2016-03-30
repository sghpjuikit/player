/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.access;

import static util.type.Util.unPrimitivize;

/**
 *
 * @author Martin Polakovic
 */
public interface TypedValue<C> {

    /**
     * Returns class type of the value. Not class of this object.
     *
     * @return value type.
     */
    Class<C> getType();

    /** @return whether type of the value is String */
    default boolean isTypeString() {
        return String.class == getType();
    }

    /**
     * Returns whether type is numeric.
     * <p/>
     * For example gui text field for this value might want to allow only numeric
     * characters to be inputted.
     *
     * @return whether type of the value is any class exntemding {@link Number}
     * or any of the number type primitives
     */
    default boolean isTypeNumber() {
        return Number.class.isAssignableFrom(unPrimitivize(getType()));
    }

    /**
     * Stricter version of {@link #isTypeNumber()}, limiting the types to those
     * that can contain floating point character '.'.
     * <p/>
     * This method might be used whether text representation of the value can
     * contain '.' character and consequently restrict input.
     * <p/>
     * Default implementation checks whether type is Float, float, Double, double
     * or and extending class of them.
     *
     * @return
     */
    default boolean isTypeFloatingNumber() {
        Class c = getType();
        return Float.class.isAssignableFrom(c) ||
                    float.class.equals(c) ||
                        Double.class.isAssignableFrom(c) ||
                            double.class.equals(c);
    }

    /**
     * Default implementation returns false.
     *
     * @return
     */
    default boolean isTypeNumberNonegative() {
        return false;
    }
}
