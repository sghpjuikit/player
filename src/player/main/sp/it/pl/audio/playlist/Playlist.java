package sp.it.pl.audio.playlist;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;
import kotlin.jvm.functions.Function1;
import sp.it.pl.audio.Song;
import sp.it.pl.gui.objects.form.Form;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.window.popup.PopWindow;
import sp.it.pl.gui.objects.window.stage.WindowBase;
import sp.it.util.async.executor.EventReducer;
import sp.it.util.collections.mapset.MapSet;
import sp.it.util.conf.Config;
import sp.it.util.conf.ValueConfig;
import sp.it.util.type.VType;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.geometry.Pos.CENTER;
import static javafx.util.Duration.millis;
import static sp.it.pl.gui.objects.window.ShowArea.WINDOW_ACTIVE;
import static sp.it.pl.main.AppBuildersKt.infoIcon;
import static sp.it.pl.main.AppFileKt.audioExtensionFilter;
import static sp.it.pl.main.AppFileKt.isAudio;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.async.AsyncKt.FX;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.async.AsyncKt.runIO;
import static sp.it.util.dev.FailKt.noNull;
import static sp.it.util.file.FileType.DIRECTORY;
import static sp.it.util.file.Util.getFilesR;
import static sp.it.util.functional.Util.map;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UtilKt.onChange;
import static sp.it.util.system.EnvironmentKt.chooseFile;
import static sp.it.util.system.EnvironmentKt.chooseFiles;

public class Playlist extends SimpleListProperty<PlaylistSong> {

	public final UUID id;
	private final ReadOnlyObjectWrapper<PlaylistSong> playingSongWrapper = new ReadOnlyObjectWrapper<>(null);
	public final ReadOnlyObjectProperty<PlaylistSong> playingSong = playingSongWrapper.getReadOnlyProperty();
	private final ReadOnlyObjectWrapper<Duration> durationWrapper = new ReadOnlyObjectWrapper<>(Duration.ZERO);
	public final ReadOnlyObjectProperty<Duration> duration = durationWrapper.getReadOnlyProperty();

	/**
	 * Needs to be invoked when {@link PlaylistSong#update()} on any song in this playlist is invoked.
	 * Prefer {@link #updateItem(sp.it.pl.audio.Song)} and {@link #updateItems()}.
	 */
	public final EventReducer<Void> durationUpdater = EventReducer.toLast(50, d -> durationWrapper.setValue(computeDuration()));

	public Playlist() {
		this(UUID.randomUUID());
	}

	public Playlist(UUID id) {
		super(observableArrayList());
		this.id = id;

		onChange(getValue(), runnable(() -> durationUpdater.push(null)));
	}

	public void updatePlayingItem(int i) {
		boolean exists = i>=0 && i<size();
		playingSongWrapper.set(exists ? get(i) : null);
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	private Function1<? super List<PlaylistSong>, ? extends List<PlaylistSong>> transformer = x -> x;

	private List<PlaylistSong> transform() {
		return transformer.invoke(this);
	}

	public void setTransformationToItemsOf(ObservableList<PlaylistSong> transformed) {
		transformer = original -> transformed;
	}

	public void setTransformation(Function1<? super List<PlaylistSong>, ? extends List<PlaylistSong>> transformer) {
		this.transformer = transformer;
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/** @return total playlist duration - a sum of all playlist song durations. */
	public Duration computeDuration() {
		double sum = stream()
				.map(PlaylistSong::getTime)
				.filter(d -> !d.isIndefinite() && !d.isUnknown())
				.mapToDouble(Duration::toMillis)
				.sum();
		return millis(sum);
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/**
	 * Returns true if specified song is playing song on the playlist. There can
	 * only be one song in the application for which this method returns true.
	 *
	 * @return true if song is played.
	 */
	public boolean isPlaying(PlaylistSong song) {
		return playingSongWrapper.get()==song;
	}

	/**
	 * Returns index of the first same song in playlist.
	 *
	 * @return song index. -1 if not in playlist.
	 * @throws java.lang.RuntimeException if any param null
	 * @see Song#same(sp.it.pl.audio.Song)
	 */
	public int indexOfSame(Song song) {
		List<PlaylistSong> transformed = transform();
		for (int i = 0; i<transformed.size(); i++)
			if (transformed.get(i).same(song)) return i;
		return -1;
	}

	/** @return index of playing song or -1 if no song is playing */
	public int indexOfPlaying() {
		return playingSongWrapper.get()==null ? -1 : indexOf(playingSongWrapper.get());
	}

	public PlaylistSong getPlaying() {
		return playingSongWrapper.get();
	}

	/** @return true when playlist contains songs same as the parameter */
	public boolean containsSame(Song song) {
		return stream().anyMatch(song::same);
	}

	/** Returns true iff any song on this playlist is being played played. */
	public boolean containsPlaying() {
		return indexOfPlaying()>=0;
	}

	/** Removes all unplayable songs from this playlist. */
	public void removeUnplayable() {
		List<PlaylistSong> staying = new ArrayList<>();
		for (int i = 0; i<size(); i++) {
			PlaylistSong p = get(i);
			if (!p.isCorrupt())
				staying.add(p);
		}
		if (staying.size()==size()) return;
		setAll(staying);
	}

	/**
	 * Removes all songs such as no two songs on the playlist are the same as
	 * in {@link Song#same(sp.it.pl.audio.Song)}.
	 */
	public void removeDuplicates() {
		MapSet<URI,Song> unique = new MapSet<>(Song::getUri);
		List<PlaylistSong> staying = new ArrayList<>();
		for (int i = 0; i<size(); i++) {
			PlaylistSong p = get(i);
			if (!unique.contains(p)) {
				unique.add(p);
				staying.add(p);
			}
		}
		if (staying.size()==size()) return;
		setAll(staying);
	}

	/**
	 * Duplicates the song if it is in playlist. If it is not, does nothing.
	 * Duplicate will appear on the next index following the original.
	 */
	public void duplicateItem(PlaylistSong song) {
		int i = indexOf(song);
		if (i!=-1) add(i + 1, song.copy());
	}

	/**
	 * Duplicates the songs if they are in playlist. If they arent, does nothing.
	 * Duplicates will appear on the next index following the last songs's index.
	 *
	 * @param songs songs to duplicate
	 */
	public void duplicateItemsAsGroup(List<PlaylistSong> songs) {
		int index = 0;
		List<PlaylistSong> to_dup = new ArrayList<>();
		for (PlaylistSong song : songs) {
			int i = indexOf(song);
			if (i!=-1) {
				to_dup.add(song.copy());
				index = i + 1;
			}
		}
		if (to_dup.isEmpty()) return;
		addAll(index, to_dup);
	}

	/**
	 * Duplicates the songs if they are in playlist. If they arent, does nothing.
	 * Each duplicate will appear at index right after its original - sort of
	 * couples will appear.
	 *
	 * @param songs songs to duplicate
	 */
	public void duplicateItemsByOne(List<PlaylistSong> songs) {
		songs.forEach(this::duplicateItem);
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/** Reverses order of the songs. Operation can not be undone. */
	public void reverse() {
		FXCollections.reverse(this);
	}

	/** Randomizes order of the songs. Operation can not be undone. */
	public void randomize() {
		FXCollections.shuffle(this);
	}

	/**
	 * Moves/shifts all specified songs by specified distance.
	 * Selected songs retain their relative positions. Items stop moving when
	 * any of them hits end/start of the playlist. Items wont rotate the list.
	 *
	 * @apiNote If this method requires real time response (for example reacting on mouse drag in table), it is important
	 * to 'cache' the behavior and allow values >1 && <-1 so the moved songs dont lag behind.
	 *
	 * @param indexes of songs to move. Must be List<Integer>.
	 * @param by distance to move songs by. Negative moves back. Zero does nothing.
	 * @return updated indexes of moved songs.
	 */
	public List<Integer> moveItemsBy(List<Integer> indexes, int by) {
		List<List<Integer>> blocks = slice(indexes);
		List<Integer> newSelected = new ArrayList<>();

		try {
			if (by>0) {
				for (int i = blocks.size() - 1; i>=0; i--) {
					newSelected.addAll(moveItemsByBlock(blocks.get(i), by));
				}
			} else if (by<0) {
				for (List<Integer> block : blocks) {
					newSelected.addAll(moveItemsByBlock(block, by));
				}
			}
		} catch (IndexOutOfBoundsException e) {
			return indexes;
		}
		return newSelected;
	}

	private List<Integer> moveItemsByBlock(List<Integer> indexes, int by) throws IndexOutOfBoundsException {
		List<Integer> newSelected = new ArrayList<>();
		try {
			if (by>0) {
				for (int i = indexes.size() - 1; i>=0; i--) {
					int ii = indexes.get(i);
					Collections.swap(this, ii, ii + by);
					newSelected.add(ii + by);
				}

			} else if (by<0) {
				for (int ii : indexes) {
					Collections.swap(this, ii, ii + by);
					newSelected.add(ii + by);
				}
			}
		} catch (IndexOutOfBoundsException ex) {
			// thrown if moved block hits start or end of the playlist
			// this gets rid of enormously complicated if statement
			// return old indexes
//            return indexes;
			throw new IndexOutOfBoundsException();
		}
		return newSelected;
	}

	// slice to monolithic blocks
	private List<List<Integer>> slice(List<Integer> indexes) {
		if (indexes.isEmpty()) return new ArrayList<>();

		List<List<Integer>> blocks = new ArrayList<>();
		blocks.add(new ArrayList<>());
		int last = indexes.get(0);
		int list = 0;
		blocks.get(list).add(indexes.get(0));
		for (int i = 1; i<indexes.size(); i++) {
			int index = indexes.get(i);
			if (index==last + 1) {
				blocks.get(list).add(index);
				last++;
			} else {
				list++;
				last = index;
				List<Integer> newL = new ArrayList<>();
				newL.add(index);
				blocks.add(newL);
			}
		}

		return blocks;
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/**
	 * Updates song.
	 * Updates all instances of the Song that are in the playlist. When application
	 * internally updates PlaylistSong it must make sure all its duplicates are
	 * updated too. Thats what this method does.
	 *
	 * @param song song to update
	 */
	public void updateItem(Song song) {
		stream().filter(song::same).forEach(PlaylistSong::update);
		durationUpdater.push(null);
	}

	/**
	 * Updates all not updated songs.
	 * <p/>
	 * If some song is not on playlist it will also be updated but it will have
	 * no effect on this playlist (but will on others, if they contain it).
	 *
	 * @param songs songs to update.
	 */
	public void updateItems(Collection<PlaylistSong> songs) {
		if (songs.isEmpty()) return;
		List<PlaylistSong> l = new ArrayList<>(songs);
		runIO(() -> {
			for (PlaylistSong i : l) {
				if (Thread.interrupted()) return;
				if (!i.isUpdated()) i.update();
			}
		}).useBy(FX, it ->
			durationUpdater.push(null)
		);
	}

	/**
	 * Use to completely refresh playlist.
	 * Updates all songs on playlist. This method guarantees that all songs
	 * will be up to date.
	 * After this method is invoked, every song will be updated at least once
	 * and reflect metadata written in the physical file.
	 * <p>
	 * Utilizes bgr thread. Its safe to call this method without any performance
	 * impact.
	 */
	public void updateItems() {
		updateItems(this);
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/** Plays given URI by searching for an existing PlaylistSong with that URI or adding it as a new Song */
	public void playUri(URI uri) {
		for (PlaylistSong song : this) {
			if(song.same(uri)) {
				playItem(song);
				return;
			}
		}
		addNplay(uri);
	}

	/** Adds a new PlaylistSong to the end of the Playlist and plays it */
	public void addNplay(URI uri) {
		addUri(uri);
		playLastItem();
	}

	// this will stay private or there would be bugs due to using bad index
	// use transformed
	private void playItem(int index) {
		try {
			playItem(transform().get(index));
		} catch (IndexOutOfBoundsException ex) {
			APP.audio.stop();
		}
	}

	/** Plays given song. Does nothing if song not on playlist or null. */
	public void playItem(PlaylistSong song) {
		playItem(song, p -> PlaylistManager.playingItemSelector.getNext(p, transform()));
	}

	volatile private PlaylistSong unplayable1st = null;

	/***
	 * Plays specified song or if not possible, uses the specified function to calculate the next song to play.
	 * <p/>
	 * This method is asynchronous.
	 *
	 * @param song song to play
	 * @param altSupplier supplier of next song to play if the song can not be
	 * played. It may for example return next song, or random song, depending
	 * on the selection strategy. If the next song to play again can not be
	 * played the process repeats until song to play is found or no song is
	 * playable.
	 */
	public void playItem(PlaylistSong song, UnaryOperator<PlaylistSong> altSupplier) {
		if (song!=null && transform().contains(song)) {
			runIO(() -> {
				// we cant play song -> we try to play next one and eventually get here again => need defend against case where no song is playable
				boolean unplayable = song.isCorrupt();  // potentially blocking
				if (unplayable) {
					boolean isNonePlayable = unplayable1st==song && stream().allMatch(PlaylistSong::isCorrupt); // potentially blocking
					runFX(() -> {
						if (isNonePlayable) {
							APP.audio.stop();
							unplayable1st = null;
						} else {
							playingSongWrapper.set(song);
							if (unplayable1st==null) unplayable1st = song;  // remember 1st unplayable
							// try to play next song, note we dont use the supplier as a fallback 2nd time
							// we use linear 'next time' supplier instead, to make sure we check every
							// song on a completely unplayable playlist and exactly once. Say provided
							// one selects random song - we could get into potentially infinite loop or
							// check songs multiple times or even skip playable songs to check completely!
							// playItem(alt_supplier.apply(song),alt_supplier);
							playItem(altSupplier.apply(song));
						}
					});
				} else {
					runFX(() -> {
						unplayable1st = null;
						PlaylistManager.active = this.id;
						PlaylistManager.playlists.forEach(p -> p.playingSongWrapper.set(p==this ? song : null));
						APP.audio.play(song);
					});
				}
			});
		}
	}

	/** Plays first song on playlist. */
	public void playFirstItem() {
		playItem(0);
	}

	/** Plays last song on playlist. */
	public void playLastItem() {
		playItem(transform().size() - 1);
	}

	/** Plays next song on playlist according to its selector logic. */
	public void playNextItem() {
		playItem(PlaylistManager.playingItemSelector.getNext(getPlaying(), transform()), p -> PlaylistManager.playingItemSelector.getNext(p, transform()));
	}

	/** Plays previous song on playlist according to its selector logic. */
	public void playPreviousItem() {
		playItem(PlaylistManager.playingItemSelector.getPrevious(getPlaying(), transform()), p -> PlaylistManager.playingItemSelector.getPrevious(p, transform()));
	}

	/**
	 * Plays new playlist.
	 * Clears active playlist completely and adds all songs from new playlist.
	 * Starts playing first file.
	 *
	 * @param songs songs to be onthe  playlist.
	 * @throws NullPointerException if param null.
	 */
	public void setNplay(Collection<? extends Song> songs) {
		setNplay(songs.stream());
	}

	/**
	 * Plays new playlist.
	 * Clears active playlist completely and adds all songs from new playlist.
	 * Starts playing song with the given index. If index is out of range for new
	 * playlist, handles according to behavior in playItem(index int) method.
	 *
	 * @param songs songs.
	 * @param from index of song to play from
	 * @throws NullPointerException if param null.
	 */
	public void setNplayFrom(Collection<? extends Song> songs, int from) {
		setNplayFrom(songs.stream(), from);
	}

	/**
	 * Plays new playlist.
	 * Clears active playlist completely and adds all songs from new playlist.
	 * Starts playing first file.
	 *
	 * @param songs songs.
	 * @throws NullPointerException if param null.
	 */
	public void setNplay(Stream<? extends Song> songs) {
		noNull(songs);
		setAll(songs.map(Song::toPlaylist).collect(toList()));
		playFirstItem();
	}

	/**
	 * Plays new playlist.
	 * Clears active playlist completely and adds all songs from new playlist.
	 * Starts playing song with the given index. If index is out of range for new
	 * playlist, handles according to behavior in playItem(index int) method.
	 *
	 * @param songs songs to add.
	 * @param from index of song to play from
	 * @throws NullPointerException if param null.
	 */
	public void setNplayFrom(Stream<? extends Song> songs, int from) {
		noNull(songs);
		setAll(songs.map(Song::toPlaylist).collect(toList()));
		playItem(get(from));
	}

	public PlaylistSong getNextPlaying() {
		return PlaylistManager.playingItemSelector.getNext(getPlaying(), transform());
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/**
	 * Adds new song to end of this playlist, based on the url (String). Use this method
	 * for  URL based Items.
	 *
	 * @throws NullPointerException when param null.
	 */
	public void addUrl(String url) {
		addUrls(Collections.singletonList(url));
	}

	/**
	 * Adds new songs to end of this playlist, based on the urls (String). Use
	 * this method for URL based Items.
	 * Malformed url's will be ignored.
	 *
	 * @throws NullPointerException when param null.
	 */
	public void addUrls(Collection<String> urls) {
		List<URI> add = new ArrayList<>();
		for (String url : urls) {
			try {
				new URI(url);	// trigger exception
				new URL(url);	// trigger exception
				add.add(URI.create(url));
			} catch (URISyntaxException|MalformedURLException e) {
				throw new RuntimeException("Invalid URL.");
			}
		}
		addUris(add, size());
	}

	/**
	 * Adds new song to specified index of playlist.
	 * Dont use for physical files..
	 *
	 * @throws NullPointerException when param null.
	 */
	public void addUrls(String url, int at) {
		List<URI> to_add = Collections.singletonList(URI.create(url));
		addUris(to_add, at);
	}

	/**
	 * Adds specified song to end of this playlist.
	 *
	 * @throws NullPointerException when param null.
	 */
	public void addFile(File file) {
		addUri(file.toURI());
	}

	/**
	 * Adds specified files to end of this playlist.
	 *
	 * @throws NullPointerException when param null.
	 */
	public void addFiles(Collection<File> files) {
		addUris(map(files, File::toURI));
	}

	/**
	 * Adds specified song to end of this playlist.
	 *
	 * @throws NullPointerException when param null.
	 */
	public void addUri(URI uri) {
		addUris(Collections.singletonList(uri));
	}

	/**
	 * Adds songs at the end of this playlist and updates them if necessary.
	 * Adding produces immediate response for all songs. Updating will take time
	 * and take effect the moment it is applied one by one.
	 *
	 * @throws NullPointerException when param null.
	 */
	public void addUris(Collection<URI> uris) {
		addUris(uris, size());
	}

	/**
	 * Adds new songs at the end of this playlist and updates them if necessary.
	 * Adding produces immediate response for all songs. Updating will take time
	 * and take effect the moment it is applied one by one.
	 *
	 * @param at Index at which songs will be added. Out of bounds index will be converted: index < 0     --> 0(first)
	 * index >= size --> size (last song)
	 * @throws NullPointerException when param null.
	 */
	public void addUris(Collection<URI> uris, int at) {
		int _at = at;
		if (_at<0) _at = 0;
		if (_at>size()) _at = size();

		List<PlaylistSong> l = new ArrayList<>();
		uris.forEach(uri -> l.add(new PlaylistSong(uri)));

		addPlaylistSongs(l, _at);
	}

	public void addItem(Song song) {
		addItems(Collections.singletonList(song), size());
	}

	/**
	 * Maps songs to playlist songs and ads songs at the end.
	 * Equivalent to {@code addItems(songs, list().size());}
	 */
	public void addItems(Collection<? extends Song> songs) {
		addItems(songs, size());
	}

	/**
	 * Adds songs at the position.
	 * Equivalent to {@code addPlaylist(map(songs, Song::toPlaylist), at);}
	 */
	public void addItems(Collection<? extends Song> songs, int at) {
		addPlaylistSongs(map(songs, Song::toPlaylist), at);
	}

	/**
	 * Adds all songs to the specified position of this playlist.
	 *
	 * @param ps playlist songs
	 * @param at Index at which songs will be added. Out of bounds index will be converted: index < 0     --> 0(first)
	 * index >= size --> size (last song)
	 * @throws NullPointerException when param null.
	 */
	public void addPlaylistSongs(Collection<PlaylistSong> ps, int at) {
		int _at = at;
		if (_at<0) _at = 0;
		if (_at>size()) _at = size();

		addAll(_at, ps);
		updateItems(ps);
	}

	@Override
	public String toString() {
		var postfix = size()>10 ? ", ..." : "";
		return "Playlist id=" + id + " size=" + size() + " items=" + stream().limit(10).map(it -> it.toString()).collect(joining(", ")) + postfix;
	}

	// TODO: move methods to PlaylistManager
	/**
	 * Open chooser and add or play new songs.
	 *
	 * @param add true to add songs, false to clear playlist and play songs
	 */
	public void addOrEnqueueFiles(boolean add) {
		chooseFiles(
				"Choose Audio Files",
			APP.audio.getBrowse(),
			APP.windowManager.getFocused().map(WindowBase::getStage).orElse(APP.windowManager.createStageOwner()),
			audioExtensionFilter()
		).ifOkUse(files -> {
			APP.audio.setBrowse(files.get(0).getParentFile());
					List<URI> queue = new ArrayList<>();
					files.forEach(f -> queue.add(f.toURI()));

					if (add) addUris(queue);
					else {
						APP.audio.stop();
						clear();
						addUris(queue);
						playFirstItem();
					}
		});
	}

	/**
	 * Open chooser and add or play new songs.
	 *
	 * @param add true to add songs, false to clear playlist and play songs
	 */
	public void addOrEnqueueFolder(boolean add) {
		chooseFile(
						"Choose Audio Files From Directory Tree",
						DIRECTORY,
			APP.audio.getBrowse(),
			APP.windowManager.getFocused().map(WindowBase::getStage).orElse(APP.windowManager.createStageOwner())
		).ifOkUse(dir -> {
			APP.audio.setBrowse(dir);
					List<URI> queue = new ArrayList<>();
					getFilesR(dir, Integer.MAX_VALUE, f -> isAudio(f)).forEach(f -> queue.add(f.toURI()));

					if (add) addUris(queue);
					else {
						APP.audio.stop();
						clear();
						addUris(queue);
						playFirstItem();
					}
		});
	}

	/**
	 * Open chooser and add or play new songs.
	 *
	 * @param add true to add songs, false to clear playlist and play songs
	 */
	@SuppressWarnings({"Convert2Lambda"})
	public void addOrEnqueueUrl(boolean add) {
		String title = add ? "Add url song." : "Play url song.";
		Config<URI> conf = new ValueConfig<>(new VType<>(URI.class, false), "Url", URI.create("http://www.example.com"), title);
		Form<?> form = Form.Companion.form(
			conf,
			consumer(new Consumer<>() {
					@Override
					public void accept(Config<URI> url) {
						if (add) {
							addUri(url.getValue());
						} else {
							APP.audio.stop();
							clear();
							addUri(url.getValue());
							playFirstItem();
						}
					}
				}
			)
		);

		Icon infoB = infoIcon(
			"Use direct url to a file, for example\n"
				+ "a file on the web. The url is the\n"
				+ "address to the file and should end \n"
				+ "with file suffix like '.mp3'. Try\n"
				+ "visiting: "
		);

		PopWindow p = new PopWindow();
		p.getTitle().setValue(title);
		p.getContent().setValue(form);
		p.getHeaderIcons().add(infoB);
		p.show(WINDOW_ACTIVE.invoke(CENTER));
	}

}