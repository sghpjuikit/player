/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playlist;

import java.io.Serializable;
import java.net.URI;
import javafx.util.Duration;
import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 * Simplified PlaylistItem class supporting Serialization. All fields
 * are primitives as opposed to property fields of PlaylistItem class.
 * <p>
 * Use this class when PlaylistItem is not suitable (Serialization, etc).
 * <p>
 * Is immutable.
 * The object can not be changed. Avoid using not updated (not once) PlaylistItem
 * to create this object. Practically, this doesnt pose a problem as PlaylistItems
 * are immediately updated on creation, but there is a possibility.
 * <p>
 * Is serializable.
 * For example used during drag transfers.
 * <p>
 * @author uranium
 */
@Immutable
public final class SimplePlaylistItem extends Item implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final URI uri;
    private final Duration time;
    private final String name;
    
    /**
     * Constructor.
     * Cnverts PlaylistItem into this object.
     * @param item 
     */
    public SimplePlaylistItem (PlaylistItem item) {
        uri = item.getURI();
        name = item.getName();
        time = item.getTime();
    }
    
    /**
     * @return PlaylistItem object based on values of this..
     */
    @Override
    public PlaylistItem toPlaylistItem() {
        return new PlaylistItem(uri, name, time.toMillis());
    }

    @Override
    public URI getURI() {
        return uri;
    }
    public Duration getTime() {
        return time;
    }
    public String getName() {
        return name;
    }
}