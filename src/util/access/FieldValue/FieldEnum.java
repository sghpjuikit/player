/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access.FieldValue;

import util.SteroidObject;
import util.Util;
import static util.Util.mapEnumConstant;
import util.access.TypedValue;

/**
 *
 * @author Plutonium_
 */
public interface FieldEnum<T> extends TypedValue, SteroidObject {
    
    /** Returns description of the field. */
    public String description();
    
    /** Returns name of the field. */
    public String name();
    
    /** 
     * Used as string converter for fielded values. For example in tables. 
     * When the object signifies empty value, empty string is returned.
     */
    public String toS(Object o, String empty_val);
    
    
    public static enum ColumnField implements FieldEnum<Object>{
        INDEX;
        
        private ColumnField() {
            mapEnumConstant(this, f -> f.ordinal()==0 ? "#" : Util.enumToHuman(f));
        }
        
        @Override
        public String description() {
            return "";
        }

        @Override
        public String toS(Object o, String empty_val) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Class getType() {
            return Integer.class;
        }
        
    }
}