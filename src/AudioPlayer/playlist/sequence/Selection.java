/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.playlist.sequence;

import java.util.List;

/**
 * Selection logic for selection of next and previous item in a list. Used in
 * combination with {@link ItemSelector}.
 * <p>
 * @author Plutonium_
 */
@FunctionalInterface
public interface Selection<E> {
    /**
     * Selects an item from list. The available parameters can but do not
     * have to be utilized in the decision logic.
     * @param size  Size of the list.
     * @param index Index of currently selected item.
     * @param item Currently selected item.
     * @param list List the selection operates on.
     * @return 
     */
    public E select(int size, int index, E item, List<E> list);

    /** Cyclic incrementing by 1. */
    public static int incrIndex(int size, int index) {
        return index==size-1 ? 0 : ++index;
    }
    /** Cyclic decrementing by 1. */
    public static int decrIndex(int size, int index) {
        return index==0 ? size-1 : --index;
    }
}