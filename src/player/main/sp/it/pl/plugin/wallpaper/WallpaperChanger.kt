package sp.it.pl.plugin.wallpaper

import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle.UNDECORATED
import javafx.stage.WindowEvent.WINDOW_HIDDEN
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.gui.objects.window.ShowArea
import sp.it.pl.gui.objects.window.stage.setNonInteractingProgmanOnBottom
import sp.it.pl.image.ImageStandardLoader
import sp.it.pl.main.APP
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.util.access.readOnly
import sp.it.util.access.vn
import sp.it.util.async.runIO
import sp.it.util.conf.Constraint.FileActor.FILE
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.only
import sp.it.util.functional.asIf
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync1IfNonNull
import sp.it.util.reactive.syncWhile
import sp.it.util.system.Os
import sp.it.util.ui.anchorPane
import sp.it.util.ui.image.FitFrom
import sp.it.util.ui.image.ImageSize
import sp.it.util.ui.lay
import sp.it.util.ui.size
import sp.it.util.ui.x
import sp.it.util.ui.xy
import java.io.File

class WallpaperChanger: PluginBase() {

   private val wallpaperImageW = vn<Image>(null)
   val wallpaperImage = wallpaperImageW.readOnly()
   val wallpaperFile by cvn<File>(null).only(FILE).def(name = "Wallpaper file") sync ::load

   private val wallpaperIsShowing = Subscribed {
      Screen.getScreens().syncWhile {
         val disposer = Disposer()

         // An all-screen window displaying the wallpaper image in a positioned thumbnail per each screen
         Stage().run {
            val root = anchorPane()
            val b = ShowArea.ALL_SCREENS.bounds().second

            initStyle(UNDECORATED)
            title = "${APP.name}-Wallpaper"
            scene = Scene(root)
            xy = 0 x 0
            size = b.size
            setNonInteractingProgmanOnBottom()

            Screen.getScreens().forEach { screen ->
               root.lay(screen.bounds.minY - b.minY, null, null, screen.bounds.minX - b.minX) += Thumbnail(screen.bounds.size).run {
                  fitFrom.value = FitFrom.OUTSIDE
                  wallpaperImageW.sync1IfNonNull(::loadImage) on disposer
                  onEventDown(WINDOW_HIDDEN) { loadImage(null) }

                  pane
               }
            }

            disposer += {
               close()
               scene.root.asIf<Pane>()?.children?.clear()
               scene = null
            }

            wallpaperImageW.sync1IfNonNull { show() } on disposer
         }
      }
   }

   private fun load(f: File?) {
      runIO {
         ImageStandardLoader(f, largestScreenSize())
      } ui { image ->
         wallpaperImageW.value = image
      }
   }

   override fun start() = wallpaperIsShowing.subscribe()

   override fun stop() {
      wallpaperIsShowing.unsubscribe()
      wallpaperImageW.value = null
   }

   companion object: PluginInfo {
      override val name = "Wallpaper"
      override val description = "Provides the ability to change wallpaper until OS shutdown. Also improves screen overlay effect performance."
      override val isSupported = Os.WINDOWS.isCurrent
      override val isEnabledByDefault = false

      fun largestScreenSize() = Screen.getScreens().map { ImageSize(it.bounds.width, it.bounds.height) }.maxBy { it.width*it.height }
         ?: ImageSize(0.0, 0.0)
   }
}