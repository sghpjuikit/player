/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.playlist;

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
import java.util.Objects;
import java.util.UUID;
import main.App;
import util.dev.Log;
import util.Util;

/**
 *
 * Playlist with a name. Intended for storage and serialization.
 * <p> 
 * --------------- Categories ----------------- Playlist can be categorized in
 * zero to multiple categories. Categories follow hierarchical taxonomy.
 * Playlist stores names of all categories it belongs to. Mantaining the
 * hierarchy of categories is not responsibility of this object and is managed
 * on different application layer.
 * <p>
 *----- Serialization ----------------------
 * Depending on the type of serialized state, it might be required to rereading 
 * all the physical items during deserialization. For example .m3u playlist file
 * format would require it.
 * <pre>
 * An example of how to serialize group of items as playlist:
 * <pre>
 * String name = "ListeningTo " + new Date(System.currentTimeMillis());
 * NamedPlaylist p = new NamedPlaylist(name, getItems());
 *               p.addCategory("Listening to...");
 *               p.serialize();
 * </pre>
 * -------- Changes ------------ Changes to items of the playlist (removing,
 * adding, editing) will become immediately serialized. This can be costly in
 * successive operations so methods for multiple successive operations are
 * implemented and should be used if possible.
 * <p>
 * @TODO Reflect changes to resource location (URI) of items in the playlist if
 * physical file has been moved etc...
 */
public class NamedPlaylist extends Playlist {
    
    private UUID id = UUID.randomUUID();
    
    /** @return unique id */
    public UUID getId() {
        return id;
    }
    
    /**
     * Returns newly constructed playlist loaded from file.
     * @param f
     * @return Playlist or null if error.
     */
    public static NamedPlaylist deserialize(File f) {
        try {
            XStream xstream = new XStream(new DomDriver());
            xstream.registerConverter(new PlaylistItemConverter());
            NamedPlaylist p = (NamedPlaylist) xstream.fromXML(f);
            return p;
        } catch (ClassCastException | StreamException ex) {
            Log.err("Unable to load playlist from the file: " + f.getName()
                    + ". The file not found or content corrupted. ");
            return null;
        }
    }
    
    /** Lazy initialized. Never null. */
    private ArrayList<PlaylistItem> items;
    /** List of names of categories. Lazy initialized. Never null. */
    private ArrayList<String> categories;
    
    /** @throws RuntimeException when name null or empty. */
    public NamedPlaylist(String name) {
        setName(name);
    }
    /** @throws RuntimeException when name null or empty or items null */
    public NamedPlaylist(String name, List<PlaylistItem> items) {
        this(name);
        this.items = new ArrayList<>();
        this.items.addAll(items);
    }

    private String name;
    /** @return the name */
    public String getName() {
        return name;
    }
    /** @param name the name to set 
     *  @throws RuntimeException when name null or empty.
     */
    public final void setName(String name) {
        Objects.requireNonNull(name, "Name must not be null");
        if(name.isEmpty()) throw new RuntimeException("Name must not be empty");
        this.name = name;
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
     * The name will be used as filename, but will have illegal characters removed.
     */
    public void serialize() {
        String f_name = Util.filenamizeString(getName());
        File save = new File(App.PLAYLIST_FOLDER(), f_name + ".pfx");
        try {
            XStream xstream = new XStream(new DomDriver());
            xstream.registerConverter(new PlaylistItemConverter());
            xstream.toXML(this, new BufferedWriter(new FileWriter(save)));
            Log.info("Saving playlist '" + getName() + "' into the file.\nSuccess.");
        } catch (IOException ex) {
            Log.err("Unable to save playlist '" + getName() + "' into the file.");
        }
    }

    @Override
    public String toString() {
        return name + ", " + list().size() + " items.";
    }
}
