/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.controller.io;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.reactfx.Subscription;

/**
 *
 * @author Plutonium_
 */
public class Input<T> extends Put<T>{
    final String name;
//    final Consumer<? super T> applier;
    final Map<Output<? extends T>,Subscription> sources = new HashMap<>();
    
    public Input(String name, Class<T> c, Consumer<? super T> action) {
        this(name, c, null, action);
    }
    
    public Input(String name, Class<T> c, T init_val, Consumer<? super T> action) {
        super(c, init_val);
        this.name = name;
//        this.applier = action;
        monitor(action);
    }
    
    
    public String getName() {
        return name;
    }
    
//    @Override
//    public void setValue(T v) {
//        super.setValue(v);
//        applier.accept(v);
//    }

    
    public Subscription bind(Output<? extends T> o) {
        Subscription s = sources.get(o);
        if(s==null) {
            s = o.monitor(this::setValue);
            sources.put(o, s);
        }
        return () -> unbind(o);
    }
    
    public void unbind(Output<? extends T> o) {
        Subscription s = sources.get(o);
        if(s!=null) s.unsubscribe();
        sources.remove(o);
    }
    
    public void unbindAll() {
        sources.values().forEach(Subscription::unsubscribe);
        sources.clear();
    }
    
    public Set<Output<? extends T>> getSources() {
        return sources.keySet();
    }

    @Override
    public String toString() {
        return name + ", " + type;
    }
}
