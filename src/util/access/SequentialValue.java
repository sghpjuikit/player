/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.access;

import java.util.List;

import util.dev.TODO;

import static util.type.Util.getEnumConstants;
import static util.dev.TODO.Purpose.UNIMPLEMENTED;

/**
 * Value that is part of a sequence.
 * <p/>
 * The sequence is normally deterministic, but is not limited to be. It can also
 * be random, or influenced externally.
 *
 * @param <V> type of value.
 */
public interface SequentialValue<V> extends CyclicValue<V> {

    /**
     * @return next sequence value.
     */
    V next();

    /**
     * @return  previous sequence value.
     */
    V previous();

    /**
     * Defers to {@link #next()}.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    default V cycle() {
        return next();
    }

/******************************************************************************/

    static int next(int v) { return v+1; }
    static int previous(int v) { return v-1; }
    static Integer next(Integer v) { return v+1; }
    static Integer previous(Integer v) { return v-1; }

    static long next(long v) { return v+1; }
    static long previous(long v) { return v-1; }
    static Long next(Long v) { return v+1; }
    static Long previous(Long v) { return v-1; }

    static short next(short v) { return (short) (v+1); }
    static short previous(short v) { return (short) (v-1); }
    @TODO(purpose = UNIMPLEMENTED)
    static Short next(Short v) { return v; }
    @TODO(purpose = UNIMPLEMENTED)
    static Short previous(Short v) { return v; }

    static boolean next(boolean v) { return !v; }
    static boolean previous(boolean v) { return !v; }
    static Boolean next(Boolean v) { return !v; }
    static Boolean previous(Boolean v) { return !v; }

    @TODO(purpose = UNIMPLEMENTED)
    static char next(char v) { return v; }
    @TODO(purpose = UNIMPLEMENTED)
    static char previous(char v) { return v; }
    @TODO(purpose = UNIMPLEMENTED)
    static Character next(Character v) { return v; }
    @TODO(purpose = UNIMPLEMENTED)
    static Character previous(Character v) { return v; }

    /**
     * Returns cyclically next enum constant value from list of all values for
     * specified enum constant.
     * @return next cyclical enum constant according to its ordinal number.
     */
    static <E extends Enum> E next(E val) {
        E vals[] = getEnumConstants(val.getClass());
        int index = (val.ordinal()+1) % vals.length;
        return vals[index];
    }

    /**
     * Returns cyclically previous enum constant value from list of all values for
     * specified enum constant.
     * @return previous cyclical enum constant according to its ordinal number.
     */
    static <E extends Enum> E previous(E val) {
        E vals[] = getEnumConstants(val.getClass());
        int ord = val.ordinal();
        int index = ord==0 ? vals.length-1 : ord-1;
        return vals[index];
    }

    /** Modular incrementing by 1. */
    static int incrIndex(int max, int index) {
        return index==max-1 ? 0 : ++index;
    }

    /** Modular incrementing by 1. */
    static int incrIndex(List<?> max, int index) {
        return incrIndex(max.size(), index);
    }

    /** Modular decrementing by 1. */
    static int decrIndex(int max, int index) {
        return index==0 ? max-1 : --index;
    }

    /** Modular decrementing by 1. */
    static int decrIndex(List<?> list, int index) {
        return decrIndex(list.size(), index);
    }

    /** @return next modular element in the list or null if empty */
    static <T> T next(List<T> list, T element) {
        if (list.isEmpty()) return null;
        if (element==null) return list.get(0);
        int index = list.indexOf(element);
        return list.get(index<0 ? 0 : incrIndex(list, index));
    }

    /** @return previous modular element in the list or null if empty */
    static <T> T previous(List<T> list, T element) {
        if (list.isEmpty()) return null;
        if (element==null) return list.get(0);
        int index = list.indexOf(element);
        return list.get(index<0 ? 0 : decrIndex(list, index));
    }

    /** @return next modular element in the array or null if empty */
    static <T> T next(T[] list, T element) {
        if (list.length==0) return null;
        if (element==null) return list[0];
        int index = -1;
        for (int i=0; i<list.length; i++)
            if (element.equals(list[i])) {
                index = i;
                break;
            }
        return list[index<0 || index==list.length ? 0 : index+1];
    }

    /** @return previous modular element in the array or null if empty */
    static <T> T previous(T[] list, T element) {
        if (list.length==0) return null;
        if (element==null) return list[0];
        int index = -1;
        for (int i=0; i<list.length; i++)
            if (element.equals(list[i])) {
                index = i;
                break;
            }
        return list[index<0 ? 0 : index==0 ? list.length-1 : index-1];
    }
}
