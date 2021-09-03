package sp.it.pl.ui.objects.table;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import sp.it.pl.audio.Song;
import sp.it.pl.audio.playlist.Playlist;
import sp.it.pl.audio.playlist.PlaylistManager;
import sp.it.pl.audio.playlist.PlaylistManagerKt;
import sp.it.pl.audio.playlist.PlaylistSong;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.audio.tagging.PlaylistSongGroup;
import sp.it.pl.ui.objects.contextmenu.ValueContextMenu;
import sp.it.pl.ui.objects.tablerow.SpitTableRow;
import sp.it.util.Sort;
import sp.it.util.access.V;
import sp.it.util.access.fieldvalue.ColumnField;
import sp.it.util.reactive.Disposer;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static java.util.Comparator.nullsLast;
import static java.util.stream.Collectors.toList;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.TransferMode.ANY;
import static sp.it.pl.audio.playlist.PlaylistReaderKt.isM3uPlaylist;
import static sp.it.pl.audio.playlist.PlaylistReaderKt.readM3uPlaylist;
import static sp.it.pl.audio.playlist.PlaylistSong.Field.LENGTH;
import static sp.it.pl.audio.playlist.PlaylistSong.Field.NAME;
import static sp.it.pl.audio.playlist.PlaylistSong.Field.TITLE;
import static sp.it.pl.main.AppDragKt.getAudio;
import static sp.it.pl.main.AppDragKt.hasAudio;
import static sp.it.pl.main.AppDragKt.installDrag;
import static sp.it.pl.main.AppDragKt.setSongsAndFiles;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.ui.objects.table.TableUtilKt.buildFieldedCell;
import static sp.it.pl.ui.objects.table.TableUtilKt.getFontOrNull;
import static sp.it.util.async.AsyncKt.FX;
import static sp.it.util.async.AsyncKt.runNew;
import static sp.it.util.functional.Util.SAME;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.Util.listRO;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.reactive.UtilKt.attach;
import static sp.it.util.reactive.UtilKt.syncC;
import static sp.it.util.ui.ContextMenuExtensionsKt.show;
import static sp.it.util.ui.Util.computeTextWidth;
import static sp.it.util.ui.Util.selectRows;
import static sp.it.util.ui.UtilKt.menuItem;
import static sp.it.util.ui.UtilKt.pseudoclass;
import static sp.it.util.units.DurationKt.toHMSMs;

/**
 * Playlist table GUI component.
 * <p/>
 * Introduces two additional TableCell css pseudoclasses: {@link #STYLE_PLAYED} and {@link #STYLE_CORRUPT} for cells
 * containing played item and corrupted item respectively.
 * <p/>
 * Always call {@link #dispose()}
 */
public class PlaylistTable extends FilteredTable<PlaylistSong> {

	public static final Insets CELL_PADDING = new Insets(0.0, 8.0, 0.0, 8.0);
	public static final double CELL_PADDING_WIDTH = CELL_PADDING.getLeft() + CELL_PADDING.getRight();
	public static final PseudoClass STYLE_CORRUPT = pseudoclass("corrupt");
	public static final PseudoClass STYLE_PLAYED = pseudoclass("played");
	private static final ValueContextMenu<PlaylistSongGroup> contextMenu = new ValueContextMenu<>();

	public final @NotNull V<@NotNull Boolean> scrollToPlaying = new V<>(true);
	private double selectionLastScreenY;
	private final Disposer disposer = new Disposer();

	@SuppressWarnings("unchecked")
	public PlaylistTable(Playlist playlist) {
		super(PlaylistSong.class, NAME, playlist);

		playlist.setTransformationToItemsOf(getItems());

		VBox.setVgrow(this, Priority.ALWAYS);

		// initialize table
		getSelectionModel().setSelectionMode(MULTIPLE);

		// initialize column factories
		setColumnFactory(f -> {
			TableColumn<PlaylistSong,Object> c = new TableColumn<>(f.toString());

			if (f==(Object) NAME) c.setCellValueFactory(cf -> (ObservableValue<Object>) (Object) cf.getValue().getNameP());
			else if (f==(Object) LENGTH) c.setCellValueFactory(cf -> (ObservableValue<Object>) (Object) cf.getValue().getTimeP());
			else c.setCellValueFactory(cf -> cf.getValue()==null ? null : new PojoV<>(f.getOf(cf.getValue())));

			c.setCellFactory(column -> buildFieldedCell(f));
			c.setResizable(true);
			return c;
		});
		setColumnState(getDefaultColumnInfo());

		// initialize row factories
		setRowFactory(t -> {
			var row = new SpitTableRow<PlaylistSong>();
			// remember position for moving selected rows on mouse drag
			row.setOnMousePressed(e -> selectionLastScreenY = e.getScreenY());
			// clear table selection on mouse released if no item
			row.setOnMouseReleased(e -> {
				if (row.getItem()==null)
					selectNone();
			});
			// left double click -> play
			row.onLeftDoubleClick((r, e) -> getPlaylist().playItem(r.getItem()));
			// right click -> show context menu
			row.onRightSingleClick((r, e) -> {
				// prep selection for context menu
				if (!row.isSelected())
					getSelectionModel().clearAndSelect(row.getIndex());

				var table = PlaylistTable.this;
				contextMenu.setItemsFor(new PlaylistSongGroup(playlist, table.getSelectedItemsCopy()));
				show(contextMenu, table, e);
			});
			// handle drag transfer
			row.setOnDragDropped(e -> dropDrag(e, row.isEmpty() ? getItems().size() : row.getIndex()));

			// additional css style classes
			row.styleRuleAdd(STYLE_PLAYED, p -> getPlaylist().isPlaying(p));
			row.styleRuleAdd(STYLE_CORRUPT, PlaylistSong::isCorruptCached);
			return row;
		});
		disposer.plusAssign(syncC(getPlaylist().playingSong, s -> updateStyleRules()));

		// resizing
		setColumnResizePolicySafe(resize -> {
			if (resize==null) return true;

			// handle column resize (except index)
			if (resize.getColumn()!=null && resize.getColumn()!=columnIndex) {
				getColumn(NAME).ifPresent(it -> it.setPrefWidth(it.getWidth() - resize.getDelta()));
				resize.getColumn().setPrefWidth(resize.getColumn().getWidth() + resize.getDelta());

				// do not return - after resizing the resized column, we go resize
				// the rest to always fill the table width
				// true means the delta is reset and won't accumulate
				// return true;
			}

			// handle table resize or index column
			var tw = resize.getTable().getWidth();
			var sw = getVScrollbarWidth();
			var gap = 3;               // prevents horizontal slider from appearing

			getColumn(ColumnField.INDEX).ifPresent(c -> c.setPrefWidth(computeIndexColumnWidth() + CELL_PADDING_WIDTH));
			getColumn(LENGTH).ifPresent(c -> {
				var maxLength = getItems().stream().mapToDouble(PlaylistSong::getTimeMs).max().orElse(6000);
				var maxLengthText = toHMSMs(new Duration(maxLength));
				var font = getFontOrNull(c);
				var width = font == null ? 100.0 : computeTextWidth(font, maxLengthText) + 7 + CELL_PADDING_WIDTH;
				c.setPrefWidth(width);
			});

			TableColumn<?,?> mc = isColumnVisible(NAME) ? getColumn(NAME).orElse(null) : getColumn(TITLE).orElse(null);
			if (mc!=null) {
				double sumW = resize.getTable().getColumns().stream()
						.filter(c -> c!=mc)
						.mapToDouble(c -> c.getWidth() + CELL_PADDING_WIDTH)
						.sum();
				mc.setPrefWidth(tw - sumW - sw - gap);
			}
			return true; // false/true, does not matter
		});

		// empty table left click -> add items
		addEventHandler(MOUSE_CLICKED, e -> {
			if (headerVisible.get() && e.getY()<getVisibleHeaderHeight()) return;
			if (e.getButton()==PRIMARY && e.getClickCount()==1 && getItemsRaw().isEmpty())
				PlaylistManagerKt.addOrEnqueueFiles(getPlaylist(), true);
		});

		// move items on drag
		addEventFilter(MOUSE_DRAGGED, e -> {
			if (e.getButton()!=MouseButton.PRIMARY || !e.isControlDown()) return;
			// we can't move items when filter on & we can't cancel filter, user would freak out
			//  if (itemsPredicate.get()!=null) return; // unreliable as non-null predicates may have no effect
			if (getItems().size()!=getItemsRaw().size()) return;

			// transform any sort (if in effect) to actual table items, we can't change order on
			// items out of natural order
			// note this is only called the 1st time (or not at all), not repeatedly
			if (itemsComparator.get()!=SAME || !getSortOrder().isEmpty()) {
				movingItems = true;
				List<PlaylistSong> l = list(getItems());
				List<Integer> sl = list(getSelectionModel().getSelectedIndices());
				setItemsRaw(listRO());      // clear items
				getSortOrder().clear();     // clear sort order
				setItemsRaw(l);             // set items back, now any sort is part of their order
				selectRows(sl, getSelectionModel()); // set selection back
				movingItems = false;
			}

			double h = getFixedCellSize();
			double dist = e.getScreenY() - selectionLastScreenY;

			int by = (int) (dist/h);
			if (by!=0) {
				selectionLastScreenY = e.getScreenY();
				moveSelectedItems(by);
			}
		});

		// set key-induced actions
		addEventHandler(KEY_PRESSED, e -> {
			if (e.isConsumed()) return;
			if (e.isControlDown() && !e.isShiftDown() && !e.isAltDown() && !e.isMetaDown()) {
				if (e.getCode()==KeyCode.UP) {
                    // table.getFocusModel().focus(-1);
					moveSelectedItems(-1);
				} else if (e.getCode()==KeyCode.DOWN) {
                    // table.getFocusModel().focus(-1);
					moveSelectedItems(1);
				}
			}
			// play selected
			if (e.getCode()==KeyCode.ENTER) {
				if (!getSelectedItems().isEmpty())
					getPlaylist().playItem(getSelectedItems().get(0));
			}
			// delete selected
			if (e.getCode()==KeyCode.DELETE) {
				List<PlaylistSong> p = getSelectedItemsCopy();
				getSelectionModel().clearSelection();
				getPlaylist().removeAll(p);
			}
		});

		// drag&drop from
		setOnDragDetected(e -> {
			if (e.getButton()==PRIMARY && !e.isControlDown() && !getSelectedItems().isEmpty() && isRowFull(getRowS(e.getSceneX(), e.getSceneY()))) {
				Dragboard db = startDragAndDrop(ANY);
				setSongsAndFiles(db, getSelectedItemsCopy());
			}
			e.consume();
		});

		// drag&drop
		// Note: Empty table has no rows => drag for empty table is handled here
		installDrag(
			this, PLAYLIST_PLUS, "Add to playlist",
			e -> hasAudio(e.getDragboard()),
			e -> e.getGestureSource()==this,// || !getItemsRaw().isEmpty(),
			consumer(e -> dropDrag(e, getItemsRaw().size()))
		);
		installDrag(
			this, PLAYLIST_PLUS, "Add to playlist",
			e -> e.getDragboard().hasFiles() && e.getDragboard().getFiles().stream().anyMatch(it -> isM3uPlaylist(it)),
//			e -> !getItemsRaw().isEmpty(),
			consumer(e -> dropDrag(e, getItemsRaw().size()))
		);

		// scroll to playing item
		disposer.plusAssign(syncC(scrollToPlaying, v -> {
			if (v)
				scrollToCenter(playlist.getPlaying());
		}));
		disposer.plusAssign(attach(playlist.playingSong, s -> {
			if (scrollToPlaying.getValue())
				scrollToCenter(playlist.getPlaying());
			return Unit.INSTANCE;
		}));

		// add sorting by db columns
		menuOrder.getItems().add(
			new Menu("Order by column (db)", null,
				Metadata.Field.Companion.getAll().stream()
					.filter(f -> f.isTypeStringRepresentable())
					.sorted(by(f -> f.name()))
					.map(f ->
						new Menu(f.name(), null,
							menuItem("Desc", null, consumer(it -> sortByDb(f, Sort.DESCENDING))),
							menuItem("Asc", null, consumer(it -> sortByDb(f, Sort.ASCENDING)))
						)
					)
					.toArray(MenuItem[]::new)
			)
		);

		// reflect selection for whole application
		getSelectionModel().selectedItemProperty().addListener(selItemListener);
		getSelectionModel().getSelectedItems().addListener(selItemsListener);

		placeholderNode.setValue(new Label("Click or drag & drop audio"));
	}

	/** Clears resources. Do not use this table after calling this method. */
	public void dispose() {
		disposer.invoke();
		getSelectionModel().selectedItemProperty().removeListener(selItemListener);
		getSelectionModel().getSelectedItems().removeListener(selItemsListener);
	}

	public Playlist getPlaylist() {
		return (Playlist) getItemsRaw();
	}

/* --------------------- SELECTION ---------------------------------------------------------------------------------- */

	public boolean movingItems = false;
	ChangeListener<PlaylistSong> selItemListener = (o, ov, nv) -> {
		if (movingItems) return;
		PlaylistManager.selectedItemES.setValue(nv);
	};
	ListChangeListener<PlaylistSong> selItemsListener = (ListChangeListener.Change<? extends PlaylistSong> c) -> {
		if (movingItems) return;
		PlaylistManager.selectedItemsES.setValue(getSelectedItemsCopy());
	};

	/**
	 * Moves/shifts all selected items by specified distance.
	 * Selected items retain their relative positions. Items stop moving when
	 * any of them hits end/start of the playlist - items will not rotate in the playlist.
	 * <br/>
	 * Selection does not change.
	 *
	 * @param by distance to move items by. Negative moves back. Zero does nothing.
	 */
	public void moveSelectedItems(int by) {
		movingItems = true;    // lock to avoid firing selectedChange event (important)

		// get selected
		// construct new list (oldS), must not be observable (like indices)
		List<Integer> oldS = new ArrayList<>(getSelectionModel().getSelectedIndices());
		// move in playlist
		List<Integer> newS = getPlaylist().moveItemsBy(oldS, by);
		// select back
		selectRows(newS, getSelectionModel());

		movingItems = false;    // release lock
	}

/* --------------------- DRAG AND DROP ------------------------------------------------------------------------------ */

	private void dropDrag(DragEvent e, int index) {
		if (hasAudio(e.getDragboard())) {
			List<Song> songs = getAudio(e.getDragboard());
			getPlaylist().addItems(songs, index);

			e.setDropCompleted(true);
			e.consume();
		}
		// TODO: move to Drag utils
		if (e.getDragboard().hasFiles() && e.getDragboard().getFiles().stream().anyMatch(it -> isM3uPlaylist(it))) {
			List<File> files = e.getDragboard().getFiles();
			runNew(() ->
				files.stream()
					.filter(it -> isM3uPlaylist(it))
					.flatMap(it -> readM3uPlaylist(it).stream())
					.collect(toList())
			).useBy(FX, items ->
				getPlaylist().addItems(items, index)
			);

			e.setDropCompleted(true);
			e.consume();
		}
	}

/* --------------------- ORDER -------------------------------------------------------------------------------------- */

	/**
	 * Sorts songs by the specified field and order.
	 * Songs are looked up by {@link sp.it.pl.plugin.impl.SongDb#getSong(sp.it.pl.audio.Song)}.
	 * If the song is not found, null value is used. Nulls follow {@link java.util.Comparator#nullsLast(java.util.Comparator)}
	 */
	@SuppressWarnings("unchecked")
	public void sortByDb(Metadata.Field<?> field, Sort order) {
		if (field.isTypeStringRepresentable())
			sort(
				by(
					song -> {
						var songMetadata = APP.db.getSong(song);
						return songMetadata==null ? null : (Comparable<Object>) field.getOf(songMetadata);
					},
					cc -> nullsLast(order.of(cc))
				)
			);
	}
}