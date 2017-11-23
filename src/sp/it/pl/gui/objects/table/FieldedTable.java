package sp.it.pl.gui.objects.table;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableRow;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.layout.Pane;
import sp.it.pl.gui.objects.contextmenu.SelectionMenuItem;
import sp.it.pl.gui.objects.table.TableColumnInfo.ColumnInfo;
import sp.it.pl.main.App;
import sp.it.pl.util.Sort;
import sp.it.pl.util.access.fieldvalue.ColumnField;
import sp.it.pl.util.access.fieldvalue.ObjectField;
import sp.it.pl.util.functional.Functors.Ƒ1;
import static java.util.stream.Collectors.toList;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.geometry.Side.BOTTOM;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static sp.it.pl.main.AppBuildersKt.appTooltip;
import static sp.it.pl.util.dev.Util.noØ;
import static sp.it.pl.util.functional.Util.SAME;
import static sp.it.pl.util.functional.Util.by;
import static sp.it.pl.util.functional.Util.map;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.type.Util.invokeMethodP0;

/**
 * Table for objects using {@link ObjectField}. This facilitates column creation, sorting and
 * potentially additional features (e.g. filtering - {@link FilteredTable}.
 * <p/>
 * Supports column state serialization and deserialization - it is possible to
 * restore previous state. This includes column order, column names, sort order,
 * and column widths.
 * <p/>
 * Nested columns are not supported.
 * <p/>
 * The columns use userData property to store the field F for identification.
 * Use {@link #setUserData(java.lang.Object)}
 * to obtain the exact F field the column represents. Every column will return
 * a value except for index column, which returns null. Never use
 * {@link #getUserData()} on the columns or column lookup will break.
 * <p/>
 * Only visible columns are built.
 * <p/>
 * Has redesigned menu for column visibility. The table header button opening it is
 * hidden by default and the menu can also be shown by right click on the table
 * header.
 * <p/>
 *
 * @param <T> type of element in the table, must be aware of its fields
 */
public class FieldedTable<T> extends ImprovedTable<T> {

	private Ƒ1<ObjectField<T,?>,ColumnInfo> colStateFact = f -> new ColumnInfo(f.toString(), f.cOrder(), f.cVisible(), f.cWidth());
	private Ƒ1<? super ObjectField<T,?>,? extends TableColumn<T,?>> colFact;
	private Ƒ1<String,String> keyNameColMapper = name -> name;

	private TableColumnInfo defColInfo;
	private TableColumnInfo columnState;
	protected final Class<T> type;
	public final Menu columnVisibleMenu = new Menu("Columns");
	public final ContextMenu columnMenu = new ContextMenu(columnVisibleMenu);

	public FieldedTable(Class<T> type) {
		super();
		this.type = type;

		// install comparator updating part I
		getSortOrder().addListener((ListChangeListener<Object>) this::updateComparator);

		// show the column control menu on right click ( + hide if shown )
		addEventHandler(MOUSE_CLICKED, e -> {
			if (e.getButton()==SECONDARY && e.getY()<getTableHeaderHeight()) {
				columnMenu.show(this, e.getScreenX(), e.getScreenY());
			} else {
				if (columnMenu.isShowing()) columnMenu.hide();
			}
		});

		// column control menu button not needed now (but still optionally usable)
		setTableMenuButtonVisible(false);
	}

	/**
	 * Returns all fields of this table. The fields are string representable.
	 */
	public List<ObjectField<T,?>> getFields() {
		return stream(App.APP.classFields.get(type))
			.filter(ObjectField::isTypeStringRepresentable)
			.sorted(by(ObjectField::name))
			.collect(toList());
	}

	/**
	 * Returns all fields of this table. The fields are string representable.
	 */
	public List<ObjectField<T,?>> getFieldsAll() {
		return stream(App.APP.classFields.get(type))
			.sorted(by(ObjectField::name))
			.collect(toList());
	}

	public void setColumnFactory(Ƒ1<? super ObjectField<? super T,Object>,TableColumn<T,Object>> columnFactory) {
		colFact = f -> {
			TableColumn<T,?> c = f==ColumnField.INDEX ? columnIndex : (TableColumn) ((Ƒ1) columnFactory).call(f);
			c.setUserData(f);
			return c;
		};
	}

	public <X> Ƒ1<? super ObjectField<? super T,X>,TableColumn<T,X>> getColumnFactory() {
		return (Ƒ1) colFact;
	}

	public void setColumnStateFactory(Ƒ1<ObjectField<T,?>,ColumnInfo> columnStateFactory) {
		colStateFact = columnStateFactory;
	}

	public Function<ObjectField<T,?>,ColumnInfo> getColumnStateFactory() {
		return colStateFact;
	}

	public void setKeyNameColMapper(Ƒ1<String,String> columnNameToKeyMapper) {
		keyNameColMapper = columnNameToKeyMapper;
	}

	public boolean isColumnVisible(ObjectField<? super T,?> f) {
		return getColumn(f).isPresent();
	}

	public void setColumnVisible(ObjectField<T,?> f, boolean v) {
		TableColumn<T,?> c = getColumn(f).orElse(null);
		if (v && c==null) {
			c = f==ColumnField.INDEX ? columnIndex : colFact.call(f);
			c.setPrefWidth(f==ColumnField.INDEX ? computeIndexColumnWidth() : columnState.columns.get(f.name()).width);
			c.setVisible(v);
			getColumns().add(c);
		} else if (!v && c!=null) {
			getColumns().remove(c);
			c.setVisible(false);
		}
	}

	public void setColumnState(TableColumnInfo state) {
		noØ(state);

		List<TableColumn<T,?>> visibleColumns = new ArrayList<>();
		state.columns.stream().filter(c -> c.visible).sorted().forEach(c -> {
			// get or build column
			TableColumn<T,?> tc = c.name.equals(ColumnField.INDEX.name())
				? columnIndex
				: colFact.call(nameToF(c.name));
			// set width
			tc.setPrefWidth(c.width);
			// set visibility
			tc.setVisible(c.visible);
			// set position (automatically, because we sorted the input)
			visibleColumns.add(tc);
		});
		// restore all at once => 1 update
		getColumns().setAll(visibleColumns);
		// restore sort order
		state.sortOrder.toTable(this);
	}

	@Deprecated
	public TableColumnInfo impl_GetColumnState() {
		return columnState;
	}

	public TableColumnInfo getColumnState() {
		columnState.update(this);
		return columnState;
	}

	public TableColumnInfo getDefaultColumnInfo() {
		if (defColInfo==null) {
			// generate column states
			List<ColumnInfo> colinfos = map(getFields(), colStateFact);
			defColInfo = new TableColumnInfo();
			defColInfo.nameKeyMapper = keyNameColMapper;
			defColInfo.columns.addAll(colinfos);
			// insert index column state manually
			defColInfo.columns.removeIf(f -> f.name.equals(ColumnField.INDEX.name()));
			defColInfo.columns.forEach(t -> t.position++);
			defColInfo.columns.add(new ColumnInfo("#", 0, true, USE_COMPUTED_SIZE));
			// leave sort order empty

			columnState = defColInfo;

			// build new table column menu
			defColInfo.columns.streamV()
				.map(c -> {
					ObjectField<T,?> f = nameToCF(c.name);
					SelectionMenuItem m = new SelectionMenuItem(c.name, c.visible, v -> setColumnVisible(f, v));
					String d = f.description();
					if (!d.isEmpty()) Tooltip.install(m.getGraphic(), appTooltip(d));
					return m;
				})
				.sorted(by(MenuItem::getText))
				.forEach(columnVisibleMenu.getItems()::add);
			// update columnVisibleMenu check icons every time we show it
			// the menu is rarely shown + this way we do not need to update it any other time
			columnVisibleMenu.setOnShowing(e -> columnVisibleMenu.getItems()
				.forEach(i -> ((SelectionMenuItem) i).selected.setValue(isColumnVisible(nameToCF(i.getText())))));

			// link table column button to our menu instead of an old one
			if (getSkin()==null) setSkin(new TableViewSkin<>(this));    // make sure skin exists
			// TableHeaderRow h = ((TableViewSkinBase)getSkin()).getTableHeaderRow(); // java9 no longer supports this
			TableHeaderRow h = (TableHeaderRow) invokeMethodP0(getSkin(), "getTableHeaderRow");

			try {
				// cornerRegion is the context menu button, use reflection
				Field f = TableHeaderRow.class.getDeclaredField("cornerRegion");
				// they just wont let us...
				f.setAccessible(true);
				// link to our custom menu
				Pane columnB = (Pane) f.get(h);
				columnB.setOnMousePressed(e -> columnMenu.show(columnB, BOTTOM, 0, 0));
				f.setAccessible(false);
			} catch (Exception ex) {
				Logger.getLogger(FieldedTable.class.getName()).log(Level.SEVERE, null, ex);
			}

			// install comparator updating part II
			// we need this because sort order list changes do not reflect
			// every sort change (when only ASCENDING-DESCENDING is changed
			// there is no list change event.
			h.setOnMouseReleased(this::updateComparator);
			h.setOnMouseClicked(this::updateComparator);

		}
		return defColInfo;
	}

	public Optional<TableColumn<T,?>> getColumn(Predicate<TableColumn<T,?>> filter) {
		return getColumns().stream().filter(filter).findFirst();
	}

	@SuppressWarnings("unchecked")
	public <R> Optional<TableColumn<T,R>> getColumn(ObjectField<? super T,R> f) {
		return getColumn(c -> c.getUserData()==f).map(c -> (TableColumn<T,R>) c);
	}

	public void refreshColumn(ObjectField<? super T,?> f) {
		getColumn(f).ifPresent(this::refreshColumn);
	}

	public void refreshColumns() {
		if (!getColumns().isEmpty()) refreshColumn(getColumns().get(0));
	}

/* --------------------- SORT --------------------------------------------------------------------------------------- */

	/**
	 * Comparator for ordering items reflecting this table's sort order.
	 * Read only, changing value will have no effect.
	 * <p/>
	 * The value is never null, rather {@link sp.it.pl.util.functional.Util#SAME} is used to indicate no
	 * particular order.
	 */
	public final ObjectProperty<Comparator<? super T>> itemsComparator = new SimpleObjectProperty<>(SAME);

	/**
	 * Sorts the items by the field. Sorting does not operate on table's sort
	 * order and is applied to items backing the table. Any sort order of
	 * the table will be removed.
	 * <p/>
	 * This is not a programmatic equivalent of sorting the table manually by
	 * clicking on their header (which operates through sort order).
	 * <p/>
	 * Works even when field's respective column is invisible.
	 * <p/>
	 * Note, that the field must support sorting - return Comparable fieldType.
	 */
	public void sortBy(ObjectField<T,?> field) {
		getSortOrder().clear();
		getItems().sort(field.comparator());
	}

	/**
	 * Sorts the items by the column.
	 * Same as {@link #sortBy(javafx.scene.control.TableColumn,
	 * javafx.scene.control.TableColumn.SortType)}, but uses
	 * {@link #getColumn(sp.it.pl.util.access.fieldvalue.ObjectField)} for column lookup.
	 */
	public void sortBy(ObjectField<T,?> field, SortType type) {
		getColumn(field).ifPresent(c -> sortBy(c, type));
		updateComparator(null);
	}

/* --------------------- CELL FACTORY ------------------------------------------------------------------------------- */

	/**
	 * Use as cell factory for columns created in column factory.
	 * <p/>
	 * This cell factory
	 * <ul>
	 * <li> sets text using {@link ObjectField#toS(Object, String)}
	 * <li> sets alignment to CENTER_LEFT for Strings and CENTER_RIGHT otherwise
	 * </ul>
	 */
	public <X> TableCell<T,X> buildDefaultCell(ObjectField<? super T,X> f) {
		Pos a = f.getType().equals(String.class) ? CENTER_LEFT : CENTER_RIGHT;
		TableCell<T,X> cell = new TableCell<>() {
			@Override
			protected void updateItem(X item, boolean empty) {
				super.updateItem(item, empty);
				TableRow<T> row = empty ? null : getTableRow();
				T rowItem = row==null ? null : row.getItem();
				setText(rowItem==null  ? "" : f.toS(rowItem, item, ""));
			}
		};
		cell.setAlignment(a);
		return cell;
	}

	/*********************************** PRIVATE **********************************/

	// sort order -> comparator, never null
	@SuppressWarnings({"unchecked", "unused"})
	private void updateComparator(Object ignored) {
		Comparator<? super T> c = getSortOrder().stream()
			.map(column -> {
				ObjectField<T,?> field = (ObjectField<T,?>) column.getUserData();
				Sort sort = Sort.of(column.getSortType());
				return sort.of(field.comparator());
			})
			.reduce(Comparator::thenComparing)
			.orElse((Comparator) SAME);
		itemsComparator.setValue(c);
	}

	private ObjectField<T,?> nameToF(String name) {
		String fieldName = keyNameColMapper.apply(name);
		return getFields().stream()
			.filter(f -> f.name().equals(fieldName)).findAny()
			.orElseThrow(() -> new RuntimeException("Cant find '" + name + "' field"));
	}

	private ObjectField<T,?> nameToCF(String name) {
		return ColumnField.INDEX.name().equals(name) ? (ObjectField) ColumnField.INDEX : nameToF(name);
	}
}