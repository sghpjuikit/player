package audio.tagging;

import audio.Player;
import java.util.*;
import java.util.stream.Stream;
import javafx.util.Duration;
import util.SwitchException;
import util.access.fieldvalue.ObjectField;
import util.dev.TODO;
import util.functional.Functors.Ƒ1;
import util.units.FileSize;
import util.units.FormattedDuration;
import util.units.RangeYear;
import static java.util.stream.Collectors.toSet;
import static util.dev.TODO.Purpose.BUG;
import static util.functional.Util.equalNonNull;
import static util.functional.Util.groupBy;

/**
 * Simple transfer class for result of a database query, that groups items by
 * one field and aggregates additional information about the groups.
 * <p/>
 * An example is listing all artists and number of items per each. A query for
 * this uses GROUP BY 'specific field'
 * <p/>
 * The accompanying information are always the same, the only changing variable
 * is the {@link Metadata.Field} according to which the items are grouped.
 *
 * @author Martin Polakovic
 */
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

    public static Set<Metadata> degroup(Collection<MetadataGroup> groups) {
        return groups.stream().flatMap(group -> group.getGrouped().stream()).collect(toSet());
    }

    public static Set<Metadata> degroup(Stream<MetadataGroup> groups) {
        return groups.flatMap(group -> group.getGrouped().stream()).collect(toSet());
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

        for (Metadata m : ms) {
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

    public String getValueS(String empty_val) {
    	return Field.VALUE.toS(this, val, empty_val);
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

    /** {@link #getFileSize()} in bytes */
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

    /**  Is playing if any of the songs belonging to this group is playing. */
    @TODO(purpose = BUG, note = "We need to check the contained metadata instead of just comparing the group value")
    public boolean isPlaying() {
        return equalNonNull(field.getOf(Player.playingItem.get()),getValue());
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

    public static class Field<T> implements ObjectField<MetadataGroup,T> {
        public static final Set<Field<?>> FIELDS = new HashSet<>();
        public static final Field<Object> VALUE = new Field<>(Object.class, MetadataGroup::getValue,"Value", "Song field to group by");
        public static final Field<Long> ITEMS = new Field<>(Long.class, MetadataGroup::getItemCount,"Items", "Number of songs in the group");
        public static final Field<Long> ALBUMS = new Field<>(Long.class, MetadataGroup::getAlbumCount, "Albums", "Number of albums in the group");
        public static final Field<Duration> LENGTH = new Field<>(Duration.class, MetadataGroup::getLength, "Length", "Total length of the group");
        public static final Field<FileSize> SIZE = new Field<>(FileSize.class, MetadataGroup::getFileSize, "Size", "Total file size of the group");
        public static final Field<Double> AVG_RATING = new Field<>(Double.class, MetadataGroup::getAvgRating, "Avg rating", "Average rating of the group = sum(rating)/items");
        public static final Field<Double> W_RATING = new Field<>(Double.class, MetadataGroup::getWeighRating, "W rating", "Weighted rating of the group = sum(rating) = avg_rating*items");
        public static final Field<RangeYear> YEAR = new Field<>(RangeYear.class, MetadataGroup::getYear, "Year", "Year or years of songs in the group");

        private final String name;
        private final String desc;
        private final Ƒ1<? super MetadataGroup,? extends T> extractor;
        private final Class type;

        Field(Class<T> type, Ƒ1<? super MetadataGroup,? extends T> extractor, String name, String description) {
            this.name = name;
            this.desc = description;
            this.extractor = extractor;
            this.type = type;
            FIELDS.add(this);
        }

        public static Field<?> valueOf(String s) {
            if (ITEMS.name().equals(s)) return ITEMS;
            if (ALBUMS.name().equals(s)) return ALBUMS;
            if (LENGTH.name().equals(s)) return LENGTH;
            if (SIZE.name().equals(s)) return SIZE;
            if (AVG_RATING.name().equals(s)) return AVG_RATING;
            if (W_RATING.name().equals(s)) return W_RATING;
            if (YEAR.name().equals(s)) return YEAR;
            else return VALUE;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public String description() {
            return desc;
        }

        @Override
        public T getOf(MetadataGroup mg) {
            return extractor.apply(mg);
        }

        public String toString(MetadataGroup group) {
            return toString(group.getField());
        }

        public String toString(Metadata.Field field) {
            return this==VALUE ? field.toString() : toString();
        }

        @Override
        public Class getType() {
            return type;
        }

        public Class getType(Metadata.Field field) {
            return (this==VALUE) ? field.getType() : getType();
        }

        @Override
        public String toS(T o, String empty_val) {
            if (this==VALUE)  return o==null || "".equals(o) ? "<none>" : o.toString();
            if (this==ITEMS || this==ALBUMS || this==LENGTH || this==SIZE || this==AVG_RATING || this==W_RATING) return o.toString();
            if (this==YEAR) throw new SwitchException(this); // year case should never execute
            throw new SwitchException(this);
        }

        @Override
        public String toS(MetadataGroup v, T o, String empty_val) {
            if (this==VALUE) {
                if (v==null || v.all_flag) return "<any>";
                return v.getField().toS(o, "<none>");
            } else if (this==YEAR) {
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
}