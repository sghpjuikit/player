/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Library;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import java.io.File;
import java.net.URI;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import utilities.FileUtil;

/**
 *
 * @author uranium
 */
public class BookmarkItem extends Item {
    private final SimpleObjectProperty<URI> uri;
    private final SimpleStringProperty name;
    
    public BookmarkItem(URI _uri) {
        uri = new SimpleObjectProperty<>(_uri);
        name = new SimpleStringProperty(FileUtil.getName(_uri));
    }    
    public BookmarkItem(Item item) {
        this(item.getURI());
    }
    public BookmarkItem(File f) {
        this(f.toURI());
    }
    public BookmarkItem(String _name, URI _uri) {
        uri = new SimpleObjectProperty<>(_uri);
        name = new SimpleStringProperty(_name);
    }
    
    @Override
    public URI getURI() {
        return uri.get();
    }
    public void setURI(URI _uri) {
        uri.set(_uri);
    }
    public StringProperty uriProperty() {
        return name;
    }
    public String getName() {
        return name.get();
    }
    public void setName(String _name) {
        name.set(_name);
    }
    public StringProperty nameProperty() {
        return name;
    }

    @Override
    public String toString() {
        String output = "";
        output = output + getName().toString() + "\n";
        output = output + getURI().toString() + "\n";
        return output;
    }
    
    /**
     * Compares by natural order - name;
     * @param o
     * @return 
     */
    public int compareTo(PlaylistItem o) {
        String n1 = getName().toLowerCase();
        String n2 = o.getName().toLowerCase();
        return (int) Math.signum(n1.compareTo(n2));
    }

}
