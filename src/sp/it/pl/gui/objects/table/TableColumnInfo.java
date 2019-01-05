package sp.it.pl.gui.objects.table;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuples;
import sp.it.pl.util.collections.mapset.MapSet;
import sp.it.pl.util.functional.Functors.Ƒ1;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static sp.it.pl.util.functional.Util.map;
import static sp.it.pl.util.functional.Util.split;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.functional.Util.toIndexedStream;
import static sp.it.pl.util.functional.Util.toS;

/**
 * Data structure for holding complete table column information, mostly for serializing and deserializing purposes.
 */
public final class TableColumnInfo {

	private static final String S1 = "+";
	private static final String S2 = ";";
	private static final String S3 = ",";

	public final MapSet<String,ColumnInfo> columns;
	public final ColumnSortInfo sortOrder;
	public Ƒ1<String,String> nameKeyMapper = name -> name;

	public TableColumnInfo() {
		columns = new MapSet<>(c -> nameKeyMapper.apply(c.name));
		sortOrder = new ColumnSortInfo();
	}

	public TableColumnInfo(List<String> all_columns) {
		this();
		// add columns as visible
		toIndexedStream(all_columns)
				.map(p -> new ColumnInfo(p.value, p.i, true, 50))
				.forEach(columns::add);
	}

	public TableColumnInfo(TableView<?> table) {
		this();
		// add visible columns
		toIndexedStream(table.getColumns())
				.map(p -> new ColumnInfo(p.i, p.value))
				.forEach(columns::add);
		sortOrder.fromTable(table);
	}

	public TableColumnInfo(TableView<?> table, List<String> all_columns) {
		this(all_columns);
		update(table);
	}

	public void update(TableView<?> table) {
		MapSet<String,ColumnInfo> old = new MapSet<>(c -> nameKeyMapper.apply(c.name));
		old.addAll(columns);
		columns.clear();
		// add visible columns
		toIndexedStream(table.getColumns())
				.map(p -> new ColumnInfo(p.i, p.value))
				.peek(old::remove)
				.forEach(columns::add);
		// add invisible columns
		int i = columns.size();
		old.stream()
				.map(p -> new ColumnInfo(p.name, i + p.position, false, p.width))
				.forEach(columns::add);

		sortOrder.fromTable(table);
	}

	@Override
	public String toString() {
		return toS(columns, Object::toString, S2) + S1 + sortOrder.toString();
	}

	public static TableColumnInfo fromString(String str) {
		String[] a = str.split("\\" + S1, -1);
		TableColumnInfo tci = new TableColumnInfo();
		tci.columns.addAll(split(a[0], S2, ColumnInfo::fromString));
		tci.sortOrder.sorts.addAll(ColumnSortInfo.fromString(a[1]).sorts);
		return tci;
	}

	/**
	 * Data structure for single table column information, mostly for serializing and deserializing purposes.
	 */
	public static final class ColumnInfo implements Comparable<ColumnInfo> {
		public String name;
		public int position;
		public boolean visible;
		public double width;

		public ColumnInfo(String name, int position, boolean visible, double width) {
			this.name = name;
			this.position = position;
			this.visible = visible;
			this.width = width;
		}

		public ColumnInfo(int position, TableColumn<?,?> c) {
			this(c.getText(), position, c.isVisible(), c.getWidth());
		}

		@Override
		public String toString() {
			return toS(S3, name, position, visible, width);
		}

		public static ColumnInfo fromString(String str) {
			String[] s = str.split(S3, -1);
			return new ColumnInfo(s[0], parseInt(s[1]), parseBoolean(s[2]), parseDouble(s[3]));
		}

		@Override
		public int compareTo(ColumnInfo o) {
			return Integer.compare(position, o.position);
		}
	}

	public static final class ColumnSortInfo {
		public final List<Tuple2<String,SortType>> sorts = new ArrayList<>();

		public ColumnSortInfo() {}

		public ColumnSortInfo(TableView<?> table) {
			fromTable(table);
		}

		public void fromTable(TableView<?> table) {
			sorts.clear();
			sorts.addAll(map(table.getSortOrder(), c -> Tuples.t(c.getText(), c.getSortType())));
		}

		public <T> void toTable(TableView<T> table) {
			List<TableColumn<T,?>> so = new ArrayList<>();
			table.getSortOrder().clear();
			sorts.forEach(t ->
					stream(table.getColumns())
							.filter(c -> t._1.equals(c.getText()))
							.findAny()
							.ifPresent(c -> {
								c.setSortType(t._2);
								so.add(c);
							})
			);
			table.getSortOrder().setAll(so);
		}

		@Override
		public String toString() {
			return toS(sorts, t -> t._1 + S3 + t._2, S2);
		}

		public static ColumnSortInfo fromString(String s) {
			ColumnSortInfo tsi = new ColumnSortInfo();
			tsi.sorts.addAll(split(s, S2, str -> {
				String[] a = str.split(S3, -1);
				return Tuples.t(a[0], SortType.valueOf(a[1]));
			}));
			return tsi;
		}

	}

}