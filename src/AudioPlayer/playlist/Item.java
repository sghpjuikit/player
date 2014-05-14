
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
 * Object maintaining URI source to file.
 * 
 * @author uranium
 */
public abstract class Item {
    boolean corrupted = false;
    
    /** @return URI as a source this object maintains.  */
    abstract public URI getURI();

    /** 
     * If there is a problem with the file being relative, call getAbsoluteFile()
     * method on the file before using it.
     * @return file representation of the item. */
    public File getFile() {
        return new File(getURI());
    }
    
    /**
     * The path removes the beginning '/' character at index 0.
     * To get valid path use getURI().getPath() instead of this method.
     * @return  string representation of the absolute path
     */
    public String getPath() {
        return getURI().getPath().substring(1);
    }
    
    /** @return parent directory of the item. */
    public File getLocation() {
        return getFile().getAbsoluteFile().getParentFile();
    }
    
    /** @return the filename without suffix or empty string if none */
    public String getFilename() {
        return FileUtil.getName(getURI());
    }
    
    /** @return the filename with suffix or empty string if none */
    public String getFilenameFull() {
        return getFile().getName();
    }
    
    /** 
     * Returns suffix part of the filename. It doesnt necessarily reflect real
     * type of the file. Dont use this method to find out type of the file.
     * @return the filename with suffix or empty string if none*/
    public String getSuffix() {
        String n = getFilenameFull();
        int p = n.lastIndexOf('.');
        if (p > 0 && p < n.length() - 1)
           return n.substring(p + 1);
        return "";
    }
    
    /** @return file format of this item as is recognized by the application. It
     * can differ from simple suffix string. This is recommended way to obtain
     * type of file as it utilizes application's built-in mechanism. */
    public AudioFileFormat getFormat() {
        return AudioFileFormat.get(getURI());
    }
    
    /** @return the filesize */    
    public FileSize getFilesize() {
        return new FileSize(getFile());
    }
    
     /**
     * Checks whether the item can be played.
     * Item is labeled corrupt if it fulfills at least one of the conditions:
     * - file doesnt exist
     * - file is not a file (is a directory)
     * - is not supported audio file
     * - file can not be read
     * 
     * @return playability/validity of the item.
     */
    public boolean isCorrupt() {
        File f = new File(getURI());
        corrupted = !f.exists() ||
                    !f.isFile() ||
                    !AudioFileFormat.isSupported(this) ||
                    !f.canRead();
        return corrupted;
    }
    /**
     * Returns true if this item was marked corrupt last time it was checked. This
     * doesn't necessarily mean the item is or is not corrupt right now. The method
     * returns cached value so the curruptness check requiring I/O can be avoided.
     * Use when performance is prioritized, for example when iterating lists.
     * @return corrupt
     */
    public boolean markedAsCorrupted() {
        return corrupted;
    }
    /**
     * Compares URI source with other Item object's. Returns true only
     * if their respective URIs equal.
     * @param source
     * @return 
     */
    public boolean same(Item source) {
        if (source == null) { return false; }
        return getURI().equals(source.getURI());
    }
    /**
     * Compares URI source with other URI. Returns true only if URIs equal.
     * More formally, the implementation is exactly:
     *      return getURI().equals(source);
     * @param source
     * @return 
     */
    public boolean same(URI source) {
        return getURI().equals(source);
    }
    
    /**
     * Rears and returns metadata of the file of this item.
     * 
     * WARNING. This method uses application thread and as such is prone to
     * cause performance problems if misused. Its fine to use this method for
     * single item. Its recommended to avoid this method in loops.
     * If this item is being played, metadata are fetched from cache, which means
     * immediate response.
     * @return metadata for this item. Null if this item is corrupt or on error.
     */
    public Metadata getMetadata() {
        if (this.same(Player.getCurrentMetadata())) 
            return Player.getCurrentMetadata();
        return MetadataReader.create(this);
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