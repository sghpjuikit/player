package sp.it.pl.ui.objects.table

import java.lang.Boolean.parseBoolean
import java.lang.Double.parseDouble
import java.lang.Integer.parseInt
import javafx.scene.control.TableColumn
import javafx.scene.control.TableColumn.SortType
import sp.it.util.collections.mapset.MapSet
import sp.it.util.collections.setTo
import sp.it.util.functional.Try
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.runTry

/** Data structure for holding complete table column information, mostly for serializing and deserializing. */
class TableColumnInfo() {

   /** See [FieldedTable.getColumnLeafs]. Contains only leaf columns. */
   @JvmField val columns: MapSet<String, ColumnInfo>
   /** See [FieldedTable.columnIdMapper] */
   @JvmField var columnIdMapper: (String) -> String = { id -> id }
   /** See [FieldedTable.sortOrder]. */
   @JvmField var sortOrder: ColumnSortInfo

   init {
      columns = MapSet(LinkedHashMap()) { columnIdMapper(it.id) }
      sortOrder = ColumnSortInfo()
   }

   constructor(allColumns: List<String>): this() {
      columns += allColumns.mapIndexed { i, name -> ColumnInfo(name, i, true, 50.0) }
   }

   constructor(table: FieldedTable<*>): this() {
      columnIdMapper = table.columnIdMapper
      columns += table.columns.mapIndexed { i, c -> ColumnInfo.fromColumn(i, c) }
      sortOrder = ColumnSortInfo.fromTable(table)
   }

   fun update(table: FieldedTable<*>) {
      val old = MapSet(columns) { columnIdMapper(it.id) }
      columns.clear()

      // add visible columns
      table.columnLeafs.mapIndexed { i, c -> ColumnInfo.fromColumn(i, c) }.forEach {
         old -= it
         columns += it
      }

      // add invisible columns
      val i = columns.size
      columns += old.map { ColumnInfo(it.id, i + it.position, false, it.width) }

      sortOrder = ColumnSortInfo.fromTable(table)
   }

   override fun toString() = columns.joinToString(S2) + S1 + sortOrder

   companion object {
      private const val S1 = "+"
      private const val S2 = ";"
      private const val S3 = ","

      fun fromString(str: String): Try<TableColumnInfo, Throwable> = runTry {
         TableColumnInfo().apply {
            val a = str.split(S1)
            columns += a[0].split(S2).filter { it.isNotEmpty() }.map { ColumnInfo.fromString(it).orThrow }
            sortOrder = ColumnSortInfo.fromString(a[1]).orThrow
         }
      }
   }

   /** Data structure for single table column information, mostly for serializing and deserializing. */
   data class ColumnInfo(@JvmField val id: String, @JvmField val position: Int, @JvmField val visible: Boolean, @JvmField val width: Double): Comparable<ColumnInfo> {

      override fun compareTo(other: ColumnInfo): Int = position compareTo other.position

      override fun toString() = sequenceOf(id, position, visible, width).joinToString(S3)

      companion object {

         fun fromColumn(position: Int, c: TableColumn<*, *>) = ColumnInfo(c.id, position, c.isVisible, c.width)

         fun fromString(str: String): Try<ColumnInfo, Throwable> = runTry {
            val s = str.split(S3)
            ColumnInfo(s[0], parseInt(s[1]), parseBoolean(s[2]), parseDouble(s[3]))
         }

      }
   }

   data class ColumnSortInfo(val sorts: List<Pair<String, SortType>> = listOf()) {

      fun <T> toTable(table: FieldedTable<T>) {
         table.sortOrder setTo sorts.mapNotNull { (id, sortType) ->
            table.columns.find { id==it.id }.ifNotNull {
               it.sortType = sortType
            }
         }
         table.sort()
      }

      override fun toString() = sorts.joinToString(S2) { (first, second) -> first + S3 + second }

      companion object {

         fun fromTable(table: FieldedTable<*>) = ColumnSortInfo(table.sortOrder.map { Pair(it.id, it.sortType) })

         fun fromString(s: String): Try<ColumnSortInfo, Throwable> = runTry {
            ColumnSortInfo(
               s.split(S2).filter { it.isNotEmpty() }.map {
                  val a = it.split(S3)
                  Pair(a[0], SortType.valueOf(a[1]))
               }
            )
         }

      }

   }

}