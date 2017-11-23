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
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import sp.it.pl.audio.Item;
import sp.it.pl.audio.Player;
import sp.it.pl.audio.playback.PLAYBACK;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.popover.PopOver;
import sp.it.pl.gui.objects.popover.ScreenPos;
import sp.it.pl.gui.objects.window.stage.WindowBase;
import sp.it.pl.unused.SimpleConfigurator;
import sp.it.pl.util.collections.mapset.MapSet;
import sp.it.pl.util.conf.ValueConfig;
import sp.it.pl.util.file.AudioFileFormat;
import sp.it.pl.util.file.AudioFileFormat.Use;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.INFO;
import static java.util.stream.Collectors.toList;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.util.Duration.millis;
import static sp.it.pl.main.App.APP;
import static sp.it.pl.main.AppBuildersKt.helpPopOver;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.dev.Util.noØ;
import static sp.it.pl.util.file.FileType.DIRECTORY;
import static sp.it.pl.util.file.Util.getFilesAudio;
import static sp.it.pl.util.functional.Util.map;
import static sp.it.pl.util.functional.Util.toS;
import static sp.it.pl.util.system.EnvironmentKt.browse;
import static sp.it.pl.util.system.EnvironmentKt.chooseFile;
import static sp.it.pl.util.system.EnvironmentKt.chooseFiles;

public class Playlist extends SimpleListProperty<PlaylistItem> {

	public final UUID id;
	private final ReadOnlyIntegerWrapper playingIWrapper = new ReadOnlyIntegerWrapper(-1);
	public final ReadOnlyIntegerProperty playingI = playingIWrapper.getReadOnlyProperty();
	private PlaylistItem playing = null;

	public Playlist() {
		this(UUID.randomUUID());
	}

	public Playlist(UUID id) {
		super(observableArrayList());
		this.id = id;
	}

	public void updatePlayingItem(int i) {
		boolean exists = i>=0 && i<size();
		updatePlayingItem(exists ? i : -1, exists ? get(i) : null);
	}

	private void updatePlayingItem(int i, PlaylistItem item) {
		playing = item;
		playingIWrapper.set(i);
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	private UnaryOperator<List<PlaylistItem>> transformer = x -> x;

	private List<PlaylistItem> transform() {
		return transformer.apply(this);
	}

	public void setTransformation(ObservableList<PlaylistItem> transformed) {
		transformer = original -> transformed;
	}

	public void setTransformation(UnaryOperator<List<PlaylistItem>> transformer) {
		this.transformer = transformer;
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/** Returns total playlist duration - a sum of all playlist item lengths. */
	public Duration getLength() {
		double sum = stream()
				.map(PlaylistItem::getTime)
				.filter(d -> !d.isIndefinite() && !d.isUnknown())   // TODO: this should not be needed
				.mapToDouble(Duration::toMillis)
				.sum();
		return millis(sum);
	}

	/**
	 * Returns true if specified item is playing item on the playlist. There can
	 * only be one item in the application for which this method returns true.
	 * Note the distinction between same file of the items and two items being
	 * the very same item.
	 *
	 * @return true if item is played.
	 */
	public boolean isItemPlaying(Item item) {
		return playing==item;
	}

	/**
	 * Returns index of the first same item in playlist.
	 *
	 * @return item index. -1 if not in playlist.
	 * @throws java.lang.RuntimeException if any param null
	 * @see Item#same(sp.it.pl.audio.Item)
	 */
	public int indexOfSame(Item item) {
		List<PlaylistItem> transformed = transform();
		for (int i = 0; i<transformed.size(); i++)
			if (transformed.get(i).same(item)) return i;
		return -1;
	}

	/** @return index of playing item or -1 if no item is playing */
	public int indexOfPlaying() {
		return playing==null ? -1 : indexOf(playing);
	}

	public PlaylistItem getPlaying() {
		return playing;
	}

	/** @return true when playlist contains items same as the parameter */
	public boolean containsSame(Item item) {
		return stream().anyMatch(item::same);
	}

	/** Returns true iff any item on this playlist is being played played. */
	public boolean containsPlaying() {
		return indexOfPlaying()>=0;
	}

	/** Removes all unplayable items from this playlist. */
	public void removeUnplayable() {
		List<PlaylistItem> staying = new ArrayList<>();
		for (int i = 0; i<size(); i++) {
			PlaylistItem p = get(i);
			if (!p.isCorrupt(Use.PLAYBACK))
				staying.add(p);
		}
		if (staying.size()==size()) return;
		setAll(staying);
	}

	/**
	 * Removes all items such as no two items on the playlist are the same as
	 * in {@link Item#same(sp.it.pl.audio.Item)}.
	 */
	public void removeDuplicates() {
		MapSet<URI,Item> unique = new MapSet<>(Item::getUri);
		List<PlaylistItem> staying = new ArrayList<>();
		for (int i = 0; i<size(); i++) {
			PlaylistItem p = get(i);
			if (!unique.contains(p)) {
				unique.add(p);
				staying.add(p);
			}
		}
		if (staying.size()==size()) return;
		setAll(staying);
	}

	/**
	 * Duplicates the item if it is in playlist. If it is not, does nothing.
	 * Duplicate will appear on the next index following the original.
	 */
	public void duplicateItem(PlaylistItem item) {
		int i = indexOf(item);
		if (i!=-1) add(i + 1, item.copy());
	}

	/**
	 * Duplicates the items if they are in playlist. If they arent, does nothing.
	 * Duplicates will appear on the next index following the last items's index.
	 *
	 * @param items items to duplicate
	 */
	public void duplicateItemsAsGroup(List<PlaylistItem> items) {
		int index = 0;
		List<PlaylistItem> to_dup = new ArrayList<>();
		for (PlaylistItem item : items) {
			int i = items.indexOf(item);
			if (i>0) { // if contains
				to_dup.add(item.copy());   // item must be cloned
				index = i + 1;
			}
		}
		if (to_dup.isEmpty()) return;
		items.addAll(index, to_dup);
	}

	/**
	 * Duplicates the items if they are in playlist. If they arent, does nothing.
	 * Each duplicate will appear at index right after its original - sort of
	 * couples will appear.
	 *
	 * @param items items to duplicate
	 */
	public void duplicateItemsByOne(List<PlaylistItem> items) {
		items.forEach(this::duplicateItem);
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/** Reverses order of the items. Operation can not be undone. */
	public void reverse() {
		FXCollections.reverse(this);
	}

	/** Randomizes order of the items. Operation can not be undone. */
	public void randomize() {
		FXCollections.shuffle(this);
	}

	/**
	 * Moves/shifts all specified items by specified distance.
	 * Selected items retain their relative positions. Items stop moving when
	 * any of them hits end/start of the playlist. Items wont rotate the list.
	 *
	 * @apiNote If this method requires real time response (for example reacting on mouse drag in table), it is important
	 * to 'cache' the behavior and allow values >1 && <-1 so the moved items dont lag behind.
	 *
	 * @param indexes of items to move. Must be List<Integer>.
	 * @param by distance to move items by. Negative moves back. Zero does nothing.
	 * @return updated indexes of moved items.
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
				for (int i = 0; i<blocks.size(); i++) {
					newSelected.addAll(moveItemsByBlock(blocks.get(i), by));
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
				for (int i = 0; i<indexes.size(); i++) {
					int ii = indexes.get(i);
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
	 * Updates item.
	 * Updates all instances of the Item that are in the playlist. When application
	 * internally updates PlaylistItem it must make sure all its duplicates are
	 * updated too. Thats what this method does.
	 *
	 * @param item item to update
	 */
	public void updateItem(Item item) {
		stream().filter(item::same).forEach(PlaylistItem::update);
		// THIS NEEDS TO FIRE DURATION UPDATE
	}

	/**
	 * Updates all not updated items.
	 * <p/>
	 * If some item is not on playlist it will also be updated but it will have
	 * no effect on this playlist (but will on others, if they contain it).
	 *
	 * @param items items to update.
	 */
	public void updateItems(Collection<PlaylistItem> items) {
		if (items.isEmpty()) return;
		List<PlaylistItem> l = new ArrayList<>(items);
		Player.IO_THREAD.execute(() -> {
			for (PlaylistItem i : l) {
				if (Thread.interrupted()) return;
				if (!i.isUpdated()) i.update();
			}
		});
		// THIS NEEDS TO FIRE DURATION UPDATE
	}

	/**
	 * Use to completely refresh playlist.
	 * Updates all items on playlist. This method guarantees that all items
	 * will be up to date.
	 * After this method is invoked, every item will be updated at least once
	 * and reflect metadata written in the physical file.
	 * <p>
	 * Utilizes bgr thread. Its safe to call this method without any performance
	 * impact.
	 */
	public void updateItems() {
		updateItems(this);
	}

	/******************************************************************************/

	// this will stay private or there would be bugs due to using bad index
	// use transformed
	private void playItem(int index) {
		try {
			playItem(transform().get(index));
		} catch (IndexOutOfBoundsException ex) {
			PLAYBACK.stop();
		}
	}

	/**
	 * Plays given item. Does nothing if item not on playlist or null.
	 */
	public void playItem(PlaylistItem item) {
		playItem(item, p -> PlaylistManager.playingItemSelector.getNext(p, transform()));
	}

	volatile private PlaylistItem unplayable1st = null;

	/***
	 * Plays specified item or if not possible, uses the specified function to calculate the next item to play.
	 * <p/>
	 * This method is asynchronous.
	 *
	 * @param item item to play
	 * @param altSupplier supplier of next item to play if the item can not be
	 * played. It may for example return next item, or random item, depending
	 * on the selection strategy. If the next item to play again can not be
	 * played the process repeats until item to play is found or no item is
	 * playable.
	 */
	public void playItem(PlaylistItem item, UnaryOperator<PlaylistItem> altSupplier) {
		if (item!=null && transform().contains(item)) {
			Player.IO_THREAD.execute(() -> {
				boolean unplayable = item.isNotPlayable(); // potentially blocking
				// we cant play item, we try to play next one and eventually get
				// here again, we must defend against situation where no item
				// is playable - we remember 1st unplayable and keep checking
				// until we check it again (thus checking all items)
				if (unplayable) {
					if (unplayable1st==item) {
						// unplayable1st is not reliable indicator (since items can
						// be selected randomly), so if we check same item twice
						// check whole playlist
						boolean isNonePlayable = stream().allMatch(PlaylistItem::isNotPlayable); // potentially blocking
						if (isNonePlayable) return;    // stop the loop

						runFX(() -> {
							PLAYBACK.stop();            // stop playback
							unplayable1st = null;       // reset the loop
						});
					}

					runFX(() -> {
						// remember 1st unplayable
						if (unplayable1st==null) unplayable1st = item;
						// try to play next item, note we dont use the supplier as a fallback 2nd time
						// we use linear 'next time' supplier instead, to make sure we check every
						// item on a completely unplayable playlist and exactly once. Say provided
						// one selects random item - we could get into potentially infinite loop or
						// check items multiple times or even skip playable items to check completely!
						// playItem(alt_supplier.apply(item),alt_supplier);
						playItem(altSupplier.apply(item));
					});
				} else {
					runFX(() -> {
						unplayable1st = null;
						PlaylistManager.active = this.id;
						PlaylistManager.playlists.stream().filter(p -> p!=this).forEach(p -> p.updatePlayingItem(-1));
						updatePlayingItem(indexOf(item), item);
						PLAYBACK.play(item);
					});
				}
			});
		}
	}

	/** Plays first item on playlist. */
	public void playFirstItem() {
		playItem(0);
	}

	/** Plays last item on playlist. */
	public void playLastItem() {
		playItem(transform().size() - 1);
	}

	/** Plays next item on playlist according to its selector logic. */
	public void playNextItem() {
		playItem(PlaylistManager.playingItemSelector.getNext(getPlaying(), transform()), p -> PlaylistManager.playingItemSelector.getNext(p, transform()));
	}

	/** Plays previous item on playlist according to its selector logic. */
	public void playPreviousItem() {
		playItem(PlaylistManager.playingItemSelector.getPrevious(getPlaying(), transform()), p -> PlaylistManager.playingItemSelector.getPrevious(p, transform()));
	}

	/**
	 * Plays new playlist.
	 * Clears active playlist completely and adds all items from new playlist.
	 * Starts playing first file.
	 *
	 * @param items items to be onthe  playlist.
	 * @throws NullPointerException if param null.
	 */
	public void setNplay(Collection<? extends Item> items) {
		setNplay(items.stream());
	}

	/**
	 * Plays new playlist.
	 * Clears active playlist completely and adds all items from new playlist.
	 * Starts playing item with the given index. If index is out of range for new
	 * playlist, handles according to behavior in playItem(index int) method.
	 *
	 * @param items items.
	 * @param from index of item to play from
	 * @throws NullPointerException if param null.
	 */
	public void setNplayFrom(Collection<? extends Item> items, int from) {
		setNplayFrom(items.stream(), from);
	}

	/**
	 * Plays new playlist.
	 * Clears active playlist completely and adds all items from new playlist.
	 * Starts playing first file.
	 *
	 * @param items items.
	 * @throws NullPointerException if param null.
	 */
	public void setNplay(Stream<? extends Item> items) {
		noØ(items);
		clear();
		addItems(items.collect(toList()));
		playFirstItem();
	}

	/**
	 * Plays new playlist.
	 * Clears active playlist completely and adds all items from new playlist.
	 * Starts playing item with the given index. If index is out of range for new
	 * playlist, handles according to behavior in playItem(index int) method.
	 *
	 * @param items items to add.
	 * @param from index of item to play from
	 * @throws NullPointerException if param null.
	 */
	public void setNplayFrom(Stream<? extends Item> items, int from) {
		noØ(items);
		clear();
		addItems(items.collect(toList()));
		playItem(get(from));
	}

	public PlaylistItem getNextPlaying() {
		return PlaylistManager.playingItemSelector.getNext(getPlaying(), transform());
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/**
	 * Adds new item to end of this playlist, based on the url (String). Use this method
	 * for  URL based Items.
	 *
	 * @throws NullPointerException when param null.
	 */
	public void addUrl(String url) {
		addUrls(Collections.singletonList(url));
	}

	/**
	 * Adds new items to end of this playlist, based on the urls (String). Use
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
	 * Adds new item to specified index of playlist.
	 * Dont use for physical files..
	 *
	 * @throws NullPointerException when param null.
	 */
	public void addUrls(String url, int at) {
		List<URI> to_add = Collections.singletonList(URI.create(url));
		addUris(to_add, at);
	}

	/**
	 * Adds specified item to end of this playlist.
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
	 * Adds specified item to end of this playlist.
	 *
	 * @throws NullPointerException when param null.
	 */
	public void addUri(URI uri) {
		addUris(Collections.singletonList(uri));
	}

	/**
	 * Adds items at the end of this playlist and updates them if necessary.
	 * Adding produces immediate response for all items. Updating will take time
	 * and take effect the moment it is applied one by one.
	 *
	 * @throws NullPointerException when param null.
	 */
	public void addUris(Collection<URI> uris) {
		addUris(uris, size());
	}

	/**
	 * Adds new items at the end of this playlist and updates them if necessary.
	 * Adding produces immediate response for all items. Updating will take time
	 * and take effect the moment it is applied one by one.
	 *
	 * @param at Index at which items will be added. Out of bounds index will be converted: index < 0     --> 0(first)
	 * index >= size --> size (last item)
	 * @throws NullPointerException when param null.
	 */
	public void addUris(Collection<URI> uris, int at) {
		int _at = at;
		if (_at<0) _at = 0;
		if (_at>size()) _at = size();

		List<PlaylistItem> l = new ArrayList<>();
		uris.forEach(uri -> l.add(new PlaylistItem(uri)));

		addPlaylist(l, _at);
	}

	public void addItem(Item item) {
		addItems(Collections.singletonList(item), size());
	}

	/**
	 * Maps items to playlist items and ads items at the end.
	 * Equivalent to {@code addItems(items, list().size());}
	 */
	public void addItems(Collection<? extends Item> items) {
		addItems(items, size());
	}

	/**
	 * Adds items at the position.
	 * Equivalent to {@code addPlaylist(map(items, Item::toPlaylist), at);}
	 */
	public void addItems(Collection<? extends Item> items, int at) {
		addPlaylist(map(items, Item::toPlaylist), at);
	}

	/**
	 * Adds all items to the specified position of this playlist.
	 *
	 * @param ps playlist items
	 * @param at Index at which items will be added. Out of bounds index will be converted: index < 0     --> 0(first)
	 * index >= size --> size (last item)
	 * @throws NullPointerException when param null.
	 */
	public void addPlaylist(Collection<PlaylistItem> ps, int at) {
		int _at = at;
		if (_at<0) _at = 0;
		if (_at>size()) _at = size();

		addAll(_at, ps);
		updateItems(ps);
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/**
	 * Open chooser and add or play new items.
	 *
	 * @param add true to add items, false to clear playlist and play items
	 */
	public void addOrEnqueueFiles(boolean add) {
		chooseFiles(
				"Choose Audio Files",
				PlaylistManager.browse,
				APP.windowManager.getFocused().map(WindowBase::getStage).orElse(APP.windowManager.createStageOwner()),
				AudioFileFormat.filter(Use.PLAYBACK)
		)
				.ifOk(files -> {
					PlaylistManager.browse = files.get(0).getParentFile();
					List<URI> queue = new ArrayList<>();
					files.forEach(f -> queue.add(f.toURI()));

					if (add) addUris(queue);
					else {
						PLAYBACK.stop();
						clear();
						addUris(queue);
						playFirstItem();
					}
				});
	}

	/**
	 * Open chooser and add or play new items.
	 *
	 * @param add true to add items, false to clear playlist and play items
	 */
	public void addOrEnqueueFolder(boolean add) {
		chooseFile(
						"Choose Audio Files From Directory Tree",
						DIRECTORY,
						PlaylistManager.browse,
						APP.windowManager.getFocused().map(WindowBase::getStage).orElse(APP.windowManager.createStageOwner())
				)
				.ifOk(dir -> {
					PlaylistManager.browse = dir;
					List<URI> queue = new ArrayList<>();
					getFilesAudio(dir, Use.APP, PlaylistManager.folder_depth).forEach(f -> queue.add(f.toURI()));

					if (add) addUris(queue);
					else {
						PLAYBACK.stop();
						clear();
						addUris(queue);
						playFirstItem();
					}
				});
	}

	/**
	 * Open chooser and add or play new items.
	 *
	 * @param add true to add items, false to clear playlist and play items
	 */
	public void addOrEnqueueUrl(boolean add) {
		// build content
		String title = add ? "Add url item." : "Play url item.";
		SimpleConfigurator content = new SimpleConfigurator<>(
				new ValueConfig<>(URI.class, "Url", URI.create("http://www.example.com"), title),
				(URI url) -> {
					if (add) {
						addUri(url);
					} else {
						PLAYBACK.stop();
						clear();
						addUri(url);
						playFirstItem();
					}
				});

		// build help content
		String uri = "http://www.musicaddict.com";
		Text t1 = new Text("Use direct url to a file, for example\n"
				+ "a file on the web. The url is the\n"
				+ "address to the file and should end \n"
				+ "with file suffix like '.mp3'. Try\n"
				+ "visiting: ");
		Text t2 = new Text(uri);
		// turn to hyperlink by assigning proper styleclass
		t2.getStyleClass().add("hyperlink");
		VBox cnt = new VBox(t1, t2);
		cnt.setSpacing(8);
		VBox.setMargin(t2, new Insets(0, 0, 0, 20));
		Icon infoB = new Icon(INFO, 11, "Help");
		infoB.setOnMouseClicked(e -> {
			PopOver helpP = helpPopOver("");
			helpP.setContentNode(cnt);	// TODO: fix this
			// open the uri in browser
			helpP.getContentNode().setOnMouseClicked(pe -> {
				browse(URI.create(uri));
				pe.consume();
			});
			helpP.show(infoB);
		});
		// build popup
		PopOver<?> p = new PopOver<>(title, content);
		p.getHeaderIcons().add(infoB);
		p.show(ScreenPos.APP_CENTER);
		p.detached.set(true);
	}

	@Override
	public String toString() {
		return "Playlist: " + id + " " + toS(this);
	}

}