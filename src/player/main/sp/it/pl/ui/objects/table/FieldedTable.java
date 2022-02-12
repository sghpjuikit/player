package sp.it.pl.ui.objects.table;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.layout.Pane;
import sp.it.pl.access.fieldvalue.AnyField.STRING_UI;
import sp.it.pl.ui.objects.contextmenu.SelectionMenuItem;
import sp.it.pl.ui.objects.table.TableColumnInfo.ColumnInfo;
import sp.it.util.Sort;
import sp.it.util.access.fieldvalue.ObjectField;
import sp.it.util.functional.Functors.F1;
import static java.util.stream.Collectors.toList;
import static javafx.geometry.Side.BOTTOM;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.stage.WindowEvent.WINDOW_HIDDEN;
import static javafx.stage.WindowEvent.WINDOW_SHOWING;
import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;
import static sp.it.pl.main.AppBuildersKt.appTooltip;
import static sp.it.pl.main.AppExtensionsKt.getEmScaled;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.ui.objects.table.TableUtilKt.buildFieldedCell;
import static sp.it.util.access.fieldvalue.ColumnField.INDEX;
import static sp.it.util.dev.FailKt.noNull;
import static sp.it.util.functional.Util.SAME;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.filter;
import static sp.it.util.functional.Util.map;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.type.Util.invokeMethodP0;
import static sp.it.util.ui.ContextMenuExtensionsKt.show;

/**
 * Table for objects using {@link ObjectField}. This facilitates column creation, sorting and
 * potentially additional features (e.g. filtering - {@link FilteredTable}).
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
 * hidden by default and the menu can also be shown by right-click on the table
 * header.
 * <p/>
 *
 * @param <T> type of element in the table, must be aware of its fields
 */
public class FieldedTable<T> extends ImprovedTable<T> {

	private F1<ObjectField<T,?>,ColumnInfo> colStateFact = f -> new ColumnInfo(f.toString(), f.cOrder(), f.cVisible(), getEmScaled(f.cWidth()));
	private F1<? super ObjectField<T,?>,? extends TableColumn<T,?>> colFact;
	private F1<String,String> keyNameColMapper = name -> name;

	private TableColumnInfo defColInfo;
	private TableColumnInfo columnState;
	/** Type of element of this table */
	protected final Class<T> type;
	/** All fields of this table viable as columns. The fields are string representable. */
	public final List<ObjectField<T,?>> fields;
	/** All fields for {@link sp.it.pl.ui.objects.table.FieldedTable#type} of this table. Not all may be viable to be used as columns. */
	public final List<ObjectField<T,?>> fieldsAll;
	public final Menu columnVisibleMenu = new Menu("Columns");
	public final ContextMenu columnMenu = new ContextMenu(columnVisibleMenu);

	@SuppressWarnings({"unchecked","rawtypes"})
	public FieldedTable(Class<T> type) {
		super();
		this.type = type;

		var fieldsAllRaw = (List<ObjectField<T,?>>) (Object) computeFieldsAll();
		this.fieldsAll = fieldsAllRaw.stream().noneMatch(it -> it!=INDEX.INSTANCE) ? (List) List.of(INDEX.INSTANCE, STRING_UI.INSTANCE) : fieldsAllRaw;
		this.fields = filter(fieldsAll, ObjectField::isTypeStringRepresentable);

		setColumnFactory(f -> {
			TableColumn<T,Object> c = new TableColumn<>(f.name());
			c.setCellValueFactory(cf -> cf.getValue()== null ? null : new PojoV<>(f.getOf(cf.getValue())));
			c.setCellFactory(col -> buildFieldedCell(f));
			return c;
		});

		// show the column control menu on right click ( + hide if shown )
		addEventHandler(MOUSE_CLICKED, e -> {
			if (e.getButton()==SECONDARY && e.getY()<getVisibleHeaderHeight()) {
				show(columnMenu, this, e);
			} else {
				if (columnMenu.isShowing()) columnMenu.hide();
			}
		});

		// column control menu button not needed now (but still optionally usable)
		setTableMenuButtonVisible(false);
	}

	protected List<ObjectField<? super T,?>> computeFieldsAll() {
		return stream(APP.getClassFields().get(getKotlinClass(type)))
			// TODO: support nested columns
			//.flatMap(it -> stream(
			//	it,
			//	stream(APP.getClassFields().get(getRaw(it.getType().getType())))
			//		.filter(itt -> itt != INDEX)
			//		.map(itt -> it.flatMap((ObjectField) itt))
			//))
			.sorted(by(ObjectField::name))
			.collect(toList());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public void setColumnFactory(F1<? super ObjectField<? super T,Object>,TableColumn<T,?>> columnFactory) {
		colFact = f -> {
			TableColumn<T,?> c = f==INDEX.INSTANCE ? columnIndex : (TableColumn) ((F1) columnFactory).call(f);
			c.setUserData(f);
			return c;
		};
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public <X> F1<? super ObjectField<? super T,X>,TableColumn<T,X>> getColumnFactory() {
		return (F1) colFact;
	}

	public void setColumnStateFactory(F1<ObjectField<T,?>,ColumnInfo> columnStateFactory) {
		colStateFact = columnStateFactory;
	}

	public Function<ObjectField<T,?>,ColumnInfo> getColumnStateFactory() {
		return colStateFact;
	}

	public void setKeyNameColMapper(F1<String,String> columnNameToKeyMapper) {
		keyNameColMapper = columnNameToKeyMapper;
	}

	public boolean isColumnVisible(ObjectField<? super T,?> f) {
		return getColumn(f).isPresent();
	}

	public void setColumnVisible(ObjectField<T,?> f, boolean v) {
		TableColumn<T,?> c = getColumn(f).orElse(null);
		if (v && c==null) {
			c = f==INDEX.INSTANCE ? columnIndex : colFact.call(f);
			c.setPrefWidth(f==INDEX.INSTANCE ? computeIndexColumnWidth() : columnState.columns.get(f.name()).width);
			c.setVisible(v);
			getColumns().add(c);
		} else if (!v && c!=null) {
			getColumns().remove(c);
			c.setVisible(false);
		}
	}

	public void setColumnState(TableColumnInfo state) {
		noNull(state);

		List<TableColumn<T,?>> visibleColumns = new ArrayList<>();
		state.columns.stream().filter(c -> c.visible).sorted().forEach(c -> {
			// get or build column
			TableColumn<T,?> tc = c.name.equals(INDEX.INSTANCE.name())
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

	// TODO: move initialization logic out of here
	public TableColumnInfo getDefaultColumnInfo() {
		if (defColInfo==null) {
			// generate column states
			defColInfo = new TableColumnInfo();
			defColInfo.nameKeyMapper = keyNameColMapper;
			defColInfo.columns.addAll(map(fields, colStateFact));
			// insert index column state manually
			defColInfo.columns.removeIf(f -> f.name.equals(INDEX.INSTANCE.name()));
			defColInfo.columns.forEach(t -> t.position++);  //TODO: position should be immutable
			defColInfo.columns.add(new ColumnInfo("#", 0, true, USE_COMPUTED_SIZE));

			columnState = defColInfo;
			// build new table column menu
			// update columnVisibleMenu check icons every time we show it
			// the menu is rarely shown + this way we do not need to update it any other time
			columnMenu.addEventHandler(WINDOW_HIDDEN, e -> columnVisibleMenu.getItems().clear());
			columnMenu.addEventHandler(WINDOW_SHOWING, e -> {
				columnVisibleMenu.getItems().addAll(
					defColInfo.columns.streamV()
						.map(c -> {
							ObjectField<T,?> f = nameToCF(c.name);
							String d = f.description();

							SelectionMenuItem m = new SelectionMenuItem(c.name, c.visible);
							m.setUserData(f);
							m.getSelected().addListener((o,ov,nv) -> setColumnVisible(f, nv));
							if (!d.isEmpty()) Tooltip.install(m.getGraphic(), appTooltip(d));

							return m;
						})
						.sorted(by(MenuItem::getText))
						.toList()
				);
				columnVisibleMenu.getItems().forEach(i -> ((SelectionMenuItem) i).getSelected().setValue(isColumnVisible(nameToCF(i.getText()))));
			});

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
	 * The value is never null, rather {@link sp.it.util.functional.Util#SAME} is used to indicate no
	 * particular order.
	 */
	private final ReadOnlyObjectWrapper<Comparator<? super T>> itemsComparatorWrapper = new ReadOnlyObjectWrapper<>(SAME);

	public final ReadOnlyObjectProperty<Comparator<? super T>> itemsComparator = itemsComparatorWrapper.getReadOnlyProperty();

	@SuppressWarnings("unused")
	public final ObjectProperty<BiFunction<? super ObjectField<?, ?>, ? super Sort, ? extends Comparator<?>>> itemsComparatorFieldFactory = new SimpleObjectProperty<>((f, sort) ->
		f.comparator(sort==Sort.DESCENDING ? Comparator::nullsFirst : Comparator::nullsLast)
	);

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
	 * {@link #getColumn(sp.it.util.access.fieldvalue.ObjectField)} for column lookup.
	 */
	public void sortBy(ObjectField<T,?> field, SortType type) {
		getColumn(field).ifPresent(c -> sortBy(c, type));
	}

/* --------------------- UTIL --------------------------------------------------------------------------------------- */

	/** Sets {@link #itemsComparator} to comparator build from {@link #getSortOrder()}} */
	@SuppressWarnings({"unchecked", "unused"})
	protected void updateComparator() {
		itemsComparatorWrapper.setValue(
			getSortOrder().stream()
				.map(column -> {
					var field = (ObjectField<T,?>) column.getUserData();
					var sort = Sort.of(column.getSortType());
					var comparator = (Comparator<T>) itemsComparatorFieldFactory.get().apply(field, sort);
					return sort.of(comparator);
				})
				.reduce(Comparator::thenComparing)
				.orElse((Comparator<T>) SAME)
		);
	}

	private ObjectField<T,?> nameToF(String name) {
		String fieldName = keyNameColMapper.apply(name);
		return fields.stream()
			.filter(f -> f.name().equals(fieldName)).findAny()
			.orElseThrow(() -> new RuntimeException("Cant find '" + name + "' field"));
	}

	@SuppressWarnings({"unchecked"})
	private ObjectField<T,?> nameToCF(String name) {
		return INDEX.INSTANCE.name().equals(name) ? (ObjectField<T,?>) INDEX.INSTANCE : nameToF(name);
	}

}