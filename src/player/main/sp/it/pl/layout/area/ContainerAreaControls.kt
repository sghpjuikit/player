package sp.it.pl.layout.area

import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.TransferMode
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.TilePane
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.layout.area.Area.Companion.PSEUDOCLASS_DRAGGED
import sp.it.pl.layout.container.bicontainer.BiContainer
import sp.it.pl.layout.container.freeformcontainer.FreeFormContainer
import sp.it.pl.main.Df
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.set
import sp.it.util.animation.Anim
import sp.it.util.functional.asIf
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.onEventDown
import sp.it.util.ui.lay
import sp.it.util.ui.layFullArea
import sp.it.util.units.millis

class ContainerAreaControls(val area: ContainerNodeBase<*>): AnchorPane() {
    @JvmField val icons = TilePane(4.0, 4.0)
    @JvmField val disposer = Disposer()
    @JvmField val a = Anim.anim(250.millis) {
        opacity = it
        isMouseTransparent = it==0.0
        area.root_.children.forEach { c ->
            if (c!==this)
                c.opacity = 1-0.8*it
        }
    }
    private var absB: Icon? = null
    private var dragB: Icon? = null
    private var autoLayoutB: Icon? = null

    init {
        id = "container-area-controls"
        styleClass += "container-area-controls"

        lay += icons

        area.root_.layFullArea += this
        disposer += { area.root_.children -= this }

        a.applyAt(0.0)
        disposer += { a.stop() }
        disposer += { a.applyAt(0.0) }
    }

    // TODO: finish and merge with WidgetArea
    fun updateIcons() {
        val c = area.container.parent
        if (c is BiContainer) {
            val isAbs = c.properties.getI("abs_size")==area.container.indexInParent()!!
            val icon = absB ?: Icon(IconFA.LINK, -1.0, "Resize component proportionally").apply {
                styleclass("header-icon")
                onClickDo {
                    area.container.parent?.asIf<BiContainer>()?.let {
                        it.ui.toggleAbsoluteSizeFor(area.container.indexInParent()!!)
                    }
                }
            }
            icon.icon(if (isAbs) IconFA.UNLINK else IconFA.LINK)
            if (icon !in icons.children) icons.children.add(2, icon)
            absB = icon
        } else {
            absB?.let(icons.children::remove)
        }

        if (c is FreeFormContainer) {
            val icon1 = dragB ?: Icon(IconFA.MAIL_REPLY, -1.0, "Move component by dragging").apply {
                styleclass("header-icon")
                onEventDown(DRAG_DETECTED) {
                    if (it.button==PRIMARY) {
                        area.root_.startDragAndDrop(*TransferMode.ANY)[Df.COMPONENT] = area.container
                        pseudoClassStateChanged(PSEUDOCLASS_DRAGGED, true)
                        it.consume()
                    }
                }
            }
            if (icon1 !in icons.children) icons.children.add(2, icon1)
            dragB = icon1

            val icon2 = autoLayoutB ?: Icon(IconMD.VIEW_DASHBOARD, -1.0, FreeFormArea.autolayoutTootlip).apply {
                styleclass("header-icon")
                onClickDo { c.ui.autoLayout(area.container) }
            }
            autoLayoutB = icon2
            if (icon2 !in icons.children) icons.children.add(2, icon2)
        } else {
            dragB?.let(icons.children::remove)
            autoLayoutB?.let(icons.children::remove)
        }
    }

}