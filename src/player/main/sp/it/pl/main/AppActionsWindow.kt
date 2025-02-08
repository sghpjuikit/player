package sp.it.pl.main

import javafx.stage.Window
import sp.it.pl.ui.pane.action

/** Denotes actions for [Window] */
object AppActionsWindow {

   val windowFocus = action<Window>("Focus", "Focus this window.", IconFA.EYE) { w -> w.requestFocus() }

   val windowResizeToContent = action<Window>("Resize to content", "Resize to content.", IconMA.CROP) { w -> w.sizeToScene() }

}