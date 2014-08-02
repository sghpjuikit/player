
package GUI;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.SimpleItem;
import AudioPlayer.playlist.SimplePlaylistItem;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import static java.util.Collections.EMPTY_LIST;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.event.EventHandler;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import utilities.AudioFileFormat;
import utilities.FileUtil;
import utilities.ImageFileFormat;

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
     * Accepts and consumes drag over event if contains {@link #widgetDF} Data 
     * format.
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
     * Accepts and consumes drag over event if contains at least 1 audio file, 
     * audio url, {@link Playlist} or list of {@link Item}.
     * <p>
     * Reusing this handler spares code duplication and multiple object instances.
     * 
     * @see #getAudioItems(javafx.scene.input.DragEvent)
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
    
    /**
     * Accepts and consumes drag over event if contains at least 1 image file.
     * @see #getImageFiles(javafx.scene.input.DragEvent)
     */
    public static final EventHandler<DragEvent> imageFileDragAccepthandler = t -> {
        Dragboard d = t.getDragboard();
        // accept if contains at least 1 audio file, audio url, playlist or items
        if ((d.hasFiles() && d.getFiles().stream().anyMatch(ImageFileFormat::isSupported)) ||
                (d.hasUrl() && ImageFileFormat.isSupported(d.getUrl()))) {
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
    
    /**
     * Obtains all supported audio items from dragboard. Looks for files, url,
     * list of items, playlist int this exact order.
     * <p>
     * Use in conjunction with {@link #audioDragAccepthandler}
     * 
     * @param e 
     * @return list of supported items derived from dragboard of the event.
     */
    public static List<Item> getAudioItems(DragEvent e) {
        Dragboard d = e.getDragboard();
        ArrayList<Item> out = new ArrayList();
        
        if (d.hasFiles()) {
            FileUtil.getAudioFiles(d.getFiles(),0).stream()
                    .map(SimpleItem::new).forEach(out::add);
        } else
        if (d.hasUrl()) {
            String url = d.getUrl();
            // watch out for non audio urls, we must filter those out, or
            // we could couse subtle bugs
            if(AudioFileFormat.isSupported(url))
                Optional.of(new SimpleItem(URI.create(url)))  // isnt this dangerous?
                        .filter(AudioFileFormat::isSupported) // isnt this pointless?
                        .ifPresent(out::add);
        } else
        if (d.hasContent(DragUtil.playlist)) {
            out.addAll(DragUtil.getPlaylist(d).getItems());
        } else 
        if (d.hasContent(DragUtil.items)) {
            out.addAll(DragUtil.getItems(d));
        }
        
        return out;
    }
    
    public static List<File> getImageItems(DragEvent e) {
        Dragboard d = e.getDragboard();
        
        if (d.hasFiles())
            return FileUtil.getImageFiles(d.getFiles());
        else
        if (d.hasUrl() && ImageFileFormat.isSupported(d.getUrl()))
            return Collections.singletonList(new File(d.getUrl()));
        else 
            return EMPTY_LIST;
    }
}
