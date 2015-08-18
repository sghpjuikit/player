
package util.graphics.drag;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javafx.event.EventHandler;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;

import AudioPlayer.Item;
import AudioPlayer.SimpleItem;
import AudioPlayer.playlist.Playlist;
import Layout.Component;
import Layout.Container;
import Layout.Widgets.controller.io.Output;
import main.App;
import util.File.AudioFileFormat;
import util.File.AudioFileFormat.Use;
import util.File.FileUtil;
import util.File.ImageFileFormat;
import util.async.future.Fut;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.*;
import static javafx.scene.input.DataFormat.FILES;
import static javafx.scene.input.TransferMode.ANY;
import static util.File.AudioFileFormat.Use.APP;
import static util.File.FileUtil.getFilesAudio;
import static util.async.future.Fut.fut;
import static util.functional.Util.filterMap;

/**
 *
 * @author uranium
 */
public final class DragUtil {
    
/******************************* data formats *********************************/
    
    /** Data Format for List<Item>. */
    public static final DataFormat itemsDF = new DataFormat("items");
    /** Data Format for WidgetTransfer. */
    public static final DataFormat widgetDF = new DataFormat("widget");
    /** Data Format for Component. */
    public static final DataFormat componentDF = new DataFormat("component");
    /** Data Format for widget output linking. */
    public static final DataFormat widget_outputDF = new DataFormat("widget-output");
    
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
            e.acceptTransferModes(ANY);
            e.consume();
        }
    };
    
    /**
     * Accepts and consumes drag over event if contains at least 1 audio file, 
     * audio url, {@link Playlist} or list of {@link Item}.
     * <p>
     * Reusing this handler spares code duplication and multiple object instances.
     * 
     * @see #hasAudio(javafx.scene.input.Dragboard) 
     * @see #getAudioItems(javafx.scene.input.DragEvent) 
     */
    public static final EventHandler<DragEvent> audioDragAccepthandler = e -> {
        if (hasAudio(e.getDragboard())) {
            e.acceptTransferModes(ANY);
            e.consume();
        }
    };
    
    /**
     * Accepts and consumes drag over event if contains images
     * 
     * @see #hasImage(javafx.scene.input.Dragboard) 
     * @see #getImage(javafx.scene.input.DragEvent) 
     * @see #getImages(javafx.scene.input.DragEvent)
     */
    public static final EventHandler<DragEvent> imageFileDragAccepthandler = e -> {
        if (hasImage(e.getDragboard())) {
            e.acceptTransferModes(ANY);
            e.consume();
        }
    };
    
    /**
     * Similar to {@link #imageFileDragAccepthandler}, but does nothing when the dragged image
     * (if more then the first) is the same as the one supplied.
     * <p>
     * Useful if we want to accept image drag only if the dragged image is not the same as some 
     * image we already have. This should only be used when we request 1 dropped image.
     * 
     * @see #hasImage(javafx.scene.input.Dragboard) 
     * @see #getImage(javafx.scene.input.DragEvent) 
     * @see #getImages(javafx.scene.input.DragEvent)
     */
    public static EventHandler<DragEvent> imageFileDragAccepthandlerNo(Supplier<File> except) {
        return e -> {
            if(DragUtil.hasImage(e.getDragboard())) {
                Fut<File> fi = getImage(e);
                File i = fi.isDone() ? fi.getDone() : null;
                boolean same = i!=null && i.equals(except.get());
                if(!same) {
                    e.acceptTransferModes(ANY);
                    e.consume();
                }
            }
        };
    }
    
    /** Always accepts and consumes drag over event. */
    public static final EventHandler<DragEvent> anyDragAccepthandler = e -> { 
        e.acceptTransferModes(ANY); 
        e.consume();
    };
    
    
    public static Object getAny(DragEvent e) {
        Dragboard d = e.getDragboard();
        // as we return immediately with the result, the order matters
        // first inapp objects, then general object (text, files, etc.)
        if(hasItemList()) return getItemsList();
        if(d.hasFiles()) return d.getFiles();
        if(d.hasImage()) return d.getImage();
        if(d.hasString()) return d.getString();
        if(d.hasUrl()) return d.hasUrl();
        return data;
    }
    
/********************************** FILES *************************************/
    
    /** Accepts and consumes drag over event if contains at least 1 image file. */
    public static final EventHandler<DragEvent> fileDragAccepthandler = e -> {
        if (hasFiles(e)) {
            e.acceptTransferModes(ANY);
            e.consume();
        }
    };
        
    /** 
     * Returns filed from dragboard.
     * @return list of files in dragboard. Never null.
     */
    public static List<File> getFiles(DragEvent e) {
        List<File> o = e.getDragboard().getFiles();
        return o==null ? EMPTY_LIST : o;
    }
    
    /** Returns whether dragboard contains files. */
    public static boolean hasFiles(DragEvent e) {
        return e.getDragboard().hasFiles();
    }
    
/*********************************** TEXT *************************************/
    
    /** Accepts and consumes drag over event if contains text. */
    public static final EventHandler<DragEvent> textDragAccepthandler = e -> {
        if (hasText(e)) {
            e.acceptTransferModes(ANY);
            e.consume();
        }
    };
        
    /** 
     * Returns text from dragboard.
     * @return string in dragboard or "" if none.
     */
    public static String getText(DragEvent e) {
        String o = e.getDragboard().getString();
        if(o==null) o = e.getDragboard().getRtf();
        return o==null ? "" : o;
    }
    
    /** Returns whether dragboard contains text. */
    public static boolean hasText(DragEvent e) {
        return e.getDragboard().hasString() || e.getDragboard().hasRtf();
    }
    
/******************************* WIDGET OUTPUT ********************************/
    
    /** Accepts and consumes drag over event if contains text. */
    public static final EventHandler<DragEvent> widgetOutputDragAccepthandler = e -> {
        if (hasWidgetOutput()) {
            e.acceptTransferModes(ANY);
            e.consume();
        }
    };
    
    public static void setWidgetOutput(Output o, Dragboard db) {
        // put fake data into dragboard
        db.setContent(singletonMap(widget_outputDF, ""));
        data = o;
        dataFormat = widget_outputDF;
    }
        
    /** Returns widget output from dragboard or runtime exceptin if none. */
    public static Output getWidgetOutput(DragEvent e) {
        if(dataFormat != widget_outputDF) throw new RuntimeException("No widget output in data available.");
        return (Output) data;
    }
    
    /** Returns whether dragboard contains text. */
    public static boolean hasWidgetOutput() {
        return dataFormat == widget_outputDF;
    }
    
/*********************************** SONGS ************************************/
    
    public static void setItemList(List<? extends Item> items, Dragboard db, boolean includeFiles) {
        // put fake data into dragboard
        db.setContent(singletonMap(itemsDF, ""));
        data = items;
        dataFormat = itemsDF;
        
        if(includeFiles) {
            HashMap<DataFormat,Object> c = new HashMap();
            c.put(FILES, filterMap(items,Item::isFileBased,Item::getFile));
            db.setContent(c);
        }
    }
    
    public static List<Item> getItemsList() {
        if(dataFormat != itemsDF) throw new RuntimeException("No item list in data available.");
        return (List<Item>) data;
    }
    
    public static boolean hasItemList() {
        return dataFormat == itemsDF;
    }
    
/********************************** LAYOUT ************************************/
    
    public static void setComponent(Container parent, Component child, Dragboard db) {
        // put fake data into dragboard
        db.setContent(singletonMap(componentDF, ""));
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
     * list of items in this exact order.
     * <p>
     * Use in conjunction with {@link #audioDragAccepthandler}
     * 
     * @param e 
     * @return list of supported items derived from dragboard of the event.
     */
    public static List<Item> getAudioItems(DragEvent e) {
        Dragboard d = e.getDragboard();
        ArrayList<Item> o = new ArrayList();
        
        if (hasItemList()) {
            o.addAll(getItemsList());
        } else
        if (d.hasFiles()) {
            getFilesAudio(d.getFiles(),Use.APP,Integer.MAX_VALUE).map(SimpleItem::new).forEach(o::add);
        } else
        if (d.hasUrl()) {
            String url = d.getUrl();
            // watch out for non audio urls, we must filter those out, or
            // we could cause subtle bugs
            if(AudioFileFormat.isSupported(url,Use.APP))
                Optional.of(new SimpleItem(URI.create(url)))  // isnt this dangerous?
                        .filter(i->!i.isCorrupt(Use.APP)) // isnt this pointless?
                        .ifPresent(o::add);
        }
        return o;
    }
    
    /**
     * @param d
     * @return true if contains at least 1 audio file, audio url or items 
     */
    public static boolean hasAudio(Dragboard d) {
        return (d.hasFiles() && FileUtil.containsAudioFiles(d.getFiles(), Use.APP)) ||
                    (d.hasUrl() && AudioFileFormat.isSupported(d.getUrl(),Use.APP)) ||
                         hasItemList();
    }
    
    /**
     * @param d
     * @return true if contains at least 1 img file, img url
     */
    public static boolean hasImage(Dragboard d) {
        return (d.hasFiles() && !FileUtil.getImageFiles(d.getFiles()).isEmpty()) ||
                    (d.hasUrl() && ImageFileFormat.isSupported(d.getUrl()));
    }

    
    /**
     * Similar to {@link #getImages(javafx.scene.input.DragEvent)}, but
     * supplies only the first image, if available or null otherwise.
     * 
     * @return supplier, never null
     */
    public static Fut<File> getImage(DragEvent e) {
        Dragboard d = e.getDragboard();
        
        if (d.hasFiles()) {
            List<File> files = d.getFiles();
            List<File> fs = FileUtil.getImageFiles(files);
            if(!fs.isEmpty())
                return fut(fs.get(0));
        }
        if (d.hasUrl() && ImageFileFormat.isSupported(d.getUrl())) {
            String url = d.getUrl();
            return fut(() -> {
                try {
                    File f = FileUtil.saveFileTo(url, App.DIR_TEMP);
                         f.deleteOnExit();
                    return f;
                } catch(IOException ex) {
                    return null;
                }
            });
        }
        return fut(null);
    }
    
    /**
     * Returns supplier of image files in the dragboard.
     * Always call {@link #hasImage(javafx.scene.input.Dragboard) } before this 
     * method to check the content.
     * <p>
     * The supplier supplies:
     * <ul>
     * <ls>If there was an url, single image will be downloaded on background thread,
     * stored as temporary file and returned as singleton list. If any error
     * occurs, empty list is returned.
     * <ls>If there were files, all image files.
     * <ls>Empty list otherwise
     * </ul>
     * 
     * @return supplier, never null
     */
    public static Fut<List<File>> getImages(DragEvent e) {
        Dragboard d = e.getDragboard();

        if (d.hasFiles()) {
            List<File> files = d.getFiles();
            List<File> images = FileUtil.getImageFiles(files);
            if(!images.isEmpty())
                return fut(images);
        }
        if (d.hasUrl() && ImageFileFormat.isSupported(d.getUrl())) {
            String url = d.getUrl();
            return fut(() -> {
                try {
                    File f = FileUtil.saveFileTo(url, App.DIR_TEMP);
                         f.deleteOnExit();
                    return singletonList(f);
                } catch(IOException ex) {
                    return EMPTY_LIST;
                }
            });
        }
        return fut(EMPTY_LIST);
    }
    
    /**
     * Returns supplier of audio items in the dragboard.
     * Always call {@link #hasAudio(javafx.scene.input.Dragboard) before this 
     * method to check the content.
     * <p>
     * The supplier supplies:
     * <ul>
     * <ls>If there was an url, stream of single http based item.
     * <ls>If there were files, all audio files.
     * <ls>If there were {@link Item}s, all items.
     * <ls>Empty stream otherwise
     * </ul>
     * 
     * @return supplier, never null
     */
    public static Fut<Stream<Item>> getSongs(DragEvent e) {
        Dragboard d = e.getDragboard();
        
        if (hasItemList()) {
            return fut(getItemsList().stream());
        }
        if (d.hasFiles()) {
            List<File> files = d.getFiles();
            return fut(getFilesAudio(files,APP,MAX_VALUE).map(SimpleItem::new));
        }
        if (d.hasUrl()) {
            String url = d.getUrl();
            return AudioFileFormat.isSupported(url,APP)
                        ? fut(Stream.of(new SimpleItem(URI.create(url))))
                        : fut(Stream.empty());
        } 
        return fut(Stream.empty());
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