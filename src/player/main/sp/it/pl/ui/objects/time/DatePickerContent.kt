package sp.it.pl.ui.objects.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javafx.scene.control.Label
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.VBox
import kotlin.math.sign
import sp.it.util.access.v
import sp.it.util.collections.setTo
import sp.it.util.collections.tabulate1
import sp.it.util.math.min
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.ui.flowPane
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.pseudoClassToggle

/** Content for picking [LocalDate] */
class DatePickerContent(locale: Locale = Locale.getDefault()): VBox() {
   val locale = locale
   /** Whether user can change [value] through ui. Only if true. Default true. */
   val editable = v(true)
   /** Date value */
   val value = v(LocalDate.now())
   private val yearMonth = v<YearMonth>(YearMonth.of(value.value.year, value.value.month)).apply {
      this@DatePickerContent.value attach { value = YearMonth.of(it.year, it.month) }
   }

   init {
      styleClass += "date-picker-content"
      editable sync { pseudoClassToggle("readonly", !editable.value) }

      lay += flowPane {
         styleClass += "date-picker-content-header"
         lay += label {
            styleClass += "date-picker-content-header-year-label"
            yearMonth sync { text = it.year.toString() }
            onEventDown(SCROLL) { e -> if (editable.value) yearMonth.setValueOf { it.plusYears(-e.deltaY.sign.toLong()) } }
         }
         lay += label {
            styleClass += "date-picker-content-header-month-label"
            yearMonth sync { text = it.month.getDisplayName(TextStyle.FULL_STANDALONE, locale) }
            onEventDown(SCROLL) { e -> if (editable.value) yearMonth.setValueOf { it.plusMonths(-e.deltaY.sign.toLong()) } }
         }
      }
      lay += flowPane {
         styleClass += "date-picker-content-cells"
         onEventDown(SCROLL) { e -> if (editable.value) yearMonth.setValueOf { it.plusMonths(-e.deltaY.sign.toLong()) } }

         val dof = DayOfWeek.values().sortedBy { it.value%7 }
         lay += dof.map {
            label(it.getDisplayName(TextStyle.SHORT, locale)) {
               styleClass += "date-picker-content-cell"
               pseudoClassChanged("weekday", true)
            }
         }

         val cells = mutableListOf<Label>()
         yearMonth sync { ym ->

            val headCount = ym.atDay(1).dayOfWeek.value%7
            val dayCount = ym.lengthOfMonth()
            val tailCount = (7 - (headCount + dayCount)%7) min 6
            val tailCount2 = if (headCount + dayCount + tailCount==35) 7 else 0
            fun dayLabels(count: Int, block: (Int) -> Label) = tabulate1(count, block).toList().toTypedArray()
            fun dayLabel(i: Int, ym: YearMonth, isCurrent: Boolean) = label {
               text = "$i".padStart(2, ' ')
               styleClass += "date-picker-content-cell"
               pseudoClassChanged("other", !isCurrent)
               onEventDown(MouseEvent.MOUSE_CLICKED, MouseButton.PRIMARY) { if (editable.value) value.value = ym.atDay(i) }
            }

            lay -= cells
            cells setTo listOf(
               *dayLabels(headCount) { i -> dayLabel(i, ym.minusMonths(1), false) },
               *dayLabels(dayCount) { i -> dayLabel(i, ym, true) },
               *dayLabels(tailCount + tailCount2) { i -> dayLabel(i, ym.plusMonths(1), false) }
            )
            lay += cells
         }
      }
   }
}