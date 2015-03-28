/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.tagging;

import java.util.function.Predicate;
import jdk.nashorn.internal.ir.annotations.Immutable;
import static util.Util.capitalizeStrong;
import static util.Util.mapEnumConstant;
import util.access.FieldValue.FieldEnum;
import util.access.FieldValue.FieldedValue;
import util.units.FileSize;
import util.units.FormattedDuration;

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
    private final double avg_rating;
    private final double weigh_rating;
    private final String year;
    
    public MetadataGroup(Metadata.Field field, Object value, long item_count, 
                         long album_count, double length, long filesize_sum, 
                         double avg_rating, String year) {
        this.field = field;
        this.value = value;
        this.items = item_count;
        this.albums = album_count;
        this.length = length;
        this.size = filesize_sum;
        this.avg_rating = avg_rating;
        this.weigh_rating = avg_rating*item_count;
        this.year = year;
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
    
    /** @return the length */
    public FormattedDuration getLength() {
        return new FormattedDuration(length);
    }
    
    /** @return the length in milliseconds */
    public double getLengthInMs() {
        return length;
    }
    
    /** get total file size */
    public FileSize getFileSize() {
        return new FileSize(size);
    }
    
    /** {@link getFilesize()} in bytes */
    public long getFileSizeInB() {
        return size;
    }
    
    public double getAvgRating() {
        return avg_rating;
    }
    public double getWeighRating() {
        return weigh_rating;
    }
    
    public String getYear() {
        return year;
    }
    
    /** {@inheritDoc} */
    @Override
    public Object getField(Field field) {
        switch(field) {
            case VALUE : return getValue();
            case ITEMS : return getItemCount(); 
            case ALBUMS : return getAlbumCount(); 
            case LENGTH : return getLength(); 
            case SIZE : return getFileSize();
            case AVG_RATING : return getAvgRating();
            case W_RATING : return getWeighRating();
            case YEAR : return getYear();
        }
        throw new AssertionError();
    }

    /** {@inheritDoc} */
    @Override
    public Field getMainField() { 
        return Field.VALUE;
    }
    
    public Predicate<Metadata> toMetadataPredicate() {
        return m->m.getField(field).equals(value);
    }

    @Override
    public String toString() {
        return getField() + ": " + getValue() + ", items: " + getItemCount() + 
                ", albums: " + getAlbumCount() + ", length: " + getLength() + 
                ", size: " + getFileSize()+ ", avgrating: " + getAvgRating() +
                ", wighted rating: " + getWeighRating()+ ", year: " + getYear();
    }
    
/***************************** COMPANION CLASS ********************************/
    
    public static enum Field implements FieldEnum<MetadataGroup> {
        VALUE,
        ITEMS,
        ALBUMS,
        LENGTH,
        SIZE,
        AVG_RATING,
        W_RATING,
        YEAR;
        
        Field() {
            mapEnumConstant(this, c->capitalizeStrong(c.name().replace('_', ' ')));
        }

        @Override
        public String getDescription() {
            switch(this) {
                case VALUE : return "Song field to group by";
                case ITEMS : return "Number of songs in the group";
                case ALBUMS : return "Number of albums in the group";
                case LENGTH : return "Total length of the group";
                case SIZE : return "Total file size of the group";
                case AVG_RATING : return "Average rating of the group = sum(rating)/items";
                case W_RATING : return "Weighted rating of the group = sum(rating) = avg_rating*items";
                case YEAR : return "Year of songs in the group or '...' if multiple";
            }
            throw new AssertionError();
        }
        
        public boolean isCommon() {
            return this!=AVG_RATING && this!=YEAR && this!=W_RATING;
        }
        
        public String toString(MetadataGroup group) {
            return this==VALUE ? group.getField().toString() : toString();
        }
        
        public String toString(Metadata.Field field) {
            return this==VALUE ? field.toString() : toString();
        }
        
        public static Field valueOfEnumString(String s) {
            if(ITEMS.name().equals(s)) return ITEMS;
            if(ALBUMS.name().equals(s)) return ALBUMS;
            if(LENGTH.name().equals(s)) return LENGTH;
            if(SIZE.name().equals(s)) return SIZE;
            if(AVG_RATING.name().equals(s)) return AVG_RATING;
            if(W_RATING.name().equals(s)) return W_RATING;
            if(YEAR.name().equals(s)) return YEAR;
            else return VALUE;
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
                case AVG_RATING : return double.class;
                case W_RATING : return double.class;
                case YEAR : return String.class;
            }
            throw new AssertionError();
        }
        
        public Class getType(Metadata.Field field) {
            return (this==VALUE) ? field.getType() : getType();
        }
        
    }
}