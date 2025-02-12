package sp.it.pl.ui.objects.table;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollToEvent;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.Nullable;
import sp.it.pl.ui.item_node.FieldedPredicateChainItemNode;
import sp.it.pl.ui.item_node.FieldedPredicateItemNode.PredicateData;
import sp.it.pl.ui.nodeinfo.TableInfo;
import sp.it.pl.ui.objects.contextmenu.MenuItemBoolean;
import sp.it.pl.ui.objects.icon.Icon;
import sp.it.pl.ui.objects.search.SearchAutoCancelable;
import sp.it.util.access.V;
import sp.it.util.access.fieldvalue.ColumnField.INDEX;
import sp.it.util.access.fieldvalue.ObjectField;
import sp.it.util.functional.TryKt;
import sp.it.util.ui.UiDelegateKt;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_MINUS;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static java.util.stream.Collectors.toMap;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.scene.control.TableColumn.SortType.ASCENDING;
import static javafx.scene.control.TableColumn.SortType.DESCENDING;
import static javafx.scene.input.KeyCode.A;
import static javafx.scene.input.KeyCode.CONTROL;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.F;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.stage.WindowEvent.WINDOW_HIDDEN;
import static javafx.stage.WindowEvent.WINDOW_SHOWING;
import static sp.it.pl.main.AppBuildersMenuKt.menuItemBool;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.ui.objects.table.TableViewExtensionsKt.computeMaxIndex;
import static sp.it.pl.ui.objects.table.TableViewExtensionsKt.rows;
import static sp.it.util.Util.digits;
import static sp.it.util.Util.zeroPad;
import static sp.it.util.async.AsyncKt.runLater;
import static sp.it.util.async.AsyncKt.runVT;
import static sp.it.util.dev.FailKt.failIf;
import static sp.it.util.dev.FailKt.noNull;
import static sp.it.util.functional.TryKt.runTry;
import static sp.it.util.functional.Util.IS;
import static sp.it.util.functional.Util.SAME;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.filter;
import static sp.it.util.functional.Util.with;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UtilKt.attach;
import static sp.it.util.reactive.UtilKt.attach1If;
import static sp.it.util.reactive.UtilKt.attachSize;
import static sp.it.util.reactive.UtilKt.map;
import static sp.it.util.reactive.UtilKt.onChange;
import static sp.it.util.reactive.UtilKt.sync;
import static sp.it.util.reactive.UtilKt.syncSize;
import static sp.it.util.text.StringExtensionsKt.keys;
import static sp.it.util.ui.TableViewSelectionModelExtensionsKt.clearAndSelect;
import static sp.it.util.ui.TableViewSelectionModelExtensionsKt.selectInverse;
import static sp.it.util.ui.TableViewSelectionModelExtensionsKt.selectNone;
import static sp.it.util.ui.UiDelegateKt.setUiDelegate;
import static sp.it.util.ui.Util.layHorizontally;
import static sp.it.util.ui.UtilKt.menuItem;

/**
 * Table with a search filter header that supports filtering with provided gui.
 */
public class FilteredTable<T> extends FieldedTable<T> {

	/** Initial filter criteria for the filter, used when filter is opened or additional filter added */
	public @Nullable ObjectField<T,?> primaryFilterField;
	private final ObservableList<T> allItems;
	private final FilteredList<T> filteredItems;
	private final ObservableList<T> sortedItems;
	private final ReadOnlyObjectWrapper<Boolean> itemsSortingWrapper = new ReadOnlyObjectWrapper<>(false);
	public final ReadOnlyObjectProperty<Boolean> itemsSorting = itemsSortingWrapper.getReadOnlyProperty();
	final VBox root = new VBox(this);

	/**
	 * @param type exact type of the item displayed in the table
	 * @param mainField to be chosen as main and default search field or null
	 */
	public FilteredTable(Class<T> type, @Nullable ObjectField<T,?> mainField) {
		this(type, mainField, observableArrayList());
	}

	/**
	 * @param type exact type of the item displayed in the table
	 * @param mainField field to determine primary filtering field and search column. Can be null. Initializes {@link
	 * sp.it.pl.ui.objects.table.FilteredTable.Search#field} and {@link #primaryFilterField}.
	 * @param backing_list non-null backing list of items to be displayed in the table
	 */
	public FilteredTable(Class<T> type, @Nullable ObjectField<T,?> mainField, ObservableList<T> backing_list) {
		super(type);

		setUiDelegate(root, this);

		var mf = computeMainField(mainField);
		allItems = noNull(backing_list);
		filteredItems = allItems.filtered(null);
		sortedItems = observableArrayList();
		itemsPredicate = filteredItems.predicateProperty();
		filteredItems.addListener(new ListChangeListener<T>() {
			@Override
			public void onChanged(Change<? extends T> c) {
				c.next();
				if (c.wasUpdated()) getSelectionModel().clearSelection();
				// without this, table would be empty
				// the sort can also throw IndexOutOfBounds exception for selection update when setAll
				// is called for non empty selection. It causes selection to be cleared, which is fine.
				// Catch is better than nothing
				runTry(() -> { sort(); return null; });
			}
		});

		var sortLock = new AtomicLong(0);
		var isInitialSort = new AtomicBoolean(true);
		setSortPolicy(it -> {
			var lock = sortLock.incrementAndGet();
			if (!isInitialSort.getAndSet(false)) {
				// TODO retain selection
//				if (getSortOrder().isEmpty()) {
//					itemsSortingWrapper.setValue(true);
//					updateComparator();
//					sortedItems.setAll(new ArrayList<>(filteredItems));
//					itemsSortingWrapper.setValue(false);
//				} else {
					itemsSortingWrapper.setValue(true);
					updateComparator();
					var fi = new ArrayList<>(filteredItems);
					var c = computeComparatorMemoized();

					// do async only if necessary
					if (c==SAME || fi.isEmpty()) {
						sortedItems.setAll(fi);
						itemsSortingWrapper.setValue(false);
					} else {
						runVT(() -> {
							if (lock!=sortLock.get()) return null;
							fi.sort(c);
							return null;
						}).ui(i -> {
							if (lock!=sortLock.get()) return null;
							sortedItems.setAll(fi);
							itemsSortingWrapper.setValue(false);
							return null;
						});
					}
//				}
			}
			return true;
		});

		setItems(sortedItems);
		VBox.setVgrow(this, ALWAYS);

		items_info.bind(this);

		// visually hint user menus are empty
		syncSize(menuAdd.getItems(), consumer(size -> menuAdd.setDisable(size==0)));
		syncSize(menuRemove.getItems(), consumer(size -> menuRemove.setDisable(size==0)));
		syncSize(menuSelected.getItems(), consumer(size -> menuSelected.setDisable(size==0)));
		syncSize(menuOrder.getItems(), consumer(size -> menuOrder.setDisable(size==0)));

		// searching
		search.setColumn(mf);
		searchQueryLabel.textProperty().bind(search.searchQuery);
		search.installOn(this);

		// filtering
		primaryFilterField = mf;
		filterPane = new Filter(filteredItems);
		filterPane.getNode().setVisible(false);
		var filterKeyHandler = filterPane.buildToggleOnKeyHandler(filterVisible, this);
		filterPane.getNode().addEventFilter(KEY_PRESSED, filterKeyHandler);
		addEventHandler(KEY_PRESSED, filterKeyHandler);  // filter would ignore first keystroke when filter turns visible

		// selecting
		addEventHandler(KEY_PRESSED, e -> {
			if (e.isConsumed()) return;
			if (e.getCode()==ESCAPE) {
				if (!getSelectionModel().isEmpty()) {
					getSelectionModel().clearSelection();
					e.consume();
				}
			}
		});

		addEventHandler(ScrollEvent.ANY, e -> {
			if (search.isActive())
				search.updateSearchStyles();
		});
		addEventHandler(KeyEvent.ANY, e -> {
			if (search.isActive())
				search.updateSearchStyles();
		});
		addEventHandler(ScrollToEvent.ANY, e -> {
			if (search.isActive())
				search.updateSearchStyles();
		});
		onChange(getItems(), runnable(() -> {
			if (search.isActive())
				search.updateSearchStyles();
		}));

		onChange(getItems(), runnable(() -> resizeIndexColumn()));
		footerVisible.set(true);

		initPlaceholder();
	}

	protected @Nullable ObjectField<T,?> computeMainField(@Nullable ObjectField<T,?> field) {
		return field;
	}

	/**
	 * The root is a container for this table and the filter. Use the root instead
	 * of this table when attaching it to the scene graph.
	 *
	 * @return the root of this table
	 */
	public VBox getRoot() {
		return root;
	}

	/**
	 * Return the items assigned to this table. Includes the filtered out items.
	 * <p/>
	 * This list can be modified, but it is recommended to use {@link #setItemsRaw(java.util.Collection)}
	 * to change the items in the table.
	 */
	public final ObservableList<T> getItemsRaw() {
		return allItems;
	}

	/**
	 * Sets items to the table. If any filter is in effect, it will be applied.
	 * <p/>
	 * Do not use {@link #setItems(javafx.collections.ObservableList)} or
	 * {@code getItems().setAll(new_items)}. It will cause the filters to stop
	 * working. The first replaces the table item list (instance of {@link FilteredList}),
	 * which must not happen. The second would throw an exception as FilteredList
	 * is not directly modifiable.
	 * </p>
	 * Because the sorting is asynchronous (on bgr thread),
	 * the changes to {@link #getItems()} and the like may not be applied when this method returns,
	 * see {@link #setItemsRaw(java.util.Collection, kotlin.jvm.functions.Function1)}.
	 */
	public void setItemsRaw(Collection<? extends T> items) {
		allItems.setAll(items);
	}

	/** {@link #setItemsRaw} with callback called after items are applied */
	public void setItemsRaw(Collection<? extends T> items, Function1<Unit, Unit> block) {
		setItemsRaw(items);
		attach1If(itemsSorting, it -> !it, it -> block.invoke(Unit.INSTANCE));
	}

	/**
	 * Maps the index of this list's filtered element to an index in the direct source list.
	 *
	 * @param index the index in filtered list of items visible in the table
	 * @return index in the unfiltered list backing this table
	 */
	public int getSourceIndex(int index) {
		return filteredItems.getSourceIndex(index);
	}

/* --------------------- TOP CONTROLS ------------------------------------------------------------------------------- */

	/** Filter pane in the top of the table. */
	public final Filter filterPane;
	/**
	 * Predicate that filters the table list. Null predicate will match all
	 * items (same as always true predicate). The value reflects the filter
	 * generated by the user through the {@link #filterPane}. Changing the
	 * predicate programmatically is possible, however the searchBox will not
	 * react on the change, its effect will merely be overridden and when
	 * search box predicate changes, it will in turn override effect of a
	 * custom predicate.
	 */
	public final ObjectProperty<Predicate<? super T>> itemsPredicate;
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
				runLater(() -> filterPane.focus());
				return;
			}

			super.set(v);

			Node sn = filterPane.getNode();
			if (v) {
				if (!root.getChildren().contains(sn))
					root.getChildren().addFirst(sn);
			} else {
				root.getChildren().remove(sn);
			}

			if (v) failIf(!filterPane.isEmpty());
			filterPane.convergeTo(v ? 1 : 0);

			filterPane.getNode().setVisible(v);

			// focus filter to allow user use filter asap
			if (v) runLater(() -> filterPane.focus());
		}
	};

/* --------------------- BOTTOM CONTROLS ---------------------------------------------------------------------------- */

	/**
	 * Visibility of the bottom controls and information panel.
	 * Displays information about table items and menu-bar.
	 */
	public final BooleanProperty footerVisible = new SimpleBooleanProperty(true) {
		@Override
		public void set(boolean v) {
			super.set(v);
			if (v) {
				if (!root.getChildren().contains(footerPane))
					root.getChildren().add(footerPane);
			} else {
				root.getChildren().remove(footerPane);
			}
		}
	};

	public final Menu menuAdd = new Menu("", new Icon(PLAYLIST_PLUS).scale(1.3).embedded());
	public final Menu menuRemove = new Menu("", new Icon(PLAYLIST_MINUS).scale(1.3).embedded());
	public final Menu menuSelected = new Menu("", new Icon(FontAwesomeIcon.CROP).embedded(),
		menuItem("Select all", null, keys(CONTROL, A), consumer(e -> getSelectionModel().selectAll())),
		menuItem("Select none", null, keys(ESCAPE), consumer(e -> selectNone(getSelectionModel()))),
		menuItem("Select inverse", null, consumer(e -> selectInverse(getSelectionModel())))
	);
	public final Menu menuOrder = new Menu("", new Icon(FontAwesomeIcon.SORT).embedded(),
		new Menu("Order by column", null,
			fields.stream()
				.filter(f -> f!=INDEX.INSTANCE)
				.map(f -> menuItem(f.name(), null, consumer(it ->
					getColumn(f).ifPresentOrElse(
						c -> {
							var sorts = new ArrayList<>(getSortOrder());
							var containedColumn = sorts.stream().anyMatch(cc -> cc == c);

							getSortOrder().clear();
							if (containedColumn) {
								if (c.getSortType()==ASCENDING) sorts.remove(c);
								c.setSortType(c.getSortType()==ASCENDING ? DESCENDING : ASCENDING);
							} else {
								c.setSortType(DESCENDING);
								sorts.add(c);
							}
							getSortOrder().setAll(sorts);
						},
						() -> sortBy(f)
					)
				)))
				.toArray(MenuItem[]::new)
		),
		menuItem("Order reverse", null, consumer(it -> sortReverse())),
		menuItem("Order randomly", null, consumer(it -> sortRandomly()))
	);
	public final Menu menuColumns = new Menu("", new Icon(FontAwesomeIcon.NAVICON).embedded().onClickDo(consumer(it -> columnMenu.show(this, Side.RIGHT, 0.0, 0.0))));
	public final Menu menuExtra = new Menu("", new Icon(FontAwesomeIcon.TASKS).embedded(),
		menuItemBool("Show filter", filterVisible, keys(CONTROL, F)),
		menuItemBool("Show header", headerVisible, null),
		menuItemBool("Show footer", footerVisible, null)
	);
	/** Table menu-bar in the bottom with menus. Feel free to modify. */
	public final MenuBar menus = new MenuBar(menuAdd, menuRemove, menuSelected, menuOrder, menuColumns, menuExtra);
	/**
	 * Labeled in the bottom displaying information on table items and selection.
	 * Feel free to provide custom implementation of {@link TableInfo#setTextFactory(kotlin.jvm.functions.Function2)}
	 * to display different information. You may want to reuse
	 * {@link sp.it.pl.ui.nodeinfo.TableInfo.Companion#getDEFAULT_TEXT_FACTORY()}.
	 */
	public final TableInfo<T> items_info = new TableInfo<>(new Label()); // can not bind here as table items list not ready
	private final Label searchQueryLabel = new Label();
	/**
	 * Pane for controls in the bottom of the table.
	 * Feel free to modify its content. Menu-bar and item info label are on the
	 * left {@link BorderPane#leftProperty()}. Search query label is on the right {@link BorderPane#rightProperty()}.
	 * Both wrapped in {@link HBox};
	 */
	public final HBox footerPane = with(new HBox(menus), footer -> {
		footer.setAlignment(CENTER_LEFT);
		footer.getStyleClass().add("table-view-footer");
		sync(map(searchQueryLabel.textProperty(), it -> it==null || it.isBlank()), consumer(it -> {
			if (it) footer.getChildren().setAll(menus, items_info.getNode());
			else footer.getChildren().setAll(menus, searchQueryLabel);
		}));
	});

	/** Table's filter node. */
	public class Filter extends FieldedPredicateChainItemNode<T,ObjectField<T,Object>> {

		public Filter(FilteredList<T> filterList) {
			super();
			setPrefTypeSupplier(FilteredTable.this::getPrimaryFilterPredicate);
			onItemChange = predicate -> filterList.setPredicate(predicate==IS ? null : predicate);
			setData(getFilterPredicates());
		}
	}

	@SuppressWarnings({"unchecked", "SimplifyOptionalCallChains"})
	private PredicateData<ObjectField<T,Object>> getPrimaryFilterPredicate() {
		return Optional.ofNullable(primaryFilterField)
			.map((Function<ObjectField<T,?>,PredicateData<? extends ObjectField<T,?>>>) PredicateData.Companion::ofField)
			.map(f -> (PredicateData<ObjectField<T,Object>>) f)
			.orElse(null);
	}

	@SuppressWarnings("unchecked")
	private List<PredicateData<ObjectField<T,Object>>> getFilterPredicates() {
		return computeFieldsAll().stream()
			.filter(ObjectField::isTypeFilterable)
			.map(PredicateData.Companion::ofField)
			.sorted(by(e -> e.name()))
			.map(f -> (PredicateData<ObjectField<T,Object>>) (Object) f)
			.toList();
	}

/* --------------------- INDEX -------------------------------------------------------------------------------------- */

	/**
	 * Shows original item's index in the unfiltered list when filter is on.
	 * False will display index within filtered list. In other words false
	 * will cause items to always be indexed from 1 to items.size. Only has
	 * effect when filter is in effect.
	 */
	public final BooleanProperty showOriginalIndex = new SimpleBooleanProperty(true);

	/**
	 * Indexes range from 1 to n, where n can differ when filter is applied.
	 * Equivalent to: {@code isShowOriginalIndex ? getItemsRaw().size() : getItems().size(); }
	 *
	 * @return max index
	 */
	@Override
	public final int getMaxIndex() {
		return showOriginalIndex.get() ? allItems.size() : getItems().size();
	}

/* --------------------- SEARCH ------------------------------------------------------------------------------------- */

	/**
	 * Item search. Has no graphics.
	 */
	public final Search search = new Search();

	public class Search extends SearchAutoCancelable {
		/**
		 * If the user types text to quick search content by scrolling table, the
		 * text matching will be done by this field. Its column cell data must be
		 * String (or search will be ignored) and column should be visible.
		 */
		private @Nullable ObjectField<T,?> field;
		/**
		 * Menu item for displaying and selecting {link {@link #field}}.
		 */
		public final Menu menu = new Menu("Search column");

		{
			columnMenu.getItems().add(menu);
			columnMenu.addEventHandler(WINDOW_HIDDEN, e -> menu.getItems().clear());
			columnMenu.addEventHandler(WINDOW_SHOWING, e -> menu.getItems().addAll(
				MenuItemBoolean.Companion.buildSingleSelectionMenu(filter(fieldsAll, ObjectField::searchSupported), field, ObjectField::name, this::setColumn)
			));
		}

		@Override
		public void doSearch(String query) {
			if (field==null) return;
			APP.getActionStream().invoke("Table search");
			Function1<? super T,Boolean> matcher = field.searchMatch(itemS -> isMatchNth(itemS, query));
			for (int i = 0; i<getItems().size(); i++) {
				T item = getItems().get(i);
				boolean isMatch = item!=null && matcher.invoke(item);
				if (isMatch) {
					scrollToCenter(i);
					updateSearchStyles();
					getSelectionModel().clearAndSelect(i);
					break;
				}
			}
		}

		/** Sets fields to be used in search. Default is main field. */
		public void setColumn(@Nullable ObjectField<T,?> field) {
			this.field = field;
		}

		@Override
		public void cancel() {
			super.cancel();
			updateSearchStyleRowsNoReset();
		}

		private void updateSearchStyles() {
			if (SearchAutoCancelable.Companion.isCancelable().getValue())
				searchAutoCanceller.start(SearchAutoCancelable.Companion.getCancelActivityDelay().getValue());
			updateSearchStyleRowsNoReset();
		}

		private void updateSearchStyleRowsNoReset() {
			if (field==null) return;
			boolean searchOn = isActive();
			for (TableRow<T> row : rows(FilteredTable.this)) {
				T item = row.getItem();
				String itemS = item==null ? null : field.getOfS(item, "");
				boolean isMatch = itemS!=null && isMatch(itemS, searchQuery.get());
				row.pseudoClassStateChanged(PC_SEARCH_MATCH, searchOn && isMatch);
				row.pseudoClassStateChanged(PC_SEARCH_MATCH_NOT, searchOn && !isMatch);
			}
		}

	}

/* --------------------- SORT --------------------------------------------------------------------------------------- */

	@Override
	public void sortBy(ObjectField<T,?> field) {
		getSortOrder().clear();
		allItems.sort(field.comparatorNNL());
	}

	/**
	 * Sorts items using provided comparator. Any sort order is cleared.
	 * @param comparator - the Comparator used to compare list elements. A null value indicates that the
	 *                   elements' natural ordering should be used
	 */
	public void sort(Comparator<T> comparator) {
		getSortOrder().clear();
		allItems.sort(comparator);
	}

	public void sortRandomly() {
		getSortOrder().clear();
		FXCollections.shuffle(allItems);
	}

	public void sortReverse() {
		if (getSortOrder().isEmpty()) {
			FXCollections.reverse(allItems);
		} else {
			var sorts = getSortOrder().stream().collect(toMap(it -> it, it -> it.getSortType(), (x, y) -> y, LinkedHashMap::new));
			getSortOrder().clear();
			sorts.forEach((c, s) -> c.setSortType(s==ASCENDING ? DESCENDING : ASCENDING));
			getSortOrder().setAll(sorts.keySet());
		}
	}

	public boolean isSortEmpty() {
		return itemsComparator.getValue()==SAME && getSortOrder().isEmpty();
	}

	public void sortMaterialize() {
		if (!isSortEmpty()) {
			var selections = new ArrayList<>(getSelectionModel().getSelectedIndices());
			var items = new ArrayList<>(sortedItems);
			getSelectionModel().clearSelection();
			getSortOrder().clear();
			setItemsRaw(items);
			clearAndSelect(getSelectionModel(), selections);
		}
	}

/* --------------------- PLACEHOLDER -------------------------------------------------------------------------------- */

	public final V<Node> placeholderNode = new V<>(new Label("No content"));
	private final Node noPlaceholderNode = new Label("");

	private void initPlaceholder() {
		attach(placeholderNode, p -> {
			updatePlaceholder(getItemsRaw().size());
			return Unit.INSTANCE;
		});
		attachSize(getItemsRaw(), size -> {
			updatePlaceholder(size);
			return Unit.INSTANCE;
		});
		updatePlaceholder(getItemsRaw().size());
	}

	private void updatePlaceholder(int itemsCount) {
		setPlaceholder(itemsCount!=0 ? noPlaceholderNode : placeholderNode.getValue());
	}

/* --------------------- HELPER ------------------------------------------------------------------------------------- */

	@Override
	protected Callback<TableColumn<T,Void>,TableCell<T,Void>> buildIndexColumnCellFactory() {
		return column -> new IndexTableCell<>();
	}

	private class IndexTableCell<S> extends TableCell<S,Void> {
		{
			setAlignment(CENTER_RIGHT);
		}

		@Override
		protected void updateItem(Void item, boolean empty) {
			super.updateItem(item, empty);
			if (empty) {
				setText(null);
			} else {
				var j = getIndex();
				var i = showOriginalIndex.getValue() ? filteredItems.getSourceIndex(j) : j;
				var txt = zeropadIndex.getValue() ? zeroPad(i + 1, digits(computeMaxIndex(getTableView())), '0') : String.valueOf(i + 1);
				setText(txt + ".");
			}
		}
	}
}