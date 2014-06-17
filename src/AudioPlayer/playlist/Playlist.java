
package AudioPlayer.playlist;

import java.util.ArrayList;
import java.util.List;
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
    public Playlist() {
    }
    /** Wraps provided items into playlist. */
    public Playlist(List<PlaylistItem> items) {
        this.items = new ArrayList<>(items);
    }

    
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
    
/******************************************************************************/
    
    /**
     * Converts the playlist into simple list of simplied PlaylistItem objects
     * to get Serializable functionality.
     * <p>
     * @return
     */
    public List<SimplePlaylistItem> toPojoList() {
        List<SimplePlaylistItem> out = new ArrayList<>();
        for (PlaylistItem i : list()) {
            out.add(new SimplePlaylistItem(i));
        }
        return out;
    }
    
    /**
     * Creates playlist from list of simplified PlaylistItems. Use for
     * deserialization from DragBoard.
     * @param items
     * @return
     */
    public static Playlist fromPojoList(List<SimplePlaylistItem> items) {
        List<PlaylistItem> l = new ArrayList<>();
        for (SimplePlaylistItem i : items) {
            l.add(i.toPlaylistItem());
        }
        return new Playlist(l);
    }
    
/******************************************************************************/

    @Override
    public String toString() {
        return "Playlist of " + list().size() + " items.";
    }
}
