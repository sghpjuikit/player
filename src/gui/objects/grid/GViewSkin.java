package gui.objects.grid;

import gui.itemnode.FieldedPredicateChainItemNode;
import gui.itemnode.FieldedPredicateItemNode;
import gui.itemnode.FieldedPredicateItemNode.PredicateData;
import gui.objects.grid.GridView.Search;
import gui.objects.grid.GridView.SelectionOn;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import main.App;
import util.access.fieldvalue.ObjectField;
import util.functional.Functors;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static javafx.application.Platform.runLater;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static util.Util.*;
import static util.dev.Util.throwIf;
import static util.functional.Util.*;
import static util.graphics.Util.layHeaderTop;
import static util.reactive.Util.listChangeListener;
import static util.reactive.Util.maintain;

public class GViewSkin<T, F> implements Skin<GridView> {

	private final GridView<T,F> grid;
	private final Flow<T,F> flow;
	private final VBox root;
	private final StackPane filterPane = new StackPane();

	public GViewSkin(GridView<T,F> control) {
		this.grid = control;
		this.flow = new Flow<>(this);

		maintain(grid.cellHeightProperty(), e -> flow.changeItems());
		maintain(grid.cellWidthProperty(), e -> flow.changeItems());
		maintain(grid.horizontalCellSpacingProperty(), e -> flow.changeItems());
		maintain(grid.verticalCellSpacingProperty(), e -> flow.changeItems());
		maintain(grid.widthProperty(), e -> flow.changeItems());
		maintain(grid.heightProperty(), e -> flow.changeItems());
		maintain(grid.cellFactoryProperty(), e -> flow.changeItems());
		maintain(grid.parentProperty(), e -> {
			if (grid.getParent()!=null && grid.isVisible())
				grid.requestLayout();
		});
		grid.focusedProperty().addListener((o, ov, nv) -> {
			if (nv) flow.requestFocus();
		});
		grid.getItemsShown().addListener(listChangeListener(e -> flow.changeItems()));

		root = layHeaderTop(10, Pos.TOP_RIGHT, filterPane, flow.root);
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
				if (selectedCI>=0) {
					selectNone();
					e.consume();
				}
			}
		});
		flow.addEventHandler(MOUSE_CLICKED, e -> {
			if (grid.selectOn.contains(SelectionOn.MOUSE_CLICK))
				selectNone();
			flow.requestFocus();
		});

		flow.changeItems();
	}

	// TODO: improve API
	public double getPosition() {
		return flow.getPosition();
	}

	// TODO: improve API
	public void setPosition(double position) {
		flow.scrollTo(position);
	}

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
	public void dispose() {}

	public Stream<GridCell<T,F>> getCells() {
		return flow.getCells();
	}

	/**
	 * Returns the number of rows needed to display the whole set of cells
	 *
	 * @return GridView row count
	 */
	public int getItemCount() {
		final ObservableList<?> items = getSkinnable().getItemsShown();
		return items==null ? 0 : (int) Math.ceil((double) items.size()/flow.computeMaxCellsInRow());
	}

/* ---------- FILTER ------------------------------------------------------------------------------------------------ */

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
			if (!v) filter.clear();

			Node sn = filter.getNode();
			if (v) {
				if (!filterPane.getChildren().contains(sn))
					filterPane.getChildren().add(0, sn);
			} else {
				filterPane.getChildren().clear();
			}
			filterPane.setMaxHeight(v ? -1 : 0);
			filter.getNode().setVisible(v);

			// focus filter to allow user use filter asap
			if (v) runLater(filter::focus);
		}
	};

	/** Table's filter node. */
	public class Filter extends FieldedPredicateChainItemNode<F,ObjectField<F,Object>> {

		private Filter(Class<F> filterType, FilteredList<T> filterList) {
			super(THIS -> {
				FieldedPredicateItemNode<F,ObjectField<F,Object>> g = new FieldedPredicateItemNode<>(
						in -> Functors.pool.getIO(in, Boolean.class),
						in -> Functors.pool.getPrefIO(in, Boolean.class)
				);
				g.setPrefTypeSupplier(THIS.getPrefTypeSupplier());
				g.setData(THIS.getData());
				return g;
			});
			setPrefTypeSupplier(GViewSkin.this::getPrimaryFilterPredicate);
			setData(getFilterPredicates(filterType));
			growTo1();
			onItemChange = predicate -> filterList.setPredicate(item -> predicate.test(getSkinnable().filterByMapper.apply(item)));

			EventHandler<KeyEvent> filterKeyHandler = e -> {
				KeyCode k = e.getCode();
				// CTRL+F -> toggle filter
				if (k==KeyCode.F && e.isShortcutDown()) {
					filterVisible.set(!filterVisible.get());
					if (!filterVisible.get()) GViewSkin.this.flow.requestFocus();
					e.consume();
					return;
				}

				if (e.isAltDown() || e.isControlDown() || e.isShiftDown()) return;
				// ESC, filter not focused -> close filter
				if (k==ESCAPE) {
					if (filterVisible.get()) {
						if (isEmpty()) {
							filterVisible.set(false);
							GViewSkin.this.flow.requestFocus();
						} else {
							clear();
						}
						e.consume();
					}
				}
			};
			getNode().addEventFilter(KEY_PRESSED, filterKeyHandler);
			getSkinnable().addEventHandler(KEY_PRESSED, filterKeyHandler); // even filter would cause ignoring first key stroke when filter turns visible
		}
	}

	private PredicateData<ObjectField<F,Object>> getPrimaryFilterPredicate() {
		return Optional.ofNullable(getSkinnable().primaryFilterField)
				.map((Function<ObjectField<F,?>,PredicateData<? extends ObjectField<F,?>>>) PredicateData::ofField)
				.map(f -> (PredicateData<ObjectField<F,Object>>) f)
				.orElse(null);
	}

	private List<PredicateData<ObjectField<F,Object>>> getFilterPredicates(Class<F> filterType) {
		return stream(App.APP.classFields.get(filterType))
				.filter(ObjectField::isTypeStringRepresentable)
				.map((Function<ObjectField<F,?>,PredicateData<? extends ObjectField<F,?>>>) PredicateData::ofField)
				.map(f -> (PredicateData<ObjectField<F,Object>>) f)
				.sorted(by(e -> e.name))
				.toList();
	}

/* ---------- SELECTION --------------------------------------------------------------------------------------------- */

	private static final int NO_SELECT = Integer.MIN_VALUE;
	int selectedCI = NO_SELECT;
	private GridCell<T,F> selectedC = null;

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
		select(min(getSkinnable().getItemsShown().size() - 1, sel));
	}

	public void selectPageUp() {
		int sel = selectedCI - flow.computeAvgVisibleCells();
		select(max(0, sel));
	}

	public void selectPageDown() {
		int sel = selectedCI + flow.computeAvgVisibleCells();
		select(min(getSkinnable().getItemsShown().size() - 1, sel));
	}

	public void selectFirst() {
		select(0);
	}

	public void selectLast() {
		select(getSkinnable().getItemsShown().size() - 1);
	}

	public void selectNone() {
		if (selectedC!=null) selectedC.updateSelected(false);
		getSkinnable().selectedItem.set(null);
		selectedC = null;
		selectedCI = NO_SELECT;
	}

	public void select(GridCell<T,F> c) {
		if (c==null || c.getItem()==null) selectNone();
		else select(c.getIndex());
	}

	public void select(T item) {
		select(getSkinnable().getItemsShown().indexOf(item));
	}

	/** Select cell (and row it is in) at index. No-op if out of range. */
	public void select(int i) {
		if (i==NO_SELECT) throw new IllegalArgumentException("Illegal selection index " + NO_SELECT);

		int itemCount = getSkinnable().getItemsShown().size();
		int iMin = 0;
		int iMax = itemCount - 1;
		if (itemCount==0 || i==selectedCI || !isInRangeInc(i, iMin, iMax)) return;

		selectNone();

		// find index
		int rows = getItemCount();
		int cols = flow.computeMaxCellsInRow();
		int row = i/cols;

		if (row<0 || row>rows) return;

		// show row & cell to select
		flow.scrollToRow(row);

		runLater(() -> {
			// find row & cell to select
			GridCell<T,F> c = flow.getVisibleCellAtIndex(i);
			if (c==null) return;

			selectedCI = i;
			selectedC = c;
			selectedC.requestFocus();
			selectedC.updateSelected(true);
			getSkinnable().selectedItem.set(c.getItem());
			flow.requestFocus();
		});
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
		private final GViewSkin<T,F> skin;
		private List<GridCell<T,F>> visibleCells = (List) getChildren();
		private ArrayLinkedList<GridCell<T,F>> cachedCells = new ArrayLinkedList<>();
		private final Rectangle clipMask = new Rectangle(0, 0);
		private final FlowScrollBar scrollbar;
		private final Scrolled root;
		private double viewStart = 0;
		private boolean needsRebuildCells = false;

		public Flow(GViewSkin<T,F> skin) {
			this.skin = skin;

			// scrollbar
			scrollbar = new FlowScrollBar(this);
			scrollbar.setOrientation(Orientation.VERTICAL);
			scrollbar.setMin(0);
			scrollbar.setMin(1);
			scrollbar.setValue(0);
			scrollbar.addEventHandler(MouseEvent.ANY, Event::consume);
			scrollbar.setVisible(false);
			getChildren().add(scrollbar);

			// clip
			clipMask.setSmooth(false);
			setClip(clipMask);

			// scrolling
			double scrollSpeedMultiplier = 3;
			addEventFilter(ScrollEvent.SCROLL, e -> {
				scrollBy(-e.getDeltaY()*scrollSpeedMultiplier);
				e.consume();
			});

			// root
			root = new Scrolled(scrollbar, this);
		}

		public Stream<GridCell<T,F>> getCells() {
			return visibleCells.stream();
		}

		public double getPosition() {
			return viewStart;
		}

		void changeItems() {
			buildCells();
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
			GridCell<T,F> cell = grid.getCellFactory().call(grid);
			cell.gridView.set(grid);
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
			return cell;
		}

		protected void buildCells() {
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
			int indexStart = computeMinVisibleCellIndex();
			int indexEnd = min(itemsAllCount - 1, computeMaxVisibleCellIndex());
			int itemsVisibleCount = indexEnd - indexStart + 1;
			double virtualHeight = computeRowHeight()*computeRowCount();
			double viewHeight = getHeight();
			double scrollableHeight = virtualHeight;

			// update scrollbar
			scrollbar.updating = true;
			scrollbar.setVisible(computeMaxFullyVisibleCells()<=itemsAllCount);
			scrollbar.setMin(0);
			scrollbar.setMax(scrollableHeight);
			scrollbar.setValue(viewStart);
			scrollbar.setVisibleAmount(viewHeight);
			scrollbar.updating = false;

			// update cells
			if (needsRebuildCells) {
				needsRebuildCells = false;

				if (itemsAllCount==0) {
					visibleCells.forEach(cachedCells::addLast);
					visibleCells.clear();
				} else {
//					System.out.println(indexStart + " -> " + indexEnd + " " + itemsAllCount);
					throwIf(indexStart>indexEnd);
					throwIf(itemsAllCount<=indexStart);
					throwIf(itemsAllCount<=indexEnd);

					// If the cell count decreased, put the removed cells to cache
					for (int i = visibleCells.size() - 1; i>=itemsVisibleCount; i--) {
						GridCell<T,F> cell = visibleCells.remove(i);
//						cell.setItem(null);
						cell.updateIndex(-1);
						cell.updateSelected(false);
						cachedCells.addLast(cell);
					}
					throwIf(visibleCells.size()>itemsVisibleCount);

					// If the cell count increased, populate cells with new ones
					repeat(itemsVisibleCount - visibleCells.size(), () -> visibleCells.add(cachedCells.isEmpty() ? createCell() : cachedCells.removeLast()));
//					repeat(itemsVisibleCount-visibleCells.size(), () -> visibleCells.add(createCell()));
					throwIf(visibleCells.size()!=itemsVisibleCount);

//					int i = 0;
//					for (int cellI=indexStart; cellI<=indexEnd; cellI++, i++) {
//						GridCell<T,F> cell = visibleCells.get(i);
////						cell.setItem(null);
//						cell.updateIndex(-1);
////						cell.setItem(items.get(cellI));
//						cell.updateIndex(cellI);
////						cell.updateSelected(skin.selectedCI==cellI);
//					}
				}
			}

			int itemCount = visibleCells.size();
			if (itemCount==0) return;

			// update cells
//			visibleCells.forEach(c -> c.updateIndex(-1));
			double cellWidth = getSkinnable().getCellWidth();
			double cellHeight = getSkinnable().getCellHeight();
			int columns = computeMaxCellsInRow();
			double vGap = getSkinnable().getVerticalCellSpacing();
			double hGap = (w - columns*cellWidth)/(columns + 1);
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
//					if (i>=itemCount) break;	// last row may not be full
					GridCell<T,F> cell = getAt(i, visibleCells);
					if (cell!=null) {
						cell.resizeRelocate(
								snapPositionX(xPos + hGap),
								snapPositionY(yPos + vGap),
								snapSizeX(cellWidth),
								snapSizeY(cellHeight)
						);
						cell.updateIndex(cellI);
						cell.update(getAt(cellI, items), cellI==skin.selectedCI);
					}
					xPos += cellGapWidth;
				}
			}

			if (wasFocused) {
				requestLayout();
			}
		}

		@SuppressWarnings("unused")
		private void scrollToRow(int row) {
			GridCell<T,F> fvc = getFirstVisibleCell();
			GridCell<T,F> lvc = getLastVisibleCell();

			boolean isNoRow = fvc==null && lvc==null;
			if (isNoRow) return;

			int rowSize = computeMaxCellsInRow();
			int fvr = fvc.getIndex()/rowSize;
			int lvr = lvc.getIndex()/rowSize;
			boolean isUp = row<=fvr;
			boolean isDown = row>=lvr;
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
				buildCells();
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
			return (int) Math.ceil((viewStart + getHeight())/computeRowHeight());
		}

		public int computeMaxVisibleCellIndex() {
			return -1 + computeMaxCellsInRow()*(computeMaxVisibleRowIndex() + 1);
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
			double gap = getSkinnable().horizontalCellSpacingProperty().doubleValue();
			return max((int) Math.floor((computeRowWidth() + gap)/computeCellWidth()), 1);
		}

		public int computeMaxRowsInView() {
			double gap = getSkinnable().verticalCellSpacingProperty().doubleValue();
			return max((int) Math.floor((getSkinnable().getHeight() + gap)/computeRowHeight()), 1);
		}

		/** @return the width of a row (should be GridView.width - GridView.Scrollbar.width) */
		protected double computeRowWidth() {
			return getSkinnable().getWidth();
		}

		protected double computeRowHeight() {
			return getSkinnable().getCellHeight() + getSkinnable().verticalCellSpacingProperty().doubleValue();
		}

		/** @return the width of a cell */
		protected double computeCellWidth() {
			return getSkinnable().cellWidthProperty().doubleValue() + getSkinnable().horizontalCellSpacingProperty().doubleValue();
		}
	}

	/** Copy-paste of See {@link javafx.scene.control.skin.VirtualFlow.ArrayLinkedList}. */
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
				double scrollbarW = scrollbar.getWidth();
				scrollbar.resizeRelocate(w - scrollbarW, 0, scrollbarW, h);
				content.resizeRelocate(0, 0, w - scrollbarW, h);
			} else {
				content.resizeRelocate(0, 0, w, h);
			}
		}
	}

	/** Scrollbar with adjusted scrolling behavior to work with {@link gui.objects.grid.GViewSkin.Flow}. */
	public static class FlowScrollBar extends ScrollBar {
		private final Flow flow;
		private boolean adjusting;
		public boolean updating;

		public FlowScrollBar(Flow flow) {
			this.flow = flow;

			valueProperty().addListener((o, ov, nv) -> {
				if (ov.doubleValue()!=nv.doubleValue() && !adjusting && !updating) {
					flow.scrollTo01(nv.doubleValue());
				}
			});
		}

		@Override
		public void decrement() {
			flow.scrollByRows(-1);
		}

		@Override
		public void increment() {
			flow.scrollByRows(1);
		}

		// Called when the user clicks the scrollbar track, we call the page-up and page-down
		@Override
		public void adjustValue(double pos) {
			adjusting = true;
			double oldValue = flow.getPosition();
			double newValue = getMin() + ((getMax() - getMin())*clip(0, pos, 1));
			int direction = (int) Math.signum(newValue - oldValue);
			flow.scrollByPage(direction);
			adjusting = false;
		}
	}
}