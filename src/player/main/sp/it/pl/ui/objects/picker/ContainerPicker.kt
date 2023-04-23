package sp.it.pl.ui.objects.picker

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Orientation.VERTICAL
import sp.it.pl.layout.Container
import sp.it.pl.layout.ContainerBi
import sp.it.pl.layout.ContainerFreeForm
import sp.it.pl.layout.ContainerSeq
import sp.it.pl.main.APP
import sp.it.pl.main.IconUN
import sp.it.pl.main.emScaled
import sp.it.pl.ui.objects.picker.ContainerPicker.CellData
import sp.it.pl.ui.objects.picker.WidgetPicker.Mode.ALL
import sp.it.pl.ui.objects.picker.WidgetPicker.Mode.LAYOUTS
import sp.it.pl.ui.objects.picker.WidgetPicker.Mode.NODE
import sp.it.pl.ui.objects.picker.WidgetPicker.Mode.WIDGET
import sp.it.util.ui.x

/** Container picker. */
class ContainerPicker(onContainerSelect: (Container<*>) -> Unit, onWidgetSelect: (WidgetPicker.Mode) -> Unit): Picker<CellData>() {
   init {
      itemSupply = {
         sequenceOf(
            CellData("Split Vertically", IconUN(0x2ff0), "Splits space to left and right layout.") { onContainerSelect(ContainerBi(HORIZONTAL)) },
            CellData("Split Horizontally", IconUN(0x2ff1), "Splits space to top and bottom layout.") { onContainerSelect(ContainerBi(VERTICAL)) },
            CellData("Sequence Horizontal", IconUN(0x2ff2), "Splits space to top and bottom layout.") { onContainerSelect(ContainerSeq(VERTICAL)) },
            CellData("Sequence Vertical", IconUN(0x2ff3), "Splits space to top and bottom layout.") { onContainerSelect(ContainerSeq(VERTICAL)) },
            CellData("FreeForm", IconUN(0x2ffb), "Free form layout. Components behave like windows.") { onContainerSelect(ContainerFreeForm()) }
         ) + (
            if (APP.widgetManager.widgets.separateWidgets.value) {
               sequenceOf(
                  CellData(choiceTemplate, IconUN(0x2ff4), "Choose previously exported part of a layout.") { onWidgetSelect(LAYOUTS) },
                  CellData(choiceWidget, IconUN(0x2b1a), "Choose a widget using a widget chooser.") { onWidgetSelect(WIDGET) },
                  CellData(choiceNode, IconUN(0x2b1c), "Choose simple ui element wrapped as a widget.") { onWidgetSelect(NODE) }
               )
            } else {
               sequenceOf(
                  CellData(choiceWidget, IconUN(0x2b1a), "Choose a widget using a widget chooser.") { onWidgetSelect(ALL) }
               )
            }
            )
      }
      textConverter = { it.text }
      infoConverter = { it.info }
      iconConverter = { it.icon }
      minCellSize.value = 150.emScaled x 70.emScaled
   }

   class CellData(val text: String, val icon: GlyphIcons?, val info: String, val onSelect: () -> Unit)

   companion object {
      const val choiceWidget = "Widget"
      const val choiceTemplate = "Exported"
      const val choiceNode = "Simple UI element"
      val choiceForTemplate: String
         get() = if (APP.widgetManager.widgets.separateWidgets.value) choiceTemplate else choiceWidget
   }
}