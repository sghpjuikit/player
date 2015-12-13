/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget.controller.io;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.reactfx.Subscription;

import Layout.Areas.IOLayer;

/**
 *
 * @author Plutonium_
 */
public class Input<T> extends Put<T>{
    final String name;
    final Map<Output<? extends T>,Subscription> sources = new HashMap<>();

    public Input(String name, Class<? super T> c, Consumer<? super T> action) {
        this(name, c, null, action);
    }

    public Input(String name, Class<? super T> c, T init_val, Consumer<? super T> action) {
        super(c, init_val);
        this.name = name;
        monitor(action);
    }


    public String getName() {
        return name;
    }

    /**
     * Binds to the output.
     * Sets its value immediately and then every time it changes.
     * Binding multiple times has no effect.
     */
    public Subscription bind(Output<? extends T> output) {
        sources.computeIfAbsent(output, o -> o.monitor(this::setValue));
        IOLayer.addConnectionE(this, output);
        return () -> unbind(output);
    }

    public void unbind(Output<? extends T> output) {
        Subscription s = sources.get(output);
        if(s!=null) s.unsubscribe();
        sources.remove(output);
        IOLayer.remConnectionE(this, output);
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
