package sp.it.pl.gui.objects.grid;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
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
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import sp.it.pl.gui.itemnode.FieldedPredicateChainItemNode;
import sp.it.pl.gui.itemnode.FieldedPredicateItemNode;
import sp.it.pl.gui.itemnode.FieldedPredicateItemNode.PredicateData;
import sp.it.pl.gui.objects.grid.GridView.CellGap;
import sp.it.pl.gui.objects.grid.GridView.Search;
import sp.it.pl.gui.objects.grid.GridView.SelectionOn;
import sp.it.util.access.fieldvalue.ObjectField;
import sp.it.util.functional.Functors;
import sp.it.util.reactive.Disposer;
import sp.it.util.reactive.SubscriptionKt;
import sp.it.util.reactive.UtilKt;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.rangeClosed;
import static javafx.application.Platform.runLater;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.ScrollEvent.SCROLL;
import static javafx.util.Duration.millis;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.Util.clip;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.collections.UtilKt.setTo;
import static sp.it.util.dev.FailKt.failIf;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.firstNotNull;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UtilKt.onChange;
import static sp.it.util.reactive.UtilKt.sync1IfInScene;
import static sp.it.util.ui.Util.layHeaderTop;

public class GridViewSkin<T, F> implements Skin<GridView> {

	private static final int NO_SELECT = Integer.MIN_VALUE;
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

			if (v) filter.growTo1();
			else filter.shrinkTo(0);

			// focus filter to allow user use filter asap
			if (v) runLater(filter::focus);
		}
	};
	int selectedCI = NO_SELECT;
	private GridCell<T,F> selectedC = null;
	private final Disposer onDispose = new Disposer();

	public GridViewSkin(GridView<T,F> control) {
		this.grid = control;
		this.flow = new Flow<>(this);

		attach(grid.cellFactory, e -> flow.rebuildCells());
		attach(grid.cellHeight, e -> flow.rebuildCells());
		attach(grid.cellWidth, e -> flow.rebuildCells());
		attach(grid.cellGap, e -> flow.rebuildCells());
		attach(grid.horizontalCellSpacing, e -> flow.rebuildCells());
		attach(grid.verticalCellSpacing, e -> flow.rebuildCells());
		attach(grid.widthProperty(), e -> flow.rebuildCells());
		attach(grid.heightProperty(), e -> flow.rebuildCells());
		attach(grid.parentProperty(), p -> {
			if (p!=null) flow.rebuildCells();
		});
		attach(grid.focusedProperty(), v -> {
			if (v) flow.requestFocus();
		});
		SubscriptionKt.on(onChange(grid.getItemsShown(), runnable(() -> flow.rebuildCells())), onDispose);

		// TODO: remove (this fixes initial layout not showing content correctly, root of the problem is unknownm applyCss partially fixes the issue)
		SubscriptionKt.on(sync1IfInScene(grid, runnable(() ->
			runFX(millis(1000), () -> {
				flow.requestLayout();
			})
		)), onDispose);

		root = layHeaderTop(0, Pos.TOP_RIGHT, filterPane, flow.root);
		filter = new Filter(grid.type, grid.itemsFiltered);

		// selection
		flow.addEventHandler(KEY_PRESSED, e -> {
			KeyCode c = e.getCode();
			if (c.isNavigationKey()) {
				if (grid.selectOn.contains(SelectionOn.KEY_PRESS)) {
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
			} else if (c==ESCAPE && !e.isConsumed()) {
				if (selectedCI >= 0) {
					selectNone();
					e.consume();
				}
			}
		});
		flow.addEventHandler(MOUSE_CLICKED, e -> {
			if (grid.selectOn.contains(SelectionOn.MOUSE_CLICK)) selectNone();
			flow.requestFocus();
		});
		flow.addEventFilter(SCROLL, e -> {
			// Select hovered cell (if enabled)
			// Newly created cells that 'appear' right under mouse cursor will not receive hover event
			// Normally we would update the selection after the cells get updated, but that happens also on regular
			// selection change, which would stop working properly.
			// Hence we find such cells and select them here
			if (getSkinnable().selectOn.contains(SelectionOn.MOUSE_HOVER)) {
				flow.getCells()
					.filter(it -> it.isHover() && !it.isSelected()).findAny()
					.ifPresent(this::select);
			}
		});
	}

	private <O> void attach(ObservableValue<O> value, Consumer<? super O> action) {
		SubscriptionKt.on(UtilKt.attach(value, consumer(action)), onDispose);
	}

	// TODO: improve API
	public double getPosition() {
		return flow.getPosition();
	}

	// TODO: improve API
	public void setPosition(double position) {
		flow.scrollTo(position);
	}

	/* ---------- FILTER ------------------------------------------------------------------------------------------------ */

	// TODO: improve API
	public void requestFocus() {
		flow.requestFocus();
	}

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

	@SuppressWarnings("unchecked")
	private PredicateData<ObjectField<F,Object>> getPrimaryFilterPredicate() {
		return Optional.ofNullable(grid.primaryFilterField)
			.map((Function<ObjectField<F,?>,PredicateData<? extends ObjectField<F,?>>>) PredicateData::ofField)
			.map(f -> (PredicateData<ObjectField<F,Object>>) f)
			.orElse(null);
	}

	@SuppressWarnings("unchecked")
	private List<PredicateData<ObjectField<F,Object>>> getFilterPredicates(Class<F> filterType) {
		return stream(APP.getClassFields().get(filterType))
			.filter(ObjectField::isTypeFilterable)
			.map((Function<ObjectField<F,?>,PredicateData<? extends ObjectField<F,?>>>) PredicateData::ofField)
			.map(f -> (PredicateData<ObjectField<F,Object>>) f)
			.sorted(by(e -> e.name))
			.collect(toList());
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
		if (selectedC!=null) selectedC.updateSelected(false);
		grid.selectedItem.set(null);
		selectedC = null;
		selectedCI = NO_SELECT;
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
		if (i==NO_SELECT) throw new IllegalArgumentException("Illegal selection index " + NO_SELECT);

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
		selectedC.requestFocus();
		selectedC.updateSelected(true);
		grid.selectedItem.set(c.getItem());
		flow.requestFocus();
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
		@SuppressWarnings("unchecked")
		private List<GridCell<T,F>> visibleCells = (List) getChildren();
		private ArrayLinkedList<GridCell<T,F>> cachedCells = new ArrayLinkedList<>();
		private final Rectangle clipMask = new Rectangle(0, 0);
		private final FlowScrollBar scrollbar;
		private final Scrolled root;
		private double viewStart = 0;
		private boolean needsRebuildCells = true;
		private double scrollSpeedMultiplier = 3;

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
			GridView<T,F> grid = getSkinnable();
			GridCell<T,F> cell = grid.cellFactory.getValue().call(grid);
			cell.getGridView().set(grid);
			cell.addEventHandler(MOUSE_CLICKED, e -> {
				if (grid.selectOn.contains(SelectionOn.MOUSE_CLICK)) {
					getSkinnable().implGetSkin().select(cell);
					e.consume();
				}
			});
			cell.hoverProperty().addListener((o, ov, nv) -> {
				if (nv && grid.selectOn.contains(SelectionOn.MOUSE_HOVER))
					getSkinnable().implGetSkin().select(cell);

			});
			cell.pseudoClassStateChanged(Search.PC_SEARCH_MATCH, false);
			cell.pseudoClassStateChanged(Search.PC_SEARCH_MATCH_NOT, false);
			cell.setManaged(false);
			return cell;
		}

		void rebuildCells() {
			needsRebuildCells = true;
			requestLayout();
		}

		@Override
		protected void layoutChildren() {
			boolean wasFocused = isFocused();
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
			scrollbar.setVisible(computeMaxFullyVisibleCells()<=itemsAllCount);
			scrollbar.setMin(0);
			scrollbar.setMax(scrollableHeight);
			scrollbar.setVisibleAmount((viewHeight/(scrollableHeight + viewHeight))*(scrollableHeight));
			scrollbar.setValue(viewStart);
			scrollbar.updating = false;

			// update cells
			if (needsRebuildCells) {
				needsRebuildCells = false;
				int indexStart = computeMinVisibleCellIndex();
				int indexEnd = min(itemsAllCount - 1, computeMaxVisibleCellIndex());
				int itemCount = indexEnd - indexStart + 1;

				if (itemsAllCount==0) {
					visibleCells.forEach(cachedCells::addLast);
					visibleCells.clear();
					cachedCells.forEach(cell -> cell.update(-1, null, false));
				} else {
					failIf(indexStart>indexEnd);
					failIf(indexStart<0);
					failIf(indexEnd<0);
					failIf(itemsAllCount<=indexStart);
					failIf(itemsAllCount<=indexEnd);

					var visibleCellOld = visibleCells.stream().collect(toMap(c -> c.getIndex(), c -> c));
					var visibleCellsNew = rangeClosed(indexStart, indexEnd).mapToObj(i ->
						firstNotNull(
							// reuse visible cells to prevent needlessly update their content
							() -> {
								var c = visibleCellOld.remove(i);
								if (c!=null) failIf(c.getIndex()!=i);
								return c;
							},
							// reuse cached cells to prevent needlessly creating cells
							() -> {
								var c = cachedCells.removeLast();
								if (c!=null) c.updateIndex(i);
								return c;
							},
							// reuse cached cells to prevent needlessly creating cells
							() -> {
								var c = createCell();
								c.updateIndex(i);
								return c;
							}
						)
					).collect(toList());
					setTo(visibleCells, visibleCellsNew);
					failIf(visibleCells.size()!=itemCount);

					visibleCellOld.values().forEach(cell -> {
						cell.update(-1, null, false);
						cachedCells.addLast(cell);
					});
				}
			}

			int itemCount = visibleCells.size();
			if (itemCount>0) {
				// update cells
				double cellWidth = getSkinnable().cellWidth.getValue();
				double cellHeight = getSkinnable().cellHeight.getValue();
				int columns = computeMaxCellsInRow();
				double vGap = getSkinnable().verticalCellSpacing.getValue();
				double hGap = getSkinnable().cellGap.getValue()==CellGap.ABSOLUTE ? getSkinnable().verticalCellSpacing.getValue() :  (w - columns*cellWidth)/(columns + 1);
				double cellGapHeight = cellHeight + vGap;
				double cellGapWidth = cellWidth + hGap;
				double viewStartY = viewStart;
				int viewStartRI = computeMinVisibleRowIndex();
				int rowCount = computeVisibleRowCount();
				int i = 0;
				for (int rowI = viewStartRI; rowI<viewStartRI + rowCount; rowI++) {
					double rowStartY = rowI*cellGapHeight;
					double xPos = 0;
					double yPos = rowStartY - viewStartY;
					for (int cellI = rowI*columns; cellI<(rowI + 1)*columns; cellI++, i++) {
						if (i>=itemCount) break;	// last row may not be full

						var cell = getAt(i, visibleCells);
						var item = getAt(cellI, items);
						failIf(cell==null);
						failIf(cell.getIndex()!=cellI);
						failIf(item==null);

						cell.resizeRelocate(snapPositionX(xPos + hGap), snapPositionY(yPos + vGap), snapSpaceX(cellWidth), snapSpaceX(cellHeight));
						cell.update(cellI, item, cellI==skin.selectedCI);

						xPos += cellGapWidth;
					}
				}
			}
			if (wasFocused)
				requestFocus();
		}

		void dispose() {
			needsRebuildCells = false;
			visibleCells.forEach(c -> c.dispose());
			cachedCells.forEach(c -> c.dispose());
			visibleCells.clear();
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
				IndexedCell cell = getFirstVisibleCell();
				if (cell==null) return;
				int row = cell.getIndex()/computeMaxCellsInRow();
				scrollToRowBottom(row);
			}
			if (sign>0) {
				IndexedCell cell = getLastVisibleCell();
				if (cell==null) return;
				int row = cell.getIndex()/computeMaxCellsInRow();
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
				rebuildCells();
			}
		}

		public GridView<T,F> getSkinnable() {
			return skin.getSkinnable();
		}

		public double computeMaxViewStart() {
			return max(0, computeRowCount()*computeRowHeight() - getHeight());
		}

		public int computeMinVisibleRowIndex() {
			return (int) Math.floor(viewStart/computeRowHeight());
		}

		public int computeMinVisibleCellIndex() {
			return computeMaxCellsInRow()*computeMinVisibleRowIndex();
		}

		public int computeMaxVisibleRowIndex() {
			return (int) Math.floor((viewStart + getHeight())/computeRowHeight());
		}

		public int computeMaxVisibleCellIndex() {
			return computeMaxCellsInRow()*(computeMaxVisibleRowIndex() + 1) - 1;
		}

		public int computeMaxVisibleCells() {
			return computeMaxCellsInRow()*(int) Math.ceil((getHeight())/computeRowHeight());
		}

		public int computeAvgVisibleCells() {
			return computeMaxCellsInRow()*(int) Math.rint((getHeight())/computeRowHeight());
		}

		public int computeMaxFullyVisibleCells() {
			return computeMaxCellsInRow()*(int) Math.floor((getHeight())/computeRowHeight());
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
			return itemCount==0 ? 0 : (int) Math.ceil((double) itemCount/computeMaxCellsInRow());
		}

		public int computeVisibleRowCount() {
			return 1 + computeMaxVisibleRowIndex() - computeMinVisibleRowIndex();
		}

		/** @return the max number of cell per row */
		public int computeMaxCellsInRow() {
			double gap = getSkinnable().horizontalCellSpacing.doubleValue();
			return max((int) Math.floor((computeRowWidth() + gap)/computeCellWidth()), 1);
		}

		public int computeMaxRowsInView() {
			double gap = getSkinnable().verticalCellSpacing.doubleValue();
			return max((int) Math.floor((getSkinnable().getHeight() + gap)/computeRowHeight()), 1);
		}

		/** @return the width of a row (should be GridView.width - GridView.Scrollbar.width) */
		protected double computeRowWidth() {
			return getSkinnable().getWidth();
		}

		protected double computeRowHeight() {
			return getSkinnable().cellHeight.getValue() + getSkinnable().verticalCellSpacing.doubleValue();
		}

		/** @return the width of a cell */
		protected double computeCellWidth() {
			return getSkinnable().cellWidth.doubleValue() + getSkinnable().horizontalCellSpacing.doubleValue();
		}
	}

	/** Copy-paste of See {@link ArrayLinkedList}. */
	public static class ArrayLinkedList<T> extends AbstractList<T> {
		/**
		 * The array list backing this class. We default the size of the array
		 * list to be fairly large so as not to require resizing during normal
		 * use, and since that many ArrayLinkedLists won't be created it isn't
		 * very painful to do so.
		 */
		private final ArrayList<T> array;

		private int firstIndex = -1;
		private int lastIndex = -1;

		public ArrayLinkedList() {
			array = new ArrayList<>(50);

			for (int i = 0; i<50; i++) {
				array.add(null);
			}
		}

		public T getFirst() {
			return firstIndex==-1 ? null : array.get(firstIndex);
		}

		public T getLast() {
			return lastIndex==-1 ? null : array.get(lastIndex);
		}

		public void addFirst(T cell) {
			// if firstIndex == -1 then that means this is the first item in the
			// list and we need to initialize firstIndex and lastIndex
			if (firstIndex==-1) {
				firstIndex = lastIndex = array.size()/2;
				array.set(firstIndex, cell);
			} else if (firstIndex==0) {
				// we're already at the head of the array, so insert at position
				// 0 and then increment the lastIndex to compensate
				array.add(0, cell);
				lastIndex++;
			} else {
				// we're not yet at the head of the array, so insert at the
				// firstIndex - 1 position and decrement first position
				array.set(--firstIndex, cell);
			}
		}

		public void addLast(T cell) {
			// if lastIndex == -1 then that means this is the first item in the
			// list and we need to initialize the firstIndex and lastIndex
			if (firstIndex==-1) {
				firstIndex = lastIndex = array.size()/2;
				array.set(lastIndex, cell);
			} else if (lastIndex==array.size() - 1) {
				// we're at the end of the array so need to "add" so as to force
				// the array to be expanded in size
				array.add(++lastIndex, cell);
			} else {
				array.set(++lastIndex, cell);
			}
		}

		public int size() {
			return firstIndex==-1 ? 0 : lastIndex - firstIndex + 1;
		}

		public boolean isEmpty() {
			return firstIndex==-1;
		}

		public T get(int index) {
			if (index>(lastIndex - firstIndex) || index<0) {
				// Commented out exception due to RT-29111
				// throw new java.lang.ArrayIndexOutOfBoundsException();
				return null;
			}

			return array.get(firstIndex + index);
		}

		public void clear() {
			for (int i = 0; i<array.size(); i++) {
				array.set(i, null);
			}

			firstIndex = lastIndex = -1;
		}

		public T removeFirst() {
			if (isEmpty()) return null;
			return remove(0);
		}

		public T removeLast() {
			if (isEmpty()) return null;
			return remove(lastIndex - firstIndex);
		}

		public T remove(int index) {
			if (index>lastIndex - firstIndex || index<0) {
				throw new ArrayIndexOutOfBoundsException();
			}

			// if the index == 0, then we're removing the first
			// item and can simply set it to null in the array and increment
			// the firstIndex unless there is only one item, in which case
			// we have to also set first & last index to -1.
			if (index==0) {
				T cell = array.get(firstIndex);
				array.set(firstIndex, null);
				if (firstIndex==lastIndex) {
					firstIndex = lastIndex = -1;
				} else {
					firstIndex++;
				}
				return cell;
			} else if (index==lastIndex - firstIndex) {
				// if the index == lastIndex - firstIndex, then we're removing the
				// last item and can simply set it to null in the array and
				// decrement the lastIndex
				T cell = array.get(lastIndex);
				array.set(lastIndex--, null);
				return cell;
			} else {
				// if the index is somewhere in between, then we have to remove the
				// item and decrement the lastIndex
				T cell = array.get(firstIndex + index);
				array.set(firstIndex + index, null);
				for (int i = (firstIndex + index + 1); i<=lastIndex; i++) {
					array.set(i - 1, array.get(i));
				}
				array.set(lastIndex--, null);
				return cell;
			}
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
		private final Flow flow;
		private boolean adjusting;
		private boolean doNotAdjust;
		public boolean updating;

		public FlowScrollBar(Flow flow) {
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
			int direction = (int) Math.signum(newValue - oldValue);
			flow.scrollByPage(direction);
			adjusting = false;
		}
	}

	/** Table's filter node. */
	public class Filter extends FieldedPredicateChainItemNode<F,ObjectField<F,Object>> {

		@SuppressWarnings("unchecked")
		private Filter(Class<F> filterType, FilteredList<T> filterList) {
			super(THIS -> {
				var g = new FieldedPredicateItemNode<F,ObjectField<F,Object>>(in -> Functors.pool.getIO(in, Boolean.class), in -> Functors.pool.getPrefIO(in, Boolean.class));
				g.setPrefTypeSupplier(THIS.getPrefTypeSupplier());
				g.setData(THIS.getData());
				return g;
			});
			setPrefTypeSupplier(GridViewSkin.this::getPrimaryFilterPredicate);
			setData(getFilterPredicates(filterType));
			onItemChange = predicate -> filterList.setPredicate(item -> predicate.test(getSkinnable().filterByMapper.apply(item)));

			var filterKeyHandler = buildToggleOnKeyHandler(filterVisible, GridViewSkin.this.flow);
			getNode().addEventFilter(KEY_PRESSED, filterKeyHandler);
			getSkinnable().addEventHandler(KEY_PRESSED, filterKeyHandler); // filter would ignore first key stroke when filter turns visible
		}
	}

	private static boolean isInRange(int i, Collection<?> c) {
		return i>=0 && i<c.size();
	}

	private static <T> T getAt(int i, List<T> list) {
		return isInRange(i, list) ? list.get(i) : null;
	}
}