/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.controller.io;

import java.util.Objects;
import java.util.UUID;

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
