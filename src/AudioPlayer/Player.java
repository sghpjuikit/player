
package AudioPlayer;

import AudioPlayer.Core.CurrentItem;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataReader;
import PseudoObjects.ReadMode;
import static PseudoObjects.ReadMode.CUSTOM;
import java.net.URI;
import java.time.Duration;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singletonList;
import java.util.List;
import static java.util.Objects.requireNonNull;
import java.util.function.Consumer;
import static javafx.application.Platform.runLater;
import javafx.beans.value.WritableValue;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import static org.reactfx.EventStreams.merge;
import org.reactfx.Subscription;
import util.collections.MapSet;
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
    
    /** Equivalent to subscribing to some of the streams in this class determined
    by a parameter. Also unsubscribes from any previous subsbscribtions. To remain
    consistant, the action is immediately invoked with current stream value upon
    subscribtion as if the event emited this value.
    @param source determines which stream will be subscribed to
    @param subscription any previous subscribtion, preferrably a result of calling
    this method in the past. Null is ignored, else it will be unsubscribed.
    @param action action to do when event emits value. A handler.  */
    public static Subscription subscribe(ReadMode source, Subscription subscription, Consumer<Metadata> action) {
        // unbind
        if (subscription != null) subscription.unsubscribe();
        // bind
        EventStream<Metadata> s = null;
        switch (source) {
            case SELECTED_PLAYLIST: s = playlistSelectedItemES; break;
            case PLAYING:           s = playingtem.itemUpdatedES; break;
            case SELECTED_LIBRARY:  s = librarySelectedItemES; break;
            case SELECTED_ANY:      s = selectedItemES; break;
            case ANY:               s = anyItemES; break;
            case CUSTOM:            s = EventStreams.never(); break;
            default: throw new AssertionError("Illegal switch value: " + source);
        }
        
        // this ignores all events in rapid succession and fires only the last one
        subscription = s.successionEnds(Duration.ofMillis(200)).subscribe(action);
        // this also fires in between events periodically
        // but unfortunately it just does not perform well for rapid table selection
        // change when gui must be updated somewhere as a result. the gui update
        // can not be offloaded to bgr thread and it causes the table selection
        // (induced by UP/DOWN key being pressed for example) to lag for a moment
        // no matter what time period we use
        // also periods > 200ms have the unfortunate effect of a lag after last
        // event which is also annoying and unnatural
        // subscription = s.thenReduceFor(Duration.ofMillis(500),(a,b)->b).subscribe(action);
        
        if(source!=CUSTOM) action.accept(((WritableValue<Metadata>)s).getValue());
        return subscription;
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
    /** Merge of playlist and library selected item streams. */
    public static final ValueStream<Metadata> selectedItemES = new ValueStream(Metadata.EMPTY, merge(librarySelectedItemES,playlistSelectedItemES));
    /** Merge of playing item and playlist and library selected item streams. */
    public static final ValueStream<Metadata> anyItemES = new ValueStream(Metadata.EMPTY, librarySelectedItemES,playlistSelectedItemES,playingtem.itemUpdatedES);
    
    /** Stream for selected items in library that remembers value. The list can
    be empty, but never null. */
    public static final ValueEventSource<List<Metadata>> librarySelectedItemsES = new ValueEventSourceN(EMPTY_LIST);
    /** Stream for selected items in playlist that remembers value. The list can
    be empty, but never null. */
    public static final ValueEventSource<List<Metadata>> playlistSelectedItemsES = new ValueEventSourceN(EMPTY_LIST);
    /** Merge of playlist and library selected items streams. */
    public static final ValueStream<List<Metadata>> selectedItemsES = new ValueStream(EMPTY_LIST, merge(librarySelectedItemsES,playlistSelectedItemsES));
    public static final ValueStream<List<Metadata>> anyItemsES = new ValueStream(EMPTY_LIST, merge(librarySelectedItemsES,playlistSelectedItemsES.map(m->singletonList(m))));
    
    
    
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
        });
    }
}