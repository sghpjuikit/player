
package GUI;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.SimpleItem;
import Layout.Component;
import Layout.Container;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import static java.util.Collections.EMPTY_LIST;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import main.App;
import utilities.AudioFileFormat;
import utilities.FileUtil;
import utilities.ImageFileFormat;
import utilities.Log;

/**
 *
 * @author uranium
 */
public final class DragUtil {
    
/******************************* data formats *********************************/
    
    /** Data Format for Playlist. A;ways use Playlist as wrapper for List<PlaylistItem> */
    public static final DataFormat playlistDF = new DataFormat("playlist");
    /** Data Format for List<Item>. */
    public static final DataFormat itemsDF = new DataFormat("items");
    /** Data Format for WidgetTransfer. */
    public static final DataFormat widgetDF = new DataFormat("widget");
    /** Data Format for Component. */
    public static final DataFormat componentDF = new DataFormat("component");
    
/********************************* dragboard **********************************/
    
    private static Object data;
    private static DataFormat dataFormat;
    
/******************************** handlers ************************************/

    /**
     * Accepts and consumes drag over event if contains Component
     * <p>
     * Reuse this handler spares code duplication and multiple object instances.
     */
    public static final EventHandler<DragEvent> componentDragAcceptHandler = e -> {
        if (hasComponent()) {
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
    public static final EventHandler<DragEvent> audioDragAccepthandler = e -> {
        if (hasAudio(e.getDragboard())) {
            e.acceptTransferModes(TransferMode.ANY);
            e.consume();
        }
    };
    
    /**
     * Accepts and consumes drag over event if contains at least 1 image file.
     * @see #getImageFiles(javafx.scene.input.DragEvent)
     */
    public static final EventHandler<DragEvent> imageFileDragAccepthandler = e -> {
        if (hasImage(e.getDragboard())) {
            e.acceptTransferModes(TransferMode.ANY);
            e.consume();
        }
    };
    
    
/******************************************************************************/

    public static void setPlaylist(Playlist p, Dragboard db) {
        // put fake data into dragboard
        db.setContent(Collections.singletonMap(playlistDF, ""));
        data = p;
        dataFormat = playlistDF;
    }
    public static Playlist getPlaylist() {
        if(dataFormat != playlistDF) throw new RuntimeException("No playlist in data available.");
        return (Playlist) data;
    }
    public static boolean hasPlaylist() {
        return dataFormat == playlistDF;
    }
    
    
    public static void setItemList(List<? extends Item> itemList, Dragboard db) {
        // put fake data into dragboard
        db.setContent(Collections.singletonMap(itemsDF, ""));
        data = itemList;
        dataFormat = itemsDF;
    }
    public static List<Item> getItemsList() {
        if(dataFormat != itemsDF) throw new RuntimeException("No item list in data available.");
        return (List<Item>) data;
    }
    public static boolean hasItemList() {
        return dataFormat == itemsDF;
    }
    
    
    public static void setComponent(Container parent, Component child, Dragboard db) {
        // put fake data into dragboard
        db.setContent(Collections.singletonMap(componentDF, ""));
        data = new WidgetTransfer(parent, child);
        dataFormat = componentDF;
    }
    public static WidgetTransfer getComponent() {
        if(dataFormat != componentDF) throw new RuntimeException("No component in data available.");
        return (WidgetTransfer) data;
    }
    public static boolean hasComponent() {
        return dataFormat == componentDF;
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
        if (hasPlaylist()) {
            out.addAll(getPlaylist().getItems());
        } else 
        if (hasItemList()) {
            out.addAll(getItemsList());
        }
        
        return out;
    }
    
     /**
     * @param d
     * @return true if contains at least 1 audio file, audio url, playlist or items 
     */
    public static boolean hasAudio(Dragboard d) {
        return (d.hasFiles() && d.getFiles().stream().anyMatch(AudioFileFormat::isSupported)) ||
                    (d.hasUrl() && AudioFileFormat.isSupported(d.getUrl())) ||
                        hasPlaylist() ||
                            hasItemList();
    }
    
    /**
     * Returns drag&dropped image files.
     * <p>
     * If image fiels were dropped, thy will be returned. IIf the item is url
     * string of an image file, it will be returned as File after appropriate 
     * conversion.
     * <p>
     * Support for url signifies also support for imges accessed remotely (http),
     * which will be downloaded as temporary files and provided when ready.
     * <p>
     * Note that execution of this method may take a very long time.
     * 
     * @param e
     * @return 
     */
    public static List<File> getImageItems(DragEvent e) {
        Dragboard d = e.getDragboard();
        
        if (d.hasUrl() && ImageFileFormat.isSupported(d.getUrl())) {
            try {
                File nf = FileUtil.saveFileTo(d.getUrl(), App.TMP_FOLDER());
                return Collections.singletonList(nf);
            } catch (IOException ex) {
                Log.err(ex.getMessage());
                return EMPTY_LIST;
            }
        } else if (d.hasFiles())
            return FileUtil.getImageFiles(d.getFiles());
        else
            return EMPTY_LIST;
    }
    /**
     * Functionally equivalent to {@link #getImageItems(javafx.scene.input.DragEvent)} and
     * then executing the action on the result.
     * <p>
     * The difference is that this method delays the execution if needed, outside
     * of the srag event, allowing it to be consumed and completed before the
     * action executes.
     * <p>
     * This is imperative when the execution takes a long time (I/O operations),
     * because the drag should be ended without any lag irrelevant of execution
     * time.
     * <p>
     * Because the image can be drag&dropped as url from web, it wil take time
     * for it to be converted into local file. Therefore, always prefer this
     * methodover {@link #getImageItems(javafx.scene.input.DragEvent)}.
     * 
     * @param e
     * @param action 
     */
    public static void doWithImageItems(DragEvent e, Consumer<List<File>> action) {
        Dragboard d = e.getDragboard();
        
        if (d.hasUrl() && ImageFileFormat.isSupported(d.getUrl())) {
            String url = d.getUrl();
            Platform.runLater(() -> {
                try {
                    File nf = FileUtil.saveFileTo(url, App.TMP_FOLDER());
                    action.accept(Collections.singletonList(nf));
                } catch (IOException ex) {
                    Log.err(ex.getMessage());
                }
            });
        } else if (d.hasFiles()) {
            action.accept(FileUtil.getImageFiles(d.getFiles()));
        }
    }
    
     /**
     * @param d
     * @return true if contains at least 1 img file, img url
     */
    public static boolean hasImage(Dragboard d) {
        return (d.hasFiles() && d.getFiles().stream().anyMatch(ImageFileFormat::isSupported)) ||
                    (d.hasUrl() && ImageFileFormat.isSupported(d.getUrl()));
    }
    
    
    /**
     * Used for drag transfer of components. When drag starts the component and
     * its parent are wrapped into this object and when drag ends the component
     * is switched with the other one in the second parent.
     * <p>
     * This makes for one portion of the component swap. The one that initializes
     * the transfer.
     *
     * @author uranium
     */
    public static class WidgetTransfer {

        public final Container container;
        public final Component child;

        public WidgetTransfer(Container container, Component child) {
            super();
            this.child = child;
            this.container = container;
        }
    }
}

