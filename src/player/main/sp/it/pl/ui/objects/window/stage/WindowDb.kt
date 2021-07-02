package sp.it.pl.ui.objects.window.stage

import javafx.stage.StageStyle
import javafx.stage.StageStyle.TRANSPARENT
import sp.it.pl.ui.objects.window.stage.WindowBase.Maximized
import sp.it.pl.ui.objects.window.stage.WindowBase.Maximized.NONE
import sp.it.pl.layout.RootContainerDb
import sp.it.pl.main.APP
import sp.it.util.functional.net

data class WindowDb(
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
   val transparent: Boolean? = null,
   val transparentContent: Boolean? = null,
   val isTaskbarVisible: Boolean = true,
   val opacity: Double? = null,
   val layout: RootContainerDb? = null
) {
   fun toDomain(): Window = APP.windowManager.create(state = this).also {
      it.X.value = x
      it.Y.value = y
      it.W.value = w
      it.H.value = h
      it.s.isIconified = minimized
      it.MaxProp.value = maximized
      it.FullProp.value = fullscreen
      it.resizable.value = resizable
      it.isHeaderVisible.value = headerVisible
      it.alwaysOnTop.value = onTop
      it.opacity.value = opacity ?: it.opacity.value
      it.opacityOverride = opacity!=null
      it.stageStyleOverride = transparent!=null
      it.transparentContent.value = transparentContent
      it.isTaskbarVisible.value = isTaskbarVisible
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
         w.s.style.net { it==TRANSPARENT }.takeIf { w.stageStyleOverride },
         w.transparentContent.value,
         w.isTaskbarVisible.value,
         w.opacity.value.takeIf { w.opacityOverride },
         w.layout?.toDb()
      )
   }
}

fun Window.toDb() = WindowDb(this)

fun Window.recreateWith(stageStyle: StageStyle, isTaskbarVisible: Boolean, onBottom: Boolean) = net { ow ->
   APP.windowManager.create(if (!isTaskbarVisible) APP.windowManager.createStageOwner() else null, stageStyle, ow.isMain.value).also { nw ->
      nw.X.value = ow.X.value
      nw.Y.value = ow.Y.value
      nw.W.value = ow.W.value
      nw.H.value = ow.H.value
      nw.s.isIconified = ow.s.isIconified
      nw.MaxProp.value = ow.MaxProp.value
      nw.FullProp.value = ow.FullProp.value
      nw.resizable.value = ow.resizable.value
      nw.isHeaderVisible.value = ow.isHeaderVisible.value
      nw.alwaysOnTop.value = ow.alwaysOnTop.value
      nw.opacity.value = ow.opacity.value
      nw.opacityOverride = ow.opacityOverride
      nw.transparentContent.value = ow.transparentContent.value
      nw.stageStyleOverride = ow.stageStyleOverride
      nw.isTaskbarVisible.value = isTaskbarVisible
      if (onBottom) nw.stage.setNonInteractingOnBottom()
   }
}