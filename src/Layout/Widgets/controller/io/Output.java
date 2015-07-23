/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.controller.io;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import util.collections.map.ClassMap;

/**
 *
 * @author Plutonium_
 */
public class Output<T> extends Put<T> {
    public final Id id;
    
        
    public Output(UUID id, String name, Class<T> c) {// System.out.println(id);
        super(c, null);
        this.id = new Id(id, name);
    }
    
    
    public String getName() {
        return id.name;
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
    
    
    private static final ClassMap<Function<Object,String>> string_coverters = new ClassMap<>();
    
    public static <T> void addStringConverter(Class<T> c, Function<? extends T,String> f) {
        string_coverters.put(c, (Function) f);
    }
    public static <T> Function<Object,String> getStringConverter(Class<T> c) {
        List<Function<Object,String>> f = string_coverters.getElementsOfSuper(c);
        return f.isEmpty() ? Object::toString : f.get(0);
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
