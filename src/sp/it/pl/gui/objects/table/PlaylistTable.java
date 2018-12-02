package sp.it.pl.gui.objects.table;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.reactfx.Subscription;
import sp.it.pl.audio.Item;
import sp.it.pl.audio.Player;
import sp.it.pl.audio.playlist.Playlist;
import sp.it.pl.audio.playlist.PlaylistItem;
import sp.it.pl.audio.playlist.PlaylistManager;
import sp.it.pl.audio.tagging.PlaylistItemGroup;
import sp.it.pl.gui.objects.contextmenu.TableContextMenuR;
import sp.it.pl.gui.objects.tablerow.ImprovedTableRow;
import sp.it.pl.util.access.V;
import sp.it.pl.util.access.fieldvalue.ColumnField;
import sp.it.pl.util.graphics.drag.DragUtil;
import sp.it.pl.util.units.Dur;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static sp.it.pl.audio.playlist.PlaylistItem.Field.LENGTH;
import static sp.it.pl.audio.playlist.PlaylistItem.Field.NAME;
import static sp.it.pl.audio.playlist.PlaylistItem.Field.TITLE;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.functional.Util.SAME;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.functional.Util.listRO;
import static sp.it.pl.util.graphics.Util.computeFontWidth;
import static sp.it.pl.util.graphics.Util.consumeOnSecondaryButton;
import static sp.it.pl.util.graphics.Util.selectRows;
import static sp.it.pl.util.graphics.drag.DragUtil.installDrag;
import static sp.it.pl.util.reactive.Util.maintain;

/**
 * Playlist table GUI component.
 * <p/>
 * Introduces two additional TableCell css pseudoclasses: {@link #STYLE_PLAYED} and {@link #STYLE_CORRUPT} for cells
 * containing played item and corrupted item respectively.
 * <p/>
 * Always call {@link #dispose()}
 */
// TODO: fix duplicate code for dragging in case of empty table case
public class PlaylistTable extends FilteredTable<PlaylistItem> {

	private static final String STYLE_CORRUPT = "corrupt";
	private static final String STYLE_PLAYED = "played";
	private static final TableContextMenuR<PlaylistItemGroup> contextMenu = new TableContextMenuR<>();

	public final V<Boolean> scrollToPlaying = new V<>(true);
	private final TableColumn<PlaylistItem,String> columnName;
	private final TableColumn<PlaylistItem,Dur> columnTime;

	private double selectionLastScreenY;
	private ArrayList<Integer> selectionTmp = new ArrayList<>();
	private final Subscription d1;

	public PlaylistTable(Playlist playlist) {
		super(PlaylistItem.class, NAME, playlist);

		playlist.setTransformation(getItems());

		VBox.setVgrow(this, Priority.ALWAYS);

		// initialize table
		getSelectionModel().setSelectionMode(MULTIPLE);

		// initialize column factories
		setColumnFactory(f -> {
			TableColumn<PlaylistItem,Object> c = new TableColumn<>(f.toString());
			boolean hasPropertyGetter = f==(PlaylistItem.Field)NAME || f==(PlaylistItem.Field)LENGTH;
			c.setCellValueFactory(hasPropertyGetter
				? new PropertyValueFactory<>(f.name().toLowerCase())
				: cf -> cf.getValue()==null ? null : new PojoV<>(f.getOf(cf.getValue()))
			);
			c.setCellFactory(column -> buildDefaultCell(f));
			c.setResizable(true);
			return c;
		});
		setColumnState(getDefaultColumnInfo());
		columnName = getColumn(NAME).get();
		columnTime = getColumn(LENGTH).get();

		// initialize row factories
		setRowFactory(t -> new ImprovedTableRow<>() {
			{
				// remember position for moving selected rows on mouse drag
				setOnMousePressed(e -> selectionLastScreenY = e.getScreenY());
				// clear table selection on mouse released if no item
				setOnMouseReleased(e -> {
					if (getItem()==null)
						selectNone();
				});
				// left double click -> play
				onLeftDoubleClick((r, e) -> getPlaylist().playItem(r.getItem()));
				// right click -> show context menu
				onRightSingleClick((r, e) -> {
					// prep selection for context menu
					if (!isSelected())
						getSelectionModel().clearAndSelect(getIndex());

					ImprovedTable<PlaylistItem> table = PlaylistTable.this;
					contextMenu.show(new PlaylistItemGroup(table.getSelectedItemsCopy()), table, e);
				});
				// handle drag transfer
				setOnDragDropped(e -> dropDrag(e, isEmpty() ? getItems().size() : getIndex()));

				// additional css style classes
				styleRuleAdd(STYLE_PLAYED, p -> getPlaylist().isItemPlaying(p));
				styleRuleAdd(STYLE_CORRUPT, PlaylistItem::isCorruptCached);
			}
		});
		// maintain playing item css by refreshing first column
		d1 = Player.playingItem.onChange(o -> refreshFirstColumn());

		// TODO: algorithm breaks down if TITLE and NAME are both visible, fix
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


			getColumn(ColumnField.INDEX).ifPresent(c -> c.setPrefWidth(computeIndexColumnWidth()));
			getColumn(LENGTH).ifPresent(c -> {
				double mt = getItems().stream().mapToDouble(PlaylistItem::getTimeMs).max().orElse(6000);
				double width = computeFontWidth(APP.ui.getFont().getValue(), new Dur(mt).toString()) + 5;
				c.setPrefWidth(width);
			});

			TableColumn<PlaylistItem,?> mc = isColumnVisible(NAME) ? columnName : getColumn(TITLE).orElse(null);
			if (mc!=null) {
				double Σcw = resize.getTable().getColumns().stream()
						.filter(c -> c!=mc)
						.mapToDouble(TableColumnBase::getWidth).sum();
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
			double dist = e.getScreenY() - selectionLastScreenY;

			int by = (int) (dist/h);
			if (by>=1 || by<=-1) {
				selectionLastScreenY = e.getScreenY();
				moveSelectedItems(by);
			}
		});

		// set key-induced actions
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
			// play selected
			if (e.getCode()==KeyCode.ENTER) {
				if (!getSelectedItems().isEmpty())
					getPlaylist().playItem(getSelectedItems().get(0));
			}
			// delete selected
			if (e.getCode()==KeyCode.DELETE) {
				List<PlaylistItem> p = getSelectedItemsCopy();
				getSelectionModel().clearSelection();
				getPlaylist().removeAll(p);
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
			if (v)
				scrollToCenter(playlist.getPlaying());
		});
		playlist.playingI.addListener((o, ov, nv) -> {
			if (scrollToPlaying.getValue())
				scrollToCenter(playlist.getPlaying());
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

}