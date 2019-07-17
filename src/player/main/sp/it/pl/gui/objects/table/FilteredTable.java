package sp.it.pl.gui.objects.table;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import sp.it.pl.gui.itemnode.FieldedPredicateChainItemNode;
import sp.it.pl.gui.itemnode.FieldedPredicateItemNode;
import sp.it.pl.gui.itemnode.FieldedPredicateItemNode.PredicateData;
import sp.it.pl.gui.nodeinfo.TableInfo;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.search.SearchAutoCancelable;
import sp.it.util.access.V;
import sp.it.util.access.fieldvalue.ObjectField;
import sp.it.util.functional.Functors;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_MINUS;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static java.util.stream.Collectors.toList;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.stage.WindowEvent.WINDOW_HIDDEN;
import static javafx.stage.WindowEvent.WINDOW_SHOWING;
import static sp.it.pl.gui.objects.contextmenu.SelectionMenuItem.buildSingleSelectionMenu;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.Util.zeroPad;
import static sp.it.util.async.AsyncKt.runLater;
import static sp.it.util.dev.FailKt.noNull;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.filter;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UtilKt.attach;
import static sp.it.util.reactive.UtilKt.attachSize;
import static sp.it.util.reactive.UtilKt.onChange;
import static sp.it.util.reactive.UtilKt.syncSize;
import static sp.it.util.ui.Util.layHorizontally;
import static sp.it.util.ui.UtilKt.menuItem;

/**
 * Table with a search filter header that supports filtering with provided gui.
 */
public class FilteredTable<T> extends FieldedTable<T> {

	/** Initial filter criteria for the filter, used when filter is opened or additional filter added */
	public ObjectField<T,?> primaryFilterField;
	private final ObservableList<T> allItems;
	private final FilteredList<T> filteredItems;
	private final SortedList<T> sortedItems;
	final VBox root = new VBox(this);

	/**
	 * @param type exact type of the item displayed in the table
	 * @param mainField to be chosen as main and default search field or null
	 */
	public FilteredTable(Class<T> type, ObjectField<T,?> mainField) {
		this(type, mainField, observableArrayList());
	}

	/**
	 * @param type exact type of the item displayed in the table
	 * @param mainField field to determine primary filtering field and search column. Can be null. Initializes {@link
	 * sp.it.pl.gui.objects.table.FilteredTable.Search#field} and {@link #primaryFilterField}.
	 * @param backing_list non null backing list of items to be displayed in the table
	 */
	public FilteredTable(Class<T> type, ObjectField<T,?> mainField, ObservableList<T> backing_list) {
		super(type);

		allItems = noNull(backing_list);
		filteredItems = new FilteredList<>(allItems);
		sortedItems = new SortedList<>(filteredItems);
		itemsPredicate = filteredItems.predicateProperty();

		setItems(sortedItems);
		sortedItems.comparatorProperty().bind(comparatorProperty());
		VBox.setVgrow(this, ALWAYS);

		items_info.bind(this);

		// visually hint user menus are empty
		syncSize(menuAdd.getItems(), consumer(size -> menuAdd.setDisable(size==0)));
		syncSize(menuRemove.getItems(), consumer(size -> menuRemove.setDisable(size==0)));
		syncSize(menuSelected.getItems(), consumer(size -> menuSelected.setDisable(size==0)));
		syncSize(menuOrder.getItems(), consumer(size -> menuOrder.setDisable(size==0)));

		// searching
		search.setColumn(mainField);
		searchQueryLabel.textProperty().bind(search.searchQuery);
		search.installOn(root);

		// filtering
		primaryFilterField = mainField;
		filterPane = new Filter(type, filteredItems);
		filterPane.getNode().setVisible(false);
		EventHandler<KeyEvent> filterKeyHandler = e -> {
			KeyCode k = e.getCode();
			// CTRL+F -> toggle filter
			if (k==KeyCode.F && e.isShortcutDown()) {
				filterVisible.set(!filterVisible.get());
				if (!filterVisible.get()) requestFocus();
				e.consume();
				return;
			}

			// ESC -> close filter
			if (e.getCode()==KeyCode.ESCAPE) {
				// clear & hide filter on single ESC
				// searchBox.clear();
				// setFilterVisible(false);

				// clear filter on 1st, hide on 2nd
				if (filterVisible.get()) {
					if (filterPane.isEmpty()) {
						filterVisible.set(false);
						requestFocus();
					} else {
						filterPane.clear();
					}
				} else {
					getSelectionModel().clearSelection();
				}
				e.consume();
			}
		};
		filterPane.getNode().addEventFilter(KEY_PRESSED, filterKeyHandler);
		root.addEventHandler(KEY_PRESSED, filterKeyHandler); // even filter would cause ignoring first key stroke when filter turns visible

		// TODO: fix the overkill
		root.addEventFilter(Event.ANY, e -> {
			if (search.isActive())
				search.updateSearchStyles();
		});
		onChange(getItems(), runnable(() -> {
			if (search.isActive())
				search.updateSearchStyles();
		}));

		onChange(getItems(), runnable(() -> {
			resizeIndexColumn();
		}));
		footerVisible.set(true);

		initPlaceholder();
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
	 * Return the items assigned to this this table. Includes the filtered out items.
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
	 * working. The first replaces the table item list (instance of {@link FilteredList},
	 * which must not happen. The second would throw an exception as FilteredList
	 * is not directly modifiable.
	 */
	public void setItemsRaw(Collection<? extends T> items) {
		allItems.setAll(items);
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
	 * also make it focused (to allow writing filter query immediatelly). If you
	 * wish for the filter to gain focus set this proeprty to true (focus will
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
					root.getChildren().add(0, sn);
			} else {
				root.getChildren().remove(sn);
			}

			if (v) filterPane.growTo1();
			else filterPane.shrinkTo(0);

			filterPane.getNode().setVisible(v);

			// focus filter to allow user use filter asap
			if (v) runLater(() -> filterPane.focus());
		}
	};

	/******************************* BOTTOM CONTROLS ******************************/

	public final Menu menuAdd = new Menu("", new Icon(PLAYLIST_PLUS).scale(1.3).embedded());
	public final Menu menuRemove = new Menu("", new Icon(PLAYLIST_MINUS).scale(1.3).embedded());
	public final Menu menuSelected = new Menu("", new Icon(FontAwesomeIcon.CROP).embedded(),
		menuItem("Select all", consumer(e -> selectAll())),
		menuItem("Select none", consumer(e -> selectNone())),
		menuItem("Select inverse", consumer(e -> selectInverse()))
	);
	public final Menu menuOrder = new Menu("", new Icon(FontAwesomeIcon.NAVICON).embedded());
	/** Table menubar in the bottom with menus. Feel free to modify. */
	public final MenuBar menus = new MenuBar(menuAdd, menuRemove, menuSelected, menuOrder);
	/**
	 * Labeled in the bottom displaying information on table items and selection.
	 * Feel free to provide custom implementation of {@link TableInfo#setTextFactory(kotlin.jvm.functions.Function2)}
	 * to display different information. You may want to reuse
	 * {@link TableInfo#DEFAULT_TEXT_FACTORY}.
	 */
	public final TableInfo<T> items_info = new TableInfo<>(new Label()); // can not bind here as table items list not ready
	private final Label searchQueryLabel = new Label();
	private final HBox bottomLeftPane = layHorizontally(5, CENTER_LEFT, menus, items_info.getNode());
	private final HBox bottomRightPane = layHorizontally(5, CENTER_RIGHT, searchQueryLabel);
	/**
	 * Pane for controls in the bottom of the table.
	 * Feel free to modify its content. Menubar and item info label are on the
	 * left {@link BorderPane#leftProperty()}. Search query label is on the right {@link BorderPane#rightProperty()}.
	 * Both wrapped in {@link HBox};
	 */
	public final BorderPane footerPane = new BorderPane(null, null, bottomRightPane, null, bottomLeftPane);

	/**
	 * Visibility of the bottom controls and information panel. Displays
	 * information about table items and menubar.
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

	/** Table's filter node. */
	public class Filter extends FieldedPredicateChainItemNode<T,ObjectField<T,Object>> {

		@SuppressWarnings("unchecked")
		public Filter(Class<T> filterType, FilteredList<T> filterList) {
			super(THIS -> {
				FieldedPredicateItemNode<T,ObjectField<T,Object>> g = new FieldedPredicateItemNode<>(
					in -> Functors.pool.getIO(in, Boolean.class),
					in -> Functors.pool.getPrefIO(in, Boolean.class)
				);
				g.setPrefTypeSupplier(THIS.getPrefTypeSupplier());
				g.setData(THIS.getData());
				return g;
			});
			maxChainLength.setValue(3);
			setPrefTypeSupplier(FilteredTable.this::getPrimaryFilterPredicate);
			onItemChange = filterList::setPredicate;
			setData(getFilterPredicates(filterType));
		}
	}

	@SuppressWarnings("unchecked")
	private PredicateData<ObjectField<T,Object>> getPrimaryFilterPredicate() {
		return Optional.ofNullable(primaryFilterField)
			.map((Function<ObjectField<T,?>,PredicateData<? extends ObjectField<T,?>>>) PredicateData::ofField)
			.map(f -> (PredicateData<ObjectField<T,Object>>) f)
			.orElse(null);
	}

	@SuppressWarnings("unchecked")
	private List<PredicateData<ObjectField<T,Object>>> getFilterPredicates(Class<T> filterType) {
		return stream(APP.classFields.get(filterType))
			.filter(ObjectField::isTypeFilterable)
			.map((Function<ObjectField<T,?>,PredicateData<? extends ObjectField<T,?>>>) PredicateData::ofField)
			.sorted(by(e -> e.name))
			.map(f -> (PredicateData<ObjectField<T,Object>>) f)
			.collect(toList());
	}

/* --------------------- INDEX -------------------------------------------------------------------------------------- */

	/**
	 * Shows original item's index in the unfiltered list when filter is on.
	 * False will display index within filtered list. In other words false
	 * will cause items to always be indexed from 1 to items.size. This has only
	 * effect when filter is in effect.
	 */
	public final BooleanProperty showOriginalIndex = new SimpleBooleanProperty(true) {
		@Override
		public void set(boolean v) {
			super.set(v);
			refreshColumn(columnIndex);
		}
	};

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
		private ObjectField<T,?> field;
		/**
		 * Menu item for displaying and selecting {link {@link #field}}.
		 */
		private Menu menu = new Menu("Search column");

		{
			columnMenu.getItems().add(menu);
			columnMenu.addEventHandler(WINDOW_HIDDEN, e -> menu.getItems().clear());
			columnMenu.addEventHandler(WINDOW_SHOWING, e -> menu.getItems().addAll(
				buildSingleSelectionMenu(filter(getFieldsAll(), ObjectField::searchSupported), field, ObjectField::name, this::setColumn)
			));
		}

		@Override
		public void doSearch(String query) {
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
		public void setColumn(ObjectField<T,?> field) {
			// TODO make sure this is always safe
			// Can not enforce this, because some Fields do not exactly specify their type, e.g., return Object.class
			// because they are dynamic, this would all be easy if Fields were not implemented as Enum (for
			// convenience), this time it plays against us.
			// yes(field.getType()==String.class);
			this.field = field;
		}

		@Override
		public void cancel() {
			super.cancel();
			updateSearchStyleRowsNoReset();
		}

		private void updateSearchStyles() {
			if (SearchAutoCancelable.Companion.isCancelable()) searchAutoCanceller.start(SearchAutoCancelable.Companion.getCancelActivityDelay());
			updateSearchStyleRowsNoReset();
		}

		private void updateSearchStyleRowsNoReset() {
			boolean searchOn = isActive();
			for (TableRow<T> row : getRows()) {
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
		allItems.sort(field.comparator());
	}

	/***
	 * Sorts items using provided comparator. Any sort order is cleared.
	 * @param comparator - the Comparator used to compare list elements. A null value indicates that the
	 *                   elements' natural ordering should be used
	 */
	public void sort(Comparator<T> comparator) {
		getSortOrder().clear();
		allItems.sort(comparator);
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
		return (column -> new TableCell<>() {
			{
				setAlignment(Pos.CENTER_RIGHT);
			}

			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				if (empty)
					setText(null);
				else {
					int j = getIndex();
					int i = showOriginalIndex.get() ? filteredItems.getSourceIndex(j) : j;
					String txt = zeropadIndex.get()
						? zeroPad(i + 1, getMaxIndex(), '0')
						: String.valueOf(i + 1);
					setText(txt + ".");
				}
			}
		});
	}

}