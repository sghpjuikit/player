/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.Pickers;

import java.util.stream.Stream;

/**
 *
 * @author Plutonium_
 */
@FunctionalInterface
public interface ItemAccumulator<E> {
   
    /**
     * Gathers items.
     * @return stream of items.
     */
    Stream<E> accumulate();
}
