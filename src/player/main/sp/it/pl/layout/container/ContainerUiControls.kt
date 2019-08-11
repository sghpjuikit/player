package sp.it.pl.layout.container

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.NodeOrientation.LEFT_TO_RIGHT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.input.DragEvent.DRAG_DONE
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.TilePane
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.layout.widget.WidgetUi.Companion.PSEUDOCLASS_DRAGGED
import sp.it.pl.main.APP
import sp.it.pl.main.Df
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.contains
import sp.it.pl.main.get
import sp.it.pl.main.infoIcon
import sp.it.pl.main.installDrag
import sp.it.util.access.toggle
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.SHORTCUT
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.text.getNamePretty
import sp.it.util.ui.lay
import sp.it.util.ui.layFullArea
import sp.it.util.ui.removeFromParent
import sp.it.util.units.millis

class ContainerUiControls(override val area: ContainerUi<*>): ComponentUiControlsBase() {
   val root = AnchorPane()
   val icons = TilePane(4.0, 4.0)
   val disposer = Disposer()
   val a = anim(250.millis) {
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

         lay += infoIcon(""
            + "Controls for managing container."
            + "\n"
            + "\nAvailable actions:"
            + "\n\tLeft click : Go to child"
            + "\n\tRight click : Go to parent"
            + "\n\tDrag : Drags widget to other area"
            + "\n\tDrag + ${SHORTCUT.getNamePretty()} : Detach widget"
         ).styleclass("header-icon")

         lay += icon(null, "Lock container's layout").onClickDo {
            area.container.locked.toggle()
            APP.actionStream("Widget layout lock")
         }.apply {
            area.container.locked sync { icon(if (it) IconFA.LOCK else IconFA.UNLOCK) } on disposer
         }

         lay += icon(IconFA.GAVEL, "Actions\n\nDisplay additional action for this container.").onClickDo {
            APP.ui.actionPane.orBuild.show(area.container)
         }

         lay += icon(IconFA.TIMES, "Close widget").onClickDo {
            area.container.close()
            APP.actionStream("Close widget")
         }
      }

      a.applyAt(0.0)
      disposer += { a.stop() }
      disposer += { a.applyAt(0.0) }

      area.root.layFullArea += root
      disposer += { root.removeFromParent() }

      // component switching on drag
      installDrag(
         root, IconFA.EXCHANGE, "Switch components",
         { e -> Df.COMPONENT in e.dragboard },
         { e -> e.dragboard[Df.COMPONENT]===area.container },
         { e -> e.dragboard[Df.COMPONENT].swapWith(area.container.parent, area.container.indexInParent()!!) }
      )
      root.onEventDown(DRAG_DETECTED) { onDragDetected(it, root) }
      root.onEventDown(DRAG_DONE) {
         root.pseudoClassStateChanged(PSEUDOCLASS_DRAGGED, false)
      }

      // switch to container/normal layout mode using right/left click
      root.onEventDown(MOUSE_CLICKED) {
         if (area.isContainerMode && it.button==PRIMARY) {
            area.setContainerMode(false)
            it.consume()
         }
      }

      updateIcons()
   }

   fun updateIcons() {
      val c = area.container.parent
      if (c is BiContainer) {
         val isAbs = c.absoluteSize.value==area.container.indexInParent()!!
         absB = icon(if (isAbs) IconFA.UNLINK else IconFA.LINK, "Resize container proportionally").addExtraIcon().onClickDo {
            c.ui.toggleAbsoluteSizeFor(area.container.indexInParent()!!)
         }
      } else {
         absB.remExtraIcon()
      }

      autoLayoutB.remExtraIcon()
      if (c is FreeFormContainer) {
         autoLayoutB = icon(IconMD.VIEW_DASHBOARD, FreeFormContainerUi.autoLayoutTooltipText).addExtraIcon().onClickDo {
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