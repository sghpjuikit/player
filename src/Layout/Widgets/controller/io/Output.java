/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.controller.io;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.reactfx.Subscription;
import static util.reactive.Util.maintain;

/**
 *
 * @author Plutonium_
 */
public class Output<T> {
    public final Id id;
    final Class<T> type;
    final ObjectProperty<T> val = new SimpleObjectProperty();
    
        
    public Output(UUID id, String name, Class c) {// System.out.println(id);
        this.id = new Id(id, name);
        this.type = c;
    }
    
    
    public String getName() {
        return id.name;
    }
    public Class<T> getType() {
        return type;
    }
    
    public T getValue() {
        return val.getValue();
    }
    
    public void setValue(T v) {
        val.setValue(v);
    }
    
    public Subscription monitor(Consumer<? super T> action) {
        return maintain(val, action);
    }

    @Override
    public boolean equals(Object o) {
        if(this==o) return true;
        return o instanceof Output ? id.equals(((Output)o).id) : false;
    }

    @Override
    public int hashCode() {
        return 5 * 89 + Objects.hashCode(this.id);
    }
    
    
    
    
    
    
    private Function<? super T,String> toS = null;

    public Output<T> setStringConverter(Function<? super T,String> c) {
        toS = c;
        return this;
    }
    
    public String getValueAsS() {
        T v = val.getValue();
        return v==null ? "null" : toS==null ? v.toString() : toS.apply(v);
    }
    
    
    public static class Id {
        public final UUID carrier_id;
        public final String name;

        public Id(UUID carrier_id, String name) {
            this.carrier_id = carrier_id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if(this==o) return true;
            if(o instanceof Id) 
                return ((Id)o).name.equals(name) && ((Id)o).carrier_id.equals(carrier_id);
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + Objects.hashCode(this.carrier_id);
            hash = 79 * hash + Objects.hashCode(this.name);
            return hash;
        }

        @Override
        public String toString() {
            return name + "," + carrier_id.toString();
        }
        
        public static Id fromString(String s) {
            int i = s.indexOf(",");
            String n = s.substring(0,i);
            UUID u = UUID.fromString(s.substring(i+1, s.length()));
            return new Id(u,n);
        }
        
    }

}
