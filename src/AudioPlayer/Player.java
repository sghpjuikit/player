
package AudioPlayer;

import AudioPlayer.ItemChangeEvent.ItemChangeHandler;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import PseudoObjects.ReadMode;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Duration;

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
    
    /** @return Metadata representing currently played item. Never null. */
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
    public static void bindPlaylistDuration(SimpleObjectProperty<Duration> observer) {
        observer.bind(PlaylistManager.lengthProperty());
    }
    

    
/******************************************************************************/
    
    /** Add behavior to playing item updated event. The event is fired every time
     * playing item changes or some of its metadata is changed such artist
     * or rating.
     * <p>
     * Use in cases requiring constantly updated information about the playing 
     * item. This event guarantees consistency with currently played item 
     * metadata at all times, even during tagging.
     */
    public static void addOnItemUpdate(ItemChangeHandler<Metadata> handler) {
        core.itemChange.addOnUpdateHandler(handler);
    }
    /** Add behavior to playing item changed event. The event is fired every time
     * playing item changes. Playing the same item again will fire the event too.
     * <p>
     * Use when only momentary information about the playing item are required.
     */
    public static void addOnItemChange(ItemChangeHandler<Metadata> handler) {
        core.itemChange.addOnChangeHandler(handler);
    }
    /** Remove behavior from playing item event.*/
    public static void remOnItemUpdate(ItemChangeHandler<Metadata>handler) {
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
            core.loadPlaylistSelectedMetadata(item);
    }
}
