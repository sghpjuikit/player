package sp.it.pl.ui.objects.window.dock

import javafx.stage.Window as WindowFx
import sp.it.pl.main.APP
import sp.it.pl.ui.objects.window.stage.Window

class DockWindow(window: Window) {
   val window: Window = window
   var isShowing = false
}

fun WindowFx.asDockWindow(): DockWindow? = APP.windowManager.dockWindow?.takeIf { it.window.stage==this }

fun WindowFx.isDockWindow(): Boolean = asDockWindow()!=null

fun Window.asDockWindow(): DockWindow? = stage.asDockWindow()

fun Window.isDockWindow(): Boolean = stage.asDockWindow()!=null