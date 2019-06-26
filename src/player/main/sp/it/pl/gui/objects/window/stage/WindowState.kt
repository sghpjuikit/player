package sp.it.pl.gui.objects.window.stage

import sp.it.pl.main.APP

internal class WindowState(window: Window) {
   val x = window.X.value!!
   val y = window.Y.value!!
   val w = window.W.value!!
   val h = window.H.value!!
   val main = window.isMain.value!!
   val resizable = window.resizable.value!!
   val minimized = window.s.iconifiedProperty().value!!
   val fullscreen = window.fullscreen.value!!
   val onTop = window.alwaysOnTop.value!!
   val maximized = window.maximized.value!!
   val layout = window.layout!!

   fun toWindow(): Window = APP.windowManager.create(main).also {
      it.X.value = x
      it.Y.value = y
      it.W.value = w
      it.H.value = h
      it.s.isIconified = minimized
      it.MaxProp.value = maximized
      it.FullProp.value = fullscreen
      it.resizable.value = resizable
      it.isAlwaysOnTop = onTop
      it.initLayout(layout)
   }

}