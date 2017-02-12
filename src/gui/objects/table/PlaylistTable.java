package gui.objects.table;

import audio.Item;
import audio.Player;
import audio.playlist.Playlist;
import audio.playlist.PlaylistItem;
import audio.playlist.PlaylistManager;
import audio.tagging.Metadata;
import gui.Gui;
import gui.objects.contextmenu.ImprovedContextMenu;
import gui.objects.contextmenu.TableContextMenuR;
import gui.objects.tablerow.ImprovedTableRow;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import layout.widget.WidgetFactory;
import layout.widget.feature.SongReader;
import layout.widget.feature.SongWriter;
import main.App;
import org.reactfx.Subscription;
import services.database.Db;
import util.access.V;
import util.dev.TODO;
import util.file.Environment;
import util.graphics.drag.DragUtil;
import util.parsing.Parser;
import util.units.FormattedDuration;
import web.SearchUriBuilder;
import static audio.playlist.PlaylistItem.Field.*;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.*;
import static layout.widget.WidgetManager.WidgetSource.NO_LAYOUT;
import static main.App.APP;
import static util.dev.TODO.Purpose.READABILITY;
import static util.functional.Util.*;
import static util.graphics.Util.*;
import static util.graphics.drag.DragUtil.installDrag;
import static util.reactive.Util.maintain;

/**
 * Playlist table GUI component.
 * <p/>
 * Introduces two additional TableCell css pseudoclasses: {@link #STYLE_PLAYED} and {@link #STYLE_CORRUPT} for cells
 * containing played item and corrupted item respectively.
 * <p/>
 * Always call {@link #dispose()}
 *
 * @author Martin Polakovic
 */
@TODO(purpose = READABILITY, note = "dragging duplicate code for empty table case")
public class PlaylistTable extends FilteredTable<PlaylistItem> {

	private static final String STYLE_CORRUPT = "corrupt";
	private static final String STYLE_PLAYED = "played";

	public final V<Boolean> scrollToPlaying = new V<>(true);
	private final TableColumn<PlaylistItem,String> columnName;
	private final TableColumn<PlaylistItem,FormattedDuration> columnTime;

	// selection helper variables
	double last;
	ArrayList<Integer> selected_temp = new ArrayList<>();

	// dependencies
	private final Subscription d1;

	public PlaylistTable(Playlist playlist) {
		super(PlaylistItem.class, NAME, playlist);

		playlist.setTransformation(getItems());

		VBox.setVgrow(this, Priority.ALWAYS);

		// initialize table
		setFixedCellSize(Gui.font.getValue().getSize() + 5);
		getSelectionModel().setSelectionMode(MULTIPLE);

		// initialize column factories
		setColumnFactory(f -> {
			if (f instanceof PlaylistItem.Field) {
				TableColumn<PlaylistItem,Object> c = new TableColumn<>(f.toString());
				c.setCellValueFactory(f==NAME || f==LENGTH
					? new PropertyValueFactory<>(f.name().toLowerCase())
					: cf -> cf.getValue()==null ? null : new PojoV<>(f.getOf(cf.getValue()))
				);
				c.setCellFactory(column -> buildDefaultCell(f));
				c.setResizable(true);
				return c;
			} else {
				TableColumn<PlaylistItem,?> c = new TableColumn<>(f.toString());
				c.setCellValueFactory(cf -> cf.getValue()==null ? null : new PojoV(f.getOf(cf.getValue())));
				c.setCellFactory(column -> buildDefaultCell(f));
				return c;
			}
		});
		setColumnState(getDefaultColumnInfo());
		columnName = (TableColumn<PlaylistItem,String>) getColumn(NAME).get();
		columnTime = (TableColumn<PlaylistItem,FormattedDuration>) getColumn(LENGTH).get();

		// initialize row factories
		setRowFactory(t -> new ImprovedTableRow<>() {
			{
				// remember position for moving selected rows on mouse drag
				setOnMousePressed(e -> last = e.getScreenY());
				// clear table selection on mouse released if no item
				setOnMouseReleased(e -> {
					if (getItem()==null)
						selectNone();
				});
				// left double ckick -> play
				onLeftDoubleClick((r, e) -> getPlaylist().playItem(r.getItem()));
				// right click -> show context menu
				onRightSingleClick((r, e) -> {
					// prep selection for context menu
					if (!isSelected())
						getSelectionModel().clearAndSelect(getIndex());
					// show context menu
					contextMenu.show(PlaylistTable.this, e);
				});
				// handle drag transfer
				setOnDragDropped(e -> dropDrag(e, isEmpty() ? getItems().size() : getIndex()));

				// additional css style classes
				styleRuleAdd(STYLE_PLAYED, p -> p==getPlaylist().getPlaying());
				styleRuleAdd(STYLE_CORRUPT, PlaylistItem::isCorruptCached);
			}
		});
		// maintain playing item css by refreshing index column
		d1 = Player.playingItem.onChange(o -> refreshColumn(columnIndex));

		// resizing
		setColumnResizePolicySafe(resize -> {
			if (resize==null) return true;

			// handle column resize (except index)
			if (resize.getColumn()!=null && resize.getColumn()!=columnIndex) {
				if (getColumns().contains(columnName))
					columnName.setPrefWidth(columnName.getWidth() - resize.getDelta());
				resize.getColumn().setPrefWidth(resize.getColumn().getWidth() + resize.getDelta());

				// do not return - after resizing the resized column, we go resize
				// the rest to always fill the table width
				// true means the delta is reset and wont accumulate
				// return true;
			}

			// handle table resize or index column

			double tw = resize.getTable().getWidth();
			double sw = getVScrollbarWidth();
			double gap = 3;               // prevents horizontal slider from appearing

			// column index
			double W1 = calculateIndexColumnWidth();

			// column time
			double mt = getItems().stream().mapToDouble(PlaylistItem::getTimeMs).max().orElse(6000);
			double W3 = computeFontWidth(Gui.font.getValue(), new FormattedDuration(mt).toString()) + 5;

			columnIndex.setPrefWidth(W1);
			columnTime.setPrefWidth(W3);

			List<TableColumn<PlaylistItem,?>> cs = new ArrayList<>(resize.getTable().getColumns());
			TableColumn<PlaylistItem,?> mc = isColumnVisible(NAME) ? columnName : getColumn(TITLE).orElse(null);
			if (mc!=null) {
				cs.remove(mc);
				double Σcw = cs.stream().mapToDouble(TableColumnBase::getWidth).sum();
				mc.setPrefWidth(tw - Σcw - sw - gap);
			}
			return true; // false/true, does not matter
		});

		// prevent selection change on right click
		addEventFilter(MOUSE_PRESSED, consumeOnSecondaryButton);

		// empty table left click -> add items
		addEventHandler(MOUSE_CLICKED, e -> {
			if (headerVisible.get() && e.getY()<getTableHeaderHeight()) return;
			if (e.getButton()==PRIMARY && e.getClickCount()==1 && getItems().isEmpty())
				getPlaylist().addOrEnqueueFiles(true);
		});

		// move items on drag
		// setOnMouseDragged(e -> { // handler !work since java 9
		// addEventFilter(MOUSE_DRAGGED, e -> { // same here
		addEventFilter(MOUSE_DRAGGED, e -> {
			if (e.getButton()!=MouseButton.PRIMARY || !e.isControlDown()) return;
			// we cant move items when filter on & we cant cancel filter, user would freak out
			//  if (itemsPredicate.get()!=null) return; // unreliable as non null predicates may have no effect
			if (getItems().size()!=getItemsRaw().size()) return;

			// transform any sort (if in effect) to actual table items, we cant change order on
			// items out of natural order
			// note this is only called the 1st time (or not at all), not repeatedly
			if (itemsComparator.get()!=SAME || !getSortOrder().isEmpty()) {
				movingItems = true;
				List<PlaylistItem> l = list(getItems());
				List<Integer> sl = list(getSelectionModel().getSelectedIndices());
				setItemsRaw(listRO());      // clear items
				getSortOrder().clear();     // clear sort order
				setItemsRaw(l);             // set items back, now any sort is part of their order
				selectRows(sl, getSelectionModel()); // set selection back
				movingItems = false;
			}

			double h = getFixedCellSize();
			double dist = e.getScreenY() - last;

			int by = (int) (dist/h);
			if (by>=1 || by<=-1) {
				last = e.getScreenY();
				moveSelectedItems(by);
			}
		});

		// set key-induced actions
		setOnKeyReleased(e -> {
			if (e.getCode()==KeyCode.ENTER) {     // play first of the selected
				if (!getSelectedItems().isEmpty())
					getPlaylist().playItem(getSelectedItems().get(0));
			} else if (e.getCode()==KeyCode.DELETE) {    // delete selected
				List<PlaylistItem> p = getSelectedItemsCopy();
				getSelectionModel().clearSelection();
				getPlaylist().removeAll(p);
			} else if (e.getCode()==KeyCode.ESCAPE) {    // deselect
				getSelectionModel().clearSelection();
			}
		});
		setOnKeyPressed(e -> {
			if (e.isControlDown()) {
				if (e.getCode()==KeyCode.UP) {
//                    table.getFocusModel().focus(-1);
					moveSelectedItems(-1);
				} else if (e.getCode()==KeyCode.DOWN) {
//                    table.getFocusModel().focus(-1);
					moveSelectedItems(1);
				}
			}
		});

		// drag&drop from
		setOnDragDetected(e -> {
			if (e.getButton()==PRIMARY && !e.isControlDown() && !getSelectedItems().isEmpty()
				&& isRowFull(getRowS(e.getSceneX(), e.getSceneY()))) {

				Dragboard db = startDragAndDrop(TransferMode.COPY);
				DragUtil.setItemList(getSelectedItemsCopy(), db, true);
			}
			e.consume();
		});

		// drag&drop to
		// handle drag (empty table has no rows so row drag event handlers
		// will not work, events fall through on table and we handle it here
		installDrag(
			this, PLAYLIST_PLUS, "Add to playlist after row",
			DragUtil::hasAudio,
			e -> e.getGestureSource()==this,
			e -> dropDrag(e, 0)
		);

		// scroll to playing item
		maintain(scrollToPlaying, v -> {
			if (v) {
				scrollToCenter(getItems().indexOf(playlist.getPlaying()));
			}
		});
		playlist.playingI.addListener((o, ov, nv) -> {
			if (scrollToPlaying.getValue())
				scrollToCenter(getItems().indexOf(playlist.getPlaying()));
		});

		// reflect selection for whole application
		getSelectionModel().selectedItemProperty().addListener(selItemListener);
		getSelectionModel().getSelectedItems().addListener(selItemsListener);

		// set up a nice placeholder
		setPlaceholder(new Label("Click or drag & drop files"));
	}

	/** Clears resources. Do not use this table after calling this method. */
	public void dispose() {
		d1.unsubscribe();
		getSelectionModel().selectedItemProperty().removeListener(selItemListener);
		getSelectionModel().getSelectedItems().removeListener(selItemsListener);
	}

	public Playlist getPlaylist() {
		return (Playlist) getItemsRaw();
	}

/* --------------------- SELECTION ---------------------------------------------------------------------------------- */

	public boolean movingItems = false;
	ChangeListener<PlaylistItem> selItemListener = (o, ov, nv) -> {
		if (movingItems) return;
		PlaylistManager.selectedItemES.push(nv);
	};
	ListChangeListener<PlaylistItem> selItemsListener = (ListChangeListener.Change<? extends PlaylistItem> c) -> {
		if (movingItems) return;
		while (c.next()) {
			PlaylistManager.selectedItemsES.push(getSelectionModel().getSelectedItems());
		}
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
		List<Integer> oldS = new ArrayList<>();
		oldS.addAll(getSelectionModel().getSelectedIndices());
		// move in playlist
		List<Integer> newS = getPlaylist().moveItemsBy(oldS, by);
		// select back
		selectRows(newS, getSelectionModel());

		movingItems = false;    // release lock
	}

/* --------------------- DRAG AND DROP ------------------------------------------------------------------------------ */

	private void dropDrag(DragEvent e, int index) {
		if (DragUtil.hasAudio(e)) {
			List<Item> items = DragUtil.getAudioItems(e);
			getPlaylist().addItems(items, index);
			e.setDropCompleted(true);
			e.consume();
		}
	}

/* --------------------- CONTEXT MENU ------------------------------------------------------------------------------- */

	private static final TableContextMenuR<PlaylistItem,PlaylistTable> contextMenu = new TableContextMenuR<>(
		() -> {
			ImprovedContextMenu<List<PlaylistItem>> m = new ImprovedContextMenu<>();
			m.getItems().addAll(menuItem("Play items", e ->
					PlaylistManager.use(p -> p.playItem(m.getValue().get(0)))
				),
				menuItem("Remove items", e ->
					PlaylistManager.use(p -> p.removeAll(m.getValue()))
				),
				new Menu("Show in", null,
					menuItems(
						APP.widgetManager.getFactories().filter(f -> f.hasFeature(SongReader.class)).toList(),
						WidgetFactory::nameGui,
						f -> APP.widgetManager.use(f.nameGui(), NO_LAYOUT, c -> ((SongReader) c.getController()).read(m.getValue()))
					)
				),
				new Menu("Edit tags in", null,
					menuItems(
						APP.widgetManager.getFactories().filter(f -> f.hasFeature(SongWriter.class)).toList(),
						WidgetFactory::nameGui,
						f -> APP.widgetManager.use(f.nameGui(), NO_LAYOUT, c -> ((SongWriter) c.getController()).read(m.getValue()))
					)
				),
				menuItem("Crop items", e ->
					PlaylistManager.use(p -> p.retainAll(m.getValue()))
				),
				menuItem("Duplicate items as group", e ->
					PlaylistManager.use(p -> p.duplicateItemsAsGroup(m.getValue()))
				),
				menuItem("Duplicate items individually", e ->
					PlaylistManager.use(p -> p.duplicateItemsByOne(m.getValue()))
				),
				menuItem("Explore items's directory", e ->
					Environment.browse(m.getValue().stream().filter(Item::isFileBased).map(Item::getFile))
				),
				menuItem("Add items to library", e -> {
					List<Metadata> items = m.getValue().stream()
						.map(Item::toMeta)
						.collect(Collectors.toList());
					Db.addItems(items);
				}),
				new Menu("Search album cover", null,
					menuItems(APP.plugins.getPlugins(SearchUriBuilder.class),
						q -> "in " + Parser.DEFAULT.toS(q),
						q -> App.itemToMeta(m.getValue().get(0), i -> Environment.browse(q.apply(i.getAlbum()))))
				)
			);
			return m;
		},
		(menu, table) -> {
			List<PlaylistItem> items = table.getSelectedItemsCopy();
			menu.setValue(items);
			if (items.isEmpty()) {
				menu.getItems().forEach(i -> i.setDisable(true));
			} else {
				menu.getItems().forEach(i -> i.setDisable(false));
			}
		}
	);
}