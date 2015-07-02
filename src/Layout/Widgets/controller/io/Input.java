/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.controller.io;

import java.util.function.Consumer;
import org.reactfx.Subscription;

/**
 *
 * @author Plutonium_
 */
public class Input<T> {
    final String name;
    final Class<T> type;
    final Consumer<T> applier;
    Subscription s = null;
    
    public Input(String name, Class<T> c, Consumer<T> action) {
        this.name = name;
        this.type = c;
        this.applier = action;
    }
    
    
    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }
    
    public void setValue(T v) {
        applier.accept(v);
    }
    
    public void bind(Output<T> o) {
        if(s!=null) s.unsubscribe();
        s = o.monitor(applier);
    }

}
