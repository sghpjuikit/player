package sp.it.pl.plugin.impl

import sp.it.util.async.coroutine.VT as VTc
import io.ktor.client.request.put
import java.io.File
import java.io.InputStream
import java.lang.ProcessBuilder.Redirect.PIPE
import java.util.regex.Pattern
import javafx.geometry.Pos.CENTER
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.Priority.ALWAYS
import mu.KLogging
import org.jetbrains.kotlin.utils.addToStdlib.butIf
import sp.it.pl.core.InfoUi
import sp.it.pl.core.NameUi
import sp.it.pl.core.bodyJs
import sp.it.pl.core.requestBodyAsJs
import sp.it.pl.layout.ComponentLoaderStrategy
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.WidgetFactory
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.AppHttp
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconMD
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.main.emScaled
import sp.it.pl.main.isAudio
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.pane.ActionData.Threading.BLOCK
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.pl.ui.pane.action
import sp.it.util.access.readOnly
import sp.it.util.access.vn
import sp.it.util.action.IsAction
import sp.it.util.async.VT
import sp.it.util.async.actor.ActorVt
import sp.it.util.async.coroutine.runSuspending
import sp.it.util.async.future.Fut
import sp.it.util.async.runFX
import sp.it.util.async.runNew
import sp.it.util.async.runOn
import sp.it.util.conf.Constraint
import sp.it.util.conf.Constraint.Multiline
import sp.it.util.conf.Constraint.MultilineRows
import sp.it.util.conf.Constraint.RepeatableAction
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
import sp.it.util.conf.uiConverter
import sp.it.util.conf.uiNoOrder
import sp.it.util.conf.valuesUnsealed
import sp.it.util.dev.fail
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Error
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.getAny
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.toUnit
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.attach
import sp.it.util.reactive.chan
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.on
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.plus
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.reactive.throttleToLast
import sp.it.util.system.EnvironmentContext
import sp.it.util.text.camelToSpaceCase
import sp.it.util.text.encodeBase64
import sp.it.util.text.equalsNc
import sp.it.util.text.lines
import sp.it.util.text.useStrings
import sp.it.util.text.words
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.textArea
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.seconds
import sp.it.util.units.version
import sp.it.util.units.year

/** Provides speech recognition and voice control capabilities. Uses whisper AI launched as python program. */
class VoiceAssistant: PluginBase() {
   private val onClose = Disposer()
   private val dir = APP.location / "speech-recognition-whisper"
   private var setup: Fut<Process>? = null
   private fun setup(): Fut<Process> {
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
            "coqui-voice=${speechEngineCoquiVoice.value}",
            "vlc-path=${APP.audio.playerVlcLocation.value.orEmpty()}",
            "chat-model=${chatModel.value}",
            "speech-recognition-model=${whisperModel.value}",
         )
         val command = EnvironmentContext.runAsProgramArgsTransformer(commandRaw)
         val process = ProcessBuilder(command)
            .directory(dir)
            .redirectOutput(PIPE).redirectError(PIPE)
            .start()

         var stdout = ""
         var stderr = ""
         val stdoutListener = process.inputStream.consume("SpeechRecognition-stdout") {
            stdout = it
               .map { it.ansi() }
               .filter { it.isNotEmpty() }
               .onEach { runFX {
                  speakingStdout.value = (speakingStdout.value ?: "") + it.un()
                  onLocalInput(it.un())
               } }
               .lines()
               .onEach { handleInputLocal(it.un()) }
               .joinToString("")
         }
         val stderrListener = process.errorStream.consume("SpeechRecognition-stderr") {
            stderr = it
               .filter { it.isNotEmpty() }
               .map { it.ansi() }
               .onEach { runFX {
                  speakingStdout.value = (speakingStdout.value ?: "") + it
                  onLocalInput(it)
               } }
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
         SpeakHandler("Resume playback", "play music") { text, command -> if (command in text) { APP.audio.resume(); Ok(null) } else null },
         SpeakHandler("Resume playback", "start music") { text, command -> if (command in text) { APP.audio.resume(); Ok(null) } else null },
         SpeakHandler("Pause playback", "stop music") { text, command -> if (command in text) { APP.audio.pause(); Ok(null) } else null },
         SpeakHandler("Pause playback", "end music") { text, command -> if (command in text) { APP.audio.pause(); Ok(null) } else null },
         SpeakHandler("Open widget by name", "[open|show] (widget)? \$widget-name (widget)?") { text, _ ->
            if (text.startsWith("open")) {
               val fName = text.removePrefix("open").trimStart().removePrefix("widget").removeSuffix("widget").trim().camelToSpaceCase()
               val f = APP.widgetManager.factories.getComponentFactories().find { it.name.camelToSpaceCase() equalsNc fName }
               if (f!=null) ComponentLoaderStrategy.DOCK.loader(f)
               if (f!=null) Ok("Ok") else Error("No widget $fName available")
            } else {
               null
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

   /** Invoked for every voice assistant local process input token. */
   val onLocalInput = Handler1<String>()

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

   /** Access token for character.ai account used when speech engine is Character.ai */
   val speechEngineCoquiVoice by cv("Ann_Daniels.flac")
      .valuesUnsealed { (dir / "voices-coqui").children().filter { it.isAudio() }.map { it.name }.toList() }
      .def(name = "Speech engine > coqui > voice", info = "Sample file of the voice to be used. User can add more audio samples to ${(dir / "voices-coqui").absolutePath}. Should be 3-10s long.")

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

   /** Invoked for every voice assistant http input token */
   val onHttpInput = Handler1<String>()

   /** Http input */
   private val httpInput by cvnro(vn<String>(null)).multilineToBottom(20).noPersist()
      .def(name = "Http input", info = "Shows input received over http.", editable = EditMode.APP)

   private val httpApi = Subscribed {
      APP.http.serverRoutes route AppHttp.Handler("/speech") {
         it.requestBodyAsJs().asJsStringValue().ifNotNull { text ->
            runFX { httpInput.value = (httpInput.value ?: "") + "\n" + text }
            handleInputHttp(text)
            onHttpInput(text)
         }
      }
   }

   private var isRunning = false

   override fun start() {
      /** Triggers event when process arg changes */
      val processChange = wakeUpWord.chan() + whisperModel.chan() + chatModel.chan() + speechEngine.chan() + speechEngineCharAiToken.chan()

      speechEngineCoquiVoice.chan().throttleToLast(2.seconds) subscribe { write("coqui-voice=$it") }

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
      write("EXIT")
      setup = null
   }

   @IsAction(name = "Restart Voice Assistant", info = "Restarts Voice Assistant python program")
   fun restart() {
      startSpeechRecognition()
      startSpeechRecognition()
   }

   private fun handleInputLocal(text: String) {
      if (text.startsWith("RAW: ")) Unit
      if (text.startsWith("USER: ")) handleSpeechRaw(text)
      if (text.startsWith("SYS: ")) handleSpeechRaw(text)
      if (text.startsWith("CHAT: ")) handleSpeechRaw(text)
      if (text.startsWith("COM: ")) handleSpeechRaw(text)
   }

   private fun handleInputHttp(text: String) {
      if (text.startsWith("RAW: ")) Unit
      if (text.startsWith("USER: ")) Unit
      if (text.startsWith("SYS: ")) speak(text.substringAfter(": "))
      if (text.startsWith("CHAT: ")) speak(text.substringAfter(": "))
      if (text.startsWith("COM: ")) speak(text.substringAfter(": "))
   }

   private fun handleSpeechRaw(text: String) {
      if (handleBy.value!=null)
         runSuspending(VTc) { APP.http.client.put("http://${handleBy.value}:${APP.http.url.port}/speech") { bodyJs(text) } }

      if (handleBy.value==null && text.startsWith("COM: "))
         runFX { handleSpeech(text.substringAfter(": ")) }
   }

   private fun handleSpeech(text: String?) {
      if (!isRunning) return
      speakingTextW.value = text
      var textSanitized = text.orEmpty().sanitize()
      var result = handlers.firstNotNullOfOrNull { h -> h.action(textSanitized, h.commandUi) } ?: Error<String?>("Unrecognized command: $text")
      result.getAny().ifNotNull(::speak)
   }

   /** Adjust speech text to make it more versatile and more likely to match command */
   private fun String.sanitize(): String = trim().removeSuffix(".").lowercase().words().filterNot(blacklistWordsSet::contains).joinToString(" ")

   private val writing = ActorVt<Pair<Fut<Process>?, String>>("SpeechRecognition-writer") { (setup, it) ->
      setup?.blockAndGetOrThrow()?.outputStream?.apply { write("$it\n".toByteArray()); flush() }
   }

   private fun write(text: String): Unit = writing(setup to text)

   @IsAction(name = "Synthesize voice", info = "Identical to \"Narrate text\"")
   fun synthesize() = speak()

   @IsAction(name = "Narrate text", info = "Narrates the specified text using synthesized voice")
   fun speak() = action<String>("Narrate text", "Narrates the specified text using synthesized voice", IconMA.RECORD_VOICE_OVER, BLOCK) { speak(it) }.invokeWithForm {
      addConstraints(Multiline).addConstraints(MultilineRows(10)).addConstraints(RepeatableAction)
   }

   fun speak(text: String) = write("SAY: ${text.encodeBase64()}")

   enum class SpeechEngine(val code: String, override val nameUi: String, override val infoUi: String): NameUi, InfoUi {
      NONE("none", "None", "No voice"),
      SYSTEM("os", "System", "System voice. Fully offline"),
      CHARACTER_AI("character-ai", "Character.ai", "Voice using www.character.ai. Requires free account and access token"),
      COQUI("coqui", "Coqui", "Voice using huggingface.co/coqui/XTTS-v2. Fully offline"),
   }

   /** Speech event handler. In ui shown as `"$name -> $commandUi"`. Action returns Try with text to speak or null if none. */
   data class SpeakHandler(val name: String, val commandUi: String, val action: (String, String) -> Try<String?, String?>?)

   companion object: PluginInfo, KLogging() {
      override val name = "Voice Assistant"
      override val description = "Provides speech recognition, synthesis, LLM chat and voice control capabilities.\nSee https://github.com/openai/whisper"
      override val isSupported = true
      override val isSingleton = true
      override val isEnabledByDefault = false

      val speechRecognitionWidgetFactory = WidgetFactory("VoiceAssistent", VoiceAssistentWidget::class, APP.location.widgets/"VoiceAssistent")

      private val ansi = Pattern.compile("\\x1B\\[[0-?]*[ -/]*[@-~]")

      private fun String.ansi() = ansi.matcher(this).replaceAll("")

      private fun String?.wrap() = if (isNullOrBlank()) "" else "\n$this"

      private fun String.un() = replace("\u2028", "\n")

      private fun InputStream.consume(name: String, block: (Sequence<String>) -> Unit) =
         runOn(VT(name)) { useStrings(1024*1024, block = block) }
   }

   class VoiceAssistentWidget(widget: Widget): SimpleController(widget) {

      init {
         val plugin = APP.plugins.plugin<VoiceAssistant>().asValue(onClose)
         root.prefSize = 500.emScaled x 500.emScaled
         root.consumeScrolling()
         root.lay += vBox(null, CENTER) {
            lay += hBox(null, CENTER) {
               lay += Icon(IconFA.COG).tooltip("Settings").onClickDo { APP.actions.app.openSettings(plugin.value?.configurableGroupPrefix) }.apply {
                  disableProperty() syncFrom plugin.map { it==null }
               }
               lay += Icon(IconFA.REFRESH).tooltip("Restart voice assistent").onClickDo { plugin.value?.restart() }
               lay += Icon(IconMD.TEXT_TO_SPEECH).tooltip("Speak text").onClickDo { plugin.value?.speak() }
               lay += Icon().apply {
                  isMouseTransparent = true
                  isFocusTraversable = false
                  plugin.sync { icon(it!=null, IconMA.MIC, IconMA.MIC_OFF) }
               }
               lay += label {
                  plugin.sync { text = if (it!=null) "Active" else "Inactive" }
               }
            }
            lay(ALWAYS) += textArea {
               isEditable = false
               isWrapText = true
               onEventDown(KEY_PRESSED, ENTER) { appendText("\n") }
               plugin.syncNonNullWhile { it.onLocalInput attach ::appendText }
               plugin.syncNonNullWhile { it.onHttpInput attach ::appendText }
            }
         }
      }

      companion object: WidgetCompanion {
         override val id = "VoiceAssistant"
         override val name = "Voice Assistant"
         override val description = "Voice Assistant plugin UI"
         override val descriptionLong = "$description."
         override val icon = IconMA.MIC
         override val version = version(1, 0, 0)
         override val isSupported = true
         override val year = year(2023)
         override val author = "spit"
         override val contributor = ""
         override val tags = setOf(UTILITY)
         override val summaryActions = listOf<ShortcutPane.Entry>()
      }
   }

}