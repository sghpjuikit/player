package sp.it.pl.plugin.wallpaper

import javafx.beans.property.Property
import javafx.beans.value.ObservableValue
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle.UNDECORATED
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.gui.objects.window.stage.setNonInteractingOnBottom
import sp.it.pl.image.ImageStandardLoader
import sp.it.pl.main.APP
import sp.it.pl.plugin.PluginBase
import sp.it.util.access.vn
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.runIO
import sp.it.util.conf.Constraint.FileActor.FILE
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.only
import sp.it.util.functional.asIf
import sp.it.util.functional.toUnit
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.on
import sp.it.util.reactive.onItemSyncWhile
import sp.it.util.reactive.sync
import sp.it.util.system.Os
import sp.it.util.ui.image.FitFrom
import sp.it.util.ui.lay
import sp.it.util.ui.min
import sp.it.util.ui.size
import sp.it.util.ui.stackPane
import sp.it.util.ui.xy
import sp.it.util.units.millis
import java.io.File

class WallpaperPlugin: PluginBase("Wallpaper", false) {

   fun <T> Property<T>.readOnly(): ObservableValue<T> = this

   private val unfocusedOwner by lazy { APP.windowManager.createStageOwner() }
   private val wallpaperImageW = vn<Image>(null)
   val wallpaperImage = wallpaperImageW.readOnly()
   val wallpaperFile by cvn<File>(null).only(FILE).def(name = "Wallpaper file") sync ::load

   private val wallpaperIsShowing = Subscribed {
      Screen.getScreens().onItemSyncWhile { screen ->
         val screenSize = screen.bounds.size
         val root = stackPane()
         val disposer = Disposer()

         Stage().run {
            initOwner(unfocusedOwner)
            initStyle(UNDECORATED)
            title = "${APP.name}-Wallpaper-$screenSize"
            scene = Scene(root)
            size = screenSize
            xy = screen.bounds.min
            setNonInteractingOnBottom()

            val anim = anim(500.millis) { opacity = it*it }.applyNow()

            root.lay += Thumbnail(screenSize).apply {
               fitFrom.value = FitFrom.OUTSIDE
               wallpaperImageW sync ::loadImage on disposer
               image sync { if (it==null) anim.playCloseDo(::close) else anim.playDoOpen(::show) } on disposer
               disposer += { loadImage(null) }
            }.pane

            disposer += {
               close()
               scene?.root?.asIf<Pane>()?.children?.clear()
               scene = null
            }

            Subscription { disposer() }
         }
      }
   }

   private fun load(f: File?) = runIO { ImageStandardLoader(f) }.ui { wallpaperImageW.value = it }.toUnit()

   override fun isSupported() = Os.WINDOWS.isCurrent

   override fun onStart() = wallpaperIsShowing.subscribe()

   override fun onStop() {
      wallpaperIsShowing.unsubscribe()
      wallpaperImageW.value = null
   }

}