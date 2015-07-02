/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.controller.io;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 *
 * @author Plutonium_
 */
public class Inputs {
    private final Map<String,Input> m;

    
    public Inputs() {
        m = new HashMap();
    }
    
    public <T> Input<T> create(String name, Class<T> type, Consumer<T> action) {
        Input<T> o = new Input(name,type,action);
        m.put(name, o);
        return o;
    }
    
    public int getSize() {
        return m.size();
    }
    
    public Collection<Input> getOutputs() {
        return m.values();
    }
}