
package Layout.Widgets.Features;

import AudioPlayer.playlist.Item;
import static java.util.Collections.singletonList;
import java.util.List;
import java.util.Objects;

/**
 * Capable to read and write data to song tags
 * 
 * @author Plutonium_
 */
@Feature(
  name = "Metadata editing", 
  description = "Capable to read and write data to song tags", 
  type = TaggingFeature.class
)
public interface TaggingFeature {
    
    /**
     * Convenience method for single item reading. For specifics read the
     * documentation for the other more general read method.
     * <p>
     * Default implementation wraps the item in a singleton list and passes it
     * to {@link #read(java.util.List)} method. More effective implementation
     * might be desirable.
     * 
     * @param item
     * @throws NullPointerException if param null
     */    
    public default void read(Item item) {
        Objects.requireNonNull(item);
        read(singletonList(item));
    };
    
    /**
     * Reads metadata on provided items and fills the data for tagging. If items
     * contains Metadata themselves, they will not be read, but their data will
     * be immediately used.
     * @param items Since Metadata extends Item it can also be passed
     * as argument. The list MUST have elements of the exactly same type.
     * @throws NullPointerException if param null
     */    
    public void read(List<? extends Item> items);
    
    /**
     * Writes edited data to tag and reloads the data and refreshes gui. The
     * result is new data from tag shown, allowing to confirm the changes really
     * happened.
     * If no items are loaded then this method is a no-op.
     */
    public void write();
}
