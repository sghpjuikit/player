package sp.it.pl.ui.objects.window

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.Pos.BASELINE_CENTER
import javafx.geometry.Pos.BASELINE_LEFT
import javafx.geometry.Pos.BASELINE_RIGHT
import javafx.geometry.Pos.BOTTOM_CENTER
import javafx.geometry.Pos.BOTTOM_LEFT
import javafx.geometry.Pos.BOTTOM_RIGHT
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Pos.TOP_CENTER
import javafx.geometry.Pos.TOP_LEFT
import javafx.geometry.Pos.TOP_RIGHT
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.stage.Screen
import javafx.stage.Window
import sp.it.pl.core.NameUi
import sp.it.pl.main.APP
import sp.it.util.functional.asIf
import sp.it.util.math.P
import sp.it.util.ui.bounds
import sp.it.util.ui.centreX
import sp.it.util.ui.centreY
import sp.it.util.ui.getScreenForMouse
import sp.it.util.ui.size
import sp.it.util.ui.x

/** Specifies how a window is to be shown, defining an owner and position. Allows unified API for showing windows. */
data class Shower(val owner: Window?, val show: (Window) -> P) {
   /** @return shower with the same [Shower.owner], but [Shower.show] mapped with the specified [mapper] */
   fun map(mapper: Window.(P) -> P): Shower = Shower(owner) { it.mapper(show(it)) }
}

enum class ShowArea(override val nameUi: String): NameUi {
   SCREEN_ACTIVE("Screen containing mouse"),
   SCREEN_PRIMARY("Main screen"),
   ALL_SCREENS("Rectangle of all screens"),
   WINDOW_MAIN("Main window"),
   WINDOW_ACTIVE("Focused window");

   operator fun invoke(pos: Pos): Shower = bounds().let { Shower(it.first) { p -> it.second.toP(pos) - p.offset(pos) + p.padding(pos) } }

   fun bounds(): Pair<Window?, Rectangle2D> = when (this) {
      SCREEN_ACTIVE -> null to getScreenForMouse().bounds
      SCREEN_PRIMARY -> null to Screen.getPrimary().bounds
      ALL_SCREENS -> {
         val ss = Screen.getScreens().map { it.bounds }
         val minX = ss.asSequence().map { it.minX }.minOrNull()!!
         val maxX = ss.asSequence().map { it.maxX }.maxOrNull()!!
         val minY = ss.asSequence().map { it.minY }.minOrNull()!!
         val maxY = ss.asSequence().map { it.maxY }.maxOrNull()!!
         null to Rectangle2D(minX, minY, maxX - minX, maxY - minY)
      }
      WINDOW_MAIN -> APP.windowManager.getMain()?.stage?.let { it to it.bounds } ?: SCREEN_ACTIVE.bounds()
      WINDOW_ACTIVE -> APP.windowManager.getFocusedFxWithChild()?.let { it to it.bounds } ?: SCREEN_ACTIVE.bounds()
   }

   private fun Window.padding(pos: Pos): P {
      val root = scene?.root?.asIf<WindowRoot>() ?: scene?.root?.childrenUnmodifiable?.firstOrNull()?.asIf<WindowRoot>()
      val p = root?.windowPadding?.value ?: Insets.EMPTY
      return when (pos) {
         TOP_LEFT -> p.left x p.top
         TOP_CENTER -> 0 x p.top
         TOP_RIGHT -> -p.right x p.top
         CENTER_LEFT -> p.left x 0
         CENTER -> 0 x 0
         CENTER_RIGHT -> -p.right x 0
         BASELINE_LEFT -> p.left x 0
         BASELINE_CENTER -> 0 x 0
         BASELINE_RIGHT -> -p.right x 0
         BOTTOM_LEFT -> p.left x -p.bottom
         BOTTOM_CENTER -> 0 x -p.bottom
         BOTTOM_RIGHT -> -p.right x -p.bottom
      }
   }

   private fun Window.offset(pos: Pos): P = when (pos) {
      TOP_LEFT -> 0 x 0
      TOP_CENTER -> width/2.0 x 0
      TOP_RIGHT -> width x 0
      CENTER_LEFT -> 0 x height
      CENTER -> width/2.0 x height/2.0
      CENTER_RIGHT -> width x height/2.0
      BASELINE_LEFT -> 0 x height/2.0
      BASELINE_CENTER -> width/2.0 x height/2.0
      BASELINE_RIGHT -> width x height/2.0
      BOTTOM_LEFT -> 0 x height
      BOTTOM_CENTER -> width/2.0 x height
      BOTTOM_RIGHT -> size
   }

   private fun Rectangle2D.toP(pos: Pos): P = when (pos) {
      TOP_LEFT -> minX x minY
      TOP_CENTER -> centreX x minY
      TOP_RIGHT -> maxX x minY
      CENTER_LEFT -> minX x centreY
      CENTER -> centreX x centreY
      CENTER_RIGHT -> maxX x centreY
      BASELINE_LEFT -> minX x centreY
      BASELINE_CENTER -> centreX x centreY
      BASELINE_RIGHT -> maxX x centreY
      BOTTOM_LEFT -> minX x maxY
      BOTTOM_CENTER -> centreX x maxY
      BOTTOM_RIGHT -> maxX x maxY
   }
}

/** Defines position in relation to a node. */
enum class NodeShow {
   CENTER,
   UP_LEFT,
   UP_CENTER,
   UP_RIGHT,
   DOWN_LEFT,
   DOWN_CENTER,
   DOWN_RIGHT,
   RIGHT_UP,
   RIGHT_CENTER,
   RIGHT_DOWN,
   LEFT_UP,
   LEFT_CENTER,
   LEFT_DOWN;

   operator fun invoke(node: Node): Shower = Shower(node.scene?.window) { computeX(node, it) x computeY(node, it) }

   private fun computeX(n: Node, popup: Window): Double {
      val w = popup.width
      val x = n.localToScreen(0.0, 0.0).x
      return when (this) {
         CENTER, DOWN_CENTER, UP_CENTER -> x + n.layoutBounds.width/2 - w/2
         LEFT_CENTER, LEFT_UP, LEFT_DOWN -> x - w
         RIGHT_CENTER, RIGHT_UP, RIGHT_DOWN -> x + n.layoutBounds.width
         UP_LEFT, DOWN_LEFT -> x
         UP_RIGHT, DOWN_RIGHT -> x + n.layoutBounds.width - w
      }
   }

   private fun computeY(n: Node, popup: Window): Double {
      val h = popup.height
      val y = n.localToScreen(0.0, 0.0).y
      return when (this) {
         UP_RIGHT, UP_CENTER, UP_LEFT -> y - h
         DOWN_CENTER, DOWN_LEFT, DOWN_RIGHT -> y + n.layoutBounds.height
         LEFT_UP, RIGHT_UP -> y
         CENTER, LEFT_CENTER, RIGHT_CENTER -> y + n.layoutBounds.height/2 - h/2
         LEFT_DOWN, RIGHT_DOWN -> y + n.layoutBounds.height - h
      }
   }

}