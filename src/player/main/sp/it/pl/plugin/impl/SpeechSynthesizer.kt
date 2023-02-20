package sp.it.pl.plugin.impl

import marytts.LocalMaryInterface
import marytts.exceptions.MaryConfigurationException
import marytts.util.data.audio.AudioPlayer
import mu.KLogging
import sp.it.pl.main.AppError
import sp.it.pl.main.ifErrorNotify
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.util.async.runVT
import sp.it.util.dev.stacktraceAsString
import sp.it.util.functional.runTry
import sp.it.util.type.volatile

/** Provides speech recognition and voice control capabilities. See https://alphacephei.com/vosk/models. */
class SpeechSynthesizer: PluginBase() {

   @Suppress("SpellCheckingInspection")
   var voiceName = "cmu-slt-hsmm"
   var mary: LocalMaryInterface? by volatile(null)

   override fun start() {
      runVT {
         val jv = System.getProperty("java.version")
         System.setProperty("java.version", "1.9")
         mary = try {
            LocalMaryInterface().apply {
               voice = voiceName
            }
         } catch (e: MaryConfigurationException) {
            e.printStackTrace()
            null
         }
         System.setProperty("java.version", jv)
      }
   }

   override fun stop() {
      mary = null
   }

   fun speak(text: String) {
      runTry { mary!!.generateAudio(text) }
         .ifErrorNotify { AppError("Failed to generate audio for text.", "Exact problem:\n${it.stacktraceAsString}") }
         .ifOk {
            AudioPlayer().apply {
               setAudio(it)
               isDaemon = true
               start()
            }
         }
   }

   companion object: PluginInfo, KLogging() {
      override val name = "Speech Synthesizer"
      override val description = "Provides ability to narrate text using voice synthesizer. See https://github.com/marytts/marytts"
      override val isSupported = true
      override val isSingleton = true
      override val isEnabledByDefault = false
   }

}