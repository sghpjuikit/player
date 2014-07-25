
package GUI;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.SimpleItem;
import AudioPlayer.playlist.SimplePlaylistItem;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.event.EventHandler;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import utilities.AudioFileFormat;

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
    public static final DataFormat widgetDF = new DataFormat("widget");
    
    /**
     * Accepts drag if contains {@link #widgetDF} Data format.
     * <p>
     * Reuse this handler spares code duplication and multiple object instances.
     */
    public static final EventHandler<DragEvent> componentDragAcceptHandler = e -> {
        Dragboard db = e.getDragboard();
        if (db.hasContent(DragUtil.widgetDF)) {
            e.acceptTransferModes(TransferMode.ANY);
            e.consume();
        }
    };
    
    /**
     * Accepts drag if contains at least 1 audio file, audio url, {@link Playlist}
     * or list of {@link Item}.
     * <p>
     * Reusing this handler spares code duplication and multiple object instances.
     */
    public static final EventHandler<DragEvent> audioDragAccepthandler = t -> {
        Dragboard d = t.getDragboard();
        // accept if contains at least 1 audio file, audio url, playlist or items
        if ((d.hasFiles() && d.getFiles().stream().anyMatch(AudioFileFormat::isSupported)) ||
                (d.hasUrl() && AudioFileFormat.isSupported(d.getUrl())) ||
                d.hasContent(DragUtil.playlist) ||
                d.hasContent(DragUtil.items)) {
            t.acceptTransferModes(TransferMode.ANY);
            t.consume();
        }
    };
    
/******************************************************************************/

    
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
    
    public static WidgetTransfer getWidgetTransfer(Dragboard db) {
        return (WidgetTransfer) db.getContent(DragUtil.widgetDF);
    }
}
