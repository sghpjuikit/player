/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playlist;

import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;


/**
 * Observable playlist implementation.
 * This object is high level object intended to have mostly long life span. It
 * is not suited to simple PlaylistItem operations.
 * The key aspects are
 * - can be observed, bind, etc
 * - all changes to list are internal, externally, the playlist is read only
 * 
 * @author uranium
 */
public final class ObservablePlaylist extends AbstractPlaylist {
    
    private final ObservableList<PlaylistItem> playlist = FXCollections.observableArrayList();
    private final ReadOnlyObjectWrapper<Duration> length = new ReadOnlyObjectWrapper<>(Duration.ZERO);
    
    /**
     * Creates empty list
     */
    public ObservablePlaylist() {
    }
    /**
     * Creates list with items.
     * @param items to add
     */
    public ObservablePlaylist(List<PlaylistItem> items) {
        playlist.addAll(items);
        updateDuration();
    }
    
    /**
     * Use to bind to this playlist. INTERNAL USE ONLY.
     * Beware that all changes to the returned list
     * of this method are also changes to this actual playlist and any needed
     * accompanying operations will not take place. Use internally only.
     * @return observable list backing this playlist.
     */
    @Override
    protected ObservableList<PlaylistItem> list() {
        return playlist;
    }
    
    /**
     * Returns unmodifiable list of items of this playlist. Be aware that any
     * attempt to change the list will result in UnmodifiableException being
     * thrown.
     * Its safe to use this list for binding and any kind of observing.
     * @return unmodifiable list of items of this playlist.
     */
    @Override
    public ObservableList<PlaylistItem>getItems() {
        return FXCollections.unmodifiableObservableList(playlist);
    }
    
    /**
     * Returns bindable read only property of total duration.
     * @return bindable total playlist duration playlistproperty.
     */
    public ReadOnlyProperty<Duration> lengthProperty() {
        return length.getReadOnlyProperty();
    }
    
    /**
     * Returns playlist length. Any change to returned object is allowed but will
     * not  be reflected. Consider it read only.
     * @return length - total duration of the playlist.
     */
    @Override
    public Duration getLength() {
        return new Duration(length.get().toMillis());
    }

    @Override
    protected void updateDuration() {
        length.setValue(calculateLength());
    }
    
}
