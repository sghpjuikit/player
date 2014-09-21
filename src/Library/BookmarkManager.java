/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Library;

import AudioPlayer.playlist.Item;
import Serialization.BookmarkItemConverter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import main.App;
import util.Parser.File.FileUtil;
import util.Log;

/**
 *
 * @author uranium
 */
public final class BookmarkManager {
    private static final ObservableList<BookmarkItem> bookmarks = FXCollections.observableArrayList();
    public static void initialize() {
        
    }
    public static ObservableList<BookmarkItem> getBookmarks() {
        loadBookmarks();
        return bookmarks;
    }
    
    public static void addBookmarks(List<? extends Item> items) {
        List b = items.stream().map(BookmarkItem::new).collect(Collectors.toList());
        bookmarks.addAll(b);
    }
    public static void addBookmarksAsURI(List<URI> items) {
        List b = items.stream().map(BookmarkItem::new).collect(Collectors.toList());
        bookmarks.addAll(b);
    }
    public static void addBookmarksAsFiles(List<File> items) {
        List b = items.stream().map(BookmarkItem::new).collect(Collectors.toList());
        bookmarks.addAll(b);
    }
    public static void removeBookmark(Item item) {
        bookmarks.removeIf(item::same);
    }
    public static void removeBookmark(BookmarkItem item) {
        bookmarks.remove(item);
    }
    /**
     * Removes all bookmarks that point to resource denoted by any of the
     * items.
     * in the parameter list.
     * @param items 
     */
    public static void removeBookmarksFor(List<Item> items) {
        items.forEach(BookmarkManager::removeBookmark);
    }
    /**
     * Removes all specified bookmarks.
     * @param items 
     */
    public static void removeBookmarks(List<BookmarkItem> items) {
        items.forEach(BookmarkManager::removeBookmark);
    }
    
    public static void loadBookmarks() {
        File dir = App.DATA_FOLDER();
        File f = new File(App.DATA_FOLDER(), "Bookmarks.xml");
        if (!FileUtil.isValidatedDirectory(dir)) {
            Log.err("Loading Bookmars failed.");
            return;
        }
        try {
            XStream xstream = new XStream(new DomDriver());
            xstream.registerConverter(new BookmarkItemConverter());
            bookmarks.setAll((Collection<? extends BookmarkItem>) xstream.fromXML(f));
        } catch (ClassCastException | StreamException ex) {
            Log.err("Unable to load bookmarks from the file: " + f.getPath() +
                    ". The file not found or content corrupted. ");
        }
    }
    
    public static void saveBookmarks() {
        File dir = App.DATA_FOLDER();
        if (!FileUtil.isValidatedDirectory(dir)) {
            Log.err("Saving Bookmars failed.");
            return;
        }
        File f = new File(dir, "Bookmarks.xml");
        ArrayList<BookmarkItem> b = new ArrayList<>(bookmarks);
        try {
            XStream xstream = new XStream(new DomDriver());
            xstream.registerConverter(new BookmarkItemConverter());
            xstream.toXML(b, new BufferedWriter(new FileWriter(f)));
        } catch (IOException | ClassCastException | StreamException ex) {
            Log.err("Unable to save bookmarks to the file: " + f.getPath() + ". The file not found. ");
        }
    }
}
