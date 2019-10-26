package sp.it.pl.plugin.waifu2k

import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.configure
import sp.it.pl.main.onErrorNotify
import sp.it.pl.plugin.PluginBase
import sp.it.util.action.Action
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.Constraint.FileActor.FILE
import sp.it.util.conf.IsConfig
import sp.it.util.conf.cvn
import sp.it.util.conf.only
import sp.it.util.dev.stacktraceAsString
import sp.it.util.reactive.attach
import sp.it.util.system.Os
import sp.it.util.system.runAsProgram
import java.io.File

class Waifu2kPlugin: PluginBase("Waifu2k", false) {

   private val action by lazy {
      Action(
         "Upscale image (waifu2k)...",
         { openUpscaleImage() },
         "Upscale specified image with a neural network algorithm",
         configurableGroupPrefix,
         "",
         true,
         false
      )
   }

   override fun isSupported() = Os.WINDOWS.isCurrent

   override fun onStart() = APP.configuration.collect(action)

   override fun onStop() = APP.configuration.drop(action)

   private fun openUpscaleImage() {
      object: ConfigurableBase<Any>() {
         @IsConfig(name = "Waifu binary", group = "1") val waiffuDir by cvn<File>(null).only(FILE)
         @IsConfig(name = "Source", group = "2") val source by cvn<File>(null).only(FILE)
         @IsConfig(name = "Destination", group = "3") val destination by cvn<File>(null).only(FILE)
      }.apply {
         source attach { destination.value = it!!.resolveSibling("${it.nameWithoutExtension}-scaled2x(waifu2x).${it.extension}") }
      }.configure("Upscale image (waifu2k)...") { c ->
         c.waiffuDir.value!!.runAsProgram(
            "-i \"" + c.source.value!!.absolutePath + "\"",
            "-o \"" + c.destination.value!!.absolutePath + "\"",
            "-m scale",
            "-s 2.0"
         ).onErrorNotify {
            AppError("Failed to upscale image ${c.source}", "Reason: ${it.stacktraceAsString}")
         }
      }
   }

}