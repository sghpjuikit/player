
package AudioPlayer.playlist;

import AudioPlayer.Player;
import AudioPlayer.tagging.FileSize;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataReader;
import java.io.File;
import java.net.URI;
import java.util.Comparator;
import utilities.AudioFileFormat;
import utilities.FileUtil;

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
     * Item based on file is represented by a file in a file system om a local
     * systen. Item can also be on the web accessed through http protocol.
     * @return whether item is filebased.*/
    public boolean isFileBased() {
        return "file".equals(getURI().getScheme());
    }
    
    /** 
     * Returns absolute file of the item, if it is file based
     * If there is a problem with the file being relative, call getAbsoluteFile()
     * method on the file before using it.
     * @return file representation of the item.
     * @throws UnsupportedOperationException if item is not file based
     */
    public File getFile() {
        if(!isFileBased()) throw new UnsupportedOperationException("Item is not file based.");
        return new File(getURI()).getAbsoluteFile();
    }
    
    /**
     * Returns human-readable string representation of the path to the resource
     * of this item. Useful for displaying pat in the graphical user interface.
     * <p>
     * The path doesnt guarantee ability to backward-reconstruct the original
     * resource (URI), and must not be used this way.
     * @return string representation of the path of the resource
     */
    public String getPath() {
        String path = getURI().getPath();
        return path==null||path.isEmpty() ? "" : getURI().getPath().substring(1);
    }
    
    /** 
     * Parent directory of the resource. Only for file based items.
     * @return parent directory of the item in the file system
     * @throws UnsupportedOperationException if item is not file based
     */
    public File getLocation() {
        if(!isFileBased()) throw new UnsupportedOperationException("Item is not file based.");
        return getFile().getParentFile();
    }
    
    /** 
     * Returns name of the file without its suffix. Only for file based items.
     * @return the filename without suffix
     * @throws UnsupportedOperationException if item is not file based
     */
    public String getFilename() {
        if(!isFileBased()) throw new UnsupportedOperationException("Item is not file based.");
        return FileUtil.getName(getFile());
    }
    
    /** 
     * Returns name of the file with its suffix. Only for file based items.
     * @return the filename with suffix or empty string if none.
     * @throws UnsupportedOperationException if item is not file based
     */
    public String getFilenameFull() {
       if(!isFileBased()) throw new UnsupportedOperationException("Item is not file based.");
        return getFile().getName();
    }
    
    /** 
     * Returns suffix part of the filename. Only for file based items.
     * <p>
     * It doesnt necessarily reflect real type of the file. Dont use this method
     * to find out type of the file. Use {@link #getFormat()}.
     * @return the suffix of the file of this item or empty string if none.
     * @throws UnsupportedOperationException if item is not file based
     */
    public String getSuffix() {
        if(!isFileBased()) throw new UnsupportedOperationException("Item is not file based.");
        String n = getFilenameFull();
        int p = n.lastIndexOf('.');
        return (p > 0 && p < n.length() - 1) ? n.substring(p + 1) : "";
    }
    
    /** 
     * Returns file type of the file. Only for file based items.
     * @return file format of this item as is recognized by the application. It
     * can differ from simple suffix string. This is recommended way to obtain
     * type of file as it utilizes application's built-in mechanism.
     * @throws UnsupportedOperationException if item is not file based
     */
    public AudioFileFormat getFormat() {
        if(!isFileBased()) throw new UnsupportedOperationException("Item is not file based.");
        return AudioFileFormat.get(getURI());
    }
    
    /** 
     * @return the filesize 
     * @throws UnsupportedOperationException if item is not file based
     */    
    public FileSize getFilesize() {
        if(!isFileBased()) throw new UnsupportedOperationException("Item is not file based.");
        return new FileSize(getFile());
    }
    
    /**
     * Returns initial name. Name derived purely from URI of the item. Use as an
     * initialisation value when only URI is known about the item and more 
     * user-friendly information is desired.
     * <p>
     * Attempts to return name of the file without suffix (even for files on the
     * web for example through http) by parsing the path part of the URI.
     * @return initial name of the item.
     */
    public String getInitialName() {
        String p = getURI().getPath();
        if(p==null || p.isEmpty()) return ""; // shouldnt happen ever, but just in case some badly damaged http URL gets through here
        int i = p.lastIndexOf('/');
        if(i==-1 || p.length()<2) return p; // another exceptional state check
        p = p.substring(i+1);       // remove leading '/' character
        i = p.lastIndexOf('.');     // remove extension
        return (i==-1) ? p : p.substring(0, i);
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
     * 
     * </pre>
     * Also see {@link #markedAsCorrupted()};
     * @return playability/validity of the item.
     */
    public boolean isCorrupt() {
        if(isFileBased()) {
            File f = getFile();
            return !f.exists() ||
                   !f.isFile() ||
                   !AudioFileFormat.isSupported(this) ||
                   !f.canRead();
        } else
            return false;
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
     * item, rather it converts this item into a Metadata object. Use when
     * Metadata is expected instead of Item and additional information is not
     * required.
     * <p>
     * Returns empty metadata that tests true for {@link #same()}
     * with this item.
     * @return 
     */
    public Metadata toMetadata() {
        return new Metadata(this);
    }
    
/******************************************************************************/
    
    /**
     * Reads and returns metadata of the file of this item.
     * <p>
     * WARNING. This method uses application thread and as such is prone to
     * cause performance problems if misused. Its fine to use this method for
     * single item. Its recommended to avoid this method in loops.
     * If this item is being played, metadata are fetched from cache, which means
     * immediate response and no performance throwback.
     * @return metadata for this item. Empty metadata, if this item is corrupt
     * or on error.
     */
    public Metadata getMetadata() {
        if (this.same(Player.getCurrentMetadata()))
            return Player.getCurrentMetadata();
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
        return ( p1, p2) -> {
            return p1.getURI().compareTo(p2.getURI());
        };
    }
}