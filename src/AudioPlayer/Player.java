
package AudioPlayer;

import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import PseudoObjects.ReadMode;
import java.util.Collections;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import utilities.TODO;

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
    public static Core.CurrentItem getCurrent() {
        return core.cI;
    }
    
    public static Metadata getPlaylistLastSelectedMetadata() {
        return core.selectedMetadata.get();
    }
    
    static SimpleObjectProperty<Metadata> playlistLastSelectedMetadataProperty() {
        return core.selectedMetadata;
    }
    
    @TODO("toBinding is a memleak, change whole method to EventStream implementation")
    public static void bindObservedMetadata(ObjectProperty<Metadata> observer,
//            Optional<Subscription> subscriptionWrapper,
            ReadMode mode) {
//        if (subscriptionWrapper.isPresent()) subscriptionWrapper.get().unsubscribe();
        switch (mode) {
            case PLAYLIST_SELECTED: observer.bind(core.selectedMetadata);
                                    break;
            case PLAYING:           observer.bind(core.cI.itemUpdatedES.map((ov,nv)->nv).toBinding(core.cI.get()));
                                    break;
            case LIBRARY_SELECTED:  //not yet implemented
                                    break;
            case CUSTOM:            observer.unbind();
                                    break;
            default:
        }
    }
    public static void bindObservedItem(ObjectProperty<PlaylistItem> observer, ReadMode mode) {
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
    
/******************************************************************************/
    
    /** 
     * Refreshes the given item for the whole application. Use when metadata of
     * the item changed.
     */
    public static void refreshItem(Item item) {

        // update all playlist items referring to this updated metadata
        PlaylistManager.updateItemsOf(item);
        
        // update library
        if (DB.exists(item))
            DB.updateItemsFromFile(Collections.singletonList(item));

        // reload metadata if played right now
        if (core.cI.get().same(item))
            core.cI.update();
        
        // reload selected playlist
        if (item.same(PlaylistManager.getSelectedItem()))
            core.loadPlaylistSelectedMetadata(item);
    }
}
