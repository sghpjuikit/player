package sp.it.pl.audio.playlist;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import sp.it.pl.audio.playlist.sequence.PlayingSequence;
import sp.it.pl.util.action.IsAction;
import sp.it.pl.util.collections.mapset.MapSet;
import sp.it.pl.util.conf.Configurable;
import sp.it.pl.util.functional.Functors.Ƒ1;
import sp.it.pl.util.reactive.ValueEventSource;
import static sp.it.pl.util.functional.Util.listRO;

/** Manages playlists. */
public class PlaylistManager implements Configurable {

	public static final MapSet<UUID,Playlist> playlists = new MapSet<>(p -> p.id);
	public static UUID active = null;
	public static final PlayingSequence playingItemSelector = new PlayingSequence();

	/** Last selected item on playlist or null if none. */
	public static final ValueEventSource<PlaylistSong> selectedItemES = new ValueEventSource<>(null);
	/** Selected items on playlist or empty list if none. */
	public static final ValueEventSource<List<PlaylistSong>> selectedItemsES = new ValueEventSource<>(listRO());

	public static void use(Consumer<Playlist> action) {
		Playlist p = null;
		if (active!=null) p = playlists.get(active);
		if (p==null) p = playlists.stream().findAny().orElse(null);
		if (p!=null) action.accept(p);
	}

	public static <T> T use(Ƒ1<Playlist,T> action, T or) {
		Playlist p = null;
		if (active!=null) p = playlists.get(active);
		if (p==null) p = playlists.stream().findAny().orElse(null);
		return p==null ? or : action.apply(p);
	}

	/** Plays first item on playlist. */
	@IsAction(name = "Play first", desc = "Plays first item on playlist.", keys = "ALT+W", global = true)
	public static void playFirstItem() {
		use(Playlist::playFirstItem);
	}

	/** Plays last item on playlist. */
	@IsAction(name = "Play last", desc = "Plays last item on playlist.", global = true)
	public static void playLastItem() {
		use(Playlist::playLastItem);
	}

	/** Plays next item on playlist according to its selector logic. */
	@IsAction(name = "Play next", desc = "Plays next item on playlist.", keys = "ALT+Z", global = true)
	public static void playNextItem() {
		use(Playlist::playNextItem);
	}

	/** Plays previous item on playlist according to its selector logic. */
	@IsAction(name = "Play previous", desc = "Plays previous item on playlist.", keys = "ALT+BACK_SLASH", global = true)
	public static void playPreviousItem() {
		use(Playlist::playPreviousItem);
	}

	/** Open chooser and add new to end of playlist. */
	@IsAction(name = "Enqueue files", desc = "Open file chooser to add files to playlist.")
	public static void chooseFilesToAdd() {
		use(p -> p.addOrEnqueueFiles(true));
	}

	/** Open chooser and add new to end of playlist. */
	@IsAction(name = "Enqueue directory", desc = "Open file chooser to add files from directory to playlist.")
	public static void chooseFolderToAdd() {
		use(p -> p.addOrEnqueueFolder(true));
	}

	/** Open chooser and add new to end of playlist. */
	@IsAction(name = "Enqueue url", desc = "Open file chooser to add url to playlist.")
	public static void chooseUrlToAdd() {
		use(p -> p.addOrEnqueueUrl(true));
	}

	/** Open chooser and play new items. Clears previous playlist */
	@IsAction(name = "Play files", desc = "Open file chooser to play files to playlist.")
	public static void chooseFilesToPlay() {
		use(p -> p.addOrEnqueueFiles(false));
	}

	/** Open chooser and play new items. Clears previous playlist */
	@IsAction(name = "Play directory", desc = "Open file chooser to play files from directory to playlist.")
	public static void chooseFolderToPlay() {
		use(p -> p.addOrEnqueueFolder(false));
	}

	/** Open chooser and play new items. Clears previous playlist */
	@IsAction(name = "Play url", desc = "Open file chooser to add url play playlist.")
	public static void chooseUrlToPlay() {
		use(p -> p.addOrEnqueueUrl(false));
	}

}