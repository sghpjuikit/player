package sp.it.pl.plugin.waifu2k

import sp.it.pl.main.APP
import sp.it.pl.main.configure
import sp.it.pl.plugin.PluginBase
import sp.it.util.action.Action
import sp.it.util.async.runNew
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.Constraint.FileActor.FILE
import sp.it.util.conf.IsConfig
import sp.it.util.conf.cvn
import sp.it.util.conf.only
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
         configurableGroup,
         "",
         true,
         false
      )
   }

   override fun isSupported() = Os.WINDOWS.isCurrent

   override fun onStart() = APP.configuration.collect(action)

   override fun onStop() = APP.configuration.drop(action)

   private fun openUpscaleImage() {
      object: ConfigurableBase<Boolean>() {
         @IsConfig(name = "Waifu binary", group = "1") val waiffuDir by cvn<File>(null).only(FILE)
         @IsConfig(name = "Source", group = "2") val source by cvn<File>(null).only(FILE)
         @IsConfig(name = "Destination", group = "3") val destination by cvn<File>(null).only(FILE)
      }.apply {
         source attach { destination.value = it!!.resolveSibling("${it.nameWithoutExtension}-scaled2x(waifu2x).${it.extension}") }
      }.configure("Upscale image (waifu2k)...") {
         runNew {
            val program = it.waiffuDir.value!!
            program.runAsProgram(
               "-i \"" + it.source.value!!.absolutePath + "\"",
               "-o \"" + it.destination.value!!.absolutePath + "\"",
               "-m scale",
               "-s 2.0"
            ).onError {
               it.printStackTrace()
            }
         }
      }
   }

}