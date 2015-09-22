/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget.controller.io;

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
    
    public <T> Input<T> create(String name, Class<? super T> type, Consumer<? super T> action) {
        Input<T> o = new Input(name,type,action);
        m.put(name, o);
        return o;
    }
    public <T> Input<T> create(String name, Class<? super T> type, T init_val, Consumer<? super T> action) {
        Input<T> o = new Input(name,type,init_val,action);
        m.put(name, o);
        return o;
    }
    
    public int getSize() {
        return m.size();
    }
    
    public Input getInput(String name) {
        return m.get(name);
    }
    
    public Collection<Input> getInputs() {
        return m.values();
    }
}