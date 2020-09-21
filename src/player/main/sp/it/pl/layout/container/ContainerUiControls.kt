package sp.it.pl.layout.container

import javafx.geometry.NodeOrientation.LEFT_TO_RIGHT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.input.DragEvent.DRAG_DONE
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.layout.widget.WidgetUi.Companion.PSEUDOCLASS_DRAGGED
import sp.it.pl.main.APP
import sp.it.pl.main.Df
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.contains
import sp.it.pl.main.get
import sp.it.pl.main.installDrag
import sp.it.util.access.toggle
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.ui.lay
import sp.it.util.ui.layFullArea
import sp.it.util.ui.removeFromParent
import sp.it.util.units.millis

class ContainerUiControls(override val area: ContainerUi<*>): ComponentUiControlsBase() {
   val disposer = Disposer()
   val anim = anim(250.millis) {
      root.opacity = it
      root.isMouseTransparent = it==0.0
      area.root.children.forEach { c ->
         if (c!==root)
            c.opacity = 1 - 0.8*it
      }
   }
   private var absB: Icon? = null
   private var autoLayoutB: Icon? = null

   init {
      root.id = "container-ui-controls"
      root.styleClass += "container-ui-controls"
      root.lay(0.0, 0.0, null, 0.0) += icons.apply {
         nodeOrientation = LEFT_TO_RIGHT
         alignment = CENTER_RIGHT
         prefColumns = 10
         prefHeight = 25.0

         lay += headerIcon(null, "Lock container's layout") {
            area.container.locked.toggle()
            APP.actionStream("Widget layout lock")
         }.apply {
            area.container.locked sync { icon(if (it) IconFA.LOCK else IconFA.UNLOCK) } on disposer
         }

         lay += headerIcon(IconFA.GAVEL, "Actions\n\nDisplay additional action for this container.") {
            APP.ui.actionPane.orBuild.show(area.container)
         }

         lay += headerIcon(IconFA.TIMES, "Close widget") {
            area.container.close()
            APP.actionStream("Close widget")
         }
      }

      anim.applyAt(0.0)
      disposer += { anim.stop() }
      disposer += { anim.applyAt(0.0) }

      area.root.layFullArea += root
      disposer += { root.removeFromParent() }

      root.onEventDown(DRAG_DETECTED) { onDragDetected(it, root) }
      root.onEventDown(DRAG_DONE) {
         root.pseudoClassStateChanged(PSEUDOCLASS_DRAGGED, false)
      }
      // switch component on drag
      root.installDrag(
         IconFA.EXCHANGE, "Switch components",
         { e -> Df.COMPONENT in e.dragboard },
         { e -> e.dragboard[Df.COMPONENT]===area.container },
         { e -> e.dragboard[Df.COMPONENT].swapWith(area.container.parent, area.container.indexInParent()!!) }
      )
      // switch to container/normal layout mode using right/left click
      root.onEventDown(MOUSE_CLICKED) {
         if (area.isContainerMode && it.button==PRIMARY && it.clickCount==1) {
            area.setContainerMode(false)
            it.consume()
         }
      }

      updateIcons()
   }

   fun updateIcons() {
      val c = area.container.parent

      absB.remExtraIcon()
      if (c is BiContainer) {
         val isAbs = c.absoluteSize.value==area.container.indexInParent()!!
         absB = headerIcon(if (isAbs) IconFA.UNLINK else IconFA.LINK, "Resize container proportionally").addExtraIcon().onClickDo {
            c.ui.toggleAbsoluteSizeFor(area.container.indexInParent()!!)
         }
      }

      autoLayoutB.remExtraIcon()
      if (c is FreeFormContainer) {
         autoLayoutB = headerIcon(IconMD.VIEW_DASHBOARD, FreeFormContainerUi.autoLayoutTooltipText).addExtraIcon().onClickDo {
            c.ui.autoLayout(area.container)
         }
      }
   }

   @JvmOverloads
   fun Icon.addExtraIcon(at: Int = 0) = apply { if (this !in icons.children) icons.children.add(2+at, this) }

   private fun Icon?.remExtraIcon() = this?.let(icons.children::remove)

}