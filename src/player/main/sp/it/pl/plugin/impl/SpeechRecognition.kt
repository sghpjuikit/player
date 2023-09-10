package sp.it.pl.plugin.impl

import java.io.InputStream
import java.lang.ProcessBuilder.Redirect.PIPE
import mu.KLogging
import sp.it.pl.layout.ComponentLoaderStrategy
import sp.it.pl.main.APP
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.util.access.readOnly
import sp.it.util.access.vn
import sp.it.util.async.future.Fut
import sp.it.util.async.runFX
import sp.it.util.async.runNew
import sp.it.util.async.runVT
import sp.it.util.conf.EditMode
import sp.it.util.conf.butElement
import sp.it.util.conf.cList
import sp.it.util.conf.cv
import sp.it.util.conf.cvnro
import sp.it.util.conf.def
import sp.it.util.conf.multilineToBottom
import sp.it.util.conf.noPersist
import sp.it.util.conf.readOnly
import sp.it.util.conf.uiConverter
import sp.it.util.dev.fail
import sp.it.util.file.div
import sp.it.util.file.parentDirOrRoot
import sp.it.util.functional.toUnit
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.reactive.throttleToLast
import sp.it.util.system.EnvironmentContext
import sp.it.util.text.equalsNc
import sp.it.util.text.words
import sp.it.util.units.seconds

/** Provides speech recognition and voice control capabilities. Uses whisper AI launched as python program. */
class SpeechRecognition: PluginBase() {

   private val onClose = Disposer()
   private var setup: Fut<Process>? = null
   private fun setup(): Fut<Process> {

      class StreamGobbler(private val inputStream: InputStream, private val consumeInputLine: (Sequence<String>) -> Unit): Runnable {
         override fun run() {
            inputStream.bufferedReader().useLines(consumeInputLine)
         }
      }

      fun String?.wrap() = if (isNullOrBlank()) "" else "\n$this"
      fun doOnError(e: Throwable?, text: String?) = logger.error(e) { "Starting whisper failed.\n${text.wrap()}" }.toUnit()

      return runVT {
         val whisper = APP.location / "speech-recognition-whisper" / "main.py"
         val commandRaw = listOf("python", whisper.absolutePath, "wake-word=${wakeUpWord.value}", "parent-process=${ProcessHandle.current().pid()}")
         val command = EnvironmentContext.runAsProgramArgsTransformer(commandRaw)
         val process = ProcessBuilder(command)
            .directory(whisper.parentDirOrRoot)
            .redirectOutput(PIPE).redirectError(PIPE)
            .start()

         var stdout = ""
         var stderr = ""
         val stdoutListener = runNew(StreamGobbler(process.inputStream) {
            stdout = it
               .onEach { runFX { speakingStdout.value = (speakingStdout.value ?: "") + "\n" + it; println(it) } }
               .onEach { if (it.startsWith("USER:")) runFX { handleSpeech(it.substring(5)) } }
               .joinToString("")
         })
         val stderrListener = runNew(StreamGobbler(process.errorStream) {
            stderr = it
               .onEach { runFX { speakingStdout.value = (speakingStdout.value ?: "") + "\n" + it } }
               .joinToString("")
         })
         runNew {
            val success = process.waitFor()
            stdoutListener.block()
            stderrListener.block()
            if (success!=0) doOnError(null, stdout + stderr)
            if (success!=0) fail { "Whisper process failed and returned $success" }
            process
         }.onError {
            doOnError(it, "")
         }

         process
      }.onError {
         doOnError(it, "")
      }
   }

   /** Speech handlers called when user has spoken. */
   val handlers by cList(
         SpeakHandler("Resume playback", "play music") { text, command -> if (command in text) APP.audio.resume() },
         SpeakHandler("Resume playback", "start music") { text, command -> if (command in text) APP.audio.resume() },
         SpeakHandler("Pause playback", "stop music") { text, command -> if (command in text) APP.audio.pause() },
         SpeakHandler("Pause playback", "end music") { text, command -> if (command in text) APP.audio.pause() },
         SpeakHandler("Open widget by name", "open \$widget-name") { text, _ ->
            if (text.startsWith("open ")) {
               val fName = text.substring(5).removeSuffix(" widget")
               val f = APP.widgetManager.factories.getComponentFactories().find { it.name equalsNc fName }
               if (f!=null) ComponentLoaderStrategy.DOCK.loader(f)
            }
         },
      )
      .noPersist().readOnly().butElement { uiConverter { "${it.name} -> ${it.commandUi}" } }
      .def(name = "Commands", info = "Shows active voice speech recognition commands.")

   /** Last spoken text - writable */
   private val speakingTextW = vn<String>(null)

   /** Last spoken text */
   val speakingText = speakingTextW.readOnly()

   /** Console output - writable */
   private val speakingStdoutW = vn<String>(null)

   /** Console output */
   val speakingStdout by cvnro(speakingStdoutW)
      .multilineToBottom()
      .noPersist()
      .def(name = "Speech recognition output", info = "Shows console output of the speech recognition Whisper AI process", editable = EditMode.APP)

   /** Words or phrases that will be removed from text representing the detected speech. Makes command matching more powerful. Case-insensitive. */
   val wakeUpWord by cv("spit")
      .def(name = "Wake up word", info = "Words or phrase that activates voice recognition. Case-insensitive.")

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
      wakeUpWord.throttleToLast(2.seconds).attach { startSpeechRecognition() } on onClose
   }

   override fun stop() {
      stopSpeechRecognition()
      onClose()
   }

   private fun startSpeechRecognition() {
      stopSpeechRecognition()
      setup = setup()
   }

   private fun stopSpeechRecognition() {
      setup?.then { it.destroy() }
      setup = null
   }

   private fun handleSpeech(text: String?) {
      speakingTextW.value = text
      println(text.orEmpty().sanitize())
      text.orEmpty().sanitize().let { handlers.forEach { h -> h.action(it, h.commandUi) } }
   }

   /** Adjust speech text to make it more versatile and more likely to match command */
   private fun String.sanitize(): String = trim().removeSuffix(".").lowercase().words().filterNot(blacklistWordsSet::contains).joinToString(" ")

   companion object: PluginInfo, KLogging() {
      override val name = "Speech Recognition"
      override val description = "Provides speech recognition and voice control capabilities. See https://github.com/openai/whisper"
      override val isSupported = true
      override val isSingleton = true
      override val isEnabledByDefault = false
   }

   /** Speech event handler */
   data class SpeakHandler(val name: String, val commandUi: String, val action: (String, String) -> Unit)

}