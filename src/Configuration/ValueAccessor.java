/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import java.util.function.Supplier;
import javafx.util.Callback;
import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 *
 * @author Plutonium_
 */
@Immutable
public class ValueAccessor<T> implements Accessor<T> {
    
    private final Callback<T,Boolean> setter;
    private final Supplier<T> getter;
    
    /**
     * @param setter defines how the password will be set
     * @param getter defines how the password will be accessed
     */
    public ValueAccessor(Callback<T,Boolean> setter, Supplier<T> getter) {
        this.getter = getter;
        this.setter = setter;
    }
    
    @Override
    public Callback<T,Boolean> getSetter() {
        return setter;
    }
    
    @Override
    public Supplier<T> getGetter() {
        return getter;
    }
}