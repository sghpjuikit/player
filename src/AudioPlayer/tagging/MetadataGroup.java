/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.tagging;

import java.time.Year;
import java.util.Set;

import jdk.nashorn.internal.ir.annotations.Immutable;
import util.access.FieldValue.ObjectField;
import util.collections.Histogram;
import util.functional.Functors.Ƒ1;
import util.units.FileSize;
import util.units.FormattedDuration;

import static util.Util.capitalizeStrong;
import static util.Util.mapEnumConstant;

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
public final class MetadataGroup {
    private final Metadata.Field field;
    private final Object value;
    private final long items;
    private final long albums;
    private final double length;
    private final long size;
    private final double avg_rating;
    private final double weigh_rating;
    private final Year year;

    public MetadataGroup(Metadata.Field field, Object value, long item_count,
                         long album_count, double length, long filesize_sum,
                         double avg_rating, Set<Year> year) {
        this.field = field;
        this.value = value;
        this.items = item_count;
        this.albums = album_count;
        this.length = length;
        this.size = filesize_sum;
        this.avg_rating = avg_rating;
        this.weigh_rating = avg_rating*item_count;
        this.year = year.size()==0 ? null : year.size()==1 ? year.stream().findAny().orElseGet(null) : Year.of(-1);
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

    public Year getYear() {
        return year;
    }

    /** {@inheritDoc} */
    public Field getMainField() {
        return Field.VALUE;
    }

    @Override
    public String toString() {
        return getField() + ": " + getValue() + ", items: " + getItemCount() +
                ", albums: " + getAlbumCount() + ", length: " + getLength() +
                ", size: " + getFileSize()+ ", avgrating: " + getAvgRating() +
                ", wighted rating: " + getWeighRating()+ ", year: " + getYear();
    }

/***************************** COMPANION CLASS ********************************/

    public static enum Field implements ObjectField<MetadataGroup> {
        VALUE(MetadataGroup::getValue,"Song field to group by"),
        ITEMS(MetadataGroup::getItemCount,"Number of songs in the group"),
        ALBUMS(MetadataGroup::getAlbumCount,"Number of albums in the group"),
        LENGTH(MetadataGroup::getLength,"Total length of the group"),
        SIZE(MetadataGroup::getFileSize,"Total file size of the group"),
        AVG_RATING(MetadataGroup::getAvgRating,"Average rating of the group = sum(rating)/items"),
        W_RATING(MetadataGroup::getWeighRating,"\"Weighted rating of the group = sum(rating) = avg_rating*items"),
        YEAR(MetadataGroup::getYear,"Year of songs in the group or '...' if multiple");

        private final String desc;
        private final Ƒ1<MetadataGroup,?> extr;

        Field(Ƒ1<MetadataGroup,?> extractor, String description) {
            mapEnumConstant(this, c -> capitalizeStrong(c.name().replace('_', ' ')));
            this.desc = description;
            this.extr = extractor;
        }

        @Override
        public String description() {
            return desc;
        }

        @Override
        public Object getOf(MetadataGroup mg) {
            return extr.apply(mg);
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
                case YEAR : return Year.class;
            }
            throw new AssertionError();
        }

        public Class getType(Metadata.Field field) {
            return (this==VALUE) ? field.getType() : getType();
        }

        @Override
        public String toS(Object o, String empty_val) {
            switch(this) {
                case VALUE : return o==Histogram.ALL ? "All" : "".equals(o) ? "<none>" : o.toString();
                case ITEMS :
                case ALBUMS :
                case LENGTH :
                case SIZE :
                case AVG_RATING :
                case W_RATING : return o.toString();
                case YEAR : return o==null ? empty_val : Year.of(-1).equals(o) ? "..." : o.toString();
                default : throw new AssertionError("Default case should never execute");
            }
        }

        @Override
        public boolean c_visible() {
            return this!=AVG_RATING && this!=YEAR && this!=W_RATING;
        }

        @Override
        public double c_width() {
            return this==VALUE ? 250 : 70;
        }


    }
}