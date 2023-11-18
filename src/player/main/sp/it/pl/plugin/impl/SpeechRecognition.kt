package sp.it.pl.plugin.impl

import sp.it.util.async.coroutine.VT as VTc
import io.ktor.client.request.put
import java.io.InputStream
import java.lang.ProcessBuilder.Redirect.PIPE
import java.util.regex.Pattern
import mu.KLogging
import sp.it.pl.core.InfoUi
import sp.it.pl.core.NameUi
import sp.it.pl.core.bodyJs
import sp.it.pl.core.requestBodyAsJs
import sp.it.pl.layout.ComponentLoaderStrategy
import sp.it.pl.main.APP
import sp.it.pl.main.AppHttp
import sp.it.pl.main.IconMA
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.pl.ui.pane.ActionData.Threading.BLOCK
import sp.it.pl.ui.pane.action
import sp.it.util.access.readOnly
import sp.it.util.access.vn
import sp.it.util.action.IsAction
import sp.it.util.async.VT
import sp.it.util.async.coroutine.runSuspending
import sp.it.util.async.future.Fut
import sp.it.util.async.runFX
import sp.it.util.async.runNew
import sp.it.util.async.runOn
import sp.it.util.conf.EditMode
import sp.it.util.conf.butElement
import sp.it.util.conf.cList
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.cvnro
import sp.it.util.conf.def
import sp.it.util.conf.multilineToBottom
import sp.it.util.conf.noPersist
import sp.it.util.conf.password
import sp.it.util.conf.readOnly
import sp.it.util.conf.readOnlyUnless
import sp.it.util.conf.uiConverter
import sp.it.util.conf.uiNoOrder
import sp.it.util.conf.valuesUnsealed
import sp.it.util.dev.fail
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.toUnit
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.chan
import sp.it.util.reactive.on
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.reactive.plus
import sp.it.util.reactive.sync
import sp.it.util.reactive.throttleToLast
import sp.it.util.system.EnvironmentContext
import sp.it.util.text.camelToSpaceCase
import sp.it.util.text.encodeBase64
import sp.it.util.text.equalsNc
import sp.it.util.text.words
import sp.it.util.units.seconds

/** Provides speech recognition and voice control capabilities. Uses whisper AI launched as python program. */
class SpeechRecognition: PluginBase() {
   private val onClose = Disposer()
   private val dir = APP.location / "speech-recognition-whisper"
   private var setup: Fut<Process>? = null
   private fun setup(): Fut<Process> {

      val ansi = Pattern.compile("\\x1B\\[[0-?]*[ -/]*[@-~]")
      fun String.ansi() = ansi.matcher(this).replaceAll("")
      fun String?.wrap() = if (isNullOrBlank()) "" else "\n$this"
      fun InputStream.consume(name: String, consumeInputLine: (Sequence<String>) -> Unit) = runOn(VT("SpeechRecognition-$name")) { bufferedReader().useLines(consumeInputLine) }
      fun doOnError(e: Throwable?, text: String?) = logger.error(e) { "Starting whisper failed.\n${text.wrap()}" }.toUnit()
      return runOn(VT("SpeechRecognition")) {
         val whisper = dir / "main.py"
         val commandRaw = listOf(
            "python", whisper.absolutePath,
            "wake-word=${wakeUpWord.value}",
            "parent-process=${ProcessHandle.current().pid()}",
            "speaking-engine=${speechEngine.value.code}",
            "character-ai-token=${speechEngineCharAiToken.value}",
            "character-ai-voice=22",
            "chat-model=${chatModel.value}",
            "speech-recognition-model=${whisperModel.value}"
         )
         val command = EnvironmentContext.runAsProgramArgsTransformer(commandRaw)
         val process = ProcessBuilder(command)
            .directory(dir)
            .redirectOutput(PIPE).redirectError(PIPE)
            .start()

         var stdout = ""
         var stderr = ""
         val stdoutListener = process.inputStream.consume("stdout") {
            stdout = it
               .map { it.ansi() }
               .filter { it.isNotBlank() }
               .map { it.replace("\u2028", "\n") }
               .onEach { runFX { speakingStdout.value = (speakingStdout.value ?: "") + "\n" + it } }
               .onEach { handleInputLocal(it) }
               .joinToString("")
         }
         val stderrListener = process.errorStream.consume("stderr") {
            stderr = it
               .filter { it.isNotBlank() }
               .map { it.ansi() }
               .onEach { runFX { speakingStdout.value = (speakingStdout.value ?: "") + "\n" + it } }
               .joinToString("")
         }
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
         SpeakHandler("Open widget by name", "[open|show] (widget)? \$widget-name (widget)?") { text, _ ->
            if (text.startsWith("command open")) {
               val fName = text.removePrefix("command open").trimStart().removePrefix("widget").removeSuffix("widget").trim().camelToSpaceCase()
               val f = APP.widgetManager.factories.getComponentFactories().find { it.name.camelToSpaceCase() equalsNc fName }
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

   /** Console output */
   val speakingStdout by cvnro(vn<String>(null)).multilineToBottom(20).noPersist()
      .def(name = "Speech recognition output", info = "Shows console output of the speech recognition Whisper AI process", editable = EditMode.APP)

   /** Words or phrases that will be removed from text representing the detected speech. Makes command matching more powerful. Case-insensitive. */
   val wakeUpWord by cv("spit")
      .def(name = "Wake up word", info = "Words or phrase that activates voice recognition. Case-insensitive.")

   /** Words or phrases that will be removed from text representing the detected speech. Makes command matching more powerful. Case-insensitive. */
   val blacklistWords by cList("a", "the", "please")
      .def(
         name = "Blacklisted words",
         info = "Words or phrases that will be removed from text representing the detected speech. Makes command matching more powerful. Case-insensitive."
      )

   /** Engine used to generate voice. May require additional configuration */
   val whisperModel by cv("base.en.pt")
      .valuesUnsealed { dir.div("models-whisper").children().map { it.name }.filter { it.endsWith("pt") }.toList() }
      .def(name = "Speech recognition model", info = "Whisper model for speech recognition.")

   /** Engine used to generate voice. May require additional configuration */
   val speechEngine by cv(SpeechEngine.SYSTEM).uiNoOrder()
      .def(name = "Speech engine", info = "Engine used to generate voice. May require additional configuration.")

   /** Access token for character.ai account used when speech engine is Character.ai */
   val speechEngineCharAiToken by cvn<String>(null).password()
      .def(name = "Speech engine > character.ai > token", info = "Access token for character.ai account used when speech engine is Character.ai.")

   /** Engine used to generate voice. May require additional configuration */
   val chatModel by cv("none")
      .valuesUnsealed { dir.div("models-llm").children().map { it.name }.filter { it.endsWith("gguf") }.toList() + "none" }
      .def(name = "Chat model", info = "LLM model for chat.")

   /** [blacklistWords] in performance-optimal format */
   private val blacklistWordsSet = mutableSetOf<String>().apply {
      blacklistWords.onChangeAndNow {
         clear()
         this += blacklistWords.map { it.lowercase() }
      }
   }

   val handleBy by cvn<String>(null).uiConverter { if (it==null) "This application" else it }
      .def(
         name = "Handle speech events by",
         info = "Optional IP address of another system where this another instance of this application is running and which will handle the speech detected by this instance"
      )

   /** Enable /speech in [AppHttp]. This API exposes speech & voice assistent functionality. Default false. */
   val httpEnabled by cv(false)
      .def(name = "Http API", info = "This API exposes speech & voice assistent functionality")

   /** Http input */
   private val httpInput by cvnro(vn<String>(null)).multilineToBottom(20).noPersist()
      .def(name = "Http input", info = "Shows input received over http.", editable = EditMode.APP)

   private val httpApi = Subscribed {
      APP.http.serverRoutes route AppHttp.Handler("/speech") {
         it.requestBodyAsJs().asJsStringValue().ifNotNull { text ->
            runFX { httpInput.value = (httpInput.value ?: "") + "\n" + text }
            handleInputHttp(text)
         }
      }
   }

   private var isRunning = false

   override fun start() {
      /** Triggers event when process arg changes */
      val processChange = wakeUpWord.chan() + whisperModel.chan() + chatModel.chan() + speechEngine.chan() + speechEngineCharAiToken.chan()

      startSpeechRecognition()
      APP.sysEvents.subscribe { startSpeechRecognition() } on onClose // restart on audio device change
      processChange.throttleToLast(2.seconds).subscribe { startSpeechRecognition() } on onClose
      isRunning = true
      httpEnabled.sync(httpApi::subscribe)
   }

   override fun stop() {
      isRunning = false
      httpApi.unsubscribe()
      stopSpeechRecognition()
      onClose()
   }

   private fun startSpeechRecognition() {
      stopSpeechRecognition()
      setup = setup()
   }

   private fun stopSpeechRecognition() {
      invoke("EXIT")
      setup = null
   }

   private fun invoke(text: String) {
      setup?.then { it.outputStream.apply { write("$text\n".toByteArray()); flush() } }
   }

   private fun handleInputLocal(text: String) {
      if (text.startsWith("USER: ")) handleSpeechRaw(text.substring(6))
      if (text.startsWith("SYS: ")) {}
      if (text.startsWith("CHAT: ")) {}
   }

   private fun handleInputHttp(text: String) {
      if (text.startsWith("USER: ")) handleSpeechRaw(text.substring(6))
      if (text.startsWith("SYS: ")) speak(text.substring(5))
      if (text.startsWith("CHAT: ")) speak(text.substring(6))
   }

   private fun handleSpeechRaw(text: String) {
      if (handleBy.value==null)
         runFX { handleSpeech(text) }
      else
         runSuspending(VTc) { APP.http.client.put { bodyJs(text) } }
   }

   private fun handleSpeech(text: String?) {
      if (!isRunning) return
      speakingTextW.value = text
      text.orEmpty().sanitize().let { handlers.forEach { h -> h.action(it, h.commandUi) } }
   }

   /** Adjust speech text to make it more versatile and more likely to match command */
   private fun String.sanitize(): String = trim().removeSuffix(".").lowercase().words().filterNot(blacklistWordsSet::contains).joinToString(" ")

   @IsAction(name = "Synthesize voice", info = "Identical to \"Narrate text\"")
   fun synthesize() = speak()

   @IsAction(name = "Narrate text", info = "Narrates the specified text using synthesized voice")
   fun speak() = action<String>("Narrate text", "Narrates the specified text using synthesized voice", IconMA.RECORD_VOICE_OVER, BLOCK) { speak(it) }.invokeWithForm()

   fun speak(text: String) = invoke("SAY: ${text.encodeBase64()}")

   companion object: PluginInfo, KLogging() {
      override val name = "Speech Recognition"
      override val description = "Provides speech recognition, synthesis, LLM chat and voice control capabilities.\nSee https://github.com/openai/whisper"
      override val isSupported = true
      override val isSingleton = true
      override val isEnabledByDefault = false
   }

   enum class SpeechEngine(val code: String, override val nameUi: String, override val infoUi: String): NameUi, InfoUi {
      NONE("none", "None", "No voice"),
      SYSTEM("os", "System", "System voice"),
      CHARACTER_AI("character-ai", "Character.ai", "Voice using www.character.ai. Requires free account and access token");
   }

   /** Speech event handler */
   data class SpeakHandler(val name: String, val commandUi: String, val action: (String, String) -> Unit)

}