
package AudioPlayer.playlist;

import AudioPlayer.Player;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.FileSize;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataReader;
import java.io.File;
import java.net.URI;
import java.util.Comparator;
import utilities.AudioFileFormat;

/**
 * Playable item.
 * <p>
 * Object maintaining URI as an audio resource.
 * <p>
 * @author uranium
 */
public abstract class Item implements Comparable<Item> {
    
    boolean corrupted = false;
    
/******************************************************************************/
    
    /** 
     * Returns the resource identifier denoting the audui resorce of this item.
     * @return URI as a resource of this item.*/
    abstract public URI getURI();
    
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
        if(!isFileBased()) throw new UnsupportedOperationException("Item is not file based.");
        // we should return absolute file or we could cause trouble in some
        // situations
        return new File(getURI()).getAbsoluteFile();
    }
    
    /**
     * Returns human-readable string representation of the path to the resource
     * of this item. Useful for displaying path in the graphical user interface.
     * <p>
     * Uses getUri().getPath() and removes leading '/' character.
     * <p>
     * The path doesnt guarantee the possibility to backward-reconstruct the 
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
     * <p>
     * Use to get location of the item, for example to fetch additional resources
     * located there, such as cover.
     * @see #getPath()
     * @return parent directory of the item in the file system
     * @throws UnsupportedOperationException if item is not file based
     */
    public File getLocation() {
        if(!isFileBased()) throw new UnsupportedOperationException("Item is not file based.");
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
        // shouldnt happen ever, but just in case some damaged URL gets through
        if(p==null || p.isEmpty()) return ""; 
        int i = p.lastIndexOf('/');
        // another exceptional state check (just in case, might be unnecessary)
        if(i==-1 || p.length()<2) return p;
        // get name portion of the path
        return p.substring(i+1);
    }
    
    /** 
     * Returns suffix of the filename. For example: "mp3, flac"
     * <p>
     * It doesnt necessarily reflect real type of the file. Dont use this method
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
     * <p>
     * Name can denote an item such as PlaylistItem.
     * <p>
     * Use as an initialization value when only URI is known about the item and 
     * more user-friendly information is desired than the raw uri.
     * <p>
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
     * - file doesnt exist
     * - file is not a file (is a directory)
     * - is not supported audio file
     * - file can not be read
     * otherwise:
     *  - always false
     * </pre>
     * Also see {@link #markedAsCorrupted()};
     * @return playability/validity of the item.
     */
    public boolean isCorrupt() {
        if(isFileBased()) {
            File f = getFile();
            corrupted = !f.exists() ||
                        !f.isFile() ||
                        !AudioFileFormat.isSupported(this) ||
                        !f.canRead();
        } else {
            // this should get improved
            corrupted =  false;
        }
        return corrupted;
    }
    /**
     * Returns true if this item was marked corrupt last time it was checked. This
     * doesn't necessarily reflect the real value. The method
     * returns cached value so the curruptness check involving I/O can be avoided.
     * Use when performance is prioritized, for example when iterating lists in
     * tables.
     * <p>
     * If the validity of the check is prioritized, use {@link #isCorrupt()}.
     * @return corrupt
     */
    public boolean markedAsCorrupted() {
        return corrupted;
    }
    
/******************************************************************************/
    
    /**
     * Compares URI source with other Item object's. Returns true only
     * if their respective URIs equal.
     * @param source
     * @return true if and only if the URIs of the items equal
     */
    public boolean same(Item source) {
        return source != null && getURI().equals(source.getURI());
    }
    
    /**
     * Compares URI source with other URI. Returns true only if URIs equal.
     * <pre>
     * More formally, the implementation is exactly:
     *      return getURI().equals(source);
     * </pre>
     * @param source
     * @return true if and only if the provided uri equals to that of the item
     */
    public boolean same(URI source) {
        return getURI().equals(source);
    }
/******************************************************************************/
    
    /**
     * Converts this item to metadata. This method doesnt read metadata on this
     * item, rather it converts this item into a Metadata object filling all
     * fields that are derivable from this item. Use when
     * Metadata is expected instead of Item and additional information is not
     * required or can not be obtained.
     * <p>
     * Developer note: Always include proper javadoc for subclasses to inform 
     * which fields will be initialized.
     * <p>
     * Developer note: the responsibility for creating correctly filled
     * Metadata object lies within Metadata's constructor which uses reflection to
     * inspect the Item type. Subclassing this class should include
     * adding the class' support in that constructor.
     * 
     * @return metadata that tests true for {@link #same()} with this item.
     */
    public Metadata toMetadata() {
        return new Metadata(this);
    }
    
    /**
     * Converts this item to {@link PlaylistItem}.
     * <p>
     * Subclases that contain values for fields contained in PlaylistItem should
     * override this method.
     * <p>
     * Default implementation is equivalent to: new PlaylistItem(getURI())
     * 
     * @return playlistItem that tests true for {@link #same()} with this item.
     */
    public PlaylistItem toPlaylistItem() {
         return new PlaylistItem(getURI());
    }
    
/******************************************************************************/
    
    /**
     * Returns metadata for this item.
     * <p>
     * It is first checked if the item is playing. Playing item already has
     * metadata available. If the metadata is cached, it is returned. Else:
     * <p>
     * If there is library available, the metadata item is looked up and returned
     * if available. Else:
     * <p>
     * Item will be read for metadata. This includes I/O operation.
     * <p>
     * If the reading fails, the item will be converted using {@link #toMetadata()}.
     * <p>
     * If the item is corrupt and the previous methods fail (it is possible the 
     * cache or library still contains the item) EMPRTY metadata will be returned.
     * <p>
     * WARNING. This method uses application thread and as such is prone to
     * cause performance problems if misused.
     * <p>
     * Its fine to use this method for single or very few items, played item and
     * items that are guaranteed to be in a library
     * 
     * @return metadata for this item. Never null or EMPTY Metadata.
     */
    public final Metadata getMetadata() {
        // try playing item
        if (same(Player.playingtem.get())) return Player.playingtem.get();
        // try library
        Metadata m = DB.getItem(this);
        if (m!=null) return m;
        // read
        return MetadataReader.create(this);
    }

    
    /**
     * <pre>
     * Natural sort order. Compares URI. Equivalent to:
     *     getURI().compareTo(i.getURI()).
     * </pre>
     * @param i
     * @return 
     */
    @Override public int compareTo(Item i) {
        return getURI().compareTo(i.getURI());
    }
    
    /**
     * @return Comparator. Compares by location - uri;
     */
    public static Comparator<PlaylistItem> getComparatorURI() {
        return (p1,p2) -> p1.getURI().compareTo(p2.getURI());
    }
}