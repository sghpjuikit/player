/*
 * Copyright (c) 2013, 2015 ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package gui.objects.grid;

import gui.itemnode.FieldedPredicateChainItemNode;
import gui.itemnode.FieldedPredicateItemNode;
import gui.itemnode.FieldedPredicateItemNode.PredicateData;
import gui.objects.grid.GridView.SelectionOn;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import main.App;
import one.util.streamex.IntStreamEx;
import util.access.fieldvalue.ObjectField;
import util.functional.Functors;
import util.type.Util;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static javafx.application.Platform.runLater;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static util.Util.isInRangeInc;
import static util.functional.Util.by;
import static util.functional.Util.stream;
import static util.graphics.Util.layHeaderTop;
import static util.type.Util.invokeMethodP0;
import static util.type.Util.invokeMethodP1;

public class GridViewSkin<T, F> implements Skin<GridView> {

	private final SkinDelegate skin;
	private final VBox root;
	private final StackPane filterPane = new StackPane();

	public GridViewSkin(GridView<T,F> control) {
		skin = new SkinDelegate(control);

		skin.flow.setId("virtual-flow");
		skin.flow.setPannable(false);
		skin.flow.setVertical(true);
		skin.flow.focusTraversableProperty().bind(control.focusTraversableProperty());
		skin.flow.setCellFactory(f -> createCell());
		control.focusedProperty().addListener((o, ov, nv) -> {
			if (nv) getFlow().requestFocus();
		});

		root = layHeaderTop(10, Pos.TOP_RIGHT, filterPane, skin.flow);
		filter = new Filter(control.type, control.itemsFiltered);

		ListChangeListener<T> itemsListener = change -> {
			if (change.next())
				updateGridViewItems();
		};
		//        WeakListChangeListener<T> weakGridViewItemsListener = new WeakListChangeListener<>(itemsListener);
		getSkinnable().getItemsShown().addListener(itemsListener);
		//        getSkinnable().getItemsRaw().addListener(itemsListener);
		//        getSkinnable().itemsFiltered.predicateProperty().addListener(p -> weakGridViewItemsListener.onChanged(null));

		updateGridViewItems();

		// selection
		skin.flow.addEventHandler(KEY_PRESSED, e -> {
			KeyCode c = e.getCode();
			if (c.isNavigationKey()) {
				if (control.selectOn.contains(SelectionOn.KEY_PRESS)) {
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
		skin.flow.addEventHandler(MOUSE_CLICKED, e -> {
			if (control.selectOn.contains(SelectionOn.MOUSE_CLICK))
				selectNone();
		});
	}

	private VirtualFlow<GridRow<T,F>> getFlow() {
		return skin.flow;
	}

	// TODO: improve API
	public double getPosition() {
		return skin.flow.getPosition();
	}

	// TODO: improve API
	public void setPosition(double position) {
		skin.flow.setPosition(position);
	}

	// TODO: improve API
	public void requestFocus() {
		skin.flow.requestFocus();
	}

	@Override
	public GridView<T,F> getSkinnable() {
		return skin.getSkinnable();
	}

	@Override
	public Node getNode() {
		return root;
	}

	@Override
	public void dispose() {
		skin.dispose();
	}

	private void updateGridViewItems() {
		flowRecreateCells();
		updateRowCount();
		getSkinnable().requestLayout();
		selectNone();
	}

	void updateRowCount() {
		if (skin.flow==null)
			return;

		int oldCount = skin.flow.getCellCount();
		int newCount = getItemCount();

		if (newCount!=oldCount) {
			skin.flow.setCellCount(newCount);
			flowRebuildCells();
		} else {
			flowReconfigureCells();
		}
		updateRows();
	}

	private GridRow<T,F> createCell() {
		GridRow<T,F> row = new GridRow<>();
		row.setGridView(getSkinnable());
		invokeMethodP1(row, "setEmpty", boolean.class, false);    // in the hopes it will fix cell updating by the flow
		return row;
	}

	public Stream<GridCell<T,F>> getCells() {
		GridRow from = getFlow().getFirstVisibleCell();
		GridRow to = getFlow().getLastVisibleCell();
		if (from==null || to==null) return stream();
		int fromI = from.getIndex();
		int toI = to.getIndex();
		if (fromI<0 || toI<0) return stream();
		return IntStreamEx.rangeClosed(fromI, toI).mapToObj(i -> getFlow().getCell(i)).flatMap(r -> r.getSkinImpl().getCells());
	}

	/**
	 * Returns the number of rows needed to display the whole set of cells
	 *
	 * @return GridView row count
	 */
	public int getItemCount() {
		final ObservableList<?> items = getSkinnable().getItemsShown();
		return items==null ? 0 : (int) Math.ceil((double) items.size()/computeMaxCellsInRow());
	}

	/**
	 * Returns the max number of cell per row
	 *
	 * @return Max cell number per row
	 */
	public int computeMaxCellsInRow() {
		double gap = getSkinnable().horizontalCellSpacingProperty().doubleValue();
		return max((int) Math.floor((computeRowWidth() + gap)/computeCellWidth()), 1);
	}

	public int computeMaxRowsInGrid() {
		double gap = getSkinnable().verticalCellSpacingProperty().doubleValue();
		return max((int) Math.floor((getSkinnable().getHeight() + gap)/computeRowHeight()), 1);
	}

	/**
	 * Returns the width of a row
	 * (should be GridView.width - GridView.Scrollbar.width)
	 *
	 * @return Computed width of a row
	 */
	protected double computeRowWidth() {
		return getSkinnable().getWidth() - getVScrollbarWidth();
	}

	protected double computeRowHeight() {
		return getSkinnable().getCellHeight() + getSkinnable().verticalCellSpacingProperty().doubleValue();
	}

	/**
	 * Returns the width of a cell
	 *
	 * @return Computed width of a cell
	 */
	protected double computeCellWidth() {
		return getSkinnable().cellWidthProperty().doubleValue() + getSkinnable().horizontalCellSpacingProperty().doubleValue();
	}

	protected double getVScrollbarWidth() {
		if (skin.flow!=null) {
			Object virtualScrollBar = Util.getFieldValue(skin.flow, "vbar"); // VirtualScrollBar.class
			// return virtualScrollBar!=null && virtualScrollBar.isVisible() ? virtualScrollBar.getWidth() : 0;
			boolean isVisible = virtualScrollBar!=null && (boolean) invokeMethodP0(virtualScrollBar, "isVisible");
			return isVisible ? (double) invokeMethodP0(virtualScrollBar, "getWidth") : 0;
		}
		return 0;
	}

	protected void updateRows() {
		boolean isAnyVisible = skin.flow.getFirstVisibleCell()!=null;
		if (!isAnyVisible) return;
		int indexStart = skin.flow.getFirstVisibleCell().getIndex();
		int indexEnd = skin.flow.getLastVisibleCell().getIndex();
		int i = 0;
		for (int rowI = indexStart; rowI<=indexEnd; rowI++, i++) {
			GridRow<T,F> row = skin.flow.getVisibleCell(i);
			if (row!=null) {
				row.updateIndex(rowI);
			}
		}
	}

	protected boolean areRowsVisible() {
		return skin.flow!=null && skin.flow.getFirstVisibleCell()!=null && skin.flow.getLastVisibleCell()!=null;
	}

	private void flowRecreateCells() {
		invokeMethodP0(skin.flow, "recreateCells");
	}

	private void flowRebuildCells() {
		invokeMethodP0(skin.flow, "rebuildCells");
	}

	private void flowReconfigureCells() {
		invokeMethodP0(skin.flow, "reconfigureCells");
	}

	private class SkinDelegate extends CustomVirtualContainerBase<GridView<T,F>,GridRow<T,F>> {
		public SkinDelegate(GridView<T,F> control) {
			super(control);

			flow.fixedCellSizeProperty().bind(control.cellHeightProperty().add(5));    // TODO: make configurable
//            registerChangeListener(control.cellHeightProperty(), e -> flowRecreateCells());
			registerChangeListener(control.cellWidthProperty(), e -> { updateRowCount(); flowRecreateCells(); });
			registerChangeListener(control.horizontalCellSpacingProperty(), e -> {
				updateRowCount(); flowRecreateCells();
			});
			registerChangeListener(control.verticalCellSpacingProperty(), e -> flowRecreateCells());
			registerChangeListener(control.widthProperty(), e -> updateRowCount());
			registerChangeListener(control.heightProperty(), e -> updateRowCount());
			registerChangeListener(control.cellFactoryProperty(), e -> flowRecreateCells());
			registerChangeListener(control.parentProperty(), e -> {
				if (getSkinnable().getParent()!=null && getSkinnable().isVisible())
					// getSkinnable().requestLayout();
					GridViewSkin.this.getSkinnable().requestLayout();
			});
		}

		@Override
		void updateRowCount() {
			GridViewSkin.this.updateRowCount();
		}
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
			setPrefTypeSupplier(GridViewSkin.this::getPrimaryFilterPredicate);
			setData(getFilterPredicates(filterType));
			growTo1();
			onItemChange = predicate -> filterList.setPredicate(item -> predicate.test(getSkinnable().filterByMapper.apply(item)));

			EventHandler<KeyEvent> filterKeyHandler = e -> {
				KeyCode k = e.getCode();
				// CTRL+F -> toggle filter
				if (k==KeyCode.F && e.isShortcutDown()) {
					filterVisible.set(!filterVisible.get());
					if (!filterVisible.get()) GridViewSkin.this.skin.flow.requestFocus();
					e.consume();
					return;
				}

				if (e.isAltDown() || e.isControlDown() || e.isShiftDown()) return;
				// ESC, filter not focused -> close filter
				if (k==ESCAPE) {
					if (filterVisible.get()) {
						if (isEmpty()) {
							filterVisible.set(false);
							GridViewSkin.this.skin.flow.requestFocus();
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
		int sel = selectedCI - computeMaxCellsInRow();
		select(max(0, sel));
	}

	public void selectDown() {
		int sel = selectedCI + computeMaxCellsInRow();
		select(min(getSkinnable().getItemsShown().size() - 1, sel));
	}

	public void selectPageUp() {
		int sel = selectedCI - computeMaxRowsInGrid()*computeMaxCellsInRow();
		select(max(0, sel));
	}

	public void selectPageDown() {
		int sel = selectedCI + computeMaxRowsInGrid()*computeMaxCellsInRow();
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
		if (skin.flow==null) return;
		if (i==NO_SELECT) throw new IllegalArgumentException("Illegal selection index " + NO_SELECT);

		int itemCount = getSkinnable().getItemsShown().size();
		int iMin = 0;
		int iMax = itemCount - 1;
		if (itemCount==0 || i==selectedCI || !isInRangeInc(i, iMin, iMax)) return;

		selectNone();

		// find index
		int rows = getItemCount();
		int cols = computeMaxCellsInRow();
		int row = i/cols;
		int col = i%cols;

		if (row<0 || row>rows) return;

		// show row & cell to select
		scrollTo(row);

		// find row & cell to select
		GridRow<T,F> r = skin.flow.getCell(row);
		if (r==null) return;
		GridCell<T,F> c = r.getSkinImpl().getCellAtIndex(col);
		if (c==null) return;

		selectedCI = i;
		selectedC = c;
		selectedC.requestFocus();
		selectedC.updateSelected(true);
		getSkinnable().selectedItem.set(c.getItem());
	}

	@SuppressWarnings("unused")
	private void scrollTo(int row) {
		GridRow<T,F> fvr = skin.flow.getFirstVisibleCell();
		GridRow<T,F> lvr = skin.flow.getLastVisibleCell();
		boolean isUp = row<=fvr.getIndex();
		boolean isDown = row>=lvr.getIndex();
		if (fvr.getIndex()>=row || row>=lvr.getIndex()) {
			if (isUp) skin.flow.scrollToTop(row);
			else scrollToBottom(row);
		}
	}

	private void scrollToBottom(int row) {
		GridRow lvr = skin.flow.getLastVisibleCell();
		if (lvr==null) return;    // TODO: this should not be necessary
		double rowBy = (row - lvr.getIndex())*skin.flow.getFixedCellSize();
		double cellBy = lvr.getLayoutY() + skin.flow.getFixedCellSize() - (double) invokeMethodP0(skin.flow, "getViewportLength");
		double by = rowBy + cellBy;
		skin.flow.scrollPixels(by);
	}

}