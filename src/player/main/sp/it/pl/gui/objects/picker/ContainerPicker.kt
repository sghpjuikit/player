package sp.it.pl.gui.objects.picker

import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Orientation.VERTICAL
import sp.it.pl.gui.objects.picker.WidgetPicker.Mode.ALL
import sp.it.pl.gui.objects.picker.WidgetPicker.Mode.LAYOUTS
import sp.it.pl.gui.objects.picker.WidgetPicker.Mode.WIDGET
import sp.it.pl.layout.container.BiContainer
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.container.FreeFormContainer
import sp.it.pl.main.APP

/** Container picker. */
class ContainerPicker(onContainerSelect: (Container<*>) -> Unit, onWidgetSelect: (WidgetPicker.Mode) -> Unit): Picker<ContainerPicker.CellData>() {
    init {
        itemSupply = {
            sequenceOf(
                    CellData("Split Vertically", "Splits space to left and right layout.") { onContainerSelect(BiContainer(HORIZONTAL)) },
                    CellData("Split Horizontally", "Splits space to top and bottom layout.") { onContainerSelect(BiContainer(VERTICAL)) },
                    CellData("FreeForm", "Free form layout. Components behave like windows.") { onContainerSelect(FreeFormContainer()) }
            ) + (
                if (APP.widgetManager.widgets.separateWidgets.value) {
                    sequenceOf(
                        CellData("Template", "Choose previously exported part of a layout.") { onWidgetSelect(LAYOUTS) },
                        CellData("Widget", "Choose a widget using a widget chooser.") { onWidgetSelect(WIDGET) }
                    )
                } else {
                    sequenceOf(
                        CellData("Widget", "Choose a widget using a widget chooser.") { onWidgetSelect(ALL) }
                    )
                }
            )
        }
        textConverter = { it.text }
        infoConverter = { it.info }
    }

    class CellData(val text: String, val info: String, val onSelect: () -> Unit)
}