package sp.it.pl.layout.area

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.NodeOrientation.LEFT_TO_RIGHT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.input.DragEvent.DRAG_DONE
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.TransferMode
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.TilePane
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.layout.area.Area.Companion.PSEUDOCLASS_DRAGGED
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.container.bicontainer.BiContainer
import sp.it.pl.layout.container.freeformcontainer.FreeFormContainer
import sp.it.pl.main.APP
import sp.it.pl.main.Df
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.contains
import sp.it.pl.main.get
import sp.it.pl.main.infoIcon
import sp.it.pl.main.installDrag
import sp.it.pl.main.set
import sp.it.util.access.toggle
import sp.it.util.animation.Anim
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.ui.lay
import sp.it.util.ui.layFullArea
import sp.it.util.ui.removeFromParent
import sp.it.util.units.millis

class ContainerAreaControls(val area: ContainerNodeBase<*>): AnchorPane() {
    @JvmField val icons = TilePane(4.0, 4.0)
    @JvmField val disposer = Disposer()
    @JvmField val a = Anim.anim(250.millis) {
        opacity = it
        isMouseTransparent = it==0.0
        area.root.children.forEach { c ->
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
        lay(0.0, 0.0, null, 0.0) += icons.apply {
            nodeOrientation = LEFT_TO_RIGHT
            alignment = CENTER_RIGHT
            prefColumns = 10
            prefHeight = 25.0

            lay += infoIcon(
                    "Container controls."+
                    "\nActions:"+
                    "\n\tLeft click: visit children"+
                    "\n\tRight click: visit parent container"+
                    "\n\tMouse drag: switch with other component"
            ).styleclass("header-icon")

            lay += icon(null, "Lock container's layout").onClickDo {
                area.container.locked.toggle()
                APP.actionStream.invoke("Widget layout lock")
            }.apply {
                area.container.locked sync { icon(if (it) IconFA.LOCK else IconFA.UNLOCK) } on disposer
            }

            lay += icon(IconFA.GAVEL, "Actions\n\nDisplay additional action for this container.").onClickDo {
                APP.actionPane.show(Container::class.java, area.container)
            }

            lay += icon(IconFA.TIMES, "Close widget").onClickDo {
                area.container.close()
                APP.actionStream.invoke("Close widget")
            }
        }

        a.applyAt(0.0)
        disposer += { a.stop() }
        disposer += { a.applyAt(0.0) }

        area.root.layFullArea += this
        disposer += { removeFromParent() }

        // component switching on drag
        installDrag(
                this, IconFA.EXCHANGE, "Switch components",
                { e -> Df.COMPONENT in e.dragboard },
                { e -> e.dragboard[Df.COMPONENT]===area.container },
                { e -> e.dragboard[Df.COMPONENT].swapWith(area.container.parent, area.container.indexInParent()!!) }
        )
        onEventDown(DRAG_DETECTED) {
            if (it.button==PRIMARY) {
                startDragAndDrop(*TransferMode.ANY)[Df.COMPONENT] = area.container
                pseudoClassStateChanged(PSEUDOCLASS_DRAGGED, true)
                it.consume()
            }
        }
        onEventDown(DRAG_DONE) {
            pseudoClassStateChanged(PSEUDOCLASS_DRAGGED, false)
        }

        // switch to container/normal layout mode using right/left click
        onEventDown(MOUSE_CLICKED) {
            if (area.isContainerMode && it.button==PRIMARY) {
                area.setContainerMode(false)
                it.consume()
            }
        }

        updateIcons()
    }

    // TODO: finish and merge with WidgetArea
    fun updateIcons() {
        val c = area.container.parent
        if (c is BiContainer) {
            val isAbs = c.properties.getI("abs_size")==area.container.indexInParent()!!
            absB = icon(if (isAbs) IconFA.UNLINK else IconFA.LINK, "Resize container proportionally").addExtraIcon().onClickDo {
                c.ui.toggleAbsoluteSizeFor(area.container.indexInParent()!!)
            }
        } else {
            absB.remExtraIcon()
        }

        dragB.remExtraIcon()
        autoLayoutB.remExtraIcon()
        if (c is FreeFormContainer) {
            dragB = icon(IconFA.MAIL_REPLY, "Move container by dragging").addExtraIcon().apply {
                onEventDown(DRAG_DETECTED) {
                    if (it.button==PRIMARY) {
                        startDragAndDrop(*TransferMode.ANY)[Df.COMPONENT] = area.container
                        pseudoClassStateChanged(PSEUDOCLASS_DRAGGED, true)
                        it.consume()
                    }
                } on disposer
            }
            autoLayoutB = icon(IconMD.VIEW_DASHBOARD, FreeFormArea.autolayoutTootlip).addExtraIcon().onClickDo {
                c.ui.autoLayout(area.container)
            }
        }
    }

    fun Icon.addExtraIcon() = apply { if (this !in icons.children) icons.children.add(2, this) }

    private fun Icon?.remExtraIcon() = this?.let(icons.children::remove)

    companion object {
        private fun icon(glyph: GlyphIcons?, tooltip: String) = Icon(glyph, -1.0, tooltip).styleclass("header-icon")
    }
}