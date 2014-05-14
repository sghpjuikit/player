
package AudioPlayer;

import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import PseudoObjects.ReadMode;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;
import utilities.functional.functor.UnProcedure;

/**
 *
 * @author uranium
 */
public class Player {
    private static final Core core = new Core();
    public static final PlayerState state = new PlayerState();
    
    public static void initialize() {
        PLAYBACK.initialize();
        core.initialize();
    }
    
    public static void loadLast() {
        state.deserialize();
        PlaylistManager.changeState();
        PLAYBACK.loadLastState();
    }
        
    public static Metadata getCurrentMetadata() {
        return core.currentMetadataCache.get();
    }

    public static SimpleObjectProperty<Metadata> currentMetadataProperty() {
        return core.currentMetadataCache;
    }
    public static Metadata getPlaylistLastSelectedMetadata() {
        return core.selectedMetadata.get();
    }
    static SimpleObjectProperty<Metadata> playlistLastSelectedMetadataProperty() {
        return core.selectedMetadata;
    }
    public static List<Metadata> getPlaylistSelectedMetadatas() {
        return FXCollections.unmodifiableObservableList(core.selectedMetadatas);
    }
    static ObservableList<Metadata> playlistSelectedMetadatasProperty() {
        return core.selectedMetadatas;
    }
    public static void bindObservedMetadata(SimpleObjectProperty<Metadata> observer, ReadMode mode) {
        observer.unbind();
        switch (mode) {
            case PLAYLIST_SELECTED: observer.bind(core.selectedMetadata);
                                    break;
            case PLAYING:           observer.bind(core.currentMetadataCache);
                                    break;
            case LIBRARY_SELECTED:  //not yet implemented
                                    break;
            case CUSTOM:            observer.unbind();
                                    break;
            default:
        }
    }
    public static void bindObservedItem(SimpleObjectProperty<PlaylistItem> observer, ReadMode mode) {
        observer.unbind();
        switch (mode) {
            case PLAYLIST_SELECTED: observer.bind(PlaylistManager.selectedItemProperty());
                                    break;
            case PLAYING:           observer.bind(PlaylistManager.playingItemProperty());
                                    break;
            case LIBRARY_SELECTED:  //not yet implemented
                                    break;
            case CUSTOM:            observer.unbind();
                                    break;
            default:
        }
    }
    public static void bindPlayingItem(SimpleObjectProperty<PlaylistItem> observer) {
        observer.bind(PlaylistManager.playingItemProperty());
    }
    public static void bindSelectedItem(SimpleObjectProperty<PlaylistItem> observer) {
        observer.bind(PlaylistManager.selectedItemProperty());
    }
    public static void bindSelectedItems(ObservableList<PlaylistItem> observer) {
        observer = FXCollections.unmodifiableObservableList(PlaylistManager.getSelectedItems());
    }
    public static void bindPlaylistDuration(SimpleObjectProperty<Duration> observer) {
        observer.bind(PlaylistManager.lengthProperty());
    }
    

    
/******************************************************************************/
    
    /** Add behavior to playing item updated event. The event is fired every time
     * currently playing item changes or some of its metadata is changed.*/
    public static void addOnItemUpdate(UnProcedure<Metadata> handler) {
        core.itemChange.addOnUpdateHandler(handler);
    }
    /** Add behavior to playing item changed event. The event is fired every time
     * currently playing item changes.*/
    public static void addOnItemChange(UnProcedure<Metadata> handler) {
        core.itemChange.addOnChangeHandler(handler);
    }
    /** Remove behavior from playing item event.*/
    public static void remOnItemUpdate(UnProcedure<Metadata>handler) {
        core.itemChange.remHandler(handler);
    }
    
    /** For internal use only. */
    public static void refreshItem(Item item) {

        // update all playlist items referring to this updated metadata
        PlaylistManager.updateItemsOf(item);                                    // what is this wasteful metadata loading!?!?

        // reload metadata if played right now
        if (item.same(PlaylistManager.getPlayingItem())) {
            core.updateCurrent();
        }
        
        // reload selected playlist
        if (item.same(PlaylistManager.getSelectedItem()))
            core.loadPlaylistSelectedMetadata();
    }
}
