package sp.it.pl.plugin.impl

import java.io.File
import java.net.URI
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import mu.KLogging
import org.vosk.Model
import org.vosk.Recognizer
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.AppProgress
import sp.it.pl.main.downloadFile
import sp.it.pl.main.ifErrorNotify
import sp.it.pl.main.reportFor
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.util.access.vn
import sp.it.util.async.coroutine.pairs
import sp.it.util.async.coroutine.runSuspendingFx
import sp.it.util.conf.butElement
import sp.it.util.conf.cList
import sp.it.util.conf.def
import sp.it.util.conf.noPersist
import sp.it.util.conf.readOnly
import sp.it.util.conf.uiConverter
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.deleteOrThrow
import sp.it.util.file.div
import sp.it.util.file.unzip
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.text.words

/** Provides speech recognition and voice control capabilities. See https://alphacephei.com/vosk/models. */
class SpeechRecognition: PluginBase() {

   private val modelDir = APP.location / "speech-recognition-ai-model" / "vosk-model-small-en-us-0.15"
   private val modelPath = modelDir.parentFile!!.name + File.separator + modelDir.name
   private val setup by lazy {
      runSuspendingFx {
         AppProgress.start("Obtaining Speech Recognition AI model").reportFor { task ->
            withContext(Dispatchers.IO) {
               val modelZip = modelDir/"vlc.zip"
               val modelLink = URI("https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip")

               if (!modelDir.exists()) {
                  downloadFile(modelLink, modelZip, task)
                  modelZip.unzip(modelDir)
                  modelZip.deleteOrThrow()
               }
            }
         }
      }.onDone {
         it.toTry().ifErrorNotify {
            AppError("Failed to obtain Speech Recognition AI model", "Reason: ${it.stacktraceAsString}")
         }
      }
   }

   private val onClose = Disposer()
   private val scope = MainScope()

   val events = Channel<String>(10)
   val handlers by cList(
         SpeakHandler("Resume playback", "play music") { APP.audio.resume() },
         SpeakHandler("Resume playback", "start music") { APP.audio.resume() },
         SpeakHandler("Pause playback", "stop music") { APP.audio.pause() },
         SpeakHandler("Pause playback", "end music") { APP.audio.pause() },
      )
      .noPersist().readOnly().butElement { uiConverter { "${it.name} -> ${it.command}" } }
      .def(name = "Commands", info = "Shows active voice speech recognition commands.")

   /** Currently spoken text */
   val speakingText = vn<String>(null)
   val speaking = speakingText.map { it!=null }

   /** Words or phrases that will be removed from text representing the detected speech. Makes command matching more powerful. Case-insensitive. */
   val blacklistWords by cList("a", "the", "please")
      .def(name = "Blacklisted words", info = "Words or phrases that will be removed from text representing the detected speech. Makes command matching more powerful. Case-insensitive.")

   /** [blacklistWords] in performance-optimal format */
   private val blacklistWordsSet = mutableSetOf<String>().apply {
      blacklistWords.onChangeAndNow {
         clear()
         this += blacklistWords.map { it.lowercase() }
      }
   }

   override fun start() {
      startSpeechRecognition()
      APP.sysEvents.subscribe { startSpeechRecognition() } on onClose // restart on audio device change
   }

   override fun stop() {
      stopSpeechRecognition()
   }

   @Suppress("OPT_IN_USAGE")
   private fun startSpeechRecognition() {
      setup.then {
         scope.runSuspendingFx {
            // microphone & recognition
            launch(newSingleThreadContext("voice recognition")) {
               val format = AudioFormat(PCM_SIGNED, 60000f, 16, 2, 4, 44100f, false)
               val info = DataLine.Info(TargetDataLine::class.java, format)
               Model(modelPath).use { model ->
                  Recognizer(model, 120000f).use { recognizer ->
                     (AudioSystem.getLine(info) as TargetDataLine).use { microphone ->
                        microphone.open(format)
                        microphone.start()
                        val chunkSize = 1024
                        var bytesRead = 1
                        val b = ByteArray(4096)
                        while (bytesRead>0) {
                           ensureActive()
                           bytesRead = microphone.read(b, 0, chunkSize)
                           if (recognizer.acceptWaveForm(b, bytesRead)) events.send(recognizer.finalResult)
                           else events.send(recognizer.partialResult)
                        }
                     }
                  }
               }
            }
            // event processing
            launch {
               events.receiveAsFlow()
                  .map { it.substringBeforeLast("\"").substringAfterLast("\"") }
                  .pairs()
                  .mapNotNull {
                     if (it.first.isEmpty() && it.second.isNotEmpty()) Speak.Start
                     else if (it.first.length > it.second.length) Speak.End(it.first)
                     else if (it.first.isNotEmpty()) Speak.Active(it.first)
                     else null
                  }
                  .onEach {
                     when (it) {
                        is Speak.Start ->
                           {}
                        is Speak.Active ->
                           speakingText.value = it.text
                        is Speak.End -> {
                           it.text.sanitize().let { handlers.find { h -> h.command in it }?.action?.invoke() }
                           speakingText.value = it.text
                           speakingText.value = null
                        }
                     }
                  }
                  .collect()
            }
         }
      }
   }

   private fun stopSpeechRecognition() {
      scope.coroutineContext.cancel()
   }

   /** Adjust speech text to make it more versatile and more likely to match command */
   private fun String.sanitize(): String = words().filterNot(blacklistWordsSet::contains).joinToString(" ")

   companion object: PluginInfo, KLogging() {
      override val name = "Speech Recognition"
      override val description = "Provides speech recognition and voice control capabilities. See https://alphacephei.com/vosk/models"
      override val isSupported = true
      override val isSingleton = true
      override val isEnabledByDefault = false
   }

   /** Speech event */
   sealed interface Speak {
      /** Speech started after silence */
      object Start: Speak
      /** Speech progressed and currently has [text] value */
      data class Active(val text: String): Speak
      /** Speech ended with silence break and with the [text] as the final value */
      data class End(val text: String): Speak
   }

   /** Speech event handler */
   data class SpeakHandler(val name: String, val command: String, val action: () -> Unit)

}