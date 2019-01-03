package sp.it.pl.gui.objects.picker

import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Orientation.VERTICAL
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.container.bicontainer.BiContainer
import sp.it.pl.layout.container.freeformcontainer.FreeFormContainer

/** Container picker. */
class ContainerPicker(onContainerSelect: (Container<*>) -> Unit, onWidgetSelect: () -> Unit): Picker<ContainerPicker.CellData>() {
    init {
        itemSupply = {
            sequenceOf(
                    CellData("Split Vertically", "Splits space to left and right layout.") { onContainerSelect(BiContainer(HORIZONTAL)) },
                    CellData("Split Horizontally", "Splits space to top and bottom layout.") { onContainerSelect(BiContainer(VERTICAL)) },
                    CellData("FreeForm", "Free form layout. Components behave like windows.") { onContainerSelect(FreeFormContainer()) },
                    CellData("Widget", "Choose a widget using a widget chooser.") { onWidgetSelect() }
            )
        }
        textConverter = { it.text }
        infoConverter = { it.info }
    }

    class CellData(val text: String, val info: String, val onSelect: () -> Unit)
}