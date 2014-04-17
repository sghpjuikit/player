
package GUI;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.SimpleItem;
import AudioPlayer.playlist.SimplePlaylistItem;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;

/**
 *
 * @author uranium
 */
public final class DragUtil {
    
    /** Data Format for Playlist. Use Playlist as wrapper for List<PlaylistItem> */
    public static final DataFormat playlist = new DataFormat("playlist");
    /** Data Format for List<Item>. */
    public static final DataFormat items = new DataFormat("items");
    /** Data Format for WidgetTransfer. */
    public static final DataFormat widgetDataFormat = new DataFormat("widget");
    

    
    public static void setContent(Dragboard db, Playlist p) {
        ClipboardContent content = new ClipboardContent();
        content.put(DragUtil.playlist, p.toPojoList());
        db.setContent(content);
    }
    
     public static void setContent(Dragboard db, List<? extends Item> items) {
        ClipboardContent content = new ClipboardContent();
        List<SimpleItem> i = new ArrayList<>();
        items.stream().map(SimpleItem::new).forEach(i::add);
        content.put(DragUtil.items, i);
        db.setContent(content);
    }
    
    public static Playlist getPlaylist(Dragboard db) {
        return Playlist.fromPojoList((List<SimplePlaylistItem>) db.getContent(DragUtil.playlist));
    }
    
    public static List<Item> getItems(Dragboard db) {
        return ((List<SimpleItem>) db.getContent(DragUtil.items))
                .stream()
                .collect(Collectors.toList());
    }

}
