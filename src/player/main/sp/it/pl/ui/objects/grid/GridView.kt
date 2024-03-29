package sp.it.pl.ui.objects.grid

import java.util.function.Predicate
import javafx.beans.property.ObjectProperty
import javafx.collections.FXCollections
import javafx.collections.FXCollections.observableSet
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.SortedList
import javafx.css.StyleConverter
import javafx.css.StyleableObjectProperty
import javafx.event.Event.ANY
import javafx.event.EventHandler
import javafx.scene.control.Control
import kotlin.reflect.KClass
import kotlin.streams.asSequence
import sp.it.pl.ui.objects.search.SearchAutoCancelable
import sp.it.util.access.StyleableCompanion
import sp.it.util.access.V
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.access.fieldvalue.StringGetter
import sp.it.util.access.sv
import sp.it.util.access.svMetaData
import sp.it.util.functional.Functors.F1
import sp.it.util.functional.asIs
import sp.it.util.math.P
import sp.it.util.reactive.onChange
import sp.it.util.type.SealedEnum

/**
 * Two-dimensional virtualized control for displaying items in a two-dimensional grid.
 * It offers similar set of features as [sp.it.pl.ui.objects.table.FilteredTable], namely sorting, searching and filtering.
 *
 * This container is virtualized - the number of [sp.it.pl.ui.objects.grid.GridCell] instances depends on the number of
 * items displayed, not number in total. This helps to improve performance and reduce memory consumption.
 *
 * @param <T> type of item displayed in a cells of this grid
 * @param <F> type of object derived from item to filter items by
 * @see GridCell
 */
class GridView<T: Any, F: Any>(type: KClass<F>, filterMapper: F1<T, F>, backingList: ObservableList<T>): Control() {
   /** Type of object derived from item to filter items by. Used primarily for filter function provider. */
   val type: KClass<F> = type
   /** Initial filter criteria for the filter, used when filter is opened or additional filter added  */
   val filterMapper: F1<T, F> = filterMapper
   /** Initial filter criteria for the filter, used when filter is opened or additional filter added  */
   var filterPrimaryField: ObjectField<F, *>? = null
   /** Filtering for the items. Maintains [itemsPredicate]. */
   val search: Search = Search()
   /** Visibility of the bottom panel that controls information about items and menu bar. */
   val footerVisible = V(true)

   /** All items, without [itemsPredicate] and [itemsComparator] applied. See [itemsShown]. */
   val itemsRaw: ObservableList<T> = backingList
   /** Filtered items. Wraps [itemsRaw]. */
   val itemsFiltered: FilteredList<T> = FilteredList(itemsRaw)
   /**
    * [FilteredList.predicateProperty] of the [itemsFiltered].
    *
    * Null predicate will match all items (same as always true predicate).
    * The value reflects the filter generated by the user through the [search].
    *
    * Changing the predicate programmatically is possible, however the searchBox will not react on the change,
    * its effect will merely be overridden and when search box predicate changes, it will in turn override effect of a
    * custom predicate.
    */
   val itemsPredicate: ObjectProperty<Predicate<in T>?> = itemsFiltered.predicateProperty()
   /** Sorted items. Wraps [itemsFiltered]. */
   val itemsSorted: SortedList<T> = SortedList(itemsFiltered)
   /** [SortedList.comparatorProperty] of the [itemsSorted]. */
   val itemsComparator: ObjectProperty<Comparator<in T>> = itemsSorted.comparatorProperty()
   /** Visible items. [itemsRaw], with [itemsPredicate] and [itemsComparator] applied. Identical to [itemsSorted] */
   val itemsShown: ObservableList<T> = itemsSorted

   /**
    * Selection change activation strategies. Can be combined. Empty set will cause this grid to ignore selection.
    * By default, ([SelectionOn.MOUSE_CLICK],[SelectionOn.KEY_PRESS]).
    */
   val selectOn = observableSet(SelectionOn.MOUSE_CLICK, SelectionOn.KEY_PRESS)!!
   /** Selected item. */
   val selectedItem = V<T?>(null)
   /** Selected items. Intended for immediate consumption (it may be backed by an observable). */
   val selectedItems: Sequence<T>
      get() = sequenceOf(selectedItem.value).filterNotNull()
   /** [selectedItems] or [itemsShown] if none selected. Intended for immediate consumption (it may be backed by an observable). */
   val selectedOrAllItems: Sequence<T>
      get() = selectedItem.value?.let { sequenceOf(it) } ?: itemsShown.asSequence()

   /** [selectedOrAllItems] if orAll is true or [selectedItems]. Intended for immediate consumption (it may be backed by an observable). */
   fun getSelectedOrAllItems(orAll: Boolean): Sequence<T> = if (orAll) selectedOrAllItems else selectedItems

   /** Horizontal gap between grid cells */
   val horizontalCellSpacing: StyleableObjectProperty<Double> by sv(HORIZONTAL_CELL_SPACING)
   /** Horizontal gap between grid cells */
   val verticalCellSpacing: StyleableObjectProperty<Double> by sv(VERTICAL_CELL_SPACING)
   /** Cell width */
   val cellWidth: StyleableObjectProperty<Double> by sv(CELL_WIDTH)
   /** Cell height */
   val cellHeight: StyleableObjectProperty<Double> by sv(CELL_HEIGHT)
   /** Cell text height. Similar to [javafx.scene.control.TableView.fixedCellSize]. Has no effect by default but can be used to calculate [cellHeight]. */
   val cellHeightFixed: StyleableObjectProperty<Double> by sv(CELL_HEIGHT_FIXED)

   val cellWidthActual
      get() = cellWidth.value.takeIf { it!=CELL_SIZE_UNBOUND } ?: width

   /** Cell gap layout strategy. Default [CellGap.CENTER]. */
   val cellAlign = V<CellGap>(CellGap.CENTER)
   /** Maximum number of cells in a horizontal row. Null indicates no limit. Default null. */
   val cellMaxColumns = V<Int?>(null)
   /** Cell factory responsible for creating cells, Null indicates to skin to use its own default factory.  Default null. */
   val cellFactory = V<((GridView<T, F>) -> GridCell<T, F>)?>(null)

   init {
      isFocusTraversable = true
      styleClass += STYLE_CLASS

      // search
      search.installOn(this)
      addEventFilter(ANY) { if (search.isActive) search.updateSearchStyles() }
      itemsShown.onChange { if (search.isActive) search.updateSearchStyles() }

      onScroll = EventHandler { it.consume() }
   }

   val skinImpl: GridViewSkin<T, F>?
      get() = skin.asIs()

   val cellsShown: Sequence<GridCell<T, F>>
      get() = skinImpl?.cells?.asSequence().orEmpty()

   val cellsAll: Sequence<GridCell<T, F>>
      get() = skinImpl?.cellsAll?.asSequence().orEmpty()

   override fun createDefaultSkin(): GridViewSkin<T, F> = GridViewSkin(this)

   override fun getControlCssMetaData() = classCssMetaData

   /** Strategy for cell selection change activation behavior. */
   enum class SelectionOn {
      MOUSE_HOVER, MOUSE_CLICK, KEY_PRESS
   }

   /** Predefined cell sizes. */
   enum class CellSize(val width: Double) {
      SMALL_12(12.0),
      SMALL_24(24.0),
      SMALL_36(36.0),
      SMALL_48(48.0),
      SMALL_60(60.0),
      SMALL(80.0),
      SMALLER(120.0),
      NORMAL(160.0),
      LARGE(240.0),
      VERY_LARGE(400.0),
      GIANT(600.0),
      EXTREME(800.0)
   }

   /** Affects cell layout. */
   sealed interface CellGap {
      fun computeGap(grid: GridView<*, *>, width: Double, columns: Int): Double
      fun computeStartX(grid: GridView<*, *>, width: Double, columns: Int): Double
      companion object: SealedEnum<CellGap>(CellGap::class)

      /**
       * Will position cells at exact positions from the left. The gap will be constant, but the cells will not be
       * horizontally center aligned.
       */
      data object CENTER: CellGap {
         override fun computeGap(grid: GridView<*, *>, width: Double, columns: Int): Double = grid.verticalCellSpacing.value
         override fun computeStartX(grid: GridView<*, *>, width: Double, columns: Int): Double = RIGHT.computeStartX(grid, width, columns)/2.0
      }

      data object LEFT: CellGap {
         override fun computeGap(grid: GridView<*, *>, width: Double, columns: Int): Double = grid.verticalCellSpacing.value
         override fun computeStartX(grid: GridView<*, *>, width: Double, columns: Int): Double = 0.0
      }

      data object RIGHT: CellGap {
         override fun computeGap(grid: GridView<*, *>, width: Double, columns: Int): Double = grid.verticalCellSpacing.value
         override fun computeStartX(grid: GridView<*, *>, width: Double, columns: Int): Double = width - columns*grid.cellWidth.value - (columns - 1)*grid.verticalCellSpacing.value
      }

      /**
       * The cells will be horizontally center aligned, but the gap size will change depending on
       * the total row width and number of cells in a row.
       */
      data object JUSTIFY: CellGap {
         override fun computeGap(grid: GridView<*, *>, width: Double, columns: Int): Double = (width - columns*grid.cellWidth.value)/(columns - 1)
         override fun computeStartX(grid: GridView<*, *>, width: Double, columns: Int): Double = 0.0
      }
   }

   inner class Search: SearchAutoCancelable() {
      var field: StringGetter<F> = StringGetter.of<F?> { value, substitute -> value?.toString() ?: substitute }

      override fun doSearch(query: String) {
         for (i in itemsShown.indices) {
            val item = itemsShown[i]
            val itemS = if (item==null) null else field.getOfS(filterMapper.apply(item), "")
            if (itemS!=null && isMatchNth(itemS, query)) {
               skinImpl!!.select(i)
               updateSearchStyles()
               break
            }
         }
      }

      override fun cancel() {
         super.cancel()
         cellsAll.forEach {
            it.pseudoClassStateChanged(PC_SEARCH_MATCH, false)
            it.pseudoClassStateChanged(PC_SEARCH_MATCH_NOT, false)
         }
      }

      fun updateSearchStyles() {
         if (isCancelable.value) searchAutoCanceller.start(cancelActivityDelay.value)
         updateSearchStyleRowsNoReset()
      }

      // TODO: move to skin
      fun updateSearchStyleRowsNoReset() {
         val searchOn = isActive
         cellsShown.forEach { cell: GridCell<T, F> ->
            val item = cell.item
            val itemS = if (item==null) null else field.getOfS(filterMapper.apply(item), "")
            val isMatch = itemS!=null && isMatch(itemS, searchQuery.get())
            cell.pseudoClassStateChanged(PC_SEARCH_MATCH, searchOn && isMatch)
            cell.pseudoClassStateChanged(PC_SEARCH_MATCH_NOT, searchOn && !isMatch)
         }
      }
   }

   companion object: StyleableCompanion() {
      const val STYLE_CLASS = "grid-view"
      const val CELL_SIZE_UNBOUND = -1.0

      /** Creates an empty GridView with specified sizes. */
      inline operator fun <T: Any, reified F: Any> invoke(filterMapper: F1<T, F>, backingList: ObservableList<T>? = null): GridView<T, F> =
         GridView(F::class, filterMapper, backingList ?: FXCollections.observableArrayList())

      /** Creates an empty GridView with specified sizes. Convenience constructor. */
      inline operator fun <T: Any, reified F: Any> invoke(filterMapper: F1<T, F>, cellSize: P, gap: P, backingList: ObservableList<T>? = null): GridView<T, F> =
         GridView(filterMapper, backingList).apply {
            cellWidth.value = cellSize.x
            cellHeight.value = cellSize.y
            horizontalCellSpacing.value = gap.x
            verticalCellSpacing.value = gap.y
         }

      val HORIZONTAL_CELL_SPACING by svMetaData<GridView<*, *>, Double>("-fx-horizontal-cell-spacing", StyleConverter.getSizeConverter().asIs(), 12.0, GridView<*, *>::horizontalCellSpacing)
      val VERTICAL_CELL_SPACING by svMetaData<GridView<*, *>, Double>("-fx-vertical-cell-spacing", StyleConverter.getSizeConverter().asIs(), 12.0, GridView<*, *>::verticalCellSpacing)
      val CELL_WIDTH by svMetaData<GridView<*, *>, Double>("-fx-cell-width", StyleConverter.getSizeConverter().asIs(), 64.0, GridView<*, *>::cellWidth)
      val CELL_HEIGHT by svMetaData<GridView<*, *>, Double>("-fx-cell-height", StyleConverter.getSizeConverter().asIs(), 64.0, GridView<*, *>::cellHeight)
      val CELL_HEIGHT_FIXED by svMetaData<GridView<*, *>, Double>("-fx-fixed-cell-size", StyleConverter.getSizeConverter().asIs(), 12.0, GridView<*, *>::cellHeightFixed)
   }
}