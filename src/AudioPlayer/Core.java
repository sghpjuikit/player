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
import java.util.function.BiConsumer;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.util.Duration;
import org.reactfx.BiEventSource;
import org.reactfx.Subscription;
import utilities.FxTimer;
import utilities.Log;

/**
 *
 * @author uranium
 */
final class Core {
    
    final CurrentItem cI = new CurrentItem();
    final SimpleObjectProperty<Metadata> selectedMetadata = new SimpleObjectProperty();

    void initialize(){
        
        PlaylistManager.playingItemProperty().addListener((o, ov, nv) -> cI.itemChanged(nv));
        
        PlaylistManager.selectedItemProperty().addListener(lastSelectedLoader);
    }

/******************************** current *************************************/
    
    public static class CurrentItem {
        Metadata val = EMPTY;
        Metadata nextMetadataCache = EMPTY;
        BiEventSource<Metadata,Metadata> itemPlayedES = new BiEventSource();
        BiEventSource<Metadata,Metadata> itemUpdatedES = new BiEventSource();
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
            Metadata old = val;
            val = value;
            if(change) itemPlayedES.push(old, val);
            itemUpdatedES.push(old, val);
        }
        
        /** 
         * Add behavior to playing item changed event. 
         * <p>
         * The event is fired every time playing item changes. This includes
         * replaying the sae item.
         * <p>
         * Use in cases requiring constantly updated information about the playing 
         * item.
         * <p>
         * Note: It is safe to call {@link #get()} method when this even fires.
         * It has already been updated.
         */
        public Subscription subscribeToChanges(BiConsumer<Metadata,Metadata> bc) {
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
         * displaying this information somewhere.
         * <p>
         * Note: It is safe to call {@link #get()} method when this even fires.
         * It has already been updated.
         */
        public Subscription subscribeToUpdates(BiConsumer<Metadata,Metadata> bc) {
            return itemUpdatedES.subscribe(bc);
        }
        
        public void update() {
            load(false, PlaylistManager.getPlayingItem());
        }
        
        void load() {
            load(true, PlaylistManager.getPlayingItem());
        }       
        
        void itemChanged(Item item) {
            // if same item, still fire change
            if(val.same(item)) {
                set(true,val);
                Log.deb("Current item metadata reused. Same item playing.");
            }
            // if preloaded, set
            if (nextMetadataCache.same(item)){
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
    
/******************************** selected ************************************/
    
    ChangeListener<Item> lastSelectedLoader = (o,ov,nv) -> loadPlaylistSelectedMetadata(nv);
    
    void loadPlaylistSelectedMetadata(Item lastSelected) {
        if(lastSelected==null) {
            selectedMetadata.set(EMPTY);
        } else {
            MetadataReader.create(lastSelected, (success,result) -> {
                if (success) {
                    Log.deb("In playlist last selected item metadata loaded.");
                    selectedMetadata.set(result);
                } else {
                    Log.deb("In playlist last selected item metadata reading failed.");
                    selectedMetadata.set(EMPTY);
                }
            });
        }
    }
}
