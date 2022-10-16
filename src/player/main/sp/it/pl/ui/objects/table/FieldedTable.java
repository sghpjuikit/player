package sp.it.pl.ui.objects.table;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import sp.it.pl.access.fieldvalue.AnyField.STRING_UI;
import sp.it.pl.ui.objects.contextmenu.SelectionMenuItem;
import sp.it.pl.ui.objects.table.TableColumnInfo.ColumnInfo;
import sp.it.util.Sort;
import sp.it.util.access.fieldvalue.MetaField;
import sp.it.util.access.fieldvalue.ObjectField;
import sp.it.util.access.fieldvalue.ObjectFieldBase;
import sp.it.util.functional.Functors.F1;
import sp.it.util.functional.Functors.F3;
import sp.it.util.type.VType;
import static javafx.geometry.Side.BOTTOM;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.stage.WindowEvent.WINDOW_HIDDEN;
import static javafx.stage.WindowEvent.WINDOW_SHOWING;
import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;
import static sp.it.pl.main.AppBuildersKt.appTooltip;
import static sp.it.pl.main.AppExtensionsKt.getEmScaled;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.ui.objects.table.FieldedTableExtensionsKt.computeColumnStateDefault;
import static sp.it.pl.ui.objects.table.FieldedTableExtensionsKt.setColumnStateImpl;
import static sp.it.pl.ui.objects.table.FieldedTableExtensionsKt.setColumnVisible;
import static sp.it.pl.ui.objects.table.TableViewExtensionsKt.autoResizeColumns;
import static sp.it.pl.ui.objects.table.TableViewExtensionsKt.buildFieldedCell;
import static sp.it.pl.ui.objects.table.TableViewExtensionsKt.exportToCsv;
import static sp.it.pl.ui.objects.table.TableViewExtensionsKt.exportToMD;
import static sp.it.pl.ui.objects.table.TableViewExtensionsKt.showColumnInfo;
import static sp.it.util.access.fieldvalue.ColumnField.INDEX;
import static sp.it.util.functional.Util.SAME;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.filter;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.functional.Util.with;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.reactive.UtilKt.sync;
import static sp.it.util.type.Util.invokeMethodP0;
import static sp.it.util.ui.ContextMenuExtensionsKt.show;
import static sp.it.util.ui.UtilKt.menu;
import static sp.it.util.ui.UtilKt.menuItem;
import static sp.it.util.ui.UtilKt.tableColumn;

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

	public static final Callback<ResizeFeatures<?>, Boolean> UNCONSTRAINED_RESIZE_POLICY_FIELDED = UnconstrainedResizePolicyFielded.INSTANCE;

	/** Factory for building default column state from field */
	public final F1<? super ObjectField<T,?>, ? extends ColumnInfo> columnStateFactory = f -> new ColumnInfo(f.name(), f.cOrder(), f.cVisible(), getEmScaled(f.cWidth()));
	/** Factory for building nested column parent from field. */
	public final F1<? super ObjectField<?, ?>, ? extends TableColumn<T, ?>> columnGroupFactory = f -> tableColumn("", consumer(it -> {
		it.setText(f.cName());
		it.setId(f.name());
		it.setUserData(f);
	}));
	/** Default factory for building column from field. See {@link #setColumnFactory(sp.it.util.functional.Functors.F1)} */
	public final F1<? super ObjectField<? super T, ?>, ? extends TableColumn<T, ?>> columnFactoryDefault = f -> tableColumn("", consumer(it -> {
		it.setText(f instanceof MetaField ? "" : f.name());
		it.setCellValueFactory(cf -> cf.getValue()== null ? null : new PojoV<>(f.getOf(cf.getValue())));
		it.setCellFactory(col -> buildFieldedCell(f));
	}));
	/** Factory for building column from field. See {@link #setColumnFactory(sp.it.util.functional.Functors.F1)} */
	private F1<? super ObjectField<T,?>,? extends TableColumn<T,?>> columnFactory;
	/** Mapper from columnId to custom columnId. Allows column to alias other columns, potentially dynamically by some logic.   */
	public F1<String, String> columnIdMapper = id -> id;
	/** Initial column state */
	private TableColumnInfo columnStateDefault;
	/** Current column state */
	private TableColumnInfo columnState;
	/** Type of element of this table */
	protected final Class<T> type;
	/** All fields of this table viable as columns. The fields are string representable. */
	public final List<ObjectField<T,?>> fields;
	/** Field for top lvl column encompassing all other columns. */
	public final ObjectField<T,T> fieldsRoot;
	/** Use {@link #fieldsRoot  to show single table-width column root. Default false. */
	public boolean fieldsRootEnabled = false;
	/** All fields for {@link sp.it.pl.ui.objects.table.FieldedTable#type} of this table. Not all may be viable to be used as columns. */
	public final List<ObjectField<T,?>> fieldsAll;

	public final Menu columnVisibleMenu = new Menu("Columns");

	public final MenuItem columnAutosizeItem = with(
		menuItem("Autosize columns to content", null, consumer(it -> autoResizeColumns(this))),
		THIS -> sync(columnResizePolicyProperty(), consumer(it -> THIS.setDisable(it!=UNCONSTRAINED_RESIZE_POLICY && it!=(Object) UNCONSTRAINED_RESIZE_POLICY_FIELDED)))
	);

	public final ContextMenu columnMenu = new ContextMenu(
		menuItem("Show column descriptions", null, consumer(it -> showColumnInfo(this))),
		columnVisibleMenu,
		columnAutosizeItem,
		menu("Export...", null, consumer(THIS -> THIS.getItems().addAll(
			menuItem("to csv", null, consumer(it -> exportToCsv(this))),
			menuItem("to markdown", null, consumer(it -> exportToMD(this)))
		)))
	);

	@SuppressWarnings({"unchecked","rawtypes"})
	public FieldedTable(Class<T> type) {
		super();
		this.type = type;
		this.fieldsRoot = new ObjectFieldBase<>(new VType<>(type, false), x -> x, APP.getClassName().get(getKotlinClass(type)), "", (x, y) -> y) {};

		var fieldsAllRaw = (List<ObjectField<T,?>>) (Object) computeFieldsAll().stream().sorted(by(it -> it.name())).toList();
		this.fieldsAll = fieldsAllRaw.stream().noneMatch(it -> it!=INDEX.INSTANCE) ? (List) List.of(INDEX.INSTANCE, STRING_UI.INSTANCE) : fieldsAllRaw;
		this.fields = filter(fieldsAll, ObjectField::isTypeStringRepresentable);

		setColumnFactory(columnFactoryDefault);

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

	@SuppressWarnings({"unchecked", "rawtypes"})
	protected List<ObjectField<? super T,?>> computeFieldsAll() {
		return (List) stream(APP.getClassFields().get(getKotlinClass(type)))
			.flatMap(it -> {
				var children = APP.getClassFields().get(it.getType()).stream()
					.filter(itt -> itt!=INDEX.INSTANCE)
					.map(itt -> (ObjectField) it.flatMap((ObjectField) itt))
					.toList();
				return children.isEmpty() ? stream((ObjectField) it) : children.stream();
			})
			.toList();
	}

	public void setColumnFactory(F1<? super ObjectField<? super T, ?>, ? extends TableColumn<T, ?>> columnFactory) {
		this.columnFactory = f -> with(f==INDEX.INSTANCE ? columnIndex : columnFactory.call(f), it -> {
		    it.setId(f.name());
		    it.setUserData(f);
		    it.setVisible(f.cVisible());
		    it.setPrefWidth(f.cWidth());
		});
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public <X> F1<? super ObjectField<? super T,X>, TableColumn<T,X>> getColumnFactory() {
		return (F1) columnFactory;
	}


	/** @return visible root columns, identical to {@link #getColumns()}  */
	public ObservableList<TableColumn<T,?>> getColumnRoots() {
		return super.getColumns();
	}

	/** @return visible leaf columns, identical to {@link #getVisibleLeafColumns()}  */
	public ObservableList<TableColumn<T,?>> getColumnLeafs() {
		return super.getVisibleLeafColumns();
	}

	public boolean isColumnVisible(ObjectField<? super T,?> f) {
		return getColumn(f).isPresent();
	}


	public void setColumnState(TableColumnInfo state) {
		setColumnStateImpl(this, state);
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
		// TODO: move initialization logic out of here
		if (columnStateDefault==null) {
			// generate column states
			columnStateDefault = computeColumnStateDefault(this);
			columnState = columnStateDefault;

			// build new table column menu
			columnMenu.addEventHandler(WINDOW_HIDDEN, e -> columnVisibleMenu.getItems().clear());
			columnMenu.addEventHandler(WINDOW_SHOWING, e -> columnVisibleMenu.getItems().addAll(
				columnStateDefault.columns.streamV()
					.map(c -> {
						var f = columnIdToF(c.id);
						var d = f.description();
						var m = new SelectionMenuItem(f.name(), isColumnVisible(f));
						m.setUserData(f);
						m.getSelected().addListener((o,ov,nv) -> setColumnVisible(this, f, nv));
						if (!d.isEmpty()) Tooltip.install(m.getGraphic(), appTooltip(d));
						return m;
					})
					.sorted(by(MenuItem::getText))
					.toList()
			));

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
		return columnStateDefault;
	}

	public Optional<TableColumn<T,?>> getColumn(Predicate<TableColumn<T,?>> filter) {
		return getColumnLeafs().stream().filter(filter).findFirst();
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
	public final ObjectProperty<F3<? super ObjectField<?, ?>, ? super ObjectField<?, ?>, ? super Sort, ? extends Comparator<?>>> itemsComparatorFieldFactory = new SimpleObjectProperty<>((fOriginal, fMemoized, sort) ->
		fMemoized.comparatorNonNull(sort==Sort.DESCENDING ? Comparator::nullsFirst : Comparator::nullsLast)
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
		getItems().sort(field.comparatorNNL());
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
	protected void updateComparator() {
		itemsComparatorWrapper.setValue(computeComparator());
	}

	protected Comparator<T> computeComparator() {
		return computeComparatorEnhanced(it -> it);
	}

	protected Comparator<T> computeComparatorMemoized() {
		return computeComparatorEnhanced(it -> it.memoized());
	}

	@SuppressWarnings({"unchecked", "unused"})
	protected Comparator<T> computeComparatorEnhanced(F1<ObjectField<T,?>, ObjectField<T,?>> enhancer) {
		return getSortOrder().stream()
			.map(column -> {
				var field = (ObjectField<T,?>) column.getUserData();
				var sort = Sort.of(column.getSortType());
				var comparator = (Comparator<T>) itemsComparatorFieldFactory.get().apply(field, enhancer.apply(field), sort);
				return sort.of(comparator);
			})
			.reduce(Comparator::thenComparing)
			.orElse((Comparator<T>) SAME);
	}

	public ObjectField<T,?> columnIdToF(String id) {
		String fieldName = columnIdMapper.apply(id);
		return fields.stream()
			.filter(f -> f.name().equals(fieldName)).findAny()
			.orElseThrow(() -> new RuntimeException("Cant find field=" + fieldName + " by columnId=" + id));
	}

}