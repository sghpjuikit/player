package sp.it.pl.layout

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LINK
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.UNLINK
import javafx.geometry.NodeOrientation.LEFT_TO_RIGHT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Side
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.effect.BoxBlur
import javafx.scene.input.DragEvent.DRAG_DONE
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.input.MouseEvent.MOUSE_MOVED
import sp.it.pl.core.CoreMenus
import sp.it.pl.core.CoreMouse
import sp.it.pl.layout.WidgetUi.Companion.PSEUDOCLASS_DRAGGED
import sp.it.pl.main.APP
import sp.it.pl.main.Actions
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.IconFA
import sp.it.pl.main.Ui.ICON_CLOSE
import sp.it.pl.main.emScaled
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.util.access.toggle
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.functional.asIf
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.ui.centre
import sp.it.util.ui.dsl
import sp.it.util.ui.lay
import sp.it.util.ui.layFullArea
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.stackPane
import sp.it.util.ui.toP

class WidgetUiControls(override val area: WidgetUi): ComponentUiControlsBase() {
   val title = Label()
   val menuB: Icon
   val absB: Icon
   val lockB: Icon

   private val anim: Anim
   private val hiderWeak: Subscribed
   var isShowing = false
      private set
   var isShowingWeak = false
      private set

   init {
      root.id = "widget-ui-controls"
      root.styleClass += "widget-ui-controls"

      root.layFullArea += stackPane {
         isMouseTransparent = true

         lay += title
      }
      root.lay(0.0, 0.0, null, null) += icons.apply {
         nodeOrientation = LEFT_TO_RIGHT
         alignment = CENTER_RIGHT
         prefRows = 1
         prefColumns = 10

         val closeB = headerIcon(ICON_CLOSE, closeIconText) { close() }
         menuB = headerIcon(IconFA.CARET_DOWN, "Widget menu") { i ->
            ContextMenu().dsl { items { CoreMenus.menuItemBuilders[area.widget] } }.show(i, Side.BOTTOM, 0.0, 0.0)
         }
         lockB = headerIcon(null, lockIconText) { toggleLocked(); APP.actionStream("Widget layout lock") }
         absB = headerIcon(IconFA.LINK, absIconText) { toggleAbsSize(); updateAbsB() }

         lay += listOf(absB, lockB, menuB, closeB)
      }

      // build animations
      val blur = BoxBlur(0.0, 0.0, 1)
      anim = anim(APP.ui.layoutModeDuration) {
         root.opacity = it
         root.isVisible = it!=0.0
         root.isMouseTransparent = it!=1.0

         blur.width = it*APP.ui.layoutModeBlurStrength
         blur.height = it*APP.ui.layoutModeBlurStrength
         area.content.effect = if (APP.ui.layoutModeBlur.value) blur else null
         area.content.opacity = if (APP.ui.layoutModeOpacity.value) 1.0 - APP.ui.layoutModeOpacityStrength*it else 1.0
         area.content.isMouseTransparent = it==1.0
      }

      // weak mode and strong mode - strong mode is show/hide called from external code
      // - weak mode is show/hide by mouse enter/exit events in the corner
      // - the weak behavior must not work in strong mode
      // weak show - activator behavior
      var inside = false
      val p = area.contentRoot
      val showS = { e: MouseEvent ->
         if (!e.isPrimaryButtonDown && !e.isSecondaryButtonDown && !e.isMiddleButtonDown)
            if (!isShowingWeak && !area.widget.lockedUnder.value && !isShowing) {
               val isIn = p.width - activatorW.emScaled<e.x && activatorH.emScaled>e.y
               if (inside!=isIn) {
                  inside = isIn
                  if (isIn) showWeak()
               }
            }
      }
      p.onEventUp(MOUSE_MOVED) { showS(it) }
      p.onEventUp(MOUSE_ENTERED) { showS(it) }
      p.onEventUp(MOUSE_EXITED) { inside = false }

      hiderWeak = Subscribed {
         val eh = { _: Any? -> tryHide() }
         Subscription(
            p.onEventUp(MOUSE_EXITED, eh),
            root.onEventUp(MOUSE_MOVED, eh),
            root.onEventUp(MOUSE_EXITED, eh),
            root.onEventUp(DRAG_DONE) { eh(null) },
         )
      }

      root.onEventDown(DRAG_DETECTED) { onDragDetected(it, root) }
      root.onEventDown(DRAG_DONE) { root.pseudoClassStateChanged(PSEUDOCLASS_DRAGGED, false) }

   }

   fun isShowSettings(): Boolean {
      val key = "settingsWindow"
      val popup = menuB.properties[key].asIf<PopWindow>()
      return popup!=null
   }

   fun showSettingsClose() {
      val key = "settingsWindow"
      menuB.properties[key]?.asIf<PopWindow>().ifNotNull { it.hide() }
   }

   fun showSettings() {
      val key = "settingsWindow"
      menuB.properties[key]?.asIf<PopWindow>().ifNotNull { it.focus() }.ifNull {
         tryHideAfterSettings()
         APP.windowManager.showSettings(area.widget, menuB).apply {
            menuB.properties[key] = this
            onHiding += { menuB.properties[key] = null }
         }
      }
   }

   private fun toggleLocked() = area.widget.locked.toggle()

   private fun close() = AppAnimator.closeAndDo(area.contentRoot) { area.container.removeChild(area.index) }

   private fun toggleAbsSize() {
      if (area.container is ContainerBi)
         area.container.ui?.toggleAbsoluteSizeFor(area.index)
   }

   fun updateAbsB() {
      if (area.container is ContainerBi) {
         absB.icon(area.container.absoluteSize.value==area.index, UNLINK, LINK)
         if (absB !in icons.children) icons.children.add(0, absB)
      } else {
         icons.children -= absB
      }
   }

   private fun showWeak() {
      isShowingWeak = true
      hiderWeak.subscribe(true)
      anim.playOpen()
      if (!APP.ui.layoutModeBroad.value) APP.actionStream(Actions.LAYOUT_MODE)

      updateAbsB()
   }

   private fun hideWeak() {
      hiderWeak.subscribe(false)
      isShowingWeak = false
      anim.playClose()
   }

   private fun tryHideAfterSettings() {
      if (isShowingWeak && !isShowing) hideWeak()
   }

   private fun tryHide() {
      val mouse = CoreMouse.mousePosition
      if (
         isShowingWeak &&  // ignore when not showing
         !isShowing &&     // ignore in strong mode
         PSEUDOCLASS_DRAGGED !in root.pseudoClassStates &&   // keep visible when dragging
         (root.scene==null || mouse !in root.localToScreen(root.layoutBounds) || icons.children.all { it.localToScreen(it.layoutBounds).centre distance mouse.toP()>100 })
      ) {
         hideWeak()
      }
   }

   fun show() {
      isShowing = true
      area.contentRoot.pseudoClassChanged("layout-mode", true)
      showWeak()
   }

   fun hide() {
      isShowing = false
      area.contentRoot.pseudoClassChanged("layout-mode", false)
      hideWeak()
   }

   companion object {
      private const val activatorW = 20.0
      private const val activatorH = 20.0
      private const val absIconText = ("Absolute size\n\n"
         + "Prevents widget from resizing proportionally to parent container's "
         + "size. Instead, the widget will keep the same size, if possible.")
      private const val lockIconText = ("Lock widget\n\n"
         + "Disallows layout mode when mouse enters top corner of the widget. \n"
         + "This can be applied separately on widgets, but also containers or "
         + "whole layout.")
      private const val propIconText = "Settings\n\n" + "Displays widget properties."
      private const val actIconText = ("Actions\n\n"
         + "Opens action chooser for this widget. Action chooser displays and "
         + "can run an action on some data, in this case this widget. Shows "
         + "non-layout operations.")
      private const val closeIconText = "Close widget\n\n" + "Closes widget and creates empty place in the container."
   }

}