package sp.it.pl.plugin.impl

import java.io.File
import sp.it.pl.main.APP
import sp.it.pl.main.configure
import sp.it.pl.main.isImage
import sp.it.pl.main.runAsAppProgram
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.pl.ui.objects.form.Validated
import sp.it.util.action.IsAction
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.min
import sp.it.util.conf.only
import sp.it.util.conf.uiOut
import sp.it.util.file.FileType.FILE
import sp.it.util.functional.Try
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.sync
import sp.it.util.reactive.zip
import sp.it.util.system.Os

class Waifu2k: PluginBase() {

   val waiffuDir by cvn<File>(null).only(FILE).def(name = "Waifu binary", info = "File 'waifu2x-converter_x64.exe'. Must be set to be able to scale images")
   val menuItemInjector = Subscribed {
      APP.contextMenus.menuItemBuilders.add<File> {
         if (value.isImage()) {
            item("Scale") {
               showUpscaleForm(it)
            }
         }
      }
   }

   override fun start() = menuItemInjector.subscribe()

   override fun stop() = menuItemInjector.unsubscribe()

   @IsAction(name = "Scale image (waiffu2k)", info = "Scales an image using waiffu2k")
   private fun showUpscaleForm() = showUpscaleForm(null)

   private fun showUpscaleForm(imageSourceFile: File?) {
      object: ConfigurableBase<Any?>(), Validated {
         val source by cvn(imageSourceFile).only(FILE).def(name = "Source", info = "Input image to scale", group = "1")
         val destination by cvn<File>(null).only(FILE).uiOut().def(name = "Destination", info = "Output image that will be created", group = "2")
         val scale by cv(2).def(name = "Scale", info = "Scaling factor. Take input image resolution into consideration and do not scale to giant resolutions.", group = "3").min(2)

         init {
            source zip scale sync { (f, sc) ->
               val isInitial = destination.value?.path?.endsWith("x(waifu2x).${f?.extension}")!=false
               if (isInitial) destination.value = f?.resolveSibling("${f.nameWithoutExtension}-scaled${sc}x(waifu2x).${f.extension}")
            }
         }

         override fun isValid() = when {
            waiffuDir.value==null -> Try.error("'waifu2x-converter_x64.exe' path is not set. See plugin settings.")
            source.value==null -> Try.error("Source image file not set")
            destination.value==null -> Try.error("Destination image file not set")
            else -> Try.ok()
         }
      }.configure("Upscale image (waifu2k)...") { c ->
         waiffuDir.value?.runAsAppProgram(
            """Upscale image "${c.source.value?.absolutePath}"""",
            "-i", c.source.value!!.absolutePath,
            "-o", c.destination.value!!.absolutePath,
            "-m", "scale",
            "--scale_ratio", c.scale.value.toString()
         )
      }
   }

   companion object: PluginInfo {
      override val name = "Waifu2k"
      override val description = "Provides action to scale image using waiffu2k AI. See https://github.com/nagadomi/waifu2x"
      override val isSupported = Os.WINDOWS.isCurrent
      override val isSingleton = false
      override val isEnabledByDefault = false
   }
}