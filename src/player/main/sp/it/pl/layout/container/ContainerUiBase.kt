/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sp.it.pl.layout.container

import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.layout.AnchorPane
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.io.IOLayer
import sp.it.pl.main.AppAnimator
import sp.it.util.access.ref.LazyR
import sp.it.util.reactive.sync
import sp.it.util.ui.pseudoClassChanged

abstract class ContainerUiBase<C: Container<*>>: ContainerUi {

    final override val root: AnchorPane
    @JvmField val container: C
    @JvmField var controls = LazyR<ContainerUiControls> { buildControls() }
    @JvmField var isLayoutMode = false
    @JvmField var isContainerMode = false

    constructor(container: C) {
        this.container = container
        this.root = AnchorPane()
        root.styleClass += "container-ui"

        // report component graphics changes
        root.parentProperty() sync { IOLayer.allLayers.forEach { it.requestLayout() } }
        root.layoutBoundsProperty() sync { IOLayer.allLayers.forEach { it.requestLayout() } }

        // switch to container/normal layout mode using right/left click
        root.setOnMouseClicked {
            if (isLayoutMode && !isContainerMode && it.button==SECONDARY) {
                if (container.children.isEmpty()) {
                    AppAnimator.closeAndDo(root) { container.close() }
                } else {
                    setContainerMode(true)
                }
                it.consume()
            }
        }
    }

    protected open fun buildControls() = ContainerUiControls(this)

    override fun show() {
        isLayoutMode = true
        root.pseudoClassChanged("layout-mode", true)

        container.children.values.forEach {
            if (it is Container<*>) it.show()
            if (it is Widget) it.areaTemp?.show()
        }
    }

    override fun hide() {
        if (isContainerMode) setContainerMode(false)

        isLayoutMode = false
        root.pseudoClassChanged("layout-mode", false)

        container.children.values.forEach {
            if (it is Container<*>) it.hide()
            if (it is Widget) it.areaTemp?.hide()
        }
    }

    internal fun setContainerMode(b: Boolean) {
        if (isContainerMode==b) return

        isContainerMode = b
        controls.get().toFront()
        controls.get().a.playFromDir(b)
        if (!b) {
            controls.get().a.setOnFinished {
                controls.get().disposer.invoke()
                controls = LazyR { buildControls() }
            }
        }
    }

    // TODO: implement & use & merge with Area.detach
    private fun detach() {}

}