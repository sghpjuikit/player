
package AudioPlayer;

import AudioPlayer.Core.CurrentItem;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataReader;
import Layout.Widgets.controller.io.InOutput;
import java.net.URI;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singletonList;
import java.util.List;
import static java.util.Objects.requireNonNull;
import java.util.UUID;
import static org.reactfx.EventStreams.merge;
import static util.async.Async.runLater;
import util.collections.map.MapSet;
import util.reactive.ValueEventSource;
import util.reactive.ValueEventSourceN;
import util.reactive.ValueStream;

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

    
/******************************************************************************/
    
    /**
     * Prvides access to Metadata representing currently played item or empty
     * metadata if none. Never null.
     */
    public static final CurrentItem playingtem = new CurrentItem();
    /** Stream for selected item in library that remembers value. Value is 
    Metadata.EMPTY if null is pushed into the stream, never null. */
    public static final ValueEventSource<Metadata> librarySelectedItemES = new ValueEventSourceN(Metadata.EMPTY);
    /** Stream for selected item in playlist that remembers value. Value is 
    Metadata.EMPTY if null is pushed into the stream, never null. */
    public static final ValueEventSource<Metadata> playlistSelectedItemES = new ValueEventSourceN(Metadata.EMPTY);
//    /** Merge of playlist and library selected item streams. */
//    public static final ValueStream<Metadata> selectedItemES = new ValueStream(Metadata.EMPTY, merge(librarySelectedItemES,playlistSelectedItemES));
//    /** Merge of playing item and playlist and library selected item streams. */
//    public static final ValueStream<Metadata> anyItemES = new ValueStream(Metadata.EMPTY, librarySelectedItemES,playlistSelectedItemES,playingtem.itemUpdatedES);
    
    /** Stream for selected items in library that remembers value. The list can
    be empty, but never null. */
    public static final ValueEventSource<List<Metadata>> librarySelectedItemsES = new ValueEventSourceN(EMPTY_LIST);
    /** Stream for selected items in playlist that remembers value. The list can
    be empty, but never null. */
    public static final ValueEventSource<List<Metadata>> playlistSelectedItemsES = new ValueEventSourceN(EMPTY_LIST);
    /** Merge of playlist and library selected items streams. */
    public static final ValueStream<List<Metadata>> selectedItemsES = new ValueStream(EMPTY_LIST, merge(librarySelectedItemsES,playlistSelectedItemsES));
    public static final ValueStream<List<Metadata>> anyItemsES = new ValueStream(EMPTY_LIST, merge(librarySelectedItemsES,playlistSelectedItemsES.map(m->singletonList(m))));
    
    
    
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
        requireNonNull(item);
        
        MetadataReader.create(item, (ok,m) -> {
            if (ok) refreshItemWithUpdated(m);
        });
    }
    
    public static void refreshItems(List<? extends Item> items) {
        requireNonNull(items);
        if(items.isEmpty()) return;
        
        MetadataReader.readMetadata(items, (ok,m) -> {
            if (ok) refreshItemsWithUpdated(m);
        });
    }

    
    public static void refreshItemWithUpdated(Metadata m) {
        requireNonNull(m);
        
        // update all playlist items referring to this updated metadata
        PlaylistManager.getItems().stream().filter(p->p.same(m)).forEach(p -> p.update(m));

        // update library
        DB.updateItems(singletonList(m));

        // rfresh playing item data
        if (playingtem.get().same(m)) playingtem.update(m);

        // refresh selection event streams
        if(librarySelectedItemES.getValue().same(m)) librarySelectedItemES.push(m);
        if(playlistSelectedItemES.getValue().same(m)) playlistSelectedItemES.push(m);
        
        // rfresh playing item data
        if(playing.i.getValue()!=null) if(playing.i.getValue().same(m)) playing.i.setValue(m);
        if(playlistSelected.i.getValue()!=null) if(playlistSelected.i.getValue().same(m)) playlistSelected.i.setValue(m.toPlaylist());
        if(librarySelected.i.getValue()!=null) if(librarySelected.i.getValue().same(m)) librarySelected.i.setValue(m);
    }
    
    public static void refreshItemsWithUpdated(List<Metadata> metas) {
        requireNonNull(metas);
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
        mm.ifHasE(librarySelectedItemES.getValue(), librarySelectedItemES::push);
        mm.ifHasE(playlistSelectedItemES.getValue(), playlistSelectedItemES::push);
        
        
        if(playing.i.getValue()!=null) mm.ifHasE(playing.i.getValue(), playing.i::setValue);
        if(playlistSelected.i.getValue()!=null) mm.ifHasK(playlistSelected.i.getValue().getURI(), m->playlistSelected.i.setValue(m.toPlaylist()));
        if(librarySelected.i.getValue()!=null) mm.ifHasE(librarySelected.i.getValue(), librarySelected.i::setValue);
    }
    
    public static void refreshItemsWithUpdatedBgr(List<Metadata> metas) {
        requireNonNull(metas);
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
            mm.ifHasE(librarySelectedItemES.getValue(), librarySelectedItemES::push);
            mm.ifHasE(playlistSelectedItemES.getValue(), playlistSelectedItemES::push);        
        
            if(playing.i.getValue()!=null) mm.ifHasE(playing.i.getValue(), playing.i::setValue);
            if(playlistSelected.i.getValue()!=null) mm.ifHasK(playlistSelected.i.getValue().getURI(), m->playlistSelected.i.setValue(m.toPlaylist()));
            if(librarySelected.i.getValue()!=null) mm.ifHasE(librarySelected.i.getValue(), librarySelected.i::setValue);
        });
    }
}