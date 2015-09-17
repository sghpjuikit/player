/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access.FieldValue;

import java.io.File;

import util.File.FileUtil;
import util.Util;
import util.access.TypedValue;
import util.functional.Functors.Ƒ1;
import util.units.FileSize;

import static util.Util.mapEnumConstant;

/**
 *
 * @author Plutonium_
 */
public interface FieldEnum<V> extends TypedValue {

    /** Returns description of the field. */
    public String description();

    /** Returns name of the field. */
    public String name();

    /**
     * Used as string converter for fielded values. For example in tables.
     * When the object signifies empty value, empty string is returned.
     */
    public String toS(Object o, String empty_val);

    /**
     * Variation of {@link #toString()} method.
     * Converts first letter of the string to upper case.
     */
    public default String toStringCapital() {
        String s = toString();
        return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * Variation of {@link #toString()} method.
     * Converts first letter of the string to upper case and all others into
     * lower case.
     */
    public default String toStringCapitalCase() {
        String s = toString();
        return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    /**
     * Variation of {@link #toString()} method.
     * Converts first letter of the string to upper case and all others into
     * lower case and replaces all '_' with ' '.
     * <p>
     * Use to make {@link Enum} constants more human readable, for gui for example.
     */
    public default String toStringEnum() {
        String s = toString().replaceAll("_", " ");
        return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }


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
    public static interface ObjectField<V> extends FieldEnum<V> {
        public Object getOf(V value);
    }

    public static enum FileField implements ObjectField<File> {

        PATH("Path",File::getPath, String.class),
        TYPE("Size",FileSize::new, FileSize.class),
        SIZE("Type",f -> f.isDirectory() ? "Directory" : FileUtil.getSuffix(f), String.class);

        private final String description;
        private final Ƒ1<File,?> mapper;
        private final Class<?> type;

        private <T> FileField(String description, Ƒ1<File,T> mapper, Class<T> type){
            this.mapper = mapper;
            this.description = description;
            this.type = type;
        }

        @Override
        public Object getOf(File value) {
            return mapper.apply(value);
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public String toS(Object o, String empty_val) {
            return o==null ? empty_val : o.toString();
        }

        @Override
        public Class getType() {
            return type;
        }
    }
}