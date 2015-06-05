
package Layout.Widgets.Features;

import AudioPlayer.playlist.Item;
import static java.util.Collections.singletonList;
import java.util.List;
import static util.dev.Util.forbidNull;

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
     * Passes item into this tagger.
     * 
     * @param item non null
     * @see #read(java.util.List)  
     */    
    public default void read(Item item) {
        forbidNull(item);
        read(singletonList(item));
    };
    
    /**
     * Passes items into this tagger.
     * <p>
     * Tagger reads metadata on items and displays them. User may be able to
     * change the data.
     * <p>
     * If items are {@link Metadata} the reading step is skipped.
     * 
     * @param items non null
     */    
    public void read(List<? extends Item> items);

}
