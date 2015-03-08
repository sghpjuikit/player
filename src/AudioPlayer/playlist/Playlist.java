
package AudioPlayer.playlist;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.util.Duration;

/**
 * Playlist is wrapper for multiple {@link PlaylistItem} providing easy
 * manipulations of the items.
 * <p>
 */
public class Playlist extends AbstractPlaylist {

    /** Lazy initialized. Never null. */
    private ArrayList<PlaylistItem> items;
    
    
    /** Creates empty playlist. */
    public Playlist() {}
    
    /** Wraps provided items into playlist. Corrupted items will be added as well.*/
    public Playlist(List<PlaylistItem> items) {
        this.items = new ArrayList(items);
    }
    
    /** 
     * Wraps provided files into playlist. Corrupted items will be added as well.
     * @param i unused parameter to avoid the same constructor method erasure. Use
     * arbitrary value. For example 0.
     */
    public Playlist(Stream<File> files, int i) {
        this(files.map(File::toURI).map(PlaylistItem::new)
                .collect(Collectors.toList()));
    }
    
    /** 
     * Wraps provided URIs into playlist. Corrupted items will be added as well.
     * @param i unused parameter to avoid the same constructor method erasure. Use
     * arbitrary value. For example true.
     */
    public Playlist(Stream<URI> uris, boolean i) {
        this(uris.map(PlaylistItem::new).collect(Collectors.toList()));
    }
    
    /** Wraps provided item into playlist. It is safe to use this method for any
      * type of {@link Item}, including {@link PlaylistItem}.
      * Corrupted items will be added as well.*/
    public Playlist (Item i) {
        items = new ArrayList();
        items.add(i.toPlaylist());
    }
    
    /** Wraps provided file into playlist. Corrupted items will be added as well.*/
    public Playlist(File f) {
        this(new PlaylistItem(f.toURI()));
    }
    
    /** Wraps provided URI into playlist. Corrupted items will be added as well.*/
    public Playlist(URI u) {
        this(new PlaylistItem(u));
    }
    
/******************************************************************************/
    
    /** {@inheritDoc} */
    @Override
    protected List<PlaylistItem> list() {
        if (items == null) items = new ArrayList<>();
        return items;
    }
    
    /** {@inheritDoc} */
    @Override
    public List<PlaylistItem> getItems() {
        return list();
    }
    
    /** {@inheritDoc} */
    @Override
    public Duration getLength() {
        return calculateLength();
    }

    
    @Override
    public String toString() {
        return "Playlist of " + list().size() + " items.";
    }
}
