
package util.graphics.drag;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;

import AudioPlayer.Item;
import AudioPlayer.SimpleItem;
import Layout.Component;
import Layout.Container;
import Layout.Widgets.controller.io.Output;
import de.jensd.fx.glyphs.GlyphIcons;
import main.App;
import util.File.AudioFileFormat;
import util.File.AudioFileFormat.Use;
import util.File.FileUtil;
import util.File.ImageFileFormat;
import util.async.future.Fut;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.*;
import static javafx.scene.input.DataFormat.FILES;
import static javafx.scene.input.DragEvent.DRAG_DROPPED;
import static javafx.scene.input.DragEvent.DRAG_OVER;
import static javafx.scene.input.TransferMode.ANY;
import static util.File.AudioFileFormat.Use.APP;
import static util.File.FileUtil.getFilesAudio;
import static util.async.future.Fut.fut;
import static util.dev.Util.log;
import static util.functional.Util.IS;
import static util.functional.Util.filterMap;

/**
 *
 * @author uranium
 */
public final class DragUtil {

/********************************** drag signal pane **************************/

    /**
     * See {@link DragPane#installDragHint(javafx.scene.Node, de.jensd.fx.glyphs.GlyphIcons, java.lang.String, java.util.function.Predicate) }
     */
    public static final void installDragHint(Node r, GlyphIcons icon, String name, Predicate<DragEvent> accept) {
        DragPane.installDragSignalPane(r, icon, name, accept);
    }

    /**
     * See {@link DragPane#installDragHint(javafx.scene.Node, de.jensd.fx.glyphs.GlyphIcons, java.util.function.Supplier, java.util.function.Predicate) }
     */
    public static final void installDragHint(Node r, GlyphIcons icon, Supplier<String> name, Predicate<DragEvent> accept) {
        DragPane.installDragSignalPane(r, icon, name, accept);
    }

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

/******************************** handlers ************************************/

    public static EventHandler<DragEvent> accept(Predicate<? super DragEvent> cond) {
        return accept(cond,false);
    }

    public static EventHandler<DragEvent> accept(Predicate<? super DragEvent> cond, boolean orConsume) {
        return accept(cond, e -> orConsume);
    }

    public static EventHandler<DragEvent> accept(Predicate<? super DragEvent> cond, Predicate<DragEvent> orConsume) {
        return e -> {
            if (cond.test(e) && !orConsume.test(e) ) {
                e.acceptTransferModes(ANY);
                e.consume();
            }
        };
    }

    public static EventHandler<DragEvent> accept(Supplier<Boolean> cond) {
        return accept(e -> cond.get());
    }

    /** Always accepts and consumes drag over event. */
    public static final EventHandler<DragEvent> anyDragAccepthandler = accept(IS);

    /** {@link #accept(java.util.function.Predicate) } using {@link #hasComponent() } as predicate. */
    public static final EventHandler<DragEvent> componentDragAcceptHandler = accept(DragUtil::hasComponent);

    /** {@link #accept(java.util.function.Predicate) } using {@link #hasAudio(javafx.scene.input.DragEvent) } as predicate. */
    public static final EventHandler<DragEvent> audioDragAccepthandler = accept(DragUtil::hasAudio);;

    /** {@link #accept(java.util.function.Predicate) } using {@link #hasImage(javafx.scene.input.DragEvent) ) } as predicate. */
    public static final EventHandler<DragEvent> imgFileDragAccepthandler = accept(DragUtil::hasImage);;

    /** {@link #accept(java.util.function.Predicate) } using {@link #hasComponent(javafx.scene.input.DragEvent) } as predicate. */
    public static final EventHandler<DragEvent> widgetOutputDragAccepthandler = accept(DragUtil::hasWidgetOutput);


    public static void installDrag(Node node, GlyphIcons icon, String description, Predicate<DragEvent> condition, Consumer<DragEvent> action) {
        installDrag(node, icon, description, condition, e -> false, action);
    }

    public static void installDrag(Node node, GlyphIcons icon, String description, Predicate<DragEvent> condition, Predicate<DragEvent> exc, Consumer<DragEvent> action) {
        installDrag(node, icon, () -> description, condition, exc, action);
    }

    public static void installDrag(Node node, GlyphIcons icon, Supplier<String> description, Predicate<DragEvent> condition, Consumer<DragEvent> action) {
        installDrag(node, icon, description, condition, e -> false, action);
    }

    public static void installDrag(Node node, GlyphIcons icon, Supplier<String> description, Predicate<DragEvent> condition, Predicate<DragEvent> exc, Consumer<DragEvent> action) {
        // accept drag if desired
        node.addEventHandler(DRAG_OVER,accept(condition,exc));
        // handle drag & clear data
        node.addEventHandler(DRAG_DROPPED,e -> {
            if (condition.test(e)) {
                action.accept(e);
                e.setDropCompleted(true);
                e.consume();
            }
        });
        // show hint
        DragPane.installDragSignalPane(node,icon,description,condition,exc);
    }

/************************************ ANY *************************************/

    public static Object getAny(DragEvent e) {
        Dragboard d = e.getDragboard();
        // as we return immediately with the result, the order matters
        // first inapp objects, then general object (text, files, etc.)
        if(hasItemList(e)) return getItemsList(e);
        if(d.hasFiles()) return d.getFiles();
        if(d.hasImage()) return d.getImage();
        if(d.hasUrl()) return d.getUrl();
        if(d.hasString()) return d.getString();
        return data;
    }

    public static Object getAnyFut(DragEvent e) {
        Dragboard d = e.getDragboard();
        // as we return immediately with the result, the order matters
        // first inapp objects, then general object (text, files, etc.)
        if(hasItemList(e)) return getItemsList(e);
        if(d.hasFiles()) return d.getFiles();
        if(d.hasImage()) return d.getImage();
        if(d.hasUrl()) return futUrl(d.getUrl());
        if(d.hasString()) return d.getString(); // must be after url
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

    public static void setWidgetOutput(Output o, Dragboard db) {
        data = o;
        db.setContent(singletonMap(widget_outputDF, ""));   // fake data
    }

    /** Returns widget output from dragboard or runtime exceptin if none. */
    public static Output getWidgetOutput(DragEvent e) {
        if(!hasWidgetOutput(e))
            throw new RuntimeException("No widget output in data available.");
        return (Output) data;
    }

    /** Returns whether dragboard contains text. */
    public static boolean hasWidgetOutput(DragEvent e) {
        return e.getDragboard().hasContent(widget_outputDF);
    }

/*********************************** SONGS ************************************/

    public static void setItemList(List<? extends Item> items, Dragboard db, boolean includeFiles) {
        data = items;
        db.setContent(singletonMap(itemsDF, ""));   // fake data

        if(includeFiles) {
            HashMap<DataFormat,Object> c = new HashMap();
            c.put(itemsDF, "");   // fake data
            c.put(FILES, filterMap(items,Item::isFileBased,Item::getFile));
            db.setContent(c);
        }
    }

    public static List<Item> getItemsList(DragEvent e) {
        if(!hasItemList(e)) throw new RuntimeException("No item list in data available.");
        return (List<Item>) data;
    }

    public static boolean hasItemList(DragEvent e) {
        return e.getDragboard().hasContent(itemsDF);
    }

/********************************** LAYOUT ************************************/

    public static void setComponent(Container parent, Component child, Dragboard db) {
        data = new WidgetTransfer(parent, child);
        db.setContent(singletonMap(componentDF, ""));   // fake data
    }

    public static WidgetTransfer getComponent(DragEvent e) {
        if(!hasComponent(e)) throw new RuntimeException("No component in data available.");
        return (WidgetTransfer) data;
    }

    public static boolean hasComponent(DragEvent e) {
        return e.getDragboard().hasContent(componentDF);
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

        if (hasItemList(e)) {
            o.addAll(getItemsList(e));
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
     * Returns true if dragboard contains audio file/s. True does not guarantee the presence of audio,
     * because directories are not traversed and may not contain any audio.
     *
     * @return true iff contains at least 1 audio file or audio url or (any) directory
     */
    public static boolean hasAudio(DragEvent e) {
        Dragboard d = e.getDragboard();
        return (d.hasFiles() && FileUtil.containsAudioFileOrDir(d.getFiles(), Use.APP)) ||
                    (d.hasUrl() && AudioFileFormat.isSupported(d.getUrl(),Use.APP)) ||
                         hasItemList(e);
    }

    /**
     * Returns true if dragboard contains an image file/s. True guarantees the presence of the image. Files
     * denoting directories are ignored.
     *
     * @return true iff contains at least 1 img file or an img url
     */
    public static boolean hasImage(DragEvent e) {
        Dragboard d = e.getDragboard();
        return (d.hasFiles() && FileUtil.containsImageFiles(d.getFiles())) ||
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

//                // for debugging purposes to simulate long running actions
//                return fut(() -> {System.out.println("IMAGE DROPPED");
//                    try {
//                        Thread.sleep(3000);
//                    } catch (InterruptedException ex) {
//                        Logger.getLogger(DragUtil.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                    return fs.get(0);
//                });
        }
        if (d.hasUrl() && ImageFileFormat.isSupported(d.getUrl())) {
            return futUrl(d.getUrl());
        }
        return fut(null);
    }

    @Deprecated    // workaround method, remove
    public static File getImageNoUrl(DragEvent e) {
        Dragboard d = e.getDragboard();

        if (d.hasFiles()) {
            List<File> files = d.getFiles();
            List<File> fs = FileUtil.getImageFiles(files);
            if(!fs.isEmpty())
                return fs.get(0);
        }
        return null;
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

        if (hasItemList(e)) {
            return fut(getItemsList(e).stream());
        }
        if (d.hasFiles()) {
            List<File> files = d.getFiles();
            return fut(() -> getFilesAudio(files,APP,MAX_VALUE).map(SimpleItem::new));
        }
        if (d.hasUrl()) {
            String url = d.getUrl();
            return AudioFileFormat.isSupported(url,APP)
                        ? fut(Stream.of(new SimpleItem(URI.create(url))))
                        : fut(Stream.empty());
        }
        return fut(Stream.empty());
    }


    private static Fut<File> futUrl(String url) {
        return fut(() -> {
            try {
                // this can all fail when the certificate is not trusted
                // security is fine, but user doesnt care if a site he uses wont work due to this...
                // e.g. anime-pictures.net
                //
                // https://code.google.com/p/jsslutils/wiki/SSLContextFactory
                File f = FileUtil.saveFileTo(url, App.DIR_TEMP);
                     f.deleteOnExit();
                return f;
            } catch(IOException e) {
                log(DragUtil.class).error("Couldnt download from url",e);
                return null;
            }
        });
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