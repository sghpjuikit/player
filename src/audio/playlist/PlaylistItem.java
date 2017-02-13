package audio.playlist;

import audio.Item;
import audio.tagging.Metadata;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.media.Media;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import services.database.Db;
import util.SwitchException;
import util.access.fieldvalue.ObjectField;
import util.file.AudioFileFormat;
import util.file.AudioFileFormat.Use;
import util.file.Util;
import util.functional.Functors.Ƒ1;
import util.units.FormattedDuration;
import static util.async.Async.runFX;
import static util.dev.Util.log;
import static util.dev.Util.throwIfFxThread;
import static util.file.AudioFileFormat.Use.APP;

/**
 * Defines item in playlist.
 * <p/>
 * As a playlist item this object carries three pieces of information: artist,
 * title and time duration, besides URI as an Item object.
 * <p/>
 * Cannot be changed, only updated. In order to limit object initialization
 * performance impact of I/O tag read operation only URI is necessary to
 * instantiate this class. In such case, the other fields must be manually
 * updated by calling the update() method.
 * <pre>
 * The lifecycle of this object is as follows:
 * - created (either updated with all fields initialized to real values or
 * requiring update with time initialized to 0 and name to {@link #getInitialName()}
 * - updated (if not created updated)
 * - updated if application decides the item is/can be out of date
 * - corrupted (if application discovers the underlying resource is no longer
 *  available)
 * </pre>
 * SERIALIZATION
 * - this class is serializable by XStream using PlaylistItemConverter.
 * <p/>
 * Note:
 * Dont try to change property implementations SimpleObjectProperty
 * into more generic ObjectProperty. It will cause XStream serializing
 * to malperform (java7)(needs more testing).
 */
public final class PlaylistItem extends Item<PlaylistItem> {

    private final SimpleObjectProperty<URI> uri;
    private final SimpleObjectProperty<FormattedDuration> time;
    /** Consists of item's artist and title separated by separator string. */
    private final SimpleStringProperty name;
    private String artist;
    private String title;
    private boolean updated;
    boolean corrupted = false;
    @Deprecated public volatile boolean playbackerror = false;

    /**
     * URI Constructor.
     * Use when only uri is known. Item is initialized to updated = false. Use
     * update() method to get other fields - update the item.
     *
     * @param _uri
     */
    public PlaylistItem(URI _uri) {
        uri = new SimpleObjectProperty<>(_uri);
        name = new SimpleStringProperty(getInitialName());
        time = new SimpleObjectProperty<>(new FormattedDuration(0));
        updated = false;
    }

    /**
     * Full Constructor.
     * Use when all info is available. Item is initialized to updated state,
     * which avoids updating it and consequent I/O operation.
     * <p/>
     * When the parameter values are still intended to be updated, do not use
     * this constructor.
     *
     * @param new_uri
     * @param art
     * @param titl
     * @param _length of the item in miliseconds.
     */
    public PlaylistItem(URI new_uri, String art, String titl, double _length) {
        uri = new SimpleObjectProperty<>(new_uri);
        name = new SimpleStringProperty();
        setATN(art, titl);
        time = new SimpleObjectProperty<>(new FormattedDuration(_length));
        updated = true;
    }

    /**
     * @return the url. Never null.
     */
    @Override
    public URI getURI() {
        return uri.get();
    }

    public StringProperty uriProperty() {
        return name;
    }

    /**
     * @return value of the {@link #name name}. Never null.
     */
    public String getName() {
        return name.get();
    }

    /**
     * @return {@link #name name property}.
     */
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * @return the artist portion of the name. Empty string if item
     * wasnt updated yet. Never null.
     */
    public String getArtist() {
        return artist==null ? "" : artist;
    }

    /**
     * @return the title portion of the name. Empty string if item
     * wasnt updated yet. Never null.
     */
    public String getTitle() {
        return title==null ? "" : title;
    }

    /** @return the time. ZERO if item wasnt updated yet. Never null. */
    public FormattedDuration getTime() {
        return time.get();
    }

    /** @return the time in millisecods. 0 if item wasnt updated yet. */
    public double getTimeMs() {
        return time.get().toMillis();
    }

    /**
     * Until the item is updated the value wrapped inside the property
     * will be 0.
     * @return
     */
    public SimpleObjectProperty<FormattedDuration> timeProperty() {
        return this.time;
    }

    /**
     * Updates this item by reading the tag of the source file.
     * Involves I/O, so dont use on main thread. Safe to call from bgr thread.
     * <p/>
     * Calling this method on updated playlist item has no effect. E.g.:
     * <ul>
     * <li> calling this method more than once
     * <li> calling this method on playlist item created from metadata
     * <p/>
     * note: {@code this.toMeta().toPlaylist()} effectively
     * prevents unupdated items from ever updating. Never use toMeta where full
     * metadata object is required.
     * </ul>
     */
    public void update() {
        if (updated || isCorrupt(APP)) return;
        updated = true;

        // if library contains the item, use it & avoid I/O
        // improves performance almost 100-fold when item in library
	    String id = getId();
        if (Db.items_byId.containsKey(id)) {
            update(Db.items_byId.get(id));
            return;
        }

        if (isFileBased()) {
		    throwIfFxThread();
            // update as file based item
            try {
                // read tag for data
                AudioFile f = AudioFileIO.read(getFile());
                Tag t = f.getTag(); // tag can be null!
                AudioHeader h = f.getAudioHeader();

                // get values
                double length = 1000 * h.getTrackLength();
                artist = t==null ? getArtist() : t.getFirst(FieldKey.ARTIST);
                title = t==null ? getTitle() : t.getFirst(FieldKey.TITLE);
                // set values always on fx thread
                runFX(() -> {
                    setATN(artist, title);
                    time.set(new FormattedDuration(length));
                });
            } catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
                log(PlaylistItem.class).error("Playlist item update failed:", this,e);
            }
        } else {
            // update as web based item
            try{
                Media m = new Media(getURI().toString());
                setATN("", "");
                time.set(new FormattedDuration(m.getDuration().toMillis()));
            } catch (IllegalArgumentException | NullPointerException | UnsupportedOperationException e) {
                corrupted = true;   // mark as corrupted on error
            }
        }
    }

    /** Updates this playlist item to data from provided metadata. No I/O. */
    public void update(Metadata m) {
        uri.set(m.getURI());
        setATN(m.getArtist(), m.getTitle());
        time.set(m.getLength());
        updated = true;
    }

    private void setATN(String art, String titl) {
        artist = art;
        title = titl.isEmpty() ? Util.getName(getURI()) : titl;
        if (artist.isEmpty() && title.isEmpty())
            name.set(getInitialName());
        else
            name.set(artist.isEmpty() ? title : artist + " - " + title);
    }

    /**
     * Returns true if the item was marked updated. Once item is updated it will
     * stay in that state. Updated item guarantees that all its values are
     * valid, but does not guarantee that they are up to date. For manipulation
     * within the application there should be no need to update the item again.
     * If the item changes, the change should be handled by the application.
     *
     * This method does not solve is as it marks items with invalid data. Because
     * there is no guarantee that every item is valid, when it is required for
     * item to be, update() can be called after this method returns false.
     *
     * Creating item in URI constructor will result in initialization of
     * updated to false. Method update() then must be called manually.
     * @return
     */
    boolean isUpdated() {
        return updated;
    }

    /** Returns true if this item is corrupted. */
    @Override
    public boolean isCorrupt(AudioFileFormat.Use use) {
        AudioFileFormat f = getFormat();
        corrupted = playbackerror || !f.isSupported(Use.PLAYBACK) || isCorruptWeak();
        return corrupted;
    }

    /**
     * Returns true if this item was marked corrupt last time it was checked. This
     * doesn't necessarily reflect the real value. The method
     * returns cached value so the curruptness check involving I/O can be avoided.
     * Use when performance is prioritized, for example when iterating lists in
     * tables.
     * <p/>
     * If the validity of the check is prioritized, use {@link #isCorrupt(util.file.AudioFileFormat.Use)}}.
     * @return cached corrupted value
     */
    public boolean isCorruptCached() {
        return corrupted;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation returns metadata with artist, length and title fields
     * set as defined in this item, leaving other fields else empty.
     * <p/>
     * This method shouldnt be run before this item is updated. See
     * {@link #isUpdated()}.
     * @return
     */
    @Override
    public Metadata toMeta() {
        return super.toMeta();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation returns this object.
     */
    @Override
    public PlaylistItem toPlaylist() {
        return this;
    }

    /** @return complete information on this item - artist, title, length */
    @Override
    public String toString() {
        return getName() + "\n"
             + getURI().toString() + "\n"
             + getTime().toString();
    }

    /**
     * Two playlistItems are equal if and only if they are the same object. Equivalent
     * to this == item, which can be used instead.
     *
     * @param item
     * @return
     */
    @Override
    public boolean equals(Object item) {
        return this == item;
    }

    @Override
    public int hashCode() {
	    return System.identityHashCode(this);
    }

    /**
     * Compares by name.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public int compareTo(PlaylistItem o) {
        return getName().compareToIgnoreCase(o.getName());
    }

    /**
     * Clones the item.
     *
     * @return copy of the item or null if parameter null.
     */
    public PlaylistItem copy() {
        PlaylistItem i = new PlaylistItem(getURI(), getArtist(), getTitle(), getTime().toMillis());
                     i.updated = updated;
                     i.corrupted = corrupted;
        return i;
    }

    public static class Field<T> implements ObjectField<PlaylistItem,T> {

        public static final Set<Field<?>> FIELDS = new HashSet<>();
        public static final Field<String> NAME = new Field<>(PlaylistItem::getName, "Name", "'Song artist' - 'Song title'");
        public static final Field<String> TITLE = new Field<>(PlaylistItem::getTitle, "Title", "Song title");
        public static final Field<String> ARTIST = new Field<>(PlaylistItem::getArtist, "Artist", "Song artist");
        public static final Field<FormattedDuration> LENGTH = new Field<>(PlaylistItem::getTime, "Time", "Song length");
        public static final Field<String> PATH = new Field<>(PlaylistItem::getPath, "Path", "Song file path");
        public static final Field<AudioFileFormat> FORMAT = new Field<>(PlaylistItem::getFormat, "Format", "Song file type");

        private final String name;
        private final String description;
        private final Ƒ1<? super PlaylistItem,? extends T> extractor;

        Field(Ƒ1<? super PlaylistItem,? extends T> extractor, String name, String description) {
            this.name = name;
            this.description = description;
            this.extractor = extractor;
            FIELDS.add(this);
        }

        public static Field<?> valueOf(String s) {
            if (NAME.name().equals(s)) return NAME;
            if (TITLE.name().equals(s)) return TITLE;
            if (ARTIST.name().equals(s)) return ARTIST;
            if (LENGTH.name().equals(s)) return LENGTH;
            if (PATH.name().equals(s)) return PATH;
            if (FORMAT.name().equals(s)) return FORMAT;
            throw new SwitchException(s);
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
            return description;
        }

        @Override
        public T getOf(PlaylistItem p) {
            return extractor.apply(p);
        }

        /**
         * Returns true.
         * <p/>
         * {@inheritDoc}
         */
        @Override
        public boolean isTypeStringRepresentable() { return true; }

        /** {@inheritDoc} */
        @Override
        public Class getType() {
            if (this==FORMAT) return AudioFileFormat.class;
            else if (this==LENGTH) return FormattedDuration.class;
            else return String.class;
        }

        /**
         * Returns true.
         * <p/>
         * {@inheritDoc}
         */
        @Override
        public boolean isTypeNumberNonegative() { return true; }

        @Override
        public String toS(T o, String empty_val) {
            if (this==NAME || this==TITLE || this==ARTIST) return "".equals(o) ? empty_val : o.toString();
            if(this==LENGTH || this==PATH || this==FORMAT) return o.toString();
            throw new SwitchException(this);
        }

        @Override
        public double c_width() {
            return 60;
        }

        @Override
        public boolean c_visible() {
            return this==NAME || this==LENGTH;
        }

    }
}