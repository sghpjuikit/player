
package AudioPlayer;

import AudioPlayer.Core.CurrentItem;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import PseudoObjects.ReadMode;
import java.util.Collections;
import static java.util.Collections.EMPTY_LIST;
import java.util.List;
import java.util.function.Consumer;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import util.access.AccessibleStream;

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
    
    @SuppressWarnings("UnusedAssignment")
    public static Subscription bindObservedMetadata(ReadMode source, Subscription subscription, Consumer<Metadata> action) {
        if (subscription != null) subscription.unsubscribe();
        
        switch (source) {
            case SELECTED_PLAYLIST: subscription = playlistSelectedItemES.subscribe(action);
                                    action.accept(playlistSelectedItemES.getValue());
                                    break;
            case PLAYING:           subscription = playingtem.itemUpdatedES.subscribe(action);
                                    action.accept(playingtem.itemUpdatedES.getValue());
                                    break;
            case SELECTED_LIBRARY:  subscription = librarySelectedItemES.subscribe(action);
                                    action.accept(librarySelectedItemES.getValue());
                                    break;
            case SELECTED_ANY:      subscription = selectedItemES.subscribe(action);
                                    action.accept(selectedItemES.getValue());
                                    break;
            case CUSTOM:            subscription = null;
                                    break;
            default: throw new AssertionError("Illegal switch value: " + source);
        }
        return subscription;
    }
    
/******************************************************************************/
    
    /**
     * Prvides access to Metadata representing currently played item or empty
     * metadata if none. Never null.
     */
    public static final CurrentItem playingtem = new CurrentItem();
    public static final AccessibleStream<Metadata> librarySelectedItemES = new AccessibleStream<Metadata>(null){
        @Override public void push(Metadata value) {
            super.push(value);
            selectedItemES.setValue(value);
        }
    };
    public static final AccessibleStream<List<Metadata>> librarySelectedItemsES = new AccessibleStream(EMPTY_LIST);
    public static final AccessibleStream<Metadata> playlistSelectedItemES = new AccessibleStream<Metadata>(null){
        @Override public void push(Metadata value) {
            super.push(value);
            selectedItemES.setValue(value);
        }
    };
    public static final AccessibleStream<List<Metadata>> playlistSelectedItemsES = new AccessibleStream(EMPTY_LIST);
    public static final AccessibleStream<Metadata> selectedItemES = new AccessibleStream(null, EventStreams.merge(librarySelectedItemES,playlistSelectedItemES));
    public static final AccessibleStream<List<Metadata>> selectedItemsES = new AccessibleStream(EMPTY_LIST, EventStreams.merge(librarySelectedItemsES,playlistSelectedItemsES));
    
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
        System.out.println(DB.exists(item) + " fff");
        // reload metadata if played right now
        if (playingtem.get().same(item))
            playingtem.update();
        
        // reload selected playlist
        if (item.same(PlaylistManager.selectedItemES.getValue()))
            core.selectedItemToMetadata(PlaylistManager.selectedItemES.getValue());
    }
}
