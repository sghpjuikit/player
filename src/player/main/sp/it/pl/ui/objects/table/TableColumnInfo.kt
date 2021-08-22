package sp.it.pl.ui.objects.table

import java.lang.Boolean.parseBoolean
import java.lang.Double.parseDouble
import java.lang.Integer.parseInt
import javafx.scene.control.TableColumn
import javafx.scene.control.TableColumn.SortType
import javafx.scene.control.TableView
import sp.it.util.collections.mapset.MapSet
import sp.it.util.collections.setTo
import sp.it.util.functional.Try
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.runTry

/** Data structure for holding complete table column information, mostly for serializing and deserializing. */
class TableColumnInfo() {

   @JvmField val columns: MapSet<String, ColumnInfo>
   @JvmField var sortOrder: ColumnSortInfo
   @JvmField var nameKeyMapper: (String) -> String = { it }

   init {
      columns = MapSet { nameKeyMapper(it.name) }
      sortOrder = ColumnSortInfo()
   }

   constructor(allColumns: List<String>): this() {
      columns += allColumns.mapIndexed { i, name -> ColumnInfo(name, i, true, 50.0) }
   }

   constructor(table: TableView<*>): this() {
      columns += table.columns.mapIndexed { i, c -> ColumnInfo.fromColumn(i, c) }
      sortOrder = ColumnSortInfo.fromTable(table)
   }

   fun update(table: TableView<*>) {
      val old = MapSet(columns) { nameKeyMapper(it.name) }
      columns.clear()

      // add visible columns
      table.columns.mapIndexed { i, c -> ColumnInfo.fromColumn(i, c) }.forEach {
         old -= it
         columns += it
      }

      // add invisible columns
      val i = columns.size
      columns += old.map { ColumnInfo(it.name, i + it.position, false, it.width) }

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
   class ColumnInfo(@JvmField val name: String, @JvmField var position: Int, @JvmField val visible: Boolean, @JvmField val width: Double): Comparable<ColumnInfo> {

      override fun compareTo(other: ColumnInfo): Int = position.compareTo(other.position)

      override fun toString() = sequenceOf(name, position, visible, width).joinToString(S3)

      companion object {

         fun fromColumn(position: Int, c: TableColumn<*, *>) = ColumnInfo(c.text, position, c.isVisible, c.width)

         fun fromString(str: String): Try<ColumnInfo, Throwable> = runTry {
            val s = str.split(S3)
            ColumnInfo(s[0], parseInt(s[1]), parseBoolean(s[2]), parseDouble(s[3]))
         }

      }
   }

   class ColumnSortInfo(val sorts: List<Pair<String, SortType>> = listOf()) {

      fun <T> toTable(table: TableView<T>) {
         table.sortOrder setTo sorts.mapNotNull { (text, sortType) ->
            table.columns.find { text==it.text }.ifNotNull {
               it.sortType = sortType
            }
         }
      }

      override fun toString() = sorts.joinToString(S2) { (first, second) -> first + S3 + second }

      companion object {

         fun fromTable(table: TableView<*>) = ColumnSortInfo(table.sortOrder.map { Pair(it.text, it.sortType) })

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