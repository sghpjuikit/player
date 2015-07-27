
package AudioPlayer;

import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import static AudioPlayer.tagging.Metadata.EMPTY;
import AudioPlayer.tagging.MetadataReader;
import Layout.Widgets.controller.io.InOutput;
import java.net.URI;
import static java.util.Collections.singletonList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.reactfx.EventSource;
import org.reactfx.Subscription;
import static util.async.Async.runLater;
import util.async.executor.FxTimer;
import util.collections.map.MapSet;
import unused.Log;
import static util.dev.Util.forbidNull;

/**
 *
 * @author uranium
 */
public class Player {
    public static final PlayerState state = new PlayerState();
    
    public static void initialize() {
        PLAYBACK.initialize();
    }
    
    public static void loadLast() {
        state.deserialize();
        PlaylistManager.changeState();
        PLAYBACK.loadLastState();
    }

    
/******************************************************************************/
    
    /**
     * Prvides access to Metadata representing currently played item or empty
     * metadata if none. Never null.
     */
    public static final CurrentItem playingtem = new CurrentItem();
//    /** Stream for selected item in library that remembers value. Value is 
//    Metadata.EMPTY if null is pushed into the stream, never null. */
//    public static final ValueEventSource<Metadata> librarySelectedItemES = new ValueEventSourceN(Metadata.EMPTY);
//    /** Stream for selected item in playlist that remembers value. Value is 
//    Metadata.EMPTY if null is pushed into the stream, never null. */
//    public static final ValueEventSource<Metadata> playlistSelectedItemES = new ValueEventSourceN(Metadata.EMPTY);
////    /** Merge of playlist and library selected item streams. */
////    public static final ValueStream<Metadata> selectedItemES = new ValueStream(Metadata.EMPTY, merge(librarySelectedItemES,playlistSelectedItemES));
////    /** Merge of playing item and playlist and library selected item streams. */
////    public static final ValueStream<Metadata> anyItemES = new ValueStream(Metadata.EMPTY, librarySelectedItemES,playlistSelectedItemES,playingtem.itemUpdatedES);
//    
//    /** Stream for selected items in library that remembers value. The list can
//    be empty, but never null. */
//    public static final ValueEventSource<List<Metadata>> librarySelectedItemsES = new ValueEventSourceN(EMPTY_LIST);
//    /** Stream for selected items in playlist that remembers value. The list can
//    be empty, but never null. */
//    public static final ValueEventSource<List<Metadata>> playlistSelectedItemsES = new ValueEventSourceN(EMPTY_LIST);
//    /** Merge of playlist and library selected items streams. */
//    public static final ValueStream<List<Metadata>> selectedItemsES = new ValueStream(EMPTY_LIST, merge(librarySelectedItemsES,playlistSelectedItemsES));
//    public static final ValueStream<List<Metadata>> anyItemsES = new ValueStream(EMPTY_LIST, merge(librarySelectedItemsES,playlistSelectedItemsES.map(m->singletonList(m))));
    
    
    
    public static final InOutput<Metadata> playing = new InOutput<>(UUID.fromString("876dcdc9-48de-47cd-ab1d-811eb5e95158"),"Playing", Metadata.class);
    public static final InOutput<PlaylistItem> playlistSelected = new InOutput<>(UUID.fromString("ca002c1d-8689-49f6-b1a0-0d0f8ff2e2a8"),"Selected in playlist", PlaylistItem.class);
    public static final InOutput<Metadata> librarySelected = new InOutput<>(UUID.fromString("ba002c1d-2185-49f6-b1a0-0d0f8ff2e2a8"),"Selected in Library", Metadata.class);
    public static final InOutput<Item> anySelected = new InOutput<>(UUID.fromString("1a01ca96-2e60-426e-831d-93b24605595f"),"Selected anywhere", Item.class);
    
    static {
        anySelected.i.bind(playlistSelected.o);
        anySelected.i.bind(librarySelected.o);
        playingtem.itemUpdatedES.subscribe(playing.i::setValue);
    }
    
    
    /** 
     * Refreshes the given item for the whole application. Use when metadata of
     * the item changed.
     */
    public static void refreshItem(Item item) {
        forbidNull(item);
        
        MetadataReader.create(item, (ok,m) -> {
            if (ok) refreshItemWithUpdated(m);
        });
    }
    
    public static void refreshItems(List<? extends Item> items) {
        forbidNull(items);
        if(items.isEmpty()) return;
        
        MetadataReader.readMetadata(items, (ok,m) -> {
            if (ok) refreshItemsWithUpdated(m);
        });
    }

    
    public static void refreshItemWithUpdated(Metadata m) {
        forbidNull(m);
        
        // update all playlist items referring to this updated metadata
        PlaylistManager.getItems().stream().filter(p->p.same(m)).forEach(p -> p.update(m));

        // update library
        DB.updateItems(singletonList(m));

        // rfresh playing item data
        if (playingtem.get().same(m)) playingtem.update(m);

        // refresh selection event streams
//        if(librarySelectedItemES.getValue().same(m)) librarySelectedItemES.push(m);
//        if(playlistSelectedItemES.getValue().same(m)) playlistSelectedItemES.push(m);
        
        // rfresh playing item data
        if(playing.i.getValue()!=null) if(playing.i.getValue().same(m)) playing.i.setValue(m);
        if(playlistSelected.i.getValue()!=null) if(playlistSelected.i.getValue().same(m)) playlistSelected.i.setValue(m.toPlaylist());
        if(librarySelected.i.getValue()!=null) if(librarySelected.i.getValue().same(m)) librarySelected.i.setValue(m);
    }
    
    public static void refreshItemsWithUpdated(List<Metadata> metas) {
        forbidNull(metas);
        if(metas.isEmpty()) return;
        
        // metadata map hashed with resource identity : O(n^2) -> O(n)
        MapSet<URI,Metadata> mm = new MapSet<>(Metadata::getURI,metas);

        // update all playlist items referring to this updated metadata
        PlaylistManager.getItems().forEach(p -> mm.ifHasK(p.getURI(), p::update));

        // update library
        DB.updateItems(metas);

        // rfresh playing item data
        mm.ifHasE(playingtem.get(), playingtem::update);

        // refresh selection event streams
//        mm.ifHasE(librarySelectedItemES.getValue(), librarySelectedItemES::push);
//        mm.ifHasE(playlistSelectedItemES.getValue(), playlistSelectedItemES::push);
        
        
        if(playing.i.getValue()!=null) mm.ifHasE(playing.i.getValue(), playing.i::setValue);
        if(playlistSelected.i.getValue()!=null) mm.ifHasK(playlistSelected.i.getValue().getURI(), m->playlistSelected.i.setValue(m.toPlaylist()));
        if(librarySelected.i.getValue()!=null) mm.ifHasE(librarySelected.i.getValue(), librarySelected.i::setValue);
    }
    
    public static void refreshItemsWithUpdatedBgr(List<Metadata> metas) {
        forbidNull(metas);
        if(metas.isEmpty()) return;
        
        // metadata map hashed with resource identity : O(n^2) -> O(n)
        MapSet<URI,Metadata> mm = new MapSet<>(Metadata::getURI,metas);

        DB.updateItemsBgr(metas);
        
        runLater(() -> {
            // update all playlist items referring to this updated metadata
            PlaylistManager.getItems().forEach(p -> mm.ifHasK(p.getURI(), p::update));
            // rfresh playing item data
            mm.ifHasE(playingtem.get(), playingtem::update);
            // refresh selection event streams
//            mm.ifHasE(librarySelectedItemES.getValue(), librarySelectedItemES::push);
//            mm.ifHasE(playlistSelectedItemES.getValue(), playlistSelectedItemES::push);        
        
            if(playing.i.getValue()!=null) mm.ifHasE(playing.i.getValue(), playing.i::setValue);
            if(playlistSelected.i.getValue()!=null) mm.ifHasK(playlistSelected.i.getValue().getURI(), m->playlistSelected.i.setValue(m.toPlaylist()));
            if(librarySelected.i.getValue()!=null) mm.ifHasE(librarySelected.i.getValue(), librarySelected.i::setValue);
        });
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    public static class CurrentItem {
        Metadata val = EMPTY;
        Metadata nextMetadataCache = EMPTY;
        EventSource<Metadata> itemPlayedES = new EventSource<>();
        EventSource<Metadata> itemUpdatedES = new EventSource<>();
        private final FxTimer nextCachePreloader = new FxTimer(400, 1, () -> preloadNext());
        
        /**
         * Returns the playing item and all its information.
         * <p>
         * Note: It is always safe to call this method, even during playing item
         * change events.
         */
        public Metadata get() {
            return val;
        }

        void set(boolean change, Metadata m) {
            val = m;
            if(change) itemPlayedES.push(m);
            itemUpdatedES.push(m);
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
        public void update(Metadata m) {
            set(false, m);
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
                    set(changeType, item.toMeta());
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
}