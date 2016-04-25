
package audio.playlist;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import audio.playlist.sequence.PlayingSequence;
import util.action.IsAction;
import util.action.IsActionable;
import util.collections.mapset.MapSet;
import util.conf.Configurable;
import util.conf.IsConfig;
import util.conf.IsConfigurable;
import util.functional.Functors.Ƒ1;
import util.reactive.ValueEventSource;

import static main.App.APP;
import static util.functional.Util.listRO;

/**
 * Provides unified handling to everything playlist related in the application
 */
@IsConfigurable("Playlist")
@IsActionable
public class PlaylistManager implements Configurable {

    public static final MapSet<UUID,Playlist> playlists = new MapSet<>(p->p.id);
    public static UUID active = null;
    public static final PlayingSequence playingItemSelector = new PlayingSequence();

    /** Last selected item on playlist or null if none. */
    public static final ValueEventSource<PlaylistItem> selectedItemES = new ValueEventSource<>(null);
    /** Selected items on playlist or empty list if none. */
    public static final ValueEventSource<List<PlaylistItem>> selectedItemsES = new ValueEventSource<>(listRO());

    public static void use(Consumer<Playlist> action) {
        Playlist p = null;
        if(active!=null) p = playlists.get(active);
        if(p==null) p = playlists.stream().findAny().orElse(null);
        if(p!=null) action.accept(p);
    }
    public static <T> T use(Ƒ1<Playlist,T> action, T or) {
        Playlist p = null;
        if(active!=null) p = playlists.get(active);
        if(p==null) p = playlists.stream().findAny().orElse(null);
        return p==null ? or : action.apply(p);
    }

    @IsConfig(name = "Default browse location", info = "Opens this location for file dialogs.")
    public static File browse = APP.DIR_APP;
    @IsConfig(name = "File search depth", info = "Depth for recursive search within directories. 0 denotes specified directory.")
    public static int folder_depth = 1;


    /** Plays first item on playlist.*/
    @IsAction(name = "Play first", desc = "Plays first item on playlist.", keys = "ALT+W", global = true)
    public static void playFirstItem() {
        use(Playlist::playFirstItem);
    }

    /** Plays last item on playlist.*/
    @IsAction(name = "Play last", desc = "Plays last item on playlist.", global = true)
    public static void playLastItem() {
        use(Playlist::playLastItem);
    }

    /** Plays next item on playlist according to its selector logic.*/
    @IsAction(name = "Play next", desc = "Plays next item on playlist.", keys = "ALT+Z", global = true)
    public static void playNextItem() {
        use(Playlist::playNextItem);
    }

    /** Plays previous item on playlist according to its selector logic.*/
    @IsAction(name = "Play previous", desc = "Plays previous item on playlist.", keys = "ALT+BACK_SLASH", global = true)
    public static void playPreviousItem() {
        use(Playlist::playPreviousItem);
    }

    /** Open chooser and add new to end of playlist. */
    @IsAction(name = "Choose and Add Files", desc = "Open file chooser to add files to playlist.")
    public static void chooseFilesToAdd() {
        use(p -> p.addOrEnqueueFiles(true));
    }
    /** Open chooser and add new to end of playlist. */
    @IsAction(name = "Choose and Add Directory", desc = "Open file chooser to add files from directory to playlist.")
    public static void chooseFolderToAdd() {
        use(p -> p.addOrEnqueueFolder(true));
    }
    /** Open chooser and add new to end of playlist. */
    @IsAction(name = "Choose and Add Url", desc = "Open file chooser to add url to playlist.")
    public static void chooseUrlToAdd() {
        use(p -> p.addOrEnqueueUrl(true));
    }
    /** Open chooser and play new items. Clears previous playlist */
    @IsAction(name = "Choose and Play Files", desc = "Open file chooser to play files to playlist.")
    public static void chooseFilesToPlay() {
        use(p -> p.addOrEnqueueFiles(false));
    }
    /** Open chooser and play new items. Clears previous playlist */
    @IsAction(name = "Choose and Play Directory", desc = "Open file chooser to play files from directory to playlist.")
    public static void chooseFolderToPlay() {
        use(p -> p.addOrEnqueueFolder(false));
    }
    /** Open chooser and play new items. Clears previous playlist */
    @IsAction(name = "Choose and Play Url", desc = "Open file chooser to add url play playlist.")
    public static void chooseUrlToPlay() {
        use(p -> p.addOrEnqueueUrl(false));
    }
}