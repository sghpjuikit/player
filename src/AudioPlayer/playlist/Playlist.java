
package AudioPlayer.playlist;

import Configuration.Configuration;
import Serialization.PlaylistItemConverter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javafx.util.Duration;
import utilities.Log;

/**
 * Playlist is wrapper for multiple Playlist Items providing methods for easy
 * manipulations of the items.
 *
 * Playlist with no/empty name - "" is not valid playlist. The distinction is
 * necessary as invalid playlist - group of items and valid playlist are the
 * same by implementation, but fill different functional roles. Invalid playlist
 * is to allow easier item manipulations and serves only temporary role in order
 * to operate on the group, while valid playlist's purpose is to be stored and
 * categorized. Invalid playlist cant have any assigned categories or be
 * serialized.
 *
 * 
 * --------------- Categories ----------------- Playlist can be categorized in
 * zero to multiple categories. Categories follow hierarchical taxonomy.
 * Playlist stores names of all categories it belongs to. Mantaining the
 * hierarchy of categories is not responsibility of this object and is managed
 * on different application layer.
 *
 *----- Serialization ----------------------
 * Depending on the type of serialized state, it might be required to rereading 
 * all the physical items during deserialization. For example .m3u playlist file
 * format would require it.
 *
 * -------- Changes ------------ Changes to items of the playlist (removing,
 * adding, editing) will become immediately serialized. This can be costly in
 * successive operations so methods for multiple successive operations are
 * implemented and should be used if possible.
 * 

 * @TODO Reflect changes to resource location (URI) of items in the playlist if
 * physical file has been moved etc...
 *
 */
public final class Playlist extends AbstractPlaylist {
    private UUID id = UUID.randomUUID();
    
    /**
     * @return id
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Returns newly constructed playlist loaded from file.
     * @param f
     * @return Playlist or null if error.
     */
    public static Playlist deserialize(File f) {
        try {
            XStream xstream = new XStream(new DomDriver());
            xstream.registerConverter(new PlaylistItemConverter());
            Playlist p = (Playlist) xstream.fromXML(f);
            return p;
        } catch (ClassCastException | StreamException ex) {
            Log.err("Unable to load playlist from the file: " + f.getName()
                    + ". The file not found or content corrupted. ");
            return null;
        }
    }

    /**
     * Creates playlist from list of simplified PlaylistItems. Use for
     * deserialization from DragBoard.
     * @param __items
     * @return
     */
    public static Playlist fromPojoList(List<SimplePlaylistItem> __items) {
        List<PlaylistItem> _items = new ArrayList<>();
        for (SimplePlaylistItem i : __items) {
            _items.add(i.toPlaylistItem());
        }
        return new Playlist(_items);
    }
    private String name;
    /**
     * Lazy initialized. Never null.
     */
    private ArrayList<PlaylistItem> items;
    /**
     * List of names of categories. Lazy initialized. Never null.
     */
    private ArrayList<String> categories;
    
    
    public Playlist() {
    }
    public Playlist(String _name) {
        name = _name;
    }
    public Playlist(String _name, List<PlaylistItem> _items) {
        name = _name;
        items = new ArrayList<>();
        items.addAll(_items);
    }
    public Playlist(List<PlaylistItem> _items) {
        name = "";
        items = new ArrayList<>();
        items.addAll(_items);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<PlaylistItem> list() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }
    @Override
    public List<PlaylistItem> getItems() {
        return list();
    }
    
    @Override
    public Duration getLength() {
        return calculateLength();
    }
    
    /**
     * Assigning null to the list is equivalent of clearing the list. This
     * method never returns null.
     *
     * @return categories
     */
    public ArrayList<String> getCategories() {
        if (categories == null) {
            categories = new ArrayList<>();
        }
        return categories;
    }
    /**
     * Assigns category to the playlist by its name. Serializes the change
     * immediately. In case of multiple categories, use analogous methos with
     * list parameter.
     *
     * @param categoryName
     */
    public void addCategory(String categoryName) {
        if (categories == null) {
            categories = new ArrayList<>();
        }
        categories.add(categoryName);
        serialize();
    }
    /**
     * Assigns multiple categories to the playlist by their names. Serializes
     * the change immediately.
     *
     * @param categoryNames Null parameter does nothing.
     */
    public void addCategories(List<String> categoryNames) {
        if (categoryNames == null) {
            return;
        }
        if (categories == null) {
            categories = new ArrayList<>();
        }
        categories.addAll(categoryNames);
        serialize();
    }

    /**
     * Removes item at specified index. Serializes the change immediately.
     *
     * @param index Out of bounds (<0, >=items.size) does nothing.
     */
    public void removeItem(int index) {
        if (list().isEmpty()) {
            return;
        }
        if (index < 0 || index >= getSize()) {
            return;
        }
        items.remove(index);
        serialize();
    }

    /**
     * Removes elements at specified indexes. Serializes the change immediately.
     *
     * @param indexes Out of bounds (<0, >=items.size), empty or null parameter
     * does nothing.
     */
    public void removeItem(List<Integer> indexes) {
        if (indexes == null || indexes.isEmpty() || list().isEmpty()) {
            return;
        }
        for (int i = getSize() - 1; i >= 0; i--) {
            if (indexes.contains(i)) {
                items.remove(i);
            }
        }
        serialize();
    }

    /**
     * Serializes the playlist into file using in house serialization mechanism.
     */
    public void serialize() {
        File save = new File(Configuration.PLAYLIST_FOLDER + File.separator + getName() + ".pfx");
        try {
            XStream xstream = new XStream(new DomDriver());
            xstream.registerConverter(new PlaylistItemConverter());
            xstream.toXML(this, new BufferedWriter(new FileWriter(save)));
            Log.mess("Saving playlist '" + getName() + "' into the file.\nSuccess.");
        } catch (IOException ex) {
            Log.err("Unable to save playlist '" + getName() + "' into the file.");
        }
    }

    /**
     * Converts the playlist into simple list of simplied PlaylistItem objects
     * to get Serializable functionality.
     *
     * @return
     */
    public List<SimplePlaylistItem> toPojoList() {
        List<SimplePlaylistItem> out = new ArrayList<>();
        for (PlaylistItem i : list()) {
            out.add(new SimplePlaylistItem(i));
        }
        return out;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    protected void updateDuration() {
        //does nothing
    }

}
