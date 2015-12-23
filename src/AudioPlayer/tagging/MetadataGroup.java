/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.tagging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import jdk.nashorn.internal.ir.annotations.Immutable;
import util.SwitchException;
import util.access.FieldValue.ObjectField;
import util.functional.Functors.Ƒ1;
import util.units.FileSize;
import util.units.FormattedDuration;
import util.units.RangeYear;

import static util.Util.capitalizeStrong;
import static util.Util.mapEnumConstant;
import static util.functional.Util.groupBy;

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
    private final Object val;
    private final long items;
    private final long albums;
    private final double length;
    private final long size;
    private final double avg_rating;
    private final double weigh_rating;
    private final RangeYear years;
    private final List<Metadata> metadatas;
    private final boolean all_flag;

    public static MetadataGroup groupOf(Metadata.Field f, Collection<Metadata> ms) {
        return new MetadataGroup(f, true, getAllValue(f), ms);
    }

    public static Stream<MetadataGroup> groupsOf(Metadata.Field f, Collection<Metadata> ms) {
        return groupBy(ms.stream(),f::getGroupedOf).entrySet().stream().map(e -> new MetadataGroup(f, false,e.getKey(),e.getValue()));
    }

    private MetadataGroup(Metadata.Field f, boolean isAll, Object value, Collection<Metadata> ms) {
        metadatas = new ArrayList<>(ms);
        field = f;
        items = ms.size();
        val = value;
        all_flag = isAll;
        years = new RangeYear();

        Set<String> albumset = new HashSet<>();
        double lengthsum = 0;
        long sizesum = 0;
        double ratingsum = 0;

        for(Metadata m : ms) {
            albumset.add(m.getAlbum());
            lengthsum += m.getLengthInMs();
            sizesum += m.getFilesizeInB();
            ratingsum += m.getRatingPercent();
            years.accumulate(m.getYearAsInt());
        }

        albums = albumset.size();
        length = lengthsum;
        size = sizesum;
        avg_rating = ratingsum/items;
        weigh_rating = avg_rating*items;
    }

    public List<Metadata> getGrouped() {
        return metadatas;
    }

    public Metadata.Field getField() {
        return field;
    }

    public Object getValue() {
        return val;
    }

    public boolean isAll() {
        return all_flag;
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

    public RangeYear getYear() {
        return years;
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
        VALUE(Object.class, MetadataGroup::getValue,"Song field to group by"),
        ITEMS(Long.class, MetadataGroup::getItemCount,"Number of songs in the group"),
        ALBUMS(Long.class, MetadataGroup::getAlbumCount,"Number of albums in the group"),
        LENGTH(Double.class, MetadataGroup::getLength,"Total length of the group"),
        SIZE(FileSize.class, MetadataGroup::getFileSize,"Total file size of the group"),
        AVG_RATING(Double.class, MetadataGroup::getAvgRating,"Average rating of the group = sum(rating)/items"),
        W_RATING(Double.class, MetadataGroup::getWeighRating,"Weighted rating of the group = sum(rating) = avg_rating*items"),
        YEAR(RangeYear.class, MetadataGroup::getYear,"Year or years of songs in the group");

        private final String desc;
        private final Ƒ1<MetadataGroup,?> extr;
        private final Class type;

        <T> Field(Class<T> type, Ƒ1<MetadataGroup,?> extractor, String description) {
            mapEnumConstant(this, c -> capitalizeStrong(c.name().replace('_', ' ')));
            this.desc = description;
            this.extr = extractor;
            this.type = type;
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
            return toString(group.getField());
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
            return type;
        }

        public Class getType(Metadata.Field field) {
            return (this==VALUE) ? field.getType() : getType();
        }

        @Override
        public String toS(Object o, String empty_val) {
            switch(this) {
                case VALUE : return o==null || "".equals(o) ? "<none>" : o.toString();
                case ITEMS :
                case ALBUMS :
                case LENGTH :
                case SIZE :
                case AVG_RATING :
                case W_RATING : return o.toString();
                case YEAR : throw new SwitchException(this); // year case should never execute
                default : throw new SwitchException(this);
            }
        }

        @Override
        public String toS(MetadataGroup v, Object o, String empty_val) {
            if(this==VALUE) {
                if(v==null || v.all_flag) return "<any>";
                return v.getField().toS(o, "<none>");
            } else if(this==YEAR) {
                return v==null || !v.years.hasSpecific() ? empty_val : v.years.toString();
            } else {
                return toS(o,empty_val);
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

    @util.dev.TODO(note = "this may need some work")
    private static Object getAllValue(Metadata.Field f) {
        return f.isTypeString() ? "" : null;
    }

//     grouping buckets - requires standalone classes...
//    private static final FileSize FILESIZE_UNKNOWN = new FileSize(-1);
//    private static final FileSize FILESIZE_UNKNOWN = new FileSize(1024);
//
//    private static Object getGroupValue(Metadata.Field f, Metadata m) {
//        if(f==FILESIZE) {
//            FileSize s = (FileSize) FILESIZE.getOf(m);
//            double b = s.inBytes();
//            if(b==-1) return
//        }
//        return f.getOf(m);
//    }
}