/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import java.util.function.Supplier;
import javafx.util.Callback;

/**
 *
 * @author Plutonium_
 */
public interface Accessor<T> {
    
    default T getValue() {
        return getGetter().get();
    }

    default boolean setValue(T val) {
        return getSetter().call(val);
    }
    
    Callback<T,Boolean> getSetter();

    Supplier<T> getGetter();

}