package sp.it.pl.ui.objects.table

import javafx.scene.control.TableColumn
import javafx.scene.control.TableColumnBase
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.access.fieldvalue.ObjectFieldFlatMapped
import sp.it.util.collections.setTo
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.orNull
import sp.it.util.functional.recurse
import sp.it.util.functional.traverse

fun <T> FieldedTable<T>.getColumnsAll(): List<TableColumn<T,*>> = columnRoots.flatMap { it.recurse { it.columns } }

fun <T> FieldedTable<T>.setColumnStateImpl(state: TableColumnInfo) {
   val csByField = HashMap<ObjectField<T, *>, TableColumn<T, *>>()
   val csVisible = state.columns.asSequence()
      .filter { it.visible }
      .sorted()
      .map { it to columnIdToF(it.id) }
      .map { (state, f) ->
         // traverse upwards flatMap hierarchy bottom -> top & compute object fields for each level (avoid recomputing f, which is last)
         val fHierarchy = f
            .traverse { it.asIf<ObjectFieldFlatMapped<T, *, *>>()?.by?.asIf<ObjectField<T,*>>() }.map { it.asIf<ObjectFieldFlatMapped<T,*,*>>()?.from ?: it }
            .toList()
            .dropLast(1).runningReduce { p, c -> p.flatMap(c.asIs()) } + f
         val cHierarchy = fHierarchy.map {
            csByField.computeIfAbsent(it) {
               if (it===f) {
                  getColumnFactory<Any?>().call(it).apply {
                     prefWidth = state.width
                     isVisible = true
                  }
               } else {
                  columnGroupFactory(it).apply {
                     isVisible = true
                  }
               }
            }
         }
         cHierarchy.windowed(2) { it[0] to it[1] }.forEach { (pc, cc) -> if (cc !in pc.columns) pc.columns += cc }
         cHierarchy.first()
      }
      .distinct()
      .toList()

   columns setTo when {
      fieldsRootEnabled -> listOf(columnGroupFactory(fieldsRoot).apply { columns setTo csVisible })
      else -> csVisible
   }
   state.sortOrder.toTable(this) // restore sort order
}

fun <T> FieldedTable<T>.setColumnVisible(f: ObjectField<T,*>, v: Boolean) {
   // table must have at least 1 column
   if (!v && columnLeafs.size==1) return

   val csByField = getColumnsAll().associateBy { it.userData as ObjectField<*, *> }.toMutableMap()
   var c = getColumn(f).orNull()
   if (v && c==null) {
      c = getColumnFactory<Any?>().call(f).apply {
         prefWidth = columnState.columns[f.name()]!!.width
         isVisible = true
      }

      fun ObjectField<*,*>.addColumnWithParents(): TableColumn<*,*> =
          if (this is ObjectFieldFlatMapped<*,*,*>) {
            val pc = from.addColumnWithParents()
                pc.isVisible = true
            val cc = if (this===f) c else csByField.computeIfAbsent(this, columnGroupFactory)
                cc.isVisible = true
            if (cc !in pc.columns) pc.columns.asIs<MutableList<Any?>>() += cc
            pc
         } else {
            val cc = if (this===f) c else csByField.computeIfAbsent(this, columnGroupFactory)
                cc.isVisible = true
            if (cc !in columns) columns += cc
            cc
         }

      f.addColumnWithParents()
   }


   fun TableColumnBase<*,*>.remColumnWithParents() {
      val pc = parentColumn
      if (pc==null) {
         this@setColumnVisible.columns.asIs<MutableList<Any>>() -= this
      } else {
         pc.columns.asIs<MutableList<Any>>() -= this
         if (pc.columns.isEmpty())
            pc.remColumnWithParents()
      }
   }
   if (!v && c!=null)
      c.remColumnWithParents()
}