/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.tagging;

import java.util.function.Predicate;
import jdk.nashorn.internal.ir.annotations.Immutable;
import util.access.FieldValue.FieldEnum;
import util.access.FieldValue.FieldedValue;

/**
 * Simple transfer class for result of a database query, that groups items by
 * one field and aggregates additional information about the groups.
 * <p>
 * An example is listing all artists and number of items per each. A query for
 * this uses GROUP BY 'specific field'
 * <p>
 * The accompanying information are always the same, the only changing variable
 * is the {@link Metadata.Field} according to which the items are grouped.
 *
 * @author Plutonium_
 */
@Immutable
public final class MetadataGroup implements FieldedValue<MetadataGroup,MetadataGroup.Field> {
    private final Metadata.Field field;
    private final Object value;
    private final long items;
    private final long albums;
    private final double length;
    private final long size;
    
    public MetadataGroup(Metadata.Field field, Object value, long item_count, long album_count, double length, long filesize_sum) {
        this.field = field;
        this.value = value;
        this.items = item_count;
        this.albums = album_count;
        this.length = length;
        this.size = filesize_sum;
    }
    
    public Metadata.Field getField() {
        return field;
    }
    
    public Object getValue() {
        return value;
    }
    
    public long getItemCount() {
        return items;
    }
    
    public long getAlbumCount() {
        return albums;
    }
    
    public FormattedDuration getLength() {
        return new FormattedDuration(length);
    }
    
    public FileSize getTotalFileSize() {
        return new FileSize(size);
    }
    
    /** {@inheritDoc} */
    @Override
    public Object getField(Field field) {
        switch(field) {
            case VALUE : return getValue();
            case ITEMS : return getItemCount(); 
            case ALBUMS : return getAlbumCount(); 
            case LENGTH : return getLength(); 
            case SIZE : return getTotalFileSize();
        }
        throw new AssertionError();
    }

    /** {@inheritDoc} */
    @Override
    public Field getMainField() { return Field.VALUE; }
    
    public Predicate<Metadata> toMetadataPredicate() {
        return m->m.getField(getField()).equals(getValue());
    }
    
/***************************** COMPANION CLASS ********************************/
    
    public static enum Field implements FieldEnum<MetadataGroup> {
        VALUE,
        ITEMS,
        ALBUMS,
        LENGTH,
        SIZE;
        
        public String toString(MetadataGroup group) {
            return this==VALUE ? group.getField().toStringEnum() : toStringEnum();
        }
        
        public String toString(Metadata.Field field) {
            return this==VALUE ? field.toStringEnum() : toStringEnum();
        }
        
        public static Field valueOfEnumString(String s) {
            if(ITEMS.toStringEnum().equals(s)) return ITEMS;
            if(ALBUMS.toStringEnum().equals(s)) return ALBUMS;
            if(LENGTH.toStringEnum().equals(s)) return LENGTH;
            if(SIZE.toStringEnum().equals(s)) return SIZE;
            else return VALUE;
//            return Enum.valueOf(Field.class, s.toUpperCase().replace(" ", "_"));
        }
        public static Field valueOfEnumString(String s, Metadata.Field field) {
            
            return Enum.valueOf(Field.class, s.toUpperCase().replace(" ", "_"));
        }
        
        /** {@inheritDoc} */
        @Override
        public Class getType() {
            switch(this) {
                case VALUE : return Object.class;
                case ITEMS : return long.class; 
                case ALBUMS : return long.class; 
                case LENGTH : return FormattedDuration.class; 
                case SIZE : return FileSize.class; 
            }
            throw new AssertionError();
        }
        
        public Class getType(Metadata.Field field) {
            switch(this) {
                case VALUE : return field.getType();
                case ITEMS : return long.class; 
                case ALBUMS : return long.class; 
                case LENGTH : return FormattedDuration.class; 
                case SIZE : return FileSize.class; 
            }
            throw new AssertionError();
        }
        
    }
}