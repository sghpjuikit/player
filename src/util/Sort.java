/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.util.Comparator;

/**
 * Sort type.
 *
 * @author Plutonium_
 */
public enum Sort {
    /** From minimal to maximal element. */
    ASCENDING,
    /** From maximal to minimal element. */
    DESCENDING,
    /** No sort. Order remains as is. */
    NONE;

    /**
     * Modifies a comparator to uphold the sorting type. If this is descending, new reverse
     * comparator is returned, otherwise no new comparator is built. By definition every comparator
     * is ascending, hence the comparator itself is returned when this is ascending. When this is
     * none, no order comparator (static instance) is returned.
     * <p>
     * Applying this method multiple times will not produce any new comparator instances. Ascending
     * and none will consistently return the same instance and reverse will flip between the reverse
     * and original instance of the comparator, i.e., reverse of a reverse will be the same comparator
     * object/instance.
     * <p>
     * Some code uses null comparator as a no comparator, hence this method accepts null. In such
     * case, null is always returned (no order has no reverse order).
     *
     * @return null if c null, c if ascending, reverse to c if descending or no order
     * comparator (which always returns 0) when none.
     */
    public <T> Comparator<? super T> cmp(Comparator<? super T> c) {
        if(c==null) return null;
        switch (this) {
            case ASCENDING: return c;
            // note the used implementation makes reversion effect on comparator void if applied
            // multiple times (the comparator instance will flip between reversed and original)
            case DESCENDING: return c.reversed();
            case NONE: return util.functional.Util.SAME;
            default: throw new SwitchException(this);
        }
    }
}
