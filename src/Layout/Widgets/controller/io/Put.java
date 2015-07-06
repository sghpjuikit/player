/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.controller.io;

import static Layout.Widgets.controller.io.Output.getStringConverter;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.reactfx.Subscription;
import static util.reactive.Util.maintain;

/**
 *
 * @author Plutonium_
 */
public class Put<T> {
    
    final Class<T> type;
    final ObjectProperty<T> val = new SimpleObjectProperty();

    public Put(Class<T> type, T init_val) {
        this.type = type;
        this.val.setValue(init_val);
    }

    public Class<T> getType() {
        return type;
    }

    public T getValue() {
        return val.get();
    }
    
    public String getValueAsS() {
        T v = val.getValue();
        return v==null ? "null" : getStringConverter(v.getClass()).apply(v);
//        return "";
    }
    
    public Subscription monitor(Consumer<? super T> action) {
        return maintain(val, action);
    }
    
    
    
}
