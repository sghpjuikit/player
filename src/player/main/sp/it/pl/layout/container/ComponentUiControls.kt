package sp.it.pl.layout.container

import javafx.scene.Node
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.TransferMode
import sp.it.pl.layout.widget.WidgetUi
import sp.it.pl.main.Df
import sp.it.pl.main.set

interface ComponentUiControls

abstract class ComponentUiControlsBase: ComponentUiControls {
    abstract val area: ComponentUiBase

    protected fun onDragDetected(e: MouseEvent, root: Node) {
        if (e.button==MouseButton.PRIMARY) {
            if (e.isShortcutDown) {
                area.detach()
                e.consume()
            } else {
                if (area.getActiveComponent().parent !is FreeFormContainer) {
                    val db = root.startDragAndDrop(*TransferMode.ANY)
                    db[Df.COMPONENT] = area.getActiveComponent()
                    root.pseudoClassStateChanged(WidgetUi.PSEUDOCLASS_DRAGGED, true)
                    e.consume()
                }
            }
        }
    }
}