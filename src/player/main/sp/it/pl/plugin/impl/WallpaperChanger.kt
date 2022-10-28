package sp.it.pl.plugin.impl

import java.io.File
import javafx.scene.image.Image
import javafx.stage.Screen
import sp.it.pl.image.ImageStandardLoader
import sp.it.pl.main.APP
import sp.it.pl.main.isImage
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.util.access.readOnly
import sp.it.util.access.vn
import sp.it.util.async.runVT
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.only
import sp.it.util.file.FileType.FILE
import sp.it.util.reactive.Subscribed
import sp.it.util.system.Os
import sp.it.util.system.Windows
import sp.it.util.ui.image.FitFrom.OUTSIDE
import sp.it.util.ui.image.ImageSize
import sp.it.util.units.uuid

class WallpaperChanger: PluginBase() {

   private val wallpaperImageW = vn<Image>(null)
   val wallpaperImage = wallpaperImageW.readOnly()
   val wallpaperFile by cvn<File>(null).only(FILE).def(name = "Wallpaper file") sync ::load
   val cacheId = uuid("e37ac005-4be3-4241-b81d-ba19dc376857")
   val menuItemInjector = Subscribed {
      APP.contextMenus.menuItemBuilders.add<File> {
         if (value.isImage()) {
            item("Use as wallpaper") {
               wallpaperFile.value = it
            }
         }
      }
   }

   private fun load(f: File?) {
      val size = largestScreenSize()
      runVT {
         if (f!=null) Windows.changeWallpaper(f)
         ImageStandardLoader.memoized(cacheId)(f, size, OUTSIDE, true)
      } ui {
         wallpaperImageW.value = it
      }
   }

   override fun start() {
      menuItemInjector.subscribe()
   }

   override fun stop() {
      menuItemInjector.unsubscribe()
      wallpaperImageW.value = null
   }

   companion object: PluginInfo {
      override val name = "Wallpaper"
      override val description = "Provides the ability to change wallpaper. Also improves screen overlay loading speed."
      override val isSupported = Os.WINDOWS.isCurrent
      override val isSingleton = true
      override val isEnabledByDefault = false

      fun largestScreenSize() = Screen.getScreens().map { ImageSize(it.bounds.width, it.bounds.height) }.maxByOrNull { it.width*it.height }?.div(2.0)
         ?: ImageSize(0.0, 0.0)
   }
}