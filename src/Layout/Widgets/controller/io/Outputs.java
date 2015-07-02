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
import org.reactfx.Subscription;
import static util.reactive.Util.maintain;

/**
 *
 * @author Plutonium_
 */
public class Outputs {
    private final Map<String,Output> m;

    
    public Outputs() {
        m = new HashMap();
    }
    
    public <T> Output<T> create(String name, Class<T> type, T val) {
        Output<T> o = new Output(name,type);
                  o.setValue(val);
        m.put(name, o);
        return o;
    }
    
    public int getSize() {
        return m.size();
    }
    
    public Collection<Output> getOutputs() {
        return m.values();
    }
    
    public <T> Subscription monitor(String name, Consumer<T> action) {
        Output<T> o = m.get(name);
        if(o==null) return Subscription.EMPTY;
        
        return maintain(o.val, action);
    }
}