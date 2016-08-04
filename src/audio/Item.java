
package audio;

import java.io.File;
import java.net.URI;
import java.util.Comparator;

import audio.playlist.PlaylistItem;
import audio.tagging.Metadata;
import util.file.AudioFileFormat;
import util.file.AudioFileFormat.Use;
import util.units.FileSize;

import static util.file.AudioFileFormat.Use.PLAYBACK;

/**
 * Representation of audio resource referenced by {@link URI}.
 * <p/>
 * Item has two distinct identities:
 * <ul>
 * <li> Resource identity revolves around the underlying resource this item
 * represents. Implementation independent (all subclasses will work this way).
 * Obtained by {@link #getURI()} and compared by {@link #same(Item)}
 * <li> Object identity revolves around identity of this item as an object in
 * the application. For example items in a playlist
 * can have same resource identity (same song being in the playlist twice), but
 * their object identity must differ (only one of them can be played at any time).
 * Implementation dependent - check each subclass for specific
 * information
 * Represented by {@link #hashCode()} and compared by {@link #equals(java.lang.Object)}
 * </ul>
 *
 * @param <CT> ComparableType. Subclasses should implements their own - self.
 * @author Martin Polakovic
 */
public abstract class Item<CT extends Item> implements Comparable<CT> {

    /**
     * Returns the resource identifier denoting the audui resorce of this item.
     * @return URI as a resource of this item.*/
    abstract public URI getURI();

    public String getId() {
        return getURI().toString();
    }

    /**
     * Item based on file is represented by a file in a file system on a local
     * system. Non file based item can for example be file on the web accessed
     * through http protocol.
     * @return whether item is filebased.
     */
    public boolean isFileBased() {
        return "file".equals(getURI().getScheme());
    }

    /**
     * Returns absolute file of the item.
     * @return file representation of the item.
     * @throws UnsupportedOperationException if item is not file based
     */
    public File getFile() {
        return new File(getURI()).getAbsoluteFile();
    }

    /**
     * Returns human-readable string representation of the path to the resource
     * of this item. Useful for displaying path in the graphical user interface.
     * <p/>
     * Uses getUri().getPath() and removes leading '/' character.
     * <p/>
     * The path does not guarantee the possibility to backward-reconstruct the
     * original URI resource, and must not be used this way.
     * @return string portion of the URI of this item or "" if not available
     */
    public String getPath() {
        String path = getURI().getPath();
        return path==null || path.isEmpty()
                ? "" : getURI().getPath().substring(1);
    }

    /**
     * Parent directory of the resource. Only for file based items.
     * <p/>
     * Use to get location of the item, for example to fetch additional resources
     * located there, such as cover.
     * @see #getPath()
     * @return parent directory of the item in the file system
     * @throws UnsupportedOperationException if item is not file based
     */
    public File getLocation() {
        if (!isFileBased()) throw new UnsupportedOperationException("Item is not file based.");
        return getFile().getParentFile();
    }

    /**
     * Returns name of the file without its suffix.
     * @return the filename without suffix
     */
    public String getFilename() {
        String n = getFilenameFull();
        // remove extension
        int p = n.lastIndexOf('.');
        return (p == - 1) ? n : n.substring(0,p);
    }

    /**
     * Returns name of the file with its suffix.
     * @return the filename with suffix or empty string if none.
     */
    public String getFilenameFull() {
        String p = getURI().getPath();
        // should not happen ever, but just in case some damaged URL gets through
        if (p==null || p.isEmpty()) return "";
        int i = p.lastIndexOf('/');
        // another exceptional state check (just in case, might be unnecessary)
        if (i==-1 || p.length()<2) return p;
        // get name portion of the path
        return p.substring(i+1);
    }

    /**
     * Returns suffix of the filename. For example: "mp3, flac"
     * <p/>
     * It does not necessarily reflect real type of the file. Dont use this method
     * to find out type of the file. Use {@link #getFormat()}.
     * @return the suffix of the file of this item or empty string if none.
     */
    public String getSuffix() {
        String n = getFilenameFull();
        // remove name
        int p = n.lastIndexOf('.');
        return (p == - 1) ? "" : n.substring(p + 1);
    }

    /**
     * Returns file type of the file.
     * @return file format of this item as recognized by the application. It
     * can differ from simple suffix string. This is recommended way to obtain
     * type of file as it utilizes application's built-in mechanism.
     */
    public AudioFileFormat getFormat() {
        return AudioFileFormat.of(getURI());
    }

    /**
     * Returns filesize of the file resource of this item. The filesize will
     * remain unknown if unable to determine.
     * @return the filesize of this item. Never null.
     */
    public FileSize getFilesize() {
        return isFileBased() ? new FileSize(getFile()) : new FileSize(0);
    }

    /**
     * Returns initial name. Name derived purely from URI of the item.
     * <p/>
     * Name can denote an item such as PlaylistItem.
     * <p/>
     * Use as an initialization value when only URI is known about the item and
     * more user-friendly information is desired than the raw uri.
     * <p/>
     * Default implementation is equivalent to {@link #getFilename()}
     *
     * @return initial name of the item.
     */
    public String getInitialName() {
        return getFilename();
    }

/******************************************************************************/

    /**
     * Checks whether the item can be played. Only non corrupted items can be
     * played.
     * <pre>
     * Item is labeled corrupt if it fulfills at least one of the conditions
     * for file based items:
     * - file does not exist
     * - file is not a file (is a directory)
     * - is not supported audio file
     * - file can not be read
     * otherwise:
     *  - always false
     * </pre>
     * Also see {@link #markedAsCorrupted()};
     * @return playability/validity of the item.
     */
    public boolean isCorrupt(Use use) {
        return !getFormat().isSupported(use) || isCorruptWeak();
    }

    protected boolean isCorruptWeak() {
        if (isFileBased()) {
            File f = getFile();
            return !f.isFile() ||
                        !f.exists() ||
                            !f.canRead();
        } else {
            // this should get improved
            return false;
        }
    }

    /** Equivalent to {@code isCorrupt(PLAYBACK);} */
    public boolean isNotPlayable() {
        return isCorrupt(PLAYBACK);
    }

/******************************************************************************/

    /**
     * Compares the items' referenced file identities using {@link #getURI()}
     * Returns true only if the respective URIs are equal.
     * <pre>
     * Equivalent to:
     *      return i!= null && getURI().equals(i.getURI());
     * </pre>
     *
     * @param i the other item
     * @return true if and only if the URIs of the items equal
     */
    public final boolean same(Item i) {
        return i!= null && getURI().equals(i.getURI());
    }

    /**
     * Compares resource identity with other URI. Returns true only if URIs are equal.
     *
     * <pre>
     * Equivalent to:
     *      return getURI().equals(source);
     * </pre>
     *
     * @param r resource
     * @return true if and only if the provided uri equals to that of the item
     */
    public final boolean same(URI r) {
        return getURI().equals(r);
    }

/******************************************************************************/

    /**
     * Converts this item to metadata. This method does not read metadata on this
     * item, rather it converts this item into a Metadata object filling all
     * fields that are derivable from this item. Use when
     * Metadata is expected instead of Item and additional information is not
     * required or can not be obtained.
     *
     * @implSpec
     * Metadata has private constructors. The responsibility for creating
     * Metadata from any Item object lies within Metadata's constructor which
     * uses reflection to inspect the Item type. Subclassing this class should include
     * modifying that constructor to take the subclass into consideration.
     *
     * @return metadata that tests true for {@link #same()} with this item.
     */
    public Metadata toMeta() {
        return new Metadata(this);
    }

    /**
     * Converts this item to {@link SimpleItem} - an {@link Item} wrapper
     * for {@link URI}.
     * <p/>
     * Use when using this item is not desirable (e.g. due to memory or possible illegal
     * state in the future) and only the resource identity needs to be
     * preserved.
     *
     * @return simple item representation retaining of this item retaining the
     * identity.
     */
    public final SimpleItem toSimple() {
        return new SimpleItem(getFile());
    }

    /**
     * Converts this item to {@link PlaylistItem}.
     * <p/>
     * Subclases that contain values for fields contained in PlaylistItem should
     * override this method.
     *
     * @return playlistItem that tests true for {@link #same()} with this item.
     */
    public PlaylistItem toPlaylist() {
         return new PlaylistItem(getURI());
    }

    /** @return true iff this item's underlying resource (e.g. file) is being played. */
    public boolean isPlayingSame() {
        return same(Player.playingItem.get());
    }

/******************************************************************************/

    /**
     * <pre>
     * Natural sort order. Compares URI. Equivalent to:
     *     getURI().compareTo(i.getURI()).
     * </pre>
     * @param i
     * @return
     */
    @Override public int compareTo(CT i) {
        return getURI().compareTo(i.getURI());
    }

    /**
     * @return Comparator. Compares by location - uri;
     */
    public static Comparator<PlaylistItem> getComparatorURI() {
        return (p1,p2) -> p1.getURI().compareTo(p2.getURI());
    }
}