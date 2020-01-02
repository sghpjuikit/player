package sp.it.pl.plugin.impl

import sp.it.pl.main.AppError
import sp.it.pl.main.configure
import sp.it.pl.main.onErrorNotify
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.util.action.IsAction
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.Constraint.FileActor.FILE
import sp.it.util.conf.c
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.min
import sp.it.util.conf.only
import sp.it.util.conf.uiOut
import sp.it.util.dev.stacktraceAsString
import sp.it.util.reactive.attach
import sp.it.util.system.Os
import sp.it.util.system.runAsProgram
import java.io.File

class Waifu2k: PluginBase() {

   val waiffuDir by cvn<File>(null).only(FILE).def(name = "Waifu binary", info = "File 'waifu2x-converter_x64.exe'. Must be set to be able to scale images")

   @IsAction(name = "Scale image (waiffu2k)", info = "Scales an image using waiffu2k")
   private fun showUpscaleForm() {
      object: ConfigurableBase<Any?>() {
         val source by cvn<File>(null).only(FILE).def(name = "Source", info = "Input image to scale", group = "1")
         val destination by cvn<File>(null).only(FILE).uiOut().def(name = "Destination", info = "Output image that will be created", group = "2")
         var scale by c(2).def(name = "Scale", info = "Scaling factor. Take input image resolution into consideration and do not scale to giant resolutions.", group = "3").min(2)
      }.apply {
         source attach {
            if (destination.value==null)
               destination.value = it!!.resolveSibling("${it.nameWithoutExtension}-scaled2x(waifu2x).${it.extension}")
         }
      }.configure("Upscale image (waifu2k)...") { c ->
         waiffuDir.value?.runAsProgram(
            "-i \"" + c.source.value!!.absolutePath + "\"",
            "-o \"" + c.destination.value!!.absolutePath + "\"",
            "-m scale",
            "--scale_ratio ${c.scale}"
         )?.onErrorNotify {
            AppError("Failed to upscale image ${c.source}", "Reason: ${it.stacktraceAsString}")
         }
      }
   }

   companion object: PluginInfo {
      override val name = "Waifu2k"
      override val description = "Provides action to scale image using waiffu2k AI. See https://github.com/nagadomi/waifu2x"
      override val isSupported = Os.WINDOWS.isCurrent
      override val isEnabledByDefault = false
   }
}