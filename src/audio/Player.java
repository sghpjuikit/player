
package audio;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.reactfx.Subscription;

import audio.playback.PLAYBACK;
import audio.playlist.Playlist;
import audio.playlist.PlaylistItem;
import audio.playlist.PlaylistManager;
import audio.tagging.Metadata;
import audio.tagging.MetadataReader;
import audio.tagging.MetadataWriter;
import static java.util.concurrent.TimeUnit.MINUTES;
import layout.widget.controller.io.InOutput;
import services.database.Db;
import util.async.Async;
import util.async.executor.EventReducer;
import util.async.executor.FxTimer;
import util.async.future.Fut;
import util.collections.mapset.MapSet;

import static audio.tagging.Metadata.EMPTY;
import static java.util.concurrent.TimeUnit.DAYS;
import static util.async.Async.FX;
import static util.async.Async.runNew;
import static util.async.executor.EventReducer.toLast;
import static util.dev.Util.log;
import static util.dev.Util.noØ;
import static util.functional.Util.list;

/**
 *
 * @author Martin Polakovic
 */
public class Player {

    // TODO: tweak thread pool to always have 1-2 threads on, but dispose of it entirely when this "service" is not used
    public static final ExecutorService IO_THREAD = new ThreadPoolExecutor(0, 8, 10, MINUTES, new LinkedBlockingQueue<>(), r -> {
            Thread t = new Thread(r);
                   t.setDaemon(true); // do not prevent application closing
                   t.setName("tagging-thread");
            return t;
        });
/********************************************* STATE **********************************************/

    public static final PlayerState state = PlayerState.deserialize();

    public static void initialize() {
        PLAYBACK.initialize();
    }

    public static void loadLast() {
        PLAYBACK.loadLastState();
    }

/******************************************************************************/

    /**
     * Prvides access to Metadata representing currently played item or empty
     * metadata if none. Never null.
     */
    public static final CurrentItem playingItem = new CurrentItem();

    public static final InOutput<Metadata> playing = new InOutput<>(UUID.fromString("876dcdc9-48de-47cd-ab1d-811eb5e95158"),"Playing", Metadata.class);
    public static final InOutput<PlaylistItem> playlistSelected = new InOutput<>(UUID.fromString("ca002c1d-8689-49f6-b1a0-0d0f8ff2e2a8"),"Selected in playlist", PlaylistItem.class);
    public static final InOutput<Metadata> librarySelected = new InOutput<>(UUID.fromString("ba002c1d-2185-49f6-b1a0-0d0f8ff2e2a8"),"Selected in Library", Metadata.class);
    public static final InOutput<Item> anySelected = new InOutput<>(UUID.fromString("1a01ca96-2e60-426e-831d-93b24605595f"),"Selected anywhere", Item.class);

    static {
        anySelected.i.bind(playlistSelected.o);
        anySelected.i.bind(librarySelected.o);
        playingItem.onUpdate(playing.i::setValue);

        // use jaudiotagger for total time value (fixes incorrect values coming from player classes)
        playingItem.onChange(m -> state.playback.duration.set(m.getLength()));
        // maintain PLAYED_FIRST_TIME & PLAYED_LAST_TIME metadata
        // note: for performance reasons we update AFTER song stops playing, not WHEN it starts
        // as with playcount incrementing, it could discrupt playback, although now we are losing
        // updates on application closing!
        playingItem.onChange((o, n) -> {
            MetadataWriter.use(o, w -> {
                w.setPlayedFirstNowIfEmpty();
                w.setPlayedLastNow();
            });
        });
    }

/**************************************** ITEM REFRESHING *****************************************/

    /**
     * Adds songs refreshed event handler. When application updates song metadata it will fire this
     * event. For example widget displaying song information may update that information, if the
     * event contains the song.
     * <p/>
     * Say, there is a widget with an input, displaying its value and sending it to its output, for
     * others to listen. And both are of {@link Item} type and updating the contents may be heavy
     * operation we want to avoid unless necessary.
     * Always update the output. It will update all inputs (of other widgets) bound to it. However,
     * there may be widgets whose inputs are not bound, but set manually. In order to not update the
     * input multiple times (by us and because it is bound) it should be checked whether input is
     * bound and then handled accordingly.
     * <p/>
     * But because the input can be bound to multiple outputs, we must check whether the input is
     * bound to the particular output it is displaying value of (and would be auto-updated from).
     * This is potentially complex or impossible (with unfortunate
     * {@link Object#equals(java.lang.Object) } implementation).
     * It is possible to use an {@link EventReducer} which will
     * reduce multiple events into one, in such case always updating input is recommended.
     */
    public static Subscription onItemRefresh(Consumer<MapSet<URI,Metadata>> handler) {
        refreshHandlers.add(handler);
        return () -> refreshHandlers.remove(handler);
    }

    /** Singleton variant of {@link #refreshItems(java.util.Collection)}. */
    public static void refreshItem(Item i) {
        noØ(i);
        refreshItems(list(i));
    }

    /**
     * Read metadata from tag of all items and invoke {@link #refreshItemsWith(java.util.List)}.
     * <p/>
     * Safe to call from any thread.
     * <p/>
     * Use when metadata of the items changed.
     */
    public static void refreshItems(Collection<? extends Item> is) {
        noØ(is);
        if (is.isEmpty()) return;

	    runNew(MetadataReader.buildReadMetadataTask(is, (ok,m) -> {
            if (ok) refreshItemsWith(m);
        }));
    }

    /** Singleton variant of {@link #refreshItemsWith(java.util.List)}. */
    public static void refreshItemWith(Metadata m) {
        noØ(m);
        refreshItemsWith(list(m));
    }
    /** Singleton variant of {@link #refreshItemsWith(java.util.List, boolean)}. */
    public static void refreshItemWith(Metadata m, boolean allowDelay) {
        noØ(m);
        refreshItemsWith(list(m),allowDelay);
    }

    /** Simple version of {@link #refreshItemsWith(java.util.List, boolean) } with false argument. */
    public static void refreshItemsWith(List<Metadata> ms) {
        refreshItemsWith(ms, false);
    }

    /**
     * Updates application (playlist, library, etc.) with latest metadata. Refreshes the given
     * data for the whole application.
     * <p/>
     * Safe to call from any thread.
     *
     * @param ms metadata to refresh
     * @param allowDelay flag for using delayed refresh to reduce refresh successions to single
     * refresh. Normally false is used.
     * <p/>
     * Use false to refresh immediatelly and true to queue the refresh for future
     * execution (will wait few seconds for next refresh request and if it comes, will wait again
     * and so on until none will come, which is when all queued refreshes execute all at once).
     */
    public static void refreshItemsWith(List<Metadata> ms, boolean allowDelay) {
        noØ(ms);
        if (allowDelay) Async.runFX(() -> red.push(ms));
        else refreshItemsWithNow(ms);
    }

    // processes delayed refreshes - queues them up and invokes refresh after some time
    // use only on fx thread
    private static EventReducer<List<Metadata>> red = toLast(3000, (o,n) -> {
        n.addAll(o);
        return n;
    }, Player::refreshItemsWithNow);

    private static final List<Consumer<MapSet<URI,Metadata>>> refreshHandlers = new ArrayList<>();

    // runs refresh on bgr thread, thread safe
    private static void refreshItemsWithNow(List<Metadata> ms) {
//        System.out.println("refreshing now " + ms.size());
        noØ(ms);
        if (ms.isEmpty()) return;

        // always on br thread
        IO_THREAD.execute(() -> {
            // metadata map hashed with resource identity : O(n^2) -> O(n)
            MapSet<URI,Metadata> mm = new MapSet<>(Metadata::getURI,ms);

            // update library
            Db.updatePer(ms);
            Db.updateInMemoryDbFromPersisted();

            Async.runFX(() -> {
                // update all playlist items referring to this updated metadata
                PlaylistManager.playlists.stream().flatMap(Playlist::stream).forEach((PlaylistItem p) -> mm.ifHasK(p.getURI(), p::update));
//                PlaylistManager.playlists.forEach(playlist -> playlist.forEach(p -> mm.ifHasK(p.getURI(), p::update)));

                // refresh playing item data
                mm.ifHasE(playingItem.get(), playingItem::update);

                if (playing.i.getValue()!=null) mm.ifHasE(playing.i.getValue(), playing.i::setValue);
                if (playlistSelected.i.getValue()!=null) mm.ifHasK(playlistSelected.i.getValue().getURI(), m->playlistSelected.i.setValue(m.toPlaylist()));
                if (librarySelected.i.getValue()!=null) mm.ifHasE(librarySelected.i.getValue(), librarySelected.i::setValue);

                // refresh rest
                refreshHandlers.forEach(h -> h.accept(mm));
            });
        });
    }




    public static class CurrentItem {
        private Metadata val = EMPTY;
        private Metadata nextMetadataCache = EMPTY;
        private final List<BiConsumer<Metadata,Metadata>> changes = new ArrayList<>();
        private final List<BiConsumer<Metadata,Metadata>> updates = new ArrayList<>();
        private final FxTimer nextCachePreloader = new FxTimer(400, 1, () -> preloadNext());

        /**
         * Returns the playing item and all its information.
         * <p/>
         * Note: It is always safe to call this method, even during playing item
         * change events.
         */
        public Metadata get() {
            return val;
        }

        void set(boolean change, Metadata new_metadata) {
            Metadata ov = val;
            Metadata nv = new_metadata;
            val = nv;

            // There is a small problem
            // During tagging it is possible the playback needs to be suspended and activated
            // This unfortunately cascades and fires this method, but suspending/activating
            // should be transparent to playback song change/update events (not when app starts,
            // only when tagging)
            //
            // This can lead to dangerous situations (rarely) for example when tagging suspends
            // playback and calls this method and there is a listener to this which calls tagging
            // this will cause infinite loop!
            //
            // for now we will use flag as dirty solution
            if (PLAYBACK.suspension_flag) return;


            if (change) changes.forEach(h -> h.accept(ov,nv));
            updates.forEach(h -> h.accept(ov,nv));
        }

        public Subscription onChange(BiConsumer<Metadata,Metadata> bc) {
            changes.add(bc);
            return () -> changes.remove(bc);
        }

        /**
         * Add behavior to playing item changed event.
         * <p/>
         * The event is fired every time playing item changes. This includes
         * replaying the same item.
         * <p/>
         * Use in cases requiring constantly updated information about the playing
         * item.
         * <p/>
         * Note: It is safe to call {@link #get()} method when this even fires.
         * It has already been updated.
         */
        public Subscription onChange(Consumer<Metadata> bc) {
            return onChange((o,n) -> bc.accept(n));
        }

        /**
         * Add behavior to playing item updated event.
         * <p/>
         * The event is fired every time playing item changes or even if some of its
         * metadata is changed such artist or rating. More eager version of change
         * event.
         * <p/>
         * Use in cases requiring not only change updates, but also constantly
         * (real time) updated information about the playing item, such as when
         * displaying this information somewhere - for example artist of the
         * played item.
         * <p/>
         * Do not use when only the identity (defined by its URI) of the played
         * item is required. For example lastFM scrobbling service would not want
         * to update played item status when the metadata of the item change as it
         * is not a change in played item - it is still the same item.
         * <p/>
         * Note: It is safe to call {@link #get()} method when this even fires.
         * It has already been updated.
         */
        public Subscription onUpdate(Consumer<Metadata> bc) {
            return onUpdate((o,n) -> bc.accept(n));
        }

        public Subscription onUpdate(BiConsumer<Metadata,Metadata> bc) {
            updates.add(bc);
            return () -> updates.remove(bc);
        }

        public void update() {
            load(false, val);
        }

        public void update(Metadata m) {
            set(false, m);
        }

        /** Execute when song starts playing. */
        public void itemChanged(Item item) {
            if (item == null) {
                set(true,EMPTY);
                log(Player.class).info("Current item metadata set to empty. No item playing.");
            }
            // if same item, still fire change
            else if (val.same(item)) {
                set(true,val);
                log(Player.class).info("Current item metadata reused. Same item playing.");
            }
            // if preloaded, set
            else if (nextMetadataCache.same(item)) {
                set(true,nextMetadataCache);
                log(Player.class).info("Current item metadata copied from next item metadata cache.");
            // else load
            } else {
                log(Player.class).info("Next item metadata cache copy failed - content does not correspond to correct item. Loading now...");
                load(true, item);
            }

            // wait 400ms, preload metadata for next item
            nextCachePreloader.start();
        }

	    // load metadata, type indicates UPDATE vs CHANGE
	    private void load(boolean changeType, Item item) {
		    Fut.fut(item)
			    .map(MetadataReader::readMetadata)
			    .use(m -> set(changeType, m.isEmpty() ? item.toMeta() : m), FX);
	    }

	    private void preloadNext() {
		    log(Player.class).info("Pre-loading metadata for next item to play.");

		    PlaylistItem next = PlaylistManager.use(Playlist::getNextPlaying,null);
		    if (next != null) {
			    Fut.fut(next)
				    .map(MetadataReader::readMetadata)
				    .use(m -> nextMetadataCache = m, FX);
		    }
	    }
    }
}