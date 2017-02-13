package gui.objects.table;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.infonode.InfoTable;
import gui.itemnode.FieldedPredicateChainItemNode;
import gui.itemnode.FieldedPredicateItemNode;
import gui.itemnode.FieldedPredicateItemNode.PredicateData;
import gui.objects.icon.Icon;
import gui.objects.search.SearchAutoCancelable;
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
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import main.App;
import util.access.fieldvalue.ObjectField;
import util.dev.TODO;
import util.functional.Functors;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_MINUS;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static gui.objects.contextmenu.SelectionMenuItem.buildSingleSelectionMenu;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_TYPED;
import static javafx.scene.layout.Priority.ALWAYS;
import static main.App.APP;
import static org.reactfx.EventStreams.changesOf;
import static util.Util.zeroPad;
import static util.async.Async.runLater;
import static util.dev.TODO.Purpose.BUG;
import static util.dev.TODO.Purpose.ILL_DEPENDENCY;
import static util.dev.Util.noØ;
import static util.functional.Util.*;
import static util.graphics.Util.layHorizontally;
import static util.graphics.Util.menuItem;
import static util.reactive.Util.sizeOf;

/**
 * Table with a search filter header that supports filtering with provided gui.
 *
 * @author Martin Polakovic
 */
public class FilteredTable<T> extends FieldedTable<T> {

	public ObjectField<T,?> primaryFilterField;
	private final ObservableList<T> allItems;
	private final FilteredList<T> filteredItems;
	private final SortedList<T> sortedItems;
	final VBox root = new VBox(this);

	/**
	 * @param type exact type of the item displayed in the table
	 * @param main_field to be chosen as main and default search field or null
	 */
	public FilteredTable(Class<T> type, ObjectField<T,?> main_field) {
		this(type, main_field, observableArrayList());
	}

	/**
	 * @param type exact type of the item displayed in the table
	 * @param main_field field to determine primary filtering field and search column. Can be null Initializes {@link
	 * gui.objects.table.FilteredTable.Search#field} and {@link #primaryFilterField}.
	 * @param backing_list non null backing list of items to be displayed in the table
	 */
	public FilteredTable(Class<T> type, ObjectField<T,?> main_field, ObservableList<T> backing_list) {
		super(type);

		allItems = noØ(backing_list);
		filteredItems = new FilteredList<>(allItems);
		sortedItems = new SortedList<>(filteredItems);
		itemsPredicate = filteredItems.predicateProperty();

		setItems(sortedItems);
		sortedItems.comparatorProperty().bind(comparatorProperty());
		VBox.setVgrow(this, ALWAYS);

		items_info.bind(this);

		// visually hint user menus are empty
		sizeOf(menuAdd.getItems(), size -> menuAdd.setDisable(size==0));
		sizeOf(menuRemove.getItems(), size -> menuRemove.setDisable(size==0));
		sizeOf(menuSelected.getItems(), size -> menuSelected.setDisable(size==0));
		sizeOf(menuOrder.getItems(), size -> menuOrder.setDisable(size==0));

		// searching
		search.field = main_field;
		searchQueryLabel.textProperty().bind(search.searchQuery);

		// filtering
		primaryFilterField = main_field;
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
					e.consume();
				}
			}
		};
		filterPane.getNode().addEventFilter(KEY_PRESSED, filterKeyHandler);
		root.addEventHandler(KEY_PRESSED, filterKeyHandler); // even filter would cause ignoring first key stroke when filter turns visible
		root.addEventHandler(KEY_TYPED, search::onKeyTyped);
		root.addEventFilter(KEY_PRESSED, search::onKeyPressed);
		root.addEventFilter(KEY_PRESSED, search::onEscPressHide);
		// TODO: fix the overkill
		root.addEventFilter(Event.ANY, e -> {
			if (search.isActive())
				search.updateSearchStyles();
		});
		changesOf(getItems()).subscribe(c -> {
			if (search.isActive())
				search.updateSearchStyles();
		});
		changesOf(getItems()).subscribe(c -> resizeIndexColumn());

		footerVisible.set(true);
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
				runLater(filterPane::focus);
				return;
			}

			super.set(v);
			if (!v) filterPane.clear();

			Node sn = filterPane.getNode();
			if (v) {
				if (!root.getChildren().contains(sn))
					root.getChildren().add(0, sn);
			} else {
				root.getChildren().remove(sn);
			}
			filterPane.getNode().setVisible(v);

			// focus filter to allow user use filter asap
			if (v) runLater(filterPane::focus);
		}
	};

	/******************************* BOTTOM CONTROLS ******************************/

	@util.dev.TODO(purpose = ILL_DEPENDENCY, note = "Hardcoded size for consistency, not good")
	public final Menu menuAdd = new Menu("", new Icon(PLAYLIST_PLUS, 18).embedded());
	public final Menu menuRemove = new Menu("", new Icon(PLAYLIST_MINUS, 18).embedded());
	public final Menu menuSelected = new Menu("", new Icon(FontAwesomeIcon.CROP).embedded(),
		menuItem("Select inverse", this::selectAll),
		menuItem("Select all", this::selectInverse),
		menuItem("Select none", this::selectNone)
	);
	public final Menu menuOrder = new Menu("", new Icon(FontAwesomeIcon.NAVICON).embedded());
	/** Table menubar in the bottom with menus. Feel free to modify. */
	public final MenuBar menus = new MenuBar(menuAdd, menuRemove, menuSelected, menuOrder);
	/**
	 * Labeled in the bottom displaying information on table items and selection.
	 * Feel free to provide custom implementation of {@link InfoTable#textFactory}
	 * to display different information. You may want to reuse
	 * {@link InfoTable#DEFAULT_TEXT_FACTORY}.
	 */
	public final InfoTable<T> items_info = new InfoTable<>(new Label()); // can not bind here as table items list not ready
	private final Label searchQueryLabel = new Label();
	private final HBox bottomLeftPane = layHorizontally(5, CENTER_LEFT, menus, items_info.node);
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

		public Filter(Class<T> filterType, FilteredList<T> filterList) {
			super(() -> {
				FieldedPredicateItemNode<T,ObjectField<T,Object>> g = new FieldedPredicateItemNode<>(
					in -> Functors.pool.getIO(in, Boolean.class),
					in -> Functors.pool.getPrefIO(in, Boolean.class)
				);
				g.setPrefTypeSupplier(FilteredTable.this::getPrimaryFilterPredicate);
				g.setData(getFilterPredicates(filterType));
				return g;
			});
			setPrefTypeSupplier(FilteredTable.this::getPrimaryFilterPredicate);
			onItemChange = filterList::setPredicate;
			setData(getFilterPredicates(filterType));
		}
	}

	private PredicateData<ObjectField<T,Object>> getPrimaryFilterPredicate() {
		return Optional.ofNullable(primaryFilterField)
			.map((Function<ObjectField<T,?>,PredicateData<? extends ObjectField<T,?>>>) PredicateData::ofField)
			.map(f -> (PredicateData<ObjectField<T,Object>>)f)
			.orElse(null);
	}

	private List<PredicateData<ObjectField<T,Object>>> getFilterPredicates(Class<T> filterType) {
		return stream(App.APP.classFields.get(filterType))
			.filter(ObjectField::isTypeStringRepresentable)
			.map((Function<ObjectField<T,?>,PredicateData<? extends ObjectField<T,?>>>) PredicateData::ofField)
			.sorted(by(e -> e.name))
			.map(f -> (PredicateData<ObjectField<T,Object>>)f)
			.toList();
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
		private Menu menu = null;

		/**
		 * Starts search, searching for the specified string in the designated column.
		 * This column is determined by {@link #field}).
		 */
		@Override
		public void onSearch(String s) {
			APP.actionStream.push("Table search");
			searchQuery.set(s);
			// scroll to first found item
			TableColumn c = field==null ? null : getColumn(field).orElse(null);
			if (c!=null && !getItems().isEmpty() && c.getCellData(0) instanceof String) {
				for (int i = 0; i<getItems().size(); i++) {
					String item = (String) field.getOf(getItems().get(i));	// TODO: make compile-time safe
					if (matches(item, searchQuery.get())) {
						scrollToCenter(i);
						updateSearchStyles();
						break;
					}
				}
			}
		}

		/** Sets fields to be used in search. Default is main field. */
		public void setColumn(ObjectField<T,?> field) {
			// TODO
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
			if (isCancelable) searchAutocanceller.start(cancelActivityDelay);
			updateSearchStyleRowsNoReset();
		}

		private void updateSearchStyleRowsNoReset() {
			boolean searchOn = isActive();
			for (TableRow<T> row : getRows()) {
				T t = row.getItem();
				Object o = t==null ? null : field.getOf(t);
				boolean isMatch = o instanceof String && matches((String) o, searchQuery.get());
				row.pseudoClassStateChanged(SEARCHMATCHPC, searchOn && isMatch);
				row.getChildrenUnmodifiable().forEach(c -> c.pseudoClassStateChanged(SEARCHMATCHPC, searchOn && isMatch));
				row.pseudoClassStateChanged(SEARCHMATCHNOTPC, searchOn && !isMatch);
				row.getChildrenUnmodifiable().forEach(c -> c.pseudoClassStateChanged(SEARCHMATCHNOTPC, searchOn && !isMatch));
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

/* --------------------- HELPER ------------------------------------------------------------------------------------- */

	@TODO(purpose = BUG)
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
					String txt;
					if (zeropadIndex.get()) {
						int i = showOriginalIndex.get() ? filteredItems.getSourceIndex(j) : j;
						txt = zeroPad(i + 1, getMaxIndex(), '0');
					} else
						txt = String.valueOf(j + 1);

					setText(txt + ".");
				}
			}
		});
	}

	@Override
	public TableColumnInfo getDefaultColumnInfo() {
		TableColumnInfo tci = super.getDefaultColumnInfo();
		boolean needs_creating = search.menu==null;
		if (needs_creating) {
			search.menu = buildSingleSelectionMenu(
				"Search column",
				filter(getFields(), f -> isContainedIn(f.getType(), String.class, Object.class)), // objects too, they can be strings // TODO
				search.field,
				ObjectField::name,
				field -> search.field = field
			);
			columnMenu.getItems().add(search.menu);
		}
		return tci;
	}

}