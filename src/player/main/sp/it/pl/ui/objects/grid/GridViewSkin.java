package sp.it.pl.ui.objects.grid;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import kotlin.reflect.KClass;
import sp.it.pl.ui.item_node.FieldedPredicateChainItemNode;
import sp.it.pl.ui.item_node.FieldedPredicateItemNode.PredicateData;
import sp.it.pl.ui.nodeinfo.GridInfo;
import sp.it.pl.ui.nodeinfo.TableInfo;
import sp.it.pl.ui.objects.contextmenu.SelectionMenuItem;
import sp.it.pl.ui.objects.grid.GridView.CellGap;
import sp.it.pl.ui.objects.grid.GridView.Search;
import sp.it.pl.ui.objects.grid.GridView.SelectionOn;
import sp.it.pl.ui.objects.icon.Icon;
import sp.it.util.access.fieldvalue.ObjectField;
import sp.it.util.reactive.Disposer;
import sp.it.util.reactive.UtilKt;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_MINUS;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.rint;
import static java.lang.Math.signum;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.rangeClosed;
import static javafx.application.Platform.runLater;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.ScrollEvent.SCROLL;
import static sp.it.pl.main.AppExtensionsKt.toUi;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.ui.objects.grid.GridView.CELL_SIZE_UNBOUND;
import static sp.it.util.Util.clip;
import static sp.it.util.collections.UtilKt.setTo;
import static sp.it.util.dev.FailKt.failIf;
import static sp.it.util.functional.Util.IS;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.functional.Util.with;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UnsubscribableKt.on;
import static sp.it.util.reactive.UtilKt.onChange;
import static sp.it.util.ui.NodeExtensionsKt.hasFocus;
import static sp.it.util.ui.Util.layHeaderTop;
import static sp.it.util.ui.Util.layHorizontally;
import static sp.it.util.ui.UtilKt.menuItem;

public class GridViewSkin<T, F> implements Skin<GridView<T,F>> {

	public static final int NO_SELECT = Integer.MIN_VALUE;
	private VBox root;
	private GridView<T,F> grid;
	private final Flow<T,F> flow;
	private final StackPane filterPane = new StackPane();
	/** Filter pane in the top of the table. */
	public final Filter filter;
	/**
	 * Visibility of the filter pane.
	 * Filter is displayed in the top of the table.
	 * <p/>
	 * Setting filter visible will
	 * also make it focused (to allow writing filter query immediately). If you
	 * wish for the filter to gain focus set this property to true (focus will
	 * be set even if filter already was visible).
	 * <p/>
	 * Setting filter invisible will also clear any search query and effectively
	 * disable filter, displaying all table items.
	 */
	public BooleanProperty filterVisible = new SimpleBooleanProperty(false) {
		@Override
		public void set(boolean v) {
			if (v && get()) {
				runLater(filter::focus);
				return;
			}

			super.set(v);

			Node sn = filter.getNode();
			if (v) {
				if (!filterPane.getChildren().contains(sn)) filterPane.getChildren().add(0, sn);
			} else {
				filterPane.getChildren().clear();
			}
			filterPane.setMaxHeight(v ? -1 : 0);
			filter.getNode().setVisible(v);

			if (v) failIf(filter.length()!=0);
			filter.convergeTo(v ? 1 : 0);

			// focus filter to allow user use filter asap
			if (v) runLater(filter::focus);
		}
	};
	public int selectedCI = NO_SELECT;
	private GridCell<T,F> selectedC = null;
	private final Disposer onDispose = new Disposer();

	public GridViewSkin(GridView<T,F> control) {
		this.grid = control;
		this.flow = new Flow<>(this);

		attach(grid.getCellFactory(), e -> flow.disposeAndRebuildCells());
		attach(grid.getCellHeight(), e -> flow.disposeAndRebuildCells());
		attach(grid.getCellWidth(), e -> flow.disposeAndRebuildCells());
		attach(grid.getCellAlign(), e -> flow.rebuildCells());
		attach(grid.getCellMaxColumns(), e -> flow.rebuildCells());
		attach(grid.getHorizontalCellSpacing(), e -> flow.rebuildCells());
		attach(grid.getVerticalCellSpacing(), e -> flow.rebuildCells());
		attach(grid.widthProperty(), e -> flow.rebuildCells());
		attach(grid.heightProperty(), e -> flow.rebuildCells());
		attach(grid.parentProperty(), p -> {
			if (p!=null) flow.rebuildCells();
		});

		root = layHeaderTop(0, Pos.TOP_RIGHT, filterPane, flow.root);

		// header
		filter = new Filter(grid.getType(), grid.getItemsFiltered());

		// footer
		sync(grid.getFooterVisible(), v -> {
			if (v && !root.getChildren().contains(footerPane))
				root.getChildren().add(footerPane);
			if (!v)
				root.getChildren().remove(footerPane);
		});
		on(onChange(grid.getItemsShown(), runnable(() -> flow.rebuildCellsNow())), onDispose);
		searchQueryLabel.textProperty().bind(control.getSearch().searchQuery);
		itemsInfo.bind(grid);
		footerPane.getStyleClass().add("grid-view-footer");

		// search
		onDispose.plusAssign(runnable(() -> searchQueryLabel.textProperty().unbind()));
		onDispose.plusAssign(runnable(() -> itemsInfo.unbind()));

		// selection
		grid.addEventHandler(KEY_PRESSED, e -> {
			if (e.isConsumed()) return;
			KeyCode c = e.getCode();
			if (c.isNavigationKey()) {
				if (grid.getSelectOn().contains(SelectionOn.KEY_PRESS)) {
					if (c==KeyCode.UP || c==KeyCode.KP_UP) selectIfNoneOr(this::selectFirst, this::selectUp);
					if (c==KeyCode.DOWN || c==KeyCode.KP_DOWN) selectIfNoneOr(this::selectFirst, this::selectDown);
					if (c==KeyCode.LEFT || c==KeyCode.KP_LEFT) selectIfNoneOr(this::selectFirst, this::selectLeft);
					if (c==KeyCode.RIGHT || c==KeyCode.KP_RIGHT) selectIfNoneOr(this::selectFirst, this::selectRight);
					if (c==KeyCode.PAGE_UP) selectIfNoneOr(this::selectFirst, this::selectPageUp);
					if (c==KeyCode.PAGE_DOWN) selectIfNoneOr(this::selectFirst, this::selectPageDown);
					if (c==KeyCode.HOME) selectFirst();
					if (c==KeyCode.END) selectLast();
				}
				e.consume();
			} else if (c==ESCAPE) {
				if (selectedCI >= 0) {
					selectNone();
					e.consume();
				}
			}
		});

		// Select grid on click. Must work even if cells consume clicks.
		flow.addEventFilter(MOUSE_CLICKED, e -> {
			if (!grid.isFocused())
				grid.requestFocus();
		});
		// Select no cell on click outside cell. Cell click selection must consume events.
		flow.addEventHandler(MOUSE_CLICKED, e -> {
			if (e.isConsumed()) return;
			if (grid.getSelectOn().contains(SelectionOn.MOUSE_CLICK))
				selectNone();
		});
		flow.addEventFilter(SCROLL, e -> {
			if (e.isConsumed()) return;
			// Select hovered cell (if enabled)
			// Newly created cells that 'appear' right under mouse cursor will not receive hover event
			// Normally we would update the selection after the cells get updated, but that happens also on regular
			// selection change, which would stop working properly.
			// Hence, we find such cells and select them here
			if (getSkinnable().getSelectOn().contains(SelectionOn.MOUSE_HOVER)) {
				flow.getCells()
					.filter(it -> it.isHover() && !it.isSelected()).findAny()
					.ifPresent(this::select);
			}
		});
	}

	private <O> void attach(ObservableValue<O> value, Consumer<? super O> action) {
		on(UtilKt.attach(value, consumer(action)), onDispose);
	}

	private <O> void sync(ObservableValue<O> value, Consumer<? super O> action) {
		on(UtilKt.sync(value, consumer(action)), onDispose);
	}

	// TODO: improve API
	public double getPosition() {
		return flow.getPosition();
	}

	// TODO: improve API
	public void setPosition(double position) {
		flow.scrollTo(position);
	}

	/* ---------- SEARCH ------------------------------------------------------------------------------------------------ */

	private final Label searchQueryLabel = new Label();

	/* ---------- FOOTER ------------------------------------------------------------------------------------------------ */

	public final Menu menuAdd = new Menu("", new Icon(PLAYLIST_PLUS).scale(1.3).embedded());
	public final Menu menuRemove = new Menu("", new Icon(PLAYLIST_MINUS).scale(1.3).embedded());
	public final Menu menuSelected = new Menu("", new Icon(FontAwesomeIcon.CROP).embedded(),
		menuItem("Select none", null, consumer(e -> selectNone()))
	);
	private final Menu menuOrderAlign = new Menu("Align");
	public final Menu menuOrder = with(new Menu("", new Icon(FontAwesomeIcon.NAVICON).embedded(), menuOrderAlign), m ->
		m.addEventHandler(Menu.ON_SHOWING, e ->
			menuOrderAlign.getItems().setAll(SelectionMenuItem.Companion.buildSingleSelectionMenu(CellGap.Companion.getValues(), grid.getCellAlign().getValue(), it -> toUi(it), it -> grid.getCellAlign().setValue(it)))
		)
	);
	/** Table menu bar in the bottom with menus. Feel free to modify. */
	public final MenuBar menus = new MenuBar(menuAdd, menuRemove, menuSelected, menuOrder);
	/**
	 * Labeled in the bottom displaying information on table items and selection.
	 * Feel free to provide custom implementation of {@link TableInfo#setTextFactory(kotlin.jvm.functions.Function2)}
	 * to display different information. You may want to reuse {@link sp.it.pl.ui.nodeinfo.TableInfo.Companion#getDEFAULT_TEXT_FACTORY()}.
	 */
	public final GridInfo<T,F> itemsInfo = new GridInfo<>(new Label(), null);
	private final HBox bottomLeftPane = layHorizontally(5, CENTER_LEFT, menus, itemsInfo.getNode());
	private final HBox bottomRightPane = layHorizontally(5, CENTER_RIGHT, searchQueryLabel);
	/**
	 * Pane for controls in the bottom of the table.
	 * Feel free to modify its content. Menu bar and item info label are on the
	 * left {@link BorderPane#leftProperty()}. Search query label is on the right {@link BorderPane#rightProperty()}.
	 * Both wrapped in {@link HBox};
	 */
	public final BorderPane footerPane = new BorderPane(null, null, bottomRightPane, null, bottomLeftPane);

	/* ---------- FILTER ------------------------------------------------------------------------------------------------ */

	@Override
	public GridView<T,F> getSkinnable() {
		return grid;
	}

	@Override
	public Node getNode() {
		return root;
	}

	@Override
	public void dispose() {
		onDispose.invoke();
		flow.dispose();
		root = null;
		grid = null;
	}

	public Stream<GridCell<T,F>> getCells() {
		return flow.getCells();
	}

	public Stream<GridCell<T,F>> getCellsAll() {
		return flow.getCellsAll();
	}

	/* ---------- SELECTION --------------------------------------------------------------------------------------------- */

	@SuppressWarnings({"unchecked", "SimplifyOptionalCallChains"})
	private PredicateData<ObjectField<F,Object>> getPrimaryFilterPredicate() {
		return Optional.ofNullable(grid.getFilterPrimaryField())
			.map((Function<ObjectField<F,?>,PredicateData<? extends ObjectField<F,?>>>) PredicateData::ofField)
			.map(f -> (PredicateData<ObjectField<F,Object>>) f)
			.orElse(null);
	}

	@SuppressWarnings("unchecked")
	private List<PredicateData<ObjectField<F,Object>>> getFilterPredicates(KClass<F> filterType) {
		return stream(APP.getClassFields().get(filterType))
			.filter(ObjectField::isTypeFilterable)
			.map((Function<ObjectField<F,?>,PredicateData<? extends ObjectField<F,?>>>) PredicateData::ofField)
			.map(f -> (PredicateData<ObjectField<F,Object>>) f)
			.sorted(by(e -> e.name()))
			.toList();
	}

	public void selectIfNoneOr(Runnable ifEmpty, Runnable otherwise) {
		if (selectedCI<0) ifEmpty.run();
		else otherwise.run();
	}

	public void selectRight() {
		select(selectedCI + 1);
	}

	public void selectLeft() {
		select(selectedCI - 1);
	}

	public void selectUp() {
		int sel = selectedCI - flow.computeMaxCellsInRow();
		select(max(0, sel));
	}

	public void selectDown() {
		int sel = selectedCI + flow.computeMaxCellsInRow();
		select(min(grid.getItemsShown().size() - 1, sel));
	}

	public void selectPageUp() {
		int sel = selectedCI - flow.computeAvgVisibleCells();
		select(max(0, sel));
	}

	public void selectPageDown() {
		int sel = selectedCI + flow.computeAvgVisibleCells();
		select(min(grid.getItemsShown().size() - 1, sel));
	}

	public void selectFirst() {
		select(0);
	}

	public void selectLast() {
		select(grid.getItemsShown().size() - 1);
	}

	public void selectNone() {
		select(NO_SELECT);
	}

	public void select(GridCell<T,F> c) {
		if (c==null || c.getItem()==null) selectNone();
		else select(c.getIndex());
	}

	public void select(T item) {
		select(grid.getItemsShown().indexOf(item));
	}

	/** Select cell (and row it is in) at index. No-op if out of range. */
	public void select(int i) {
		if (i==NO_SELECT) {
			if (selectedC!=null) selectedC.updateSelected(false);
			selectedC = null;
			selectedCI = NO_SELECT;
			grid.getSelectedItem().setValue(null);
			return;
		}

		int itemCount = grid.getItemsShown().size();
		int iMin = 0;
		int iMax = itemCount - 1;
		if (itemCount==0 || i==selectedCI || i<iMin || i>iMax) return;

		int rows = flow.computeRowCount();
		int cols = flow.computeMaxCellsInRow();
		int row = i/cols;

		if (row<0 || row>rows) return;

		flow.scrollToRow(row);

		GridCell<T,F> c = flow.getVisibleCellAtIndex(i);
		if (c==null) return;

		selectedCI = i;
		if (selectedC!=null) selectedC.updateSelected(false);
		selectedC = c;
		selectedC.updateSelected(true);
		grid.getSelectedItem().setValue(c.getItem());
	}

	/**
	 * Simple implementation of virtual container, much like {@link javafx.scene.control.skin.VirtualFlow}, but for 2D
	 * grid-like content.
	 * <p/>
	 * Limitations:
	 * <ul>
	 * <li/> Vertical orientation only
	 * <li/> No horizontal scrolling, content must fit the view
	 * <li/> No touch support
	 * <li/> No panning support
	 * </ul>
	 * <p/>
	 * Improvements over 2D solution using virtual flows in virtual flow:
	 * <ul>
	 * <li/> Extensible, modifiable
	 * <li/> No use of reflection to get around visibility problems
	 * <li/> Simple and predictable behavior (particularly layout, but also scrolling or cell life cycle
	 * <li/> Fixed mouse scrolling as well as scrollbar tick scrolling being painfully slow
	 * <li/> Fixed clipping (Sometimes the clip mask would leave few px gap, showing unwanted content near scrollbar)
	 * <li/> Scrollbar does not complicate layout and is completely separate
	 * </ul>
	 *
	 * @param <T> type of item
	 * @param <F> type of cell
	 */
	public static class Flow<T, F> extends Pane {
		private final GridViewSkin<T,F> skin;
		@SuppressWarnings({"unchecked", "rawtypes"})
		private final List<GridCell<T,F>> visibleCells = (List) getChildren();
		private final LinkedList<GridCell<T,F>> cachedCells = new LinkedList<>();
		private final Rectangle clipMask = new Rectangle(0, 0);
		private final FlowScrollBar scrollbar;
		private final Scrolled root;
		private double viewStart = 0;
		private boolean needsAdjustSize = false;
		private boolean needsRemoveCachedCells = false;
		private boolean needsRebuildCells = true;
		private final double scrollSpeedMultiplier = 3;

		public Flow(GridViewSkin<T,F> skin) {
			this.skin = skin;

			// scrollbar
			scrollbar = new FlowScrollBar(this);
			scrollbar.setOrientation(Orientation.VERTICAL);
			scrollbar.setMin(0);
			scrollbar.setMax(1);
			scrollbar.setValue(0);
			scrollbar.addEventHandler(MouseEvent.ANY, Event::consume);
			scrollbar.setVisible(false);
			getChildren().add(scrollbar);

			// clip
			clipMask.setSmooth(false);
			setClip(clipMask);

			// scrolling
			addEventHandler(SCROLL, e -> {
				scrollBy(-e.getDeltaY()*scrollSpeedMultiplier);
				e.consume();
			});

			// root
			root = new Scrolled(scrollbar, this);
		}

		public Stream<GridCell<T,F>> getCells() {
			return visibleCells.stream();
		}

		public Stream<GridCell<T,F>> getCellsAll() {
			return Stream.concat(visibleCells.stream(), cachedCells.stream()).distinct();
		}

		public double getPosition() {
			return viewStart;
		}

		public GridCell<T,F> getVisibleCellAtIndex(int i) {
			int indexOffset = computeMinVisibleCellIndex();
			return getAt(i - indexOffset, visibleCells);
		}

		public GridCell<T,F> getFirstVisibleCell() {
			return visibleCells.isEmpty() ? null : visibleCells.get(0);
		}

		public GridCell<T,F> getLastVisibleCell() {
			return visibleCells.isEmpty() ? null : visibleCells.get(visibleCells.size() - 1);
		}

		private GridCell<T,F> createCell() {
			var grid = getSkinnable();
			var cellFactory = grid.getCellFactory().getValue();
			var cell = cellFactory==null ? new GridCell<T,F>() : cellFactory.invoke(grid);
			cell.getGridView().set(grid);
			cell.addEventHandler(MOUSE_CLICKED, e -> {
				if (grid.getSelectOn().contains(SelectionOn.MOUSE_CLICK)) {
					getSkinnable().getSkinImpl().select(cell);
					e.consume();
				}
			});
			cell.hoverProperty().addListener((o, ov, nv) -> {
				if (nv && grid.getSelectOn().contains(SelectionOn.MOUSE_HOVER))
					getSkinnable().getSkinImpl().select(cell);
			});
			cell.pseudoClassStateChanged(Search.PC_SEARCH_MATCH, false);
			cell.pseudoClassStateChanged(Search.PC_SEARCH_MATCH_NOT, false);
			cell.setManaged(false);
			return cell;
		}

		void disposeAndRebuildCells() {
			needsRemoveCachedCells = true;
			needsAdjustSize = true;
			rebuildCells();
		}

		void rebuildCells() {
			needsRebuildCells = true;
			requestLayout();
		}

		void rebuildCellsNow() {
			needsRebuildCells = true;
			layoutChildren();
		}

		@Override
		protected <E extends Node> List<E> getManagedChildren() {
			return list();
		}

		@Override
		protected void layoutChildren() {

			boolean wasFocused = hasFocus(skin.flow);
			double w = getWidth();
			double h = getHeight();

			// update clip
			clipMask.setX(0);
			clipMask.setY(0);
			clipMask.setWidth(w);
			clipMask.setHeight(h);

			// update position in case resize put it out of view
			viewStart = min(viewStart, computeMaxViewStart());

			List<T> items = getSkinnable().getItemsShown();
			int itemsAllCount = items.size();
			double virtualHeight = computeRowHeight()*computeRowCount();
			double viewHeight = getHeight();
			double scrollableHeight = virtualHeight - viewHeight;

			// update scrollbar
			scrollbar.updating = true;
			scrollbar.setVisible(computeMaxFullyVisibleCells()<itemsAllCount);
			scrollbar.setMin(0);
			scrollbar.setMax(scrollableHeight);
			scrollbar.setVisibleAmount(viewHeight/virtualHeight*scrollableHeight);
			scrollbar.setValue(viewStart);
			scrollbar.updating = false;

			var grid = getSkinnable();
			var cellWidthRaw = grid.getCellWidth().getValue();
			var cellWidth = cellWidthRaw == CELL_SIZE_UNBOUND ? w : cellWidthRaw;
			var cellHeight = grid.getCellHeight().getValue();
			var cellWidthSnapped = snapSizeX(max(cellWidth, 10));
			var cellHeightSnapped = snapSizeY(max(cellHeight, 10));

			// update cells
			if (needsRebuildCells) {
				needsRebuildCells = false;
				if (needsRemoveCachedCells) {
					needsRemoveCachedCells = false;
					var cells = List.copyOf(visibleCells);
					visibleCells.clear();
					cachedCells.clear();
					cells.forEach(cell -> cell.update(-1, null, false));
				}
				int indexStart = computeMinVisibleCellIndex();
				int indexEnd = min(itemsAllCount - 1, computeMaxVisibleCellIndex());
				int itemCount = indexEnd - indexStart + 1;

				if (itemsAllCount==0) {
					var cells = List.copyOf(visibleCells);
					visibleCells.clear();
					cells.forEach(cachedCells::addLast);
					cachedCells.forEach(cell -> cell.update(-1, null, false));
				} else {
					failIf(indexStart>indexEnd);
					failIf(indexStart<0);
					failIf(indexEnd<0);
					failIf(itemsAllCount<=indexStart);
					failIf(itemsAllCount<=indexEnd);

					var visibleCellOld = visibleCells.stream().collect(toMap(c -> c.getIndex(), c -> c));
					var visibleCellsNew = rangeClosed(indexStart, indexEnd).mapToObj(i -> {
							{   // reuse visible cells to prevent needlessly update their content
								var c = visibleCellOld.remove(i);
								if (c!=null) failIf(c.getIndex()!=i);
								if (c!=null) return c;
							}
							{   // reuse cached cells to prevent needlessly creating cells
								var c = cachedCells.isEmpty() ? null : cachedCells.removeLast();
								if (c!=null) c.updateIndex(i);
								if (c!=null) return c;
							}
							{   // or create cell
								var c = createCell();
								c.updateIndex(i);
								c.getProperties().put("xxx", "xxx");
//							c.resize(cellWidthSnapped, cellHeightSnapped);
								return c;
							}
						}).toList();
					setTo(visibleCells, visibleCellsNew);
					failIf(visibleCells.size()!=itemCount);

					visibleCellOld.values().forEach(cell -> {
						cell.update(-1, null, false);
						cachedCells.addLast(cell);
					});
				}

			}

			// update cells
			int itemCount = visibleCells.size();
			if (itemCount>0) {
				var columns = computeMaxCellsInRow();
				var vGap = grid.getVerticalCellSpacing().getValue();
				var hGap = grid.getCellAlign().getValue().computeGap(grid, w, columns);
				var cellGapHeight = cellHeight + vGap;
				var cellGapWidth = cellWidth + hGap;
				var viewStartY = viewStart;
				var cellWidthIsUnbound = cellWidthRaw == CELL_SIZE_UNBOUND;
				var cellXsInitial = cellWidthIsUnbound ? 0.0 : grid.getCellAlign().getValue().computeStartX(grid, w, columns);
				var cellXs = Stream.iterate(cellXsInitial, it -> it + cellGapWidth).limit(columns).map(it -> snapPositionX(it)).toArray(Double[]::new);
				var viewStartRI = computeMinVisibleRowIndex();
				var rowCount = computeVisibleRowCount();
				for (int rowI = viewStartRI, yi = 0, i = 0; rowI<viewStartRI + rowCount; rowI++, yi++) {
					double rowStartY = rowI*cellGapHeight;
					double yPos = rowStartY - viewStartY;
					double yPosSnapped = snapPositionY(yPos);
					for (int cellI = rowI*columns, xi = 0; cellI<(rowI + 1)*columns; cellI++, i++, xi++) {
						if (i>=itemCount) break;	// last row may not be full

						var cell = getAt(i, visibleCells);
						var item = getAt(cellI, items);
						failIf(cell==null);
						failIf(cell.getIndex()!=cellI);
						failIf(item==null);

						if (needsAdjustSize || cell.getProperties().containsKey("xxx")) cell.resizeRelocate(cellXs[xi], yPosSnapped, cellWidthSnapped, cellHeightSnapped);
						else cell.relocate(cellXs[xi], yPosSnapped);
						cell.update(cellI, item, cellI==skin.selectedCI);
						cell.pseudoClassStateChanged(GridCell.pseudoclassFullWidth, cellWidthIsUnbound);
					}
				}
			}

			// update selection (if items changed, same cell may remain selected, but selected item may be different)
			// TODO: enable (requires observation of cases where this should be invoked, it should not invoke when scrolling takes place)
//			skin.selectedCI = skin.selectedC==null ? NO_SELECT : skin.selectedC.getIndex();
//			skin.grid.getSelectedItem().set(skin.selectedC==null ? null : skin.selectedC.getItem());

			needsAdjustSize = false;

			// retain focus (cells should not be focus-traversable, but just in case)
			if (wasFocused) skin.grid.requestFocus();
		}

		void dispose() {
			needsRebuildCells = false;
			var cells = List.copyOf(visibleCells);
			visibleCells.clear();
			cells.forEach(c -> c.dispose());
			cachedCells.forEach(c -> c.dispose());
			cachedCells.clear();
		}

		@SuppressWarnings("unused")
		private void scrollToRow(int row) {
			GridCell<T,F> fvc = getFirstVisibleCell();
			GridCell<T,F> lvc = getLastVisibleCell();

			boolean isNoRow = fvc==null || lvc==null;
			if (isNoRow) return;

			int rowSize = computeMaxCellsInRow();
			int fvr = fvc.getIndex()/rowSize;
			int lvr = lvc.getIndex()/rowSize;
			boolean isUp = row<=fvr;
			boolean isDown = row >= lvr;
			boolean isSingleRow = isUp && isDown;
			if (isSingleRow) return;
			if (isUp) scrollToRowTop(row);
			if (isDown) scrollToRowBottom(row);
		}

		private void scrollToRowTop(int row) {
			scrollTo(row*computeRowHeight());
		}

		private void scrollToRowBottom(int row) {
			GridCell<T,F> lvc = getLastVisibleCell();
			double rowHeight = computeRowHeight();
			int rowSize = computeMaxCellsInRow();
			int lvr = lvc.getIndex()/rowSize;
			double rowBy = (row - lvr)*rowHeight;
			double cellBy = lvc.getLayoutY() + rowHeight - getHeight();
			double by = rowBy + cellBy;
			scrollBy(by);
		}

		public void scrollBy(double by) {
			scrollTo(viewStart + by);
		}

		public void scrollByRows(int by) {
			scrollBy(by*computeRowHeight());
		}

		public void scrollByPage(int sign) {
			if (sign<0) {
				var cell = getFirstVisibleCell();
				if (cell==null) return;
				var row = cell.getIndex()/computeMaxCellsInRow();
				scrollToRowBottom(row);
			}
			if (sign>0) {
				var cell = getLastVisibleCell();
				if (cell==null) return;
				var row = cell.getIndex()/computeMaxCellsInRow();
				scrollToRowTop(row);
			}
		}

		@SuppressWarnings("unused")
		public void scrollTo01(double to) {
			double virtualHeight = computeRowHeight()*computeRowCount();
			double viewHeight = getHeight();
			double scrollableHeight = virtualHeight - viewHeight;
			scrollTo(to*scrollableHeight);
		}

		public void scrollTo(double to) {
			double minY = 0;
			double maxY = max(0, computeRowCount(getSkinnable().getItemsShown())*computeRowHeight() - getHeight());
			double newY = clip(minY, to, maxY);
			if (viewStart!=newY) {
				viewStart = newY;
				rebuildCellsNow();
			}
		}

		public GridView<T,F> getSkinnable() {
			return skin.getSkinnable();
		}

		public double computeMaxViewStart() {
			return max(0, computeRowCount()*computeRowHeight() - getHeight());
		}

		public int computeMinVisibleRowIndex() {
			return (int) floor(viewStart/computeRowHeight());
		}

		public int computeMinVisibleCellIndex() {
			return computeMaxCellsInRow()*computeMinVisibleRowIndex();
		}

		public int computeMaxVisibleRowIndex() {
			return (int) floor((viewStart + getHeight())/computeRowHeight());
		}

		public int computeMaxVisibleCellIndex() {
			return computeMaxCellsInRow()*(computeMaxVisibleRowIndex() + 1) - 1;
		}

		public int computeMaxVisibleCells() {
			return computeMaxCellsInRow()*(int) ceil((getHeight())/computeRowHeight());
		}

		public int computeAvgVisibleCells() {
			return computeMaxCellsInRow()*(int) rint((getHeight())/computeRowHeight());
		}

		public int computeMaxFullyVisibleCells() {
			return computeMaxCellsInRow()*(int) floor((getHeight())/computeRowHeight());
		}

		/** @return the number of rows needed to display the whole set of cells */
		public int computeRowCount() {
			return computeRowCount(getSkinnable().getItemsShown());
		}

		/** @return the number of rows needed to display the whole set of cells */
		public int computeRowCount(List<T> items) {
			return computeRowCount(items==null ? 0 : items.size());
		}

		/** @return the number of rows needed to display the whole set of cells */
		public int computeRowCount(int itemCount) {
			return itemCount==0 ? 0 : (int) ceil((double) itemCount/computeMaxCellsInRow());
		}

		public int computeVisibleRowCount() {
			return 1 + computeMaxVisibleRowIndex() - computeMinVisibleRowIndex();
		}

		/** @return the max number of cell per row */
		public int computeMaxCellsInRow() {
			var cw = getSkinnable().getCellWidth().getValue();
			if (cw == GridView.CELL_SIZE_UNBOUND) {
				return 1;
			} else {
				var gap = getSkinnable().getHorizontalCellSpacing().getValue();
				var maxColumnsRaw = getSkinnable().getCellMaxColumns().getValue();
				var maxColumns = maxColumnsRaw != null ? maxColumnsRaw : Integer.MAX_VALUE;
				return clip(1, (int) floor((computeRowWidth() + gap)/(cw + gap)), maxColumns);
			}
		}

		public int computeMaxRowsInView() {
			var gap = getSkinnable().getVerticalCellSpacing().getValue();
			return clip(1, (int) floor((getSkinnable().getHeight() + gap)/computeRowHeight()), Integer.MAX_VALUE);
		}

		/** @return the width of a row (should be `GridView.width - GridView.Scrollbar.width`) */
		protected double computeRowWidth() {
			return getSkinnable().getWidth();
		}

		protected double computeRowHeight() {
			return getSkinnable().getCellHeight().getValue() + getSkinnable().getVerticalCellSpacing().getValue();
		}

	}

	/** Pane which wraps a content and adds a vertical scrollbar on the right. */
	public static class Scrolled extends Pane {
		private final ScrollBar scrollbar;
		private final Pane content;

		public Scrolled(ScrollBar scrollbar, Pane content) {
			this.scrollbar = scrollbar;
			this.content = content;
			scrollbar.visibleProperty().addListener((o, ov, nv) -> requestLayout());
			getChildren().addAll(scrollbar, content);
		}

		@Override
		protected void layoutChildren() {
			double w = getWidth(), h = getHeight();
			boolean scrollbarVisible = scrollbar.isVisible();
			if (scrollbarVisible) {
				double scrollbarW = scrollbar.prefWidth(-1);
				scrollbar.resizeRelocate(w - scrollbarW, 0, scrollbarW, h);
				content.resizeRelocate(0, 0, w - scrollbarW, h);
			} else {
				content.resizeRelocate(0, 0, w, h);
			}
		}
	}

	/** Scrollbar with adjusted scrolling behavior to work with {@link GridViewSkin.Flow}. */
	public static class FlowScrollBar extends ScrollBar {
		private final Flow<?,?> flow;
		private boolean adjusting;
		private boolean doNotAdjust;
		public boolean updating;

		public FlowScrollBar(Flow<?,?> flow) {
			this.flow = flow;

			valueProperty().addListener((o, ov, nv) -> {
				if (ov.doubleValue()!=nv.doubleValue() && !adjusting && !updating) {
					flow.scrollTo(nv.doubleValue());
				}
			});
		}

		@Override
		public void decrement() {
			doNotAdjust = true;
			flow.scrollByRows(-1);
			doNotAdjust = false;
		}

		@Override
		public void increment() {
			doNotAdjust = true;
			flow.scrollByRows(1);
			doNotAdjust = false;
		}

		// Called when the user clicks the scrollbar track, we call the page-up and page-down
		@Override
		public void adjustValue(double pos) {
			if (doNotAdjust) return;
			adjusting = true;
			double oldValue = flow.getPosition();
			double newValue = getMin() + ((getMax() - getMin())*clip(0, pos, 1));
			int direction = (int) signum(newValue - oldValue);
			flow.scrollByPage(direction);
			adjusting = false;
		}
	}

	/** Table's filter node. */
	public class Filter extends FieldedPredicateChainItemNode<F,ObjectField<F,Object>> {

		private Filter(KClass<F> filterType, FilteredList<T> filterList) {
			super();
			setPrefTypeSupplier(GridViewSkin.this::getPrimaryFilterPredicate);
			setData(getFilterPredicates(filterType));
			onItemChange = predicate -> filterList.setPredicate(predicate==IS ? null : item -> predicate.test(getSkinnable().getFilterMapper().apply(item)));

			var filterKeyHandler = buildToggleOnKeyHandler(filterVisible, GridViewSkin.this.flow);
			getNode().addEventFilter(KEY_PRESSED, filterKeyHandler);
			getSkinnable().addEventHandler(KEY_PRESSED, filterKeyHandler); // filter would ignore first keystroke when filter turns visible
		}
	}

	private static boolean isInRange(int i, Collection<?> c) {
		return i>=0 && i<c.size();
	}

	private static <T> T getAt(int i, List<T> list) {
		return isInRange(i, list) ? list.get(i) : null;
	}
}