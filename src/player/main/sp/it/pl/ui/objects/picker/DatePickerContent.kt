package sp.it.pl.ui.objects.picker

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle.FULL_STANDALONE
import java.time.format.TextStyle.SHORT
import java.time.temporal.WeekFields
import java.util.Locale
import javafx.geometry.Orientation.HORIZONTAL
import javafx.scene.control.Label
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.VBox
import kotlin.math.sign
import sp.it.util.access.v
import sp.it.util.collections.setTo
import sp.it.util.collections.tabulate0
import sp.it.util.math.min
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.ui.flowPane
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.pseudoClassToggle
import sp.it.util.ui.tilePane

/** Content for picking [LocalDate] */
class DatePickerContent(locale: Locale = Locale.getDefault()): VBox() {
   val locale = locale
   /** Whether user can change [value] through ui. Only if true. Default true. */
   val editable = v(true)
   /** Date value */
   val value = v<LocalDate>(LocalDate.now())
   private val yearMonth = v<YearMonth>(YearMonth.of(value.value.year, value.value.month)).apply {
      this@DatePickerContent.value attach { value = YearMonth.of(it.year, it.month) }
   }

   private val day1OfWeek = WeekFields.of(locale).firstDayOfWeek
   private val daysOfWeek = DayOfWeek.values().associateWith { (it.value - day1OfWeek.value + 7) %7 + 1 }
   private val DayOfWeek.valueLocal: Int get() = daysOfWeek[this]!!

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
            yearMonth sync { text = it.month.getDisplayName(FULL_STANDALONE, locale) }
            onEventDown(SCROLL) { e -> if (editable.value) yearMonth.setValueOf { it.plusMonths(-e.deltaY.sign.toLong()) } }
         }
      }
      lay += tilePane {
         styleClass += "date-picker-content-cells"
         orientation = HORIZONTAL
         prefColumns = 7
         onEventDown(SCROLL) { e -> if (editable.value) yearMonth.setValueOf { it.plusMonths(-e.deltaY.sign.toLong()) } }

         val dof = DayOfWeek.values().sortedBy { it.valueLocal }
         lay += dof.map {
            label(it.getDisplayName(SHORT, locale)) {
               styleClass += "date-picker-content-cell"
               pseudoClassChanged("weekday", true)
            }
         }

         val cells = mutableListOf<Label>()
         yearMonth sync { ym ->
            val headCount = ym.atDay(1).dayOfWeek.valueLocal
            val dayCount = ym.lengthOfMonth()
            val tailCount = (7 - (headCount + dayCount)%7) min 6
            val tailCount2 = if (headCount + dayCount + tailCount==35) 7 else 0
            val ymPrev = ym.minusMonths(1)
            val ymPrevLength = ymPrev.lengthOfMonth()
            val ymNext = ym.plusMonths(1)
            fun dayLabels(range: IntProgression, block: (Int) -> Label) = tabulate0(range.count()) { range.first + it }.map(block).toList().toTypedArray()
            fun dayLabel(i: Int, ym: YearMonth, isCurrent: Boolean) = label {
               text = "$i".padStart(2, ' ')
               styleClass += "date-picker-content-cell"
               pseudoClassChanged("other", !isCurrent)
               onEventDown(MOUSE_CLICKED, PRIMARY) { if (editable.value) value.value = ym.atDay(i) }
            }

            lay -= cells
            cells setTo listOf(
               *dayLabels(ymPrevLength - headCount + 1..ymPrevLength) { i -> dayLabel(i, ymPrev, false) },
               *dayLabels(1..dayCount) { i -> dayLabel(i, ym, true) },
               *dayLabels(1..(tailCount + tailCount2)) { i -> dayLabel(i, ymNext, false) }
            )
            lay += cells
         }
      }
   }
}