package sp.it.pl.ui.objects.window.stage

import sp.it.pl.ui.objects.window.stage.WindowBase.Maximized
import sp.it.pl.ui.objects.window.stage.WindowBase.Maximized.NONE
import sp.it.pl.layout.RootContainerDb
import sp.it.pl.main.APP

class WindowDb(
   val x: Double = 0.0,
   val y: Double = 0.0,
   val w: Double = 400.0,
   val h: Double = 400.0,
   val main: Boolean = false,
   val resizable: Boolean = true,
   val headerVisible: Boolean = true,
   val minimized: Boolean = false,
   val fullscreen: Boolean = false,
   val onTop: Boolean = false,
   val maximized: Maximized = NONE,
   val layout: RootContainerDb? = null
) {
   fun toDomain(): Window = APP.windowManager.create(main).also {
      it.X.value = x
      it.Y.value = y
      it.W.value = w
      it.H.value = h
      it.s.isIconified = minimized
      it.MaxProp.value = maximized
      it.FullProp.value = fullscreen
      it.resizable.value = resizable
      it.isHeaderVisible.value = headerVisible
      it.isAlwaysOnTop = onTop
      it.initLayout(layout?.toDomain())
   }

   companion object {
      operator fun invoke(w: Window) = WindowDb(
         w.X.value,
         w.Y.value,
         w.W.value,
         w.H.value,
         w.isMain.value,
         w.resizable.value,
         w.isHeaderVisible.value,
         w.s.isIconified,
         w.fullscreen.value,
         w.alwaysOnTop.value,
         w.maximized.value,
         w.layout?.toDb()
      )
   }
}