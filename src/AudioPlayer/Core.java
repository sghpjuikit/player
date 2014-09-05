/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import static AudioPlayer.tagging.Metadata.EMPTY;
import AudioPlayer.tagging.MetadataReader;
import java.util.List;
import java.util.function.Consumer;
import javafx.util.Duration;
import org.reactfx.Subscription;
import utilities.FxTimer;
import utilities.Log;
import utilities.access.AccessibleStream;

/**
 *
 * @author uranium
 */
final class Core {
    
    

    void initialize(){
        PlaylistManager.selectedItemES.subscribe(this::selectedItemToMetadata);
        PlaylistManager.selectedItemsES.subscribe(this::selectedItemsToMetadata);
    }

/******************************** current *************************************/
    
    public static class CurrentItem {
        Metadata val = EMPTY;
        Metadata nextMetadataCache = EMPTY;
        AccessibleStream<Metadata> itemPlayedES = new AccessibleStream(EMPTY);
        AccessibleStream<Metadata> itemUpdatedES = new AccessibleStream(EMPTY);
        private final FxTimer nextCachePreloader = FxTimer.create(Duration.millis(400), () -> preloadNext());
        
        /**
         * Returns the playing item and all its information.
         * <p>
         * Note: It is always safe to call this method, even during playing item
         * change events.
         */
        public Metadata get() {
            return val;
        }

        void set(boolean change, Metadata value) {
            val = value;
            if(change) itemPlayedES.push(val);
            itemUpdatedES.push(val);
        }
        
        /** 
         * Add behavior to playing item changed event. 
         * <p>
         * The event is fired every time playing item changes. This includes
         * replaying the same item.
         * <p>
         * Use in cases requiring constantly updated information about the playing 
         * item.
         * <p>
         * Note: It is safe to call {@link #get()} method when this even fires.
         * It has already been updated.
         */
        public Subscription subscribeToChanges(Consumer<Metadata> bc) {
            return itemPlayedES.subscribe(bc);
        }
        
        /** 
         * Add behavior to playing item updated event. 
         * <p>
         * The event is fired every time playing item changes or even if some of its 
         * metadata is changed such artist or rating. More eager version of change
         * event.
         * <p>
         * Use in cases requiring not only change updates, but also constantly
         * (real time) updated information about the playing item, such as when
         * displaying this information somewhere - for example artist of the
         * played item.
         * <p>
         * Do not use when only the identity (defined by its URI) of the played 
         * item is required. For example lastFM scrobbling service would not want
         * to update played item status when the metadata of the item change as it
         * isnt a change in played item - it is still the same item.
         * <p>
         * Note: It is safe to call {@link #get()} method when this even fires.
         * It has already been updated.
         */
        public Subscription subscribeToUpdates(Consumer<Metadata> bc) {
            return itemUpdatedES.subscribe(bc);
        }
        
        public void update() {
            load(false, val);
        }
        
        void load() {
            load(true, val);
        }       
        
        public void itemChanged(Item item) {
            if(item == null) {
                set(true,EMPTY);
                Log.deb("Current item metadata set to empty. No item playing.");
            } 
            // if same item, still fire change
            else if(val.same(item)) {
                set(true,val);
                Log.deb("Current item metadata reused. Same item playing.");
            }
            // if preloaded, set
            else if (nextMetadataCache.same(item)) {
                set(true,nextMetadataCache);
                Log.deb("Current item metadata copied from next item metadata cache.");
            // else load
            } else {
                Log.deb("Next item metadata cache copy failed - content doesnt correspond to correct item. Loading now...");
                load(true, item);
            }
            
            // wait 400ms, preload metadata for next item
            nextCachePreloader.restart();
        }
        
        // load metadata, type indicates UPDATE vs CHANGE
        private void load(boolean changeType, Item item){
            MetadataReader.create(item, (success, result) -> {
                if (success){
                    set(changeType, result);
                    Log.deb("Current metadata loaded.");
                } else {
                    set(changeType, item.toMetadata());
                    Log.deb("Current metadata load fail. Metadata will be not be fully formed.");
                }
            });
        }
        
        private void preloadNext(){
            Log.deb("Preloading metadata for next item to play.");
            PlaylistItem next = PlaylistManager.playingItemSelector.getNextPlaying();
            if (next == null){
                Log.deb("Preloading aborted. No next playing item.");
            } else {
                MetadataReader.create(next,(success, result) -> {
                    if (success){
                        nextMetadataCache = result;
                        Log.deb("Next item metadata cache preloaded.");
                    } else {
                        // dont set any value, not even empty
                        Log.deb("Preloading next item metadata into cache failed.");
                    }
                });
            }
        }
    }
    
/************************** selected playlist items ***************************/
    
    void selectedItemToMetadata(PlaylistItem item) {
        if(item==null) {
            Player.playlistSelectedItemES.push(EMPTY);
        } else {
            MetadataReader.create(item, (success,result) -> {
                if (success) {
                    Log.deb("Last selected playlist item metadata loaded.");
                    Player.playlistSelectedItemES.push(result);
                } else {
                    Log.deb("Last selected playlistitem metadata reading failed.");
                    Player.playlistSelectedItemES.push(EMPTY);
                }
            });
        }
    }
    
    void selectedItemsToMetadata(List<PlaylistItem> items) {
        MetadataReader.readMetadata(items, (success,result) -> {
            if (success) {
                Log.deb("Selected playlist items metadata reading finished successfully.");
                Player.playlistSelectedItemsES.push(result);
            } else {
                Log.deb("Selected playlist items metadata reading failed.");
            }
        });
    }
}
