package sp.it.pl.plugin.impl

import sp.it.util.async.coroutine.VT as VTc
import io.ktor.client.request.put
import java.io.InputStream
import java.lang.ProcessBuilder.Redirect.PIPE
import java.time.LocalDate
import java.time.LocalTime
import java.util.regex.Pattern
import javafx.geometry.Pos.CENTER
import javafx.scene.control.ScrollPane
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.SHIFT
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Priority.NEVER
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Line
import javax.sound.sampled.TargetDataLine
import mu.KLogging
import sp.it.pl.core.InfoUi
import sp.it.pl.core.NameUi
import sp.it.pl.core.bodyJs
import sp.it.pl.core.requestBodyAsJs
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
import sp.it.pl.main.appTooltip
import sp.it.pl.main.emScaled
import sp.it.pl.main.isAudio
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.pl.ui.ValueToggleButtonGroup
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.pane.ActionData.Threading.BLOCK
import sp.it.pl.ui.pane.ConfigPane
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.pl.ui.pane.action
import sp.it.pl.voice.toVoiceS
import sp.it.util.access.V
import sp.it.util.access.readOnly
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.action.IsAction
import sp.it.util.async.NEW
import sp.it.util.async.actor.ActorVt
import sp.it.util.async.coroutine.runSuspending
import sp.it.util.async.future.Fut
import sp.it.util.async.runFX
import sp.it.util.async.runOn
import sp.it.util.collections.setTo
import sp.it.util.conf.Constraint.Multiline
import sp.it.util.conf.Constraint.MultilineRows
import sp.it.util.conf.Constraint.RepeatableAction
import sp.it.util.conf.EditMode
import sp.it.util.conf.ListConfigurable
import sp.it.util.conf.between
import sp.it.util.conf.butElement
import sp.it.util.conf.cList
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.cvnro
import sp.it.util.conf.def
import sp.it.util.conf.getDelegateConfig
import sp.it.util.conf.min
import sp.it.util.conf.multiline
import sp.it.util.conf.multilineToBottom
import sp.it.util.conf.noPersist
import sp.it.util.conf.password
import sp.it.util.conf.readOnly
import sp.it.util.conf.uiConverter
import sp.it.util.conf.uiNoCustomUnsealedValue
import sp.it.util.conf.uiNoOrder
import sp.it.util.conf.values
import sp.it.util.conf.valuesUnsealed
import sp.it.util.dev.fail
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.getAny
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.functional.supplyIf
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
import sp.it.util.reactive.zip
import sp.it.util.reactive.zip2
import sp.it.util.system.EnvironmentContext
import sp.it.util.text.applyBackspace
import sp.it.util.text.camelToSpaceCase
import sp.it.util.text.concatApplyBackspace
import sp.it.util.text.encodeBase64
import sp.it.util.text.keys
import sp.it.util.text.lines
import sp.it.util.text.nameUi
import sp.it.util.text.split2
import sp.it.util.text.splitTrimmed
import sp.it.util.text.useStrings
import sp.it.util.text.words
import sp.it.util.ui.appendTextSmart
import sp.it.util.ui.hBox
import sp.it.util.ui.insertNewline
import sp.it.util.ui.isNewlineOnShiftEnter
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.singLineProperty
import sp.it.util.ui.stackPane
import sp.it.util.ui.styleclassToggle
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
      return runOn(NEW("SpeechRecognition-starter")) {
         val whisper = dir / "main.py"
         val commandRaw = listOf(
            "python", whisper.absolutePath,
            "wake-word=${wakeUpWord.value}",
            "printRaw=${printRaw.value}",
            "mic-on=${micOn.value}",
            "mic-name=${micName.value ?: ""}",
            "mic-energy=${micEnergy.value}",
            "mic-energy-debug=${micEnergyDebug.value}",
            "parent-process=${ProcessHandle.current().pid()}",
            "speech-on=${speechOn.value}",
            "speech-engine=${speechEngine.value.code}",
            "character-ai-token=${speechEngineCharAiToken.value}",
            "character-ai-voice=22",
            "coqui-voice=${speechEngineCoquiVoice.value}",
            "coqui-cuda-device=${speechEngineCoquiCudaDevice.value ?: ""}",
            "coqui-server=${if (speechServer.value) speechServerUrl.value else ""}",
            "speech-server=${speechEngineHttpUrl.value}",
            "llm-engine=${llmEngine.value.code}",
            "llm-gpt4all-model=${llmGpt4AllModel.value}",
            "llm-openai-url=${llmOpenAiUrl.value}",
            "llm-openai-bearer=${llmOpenAiBearer.value}",
            "llm-openai-model=${llmOpenAiModel.value}",
            "llm-chat-sys-prompt=${llmChatSysPrompt.value}",
            "llm-chat-max-tokens=${llmChatMaxTokens.value}",
            "llm-chat-temp=${llmChatTemp.value}",
            "llm-chat-topp=${llmChatTopP.value}",
            "llm-chat-topk=${llmChatTopK.value}",
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
                  speakingStdout.value = (speakingStdout.value ?: "").concatApplyBackspace(it.un())
                  onLocalInput(it.un())
               } }
               .lines()
               .map { it.applyBackspace() }
               .onEach { handleInput(it.un()) }
               .joinToString("")
         }
         val stderrListener = process.errorStream.consume("SpeechRecognition-stderr") {
            stderr = it
               .filter { it.isNotEmpty() }
               .map { it.ansi() }
               .map { it.applyBackspace() }
               .onEach { runFX {
                  speakingStdout.value = (speakingStdout.value ?: "") + it
                  onLocalInput(it)
               } }
               .joinToString("")
         }

         // run
         runOn(NEW("SpeechRecognition")) {
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

   var commandWidgetNames = mapOf<String, String>()
      private set

   private val commandWidgetNamesRaw by cv("")
      .def(
         name = "Commands > widget names",
         info = "Alternative names for widgets for voice control, either for customization or easier recognition. " +
            "V" +
            "Comma separated names, widget per line, i.e.: `My widget = name1, name2, ..., nameN`"
      ).multiline(10) sync {
         commandWidgetNames = it.lines()
            .filter { it.isNotBlank() }
            .map { runTry { it.split2("=").net { (w, names) -> names.splitTrimmed(",").map { it.trim().lowercase() to w.trim().camelToSpaceCase() } } }.orNull() }
            .filterNotNull()
            .flatMap { it }
            .associate { it }
      }

   /** Speech handlers called when user has spoken. Matched in order. */
   val handlers by cList(
         SpeakHandler(                               "Help", "help")                                         { if (matches(it)) Ok("List commands by saying, list commands") else null },
         SpeakHandler(                         "Do nothing", "ignore")                                       { if (matches(it)) Ok(null) else null },
         SpeakHandler(                      "Help Commands", "list commands")                                { if (matches(it)) Ok(handlersHelpText()) else null },
         SpeakHandler(                       "Current time", "what time is it")                              { if (matches(it)) Ok(LocalTime.now().net { "Right now it is ${it.toVoiceS()}" }) else null },
         SpeakHandler(                       "Current date", "what date is it")                              { if (matches(it)) Ok(LocalDate.now().net { "Today is ${it.toVoiceS()}" }) else null },
         SpeakHandler(                    "Resume playback", "play|start|resume|continue music|playback")    { if (matches(it)) { APP.audio.resume(); Ok(null) } else null },
         SpeakHandler(                     "Pause playback", "stop|end|pause music|playback")                { if (matches(it)) { APP.audio.pause(); Ok(null) } else null },
         SpeakHandler(                 "Play previous song", "play previous song")                           { if (matches(it)) { APP.audio.playlists.playPreviousItem(); Ok(null) } else null },
         SpeakHandler(                     "Play next song", "play next song")                               { if (matches(it)) { APP.audio.playlists.playNextItem(); Ok(null) } else null },
         SpeakHandler( "Generate llm answer from clipboard", "generate|answer|write from? clipboard \$text") { voiceCommandGenerateClipboard(it) },
         SpeakHandler(                "Generate llm answer", "generate|answer|write \$text")                 { voiceCommandGenerate(it) },
         SpeakHandler(               "Speak from clipboard", "speak|say from? clipboard")                    { voiceCommandSpeakClipboard(it) },
         SpeakHandler(                              "Speak", "speak|say \$text")                             { voiceCommandSpeakText(it) },
         SpeakHandler(   "Close window (${keys("ALT+F4")})", "close|hide window")                            { voiceCommandAltF4(it) },
         SpeakHandler(                "Open widget by name", "open|show widget? \$widget-name widget?")      { voiceCommandOpenWidget(it) },
      )
      .noPersist().readOnly().butElement { uiConverter { "${it.name} -> ${it.commandUi}" } }
      .def(
         name = "Commands",
         info = "Shows active voice speech recognition commands.\n Matched in order.\n '?' means optional word. '|' means alternative word."
      )

   /** [handlers] help text */
   private fun handlersHelpText(): String =
      handlers
         .mapIndexed { i, h -> "\n$i. ${h.name}, activate by saying ${h.commandUi}" }
         .joinToString("", prefix = "Here is list of currently active commands:")

   /** Last spoken text - writable */
   private val speakingTextW = vn<String>(null)

   /** Last spoken text */
   val speakingText = speakingTextW.readOnly()

   /** Console output */
   val speakingStdout by cvnro(vn<String>(null)).multilineToBottom(20).noPersist()
      .def(name = "Speech recognition output", info = "Shows console output of the speech recognition Whisper AI process", editable = EditMode.APP)

   /** Whether microphone listening is allowed. */
   val micOn by cv(true)
      .def(name = "Microphone enabled", info = "Whether microphone listening is allowed. In general, this also prevents initial loading of Whisper speech-to-text AI model until enabled.")

   /** Microphone to be used. Null if auto. */
   val micName by cvn<String>(null)
      .valuesUnsealed {
         AudioSystem.getMixerInfo()
            .filter { AudioSystem.getMixer(it).net { m -> m.targetLineInfo.isNotEmpty() && m.isLineSupported(Line.Info(TargetDataLine::class.java)) } }
            .map { it.name }
      }
      .uiNoCustomUnsealedValue()
      .def(name = "Microphone name", info = "Microphone to be used. Null causes automatic microphone selection.")

   /** Microphone energy voice treshold. Volume above this number is considered speech. */
   val micEnergy by cv(120).min(0)
      .def(name = "Microphone energy", info = "Microphone energt. Volume above this number is considered speech.")

   val micEnergyDebug by cv(false)
      .def(name = "Microphone energy > debug", info = "Whether current microphone energy lvl is active. Use to setup microphone energy voice treshold.")

   /** Whether `RAW: $text` values will be shown. */
   val printRaw by cv(true)
      .def(name = "Print raw", info = "Whether `RAW: \$text` values will be shown.")

   /** Invoked for every voice assistant local process input token. */
   val onLocalInput = Handler1<String>()

   /** Words or phrases that will be removed from text representing the detected speech. Makes command matching more powerful. Case-insensitive. */
   val wakeUpWord by cv("system")
      .def(name = "Wake up word", info = "Words or phrase that activates voice recognition. Case-insensitive.")

   /** Engine used to generate voice. May require additional configuration */
   val whisperModel by cv("base.en")
      .values { listOf("tiny.en", "tiny", "base.en", "base", "small.en", "small", "medium.en", "medium", "large", "large-v1", "large-v2", "large-v3") }
      .uiNoOrder()
      .def(name = "Speech recognition model", info = "Whisper model for speech recognition.")

   /** Whether speech is allowed. */
   val speechOn by cv(true)
      .def(name = "Speech enabled", info = "Whether speech is allowed. In general, this also prevents initial loading of speech AI model until enabled.")

   /** Engine used to generate voice. May require additional configuration */
   val speechEngine by cv(SpeechEngine.SYSTEM).uiNoOrder()
      .def(name = "Speech engine", info = "Engine used to generate voice. May require additional configuration")

   /** Access token for character.ai account used when speech engine is Character.ai */
   val speechEngineCharAiToken by cvn<String>(null).password()
      .def(
         name = "Speech engine > character.ai > token",
         info = "Access token for character.ai account used when using ${SpeechEngine.CHARACTER_AI.nameUi} speech engine"
      )

   /** Access token for character.ai account used when speech engine is Character.ai */
   val speechEngineCoquiVoice by cv("Ann_Daniels.flac")
      .valuesUnsealed { (dir / "voices-coqui").children().filter { it.isAudio() }.map { it.name }.toList() }
      .def(
         name = "Speech engine > coqui > voice",
         info = "" +
            "Voice when using ${SpeechEngine.COQUI.nameUi} speech engine. " +
            "Sample file of the voice to be used. User can add more audio samples to ${(dir / "voices-coqui").absolutePath}. " +
            "Should be 3-10s long."
      )

   /** Access token for character.ai account used when speech engine is Character.ai */
   val speechEngineCoquiCudaDevice by cvn<Int>(null)
      .min(0)
      .def(
         name = "Speech engine > coqui > cuda device",
         info = "Cuda device for speech generation when using ${SpeechEngine.COQUI.nameUi} speech engine."
      )

   /** Speech server address and port to connect to. */
   val speechEngineHttpUrl by cv("localhost:1235")
      .def(
         name = "Speech engine > http > url",
         info = "Speech server address and port to connect to when using ${SpeechEngine.HTTP.nameUi} speech engine."
      )

   /** Whether http server providing speech generation should be started. The server will use coqui speech engine. */
   val speechServer by cv(false)
      .def(name = "Speech server",
         info = "Whether http server providing speech generation should be started. The server will use coqui speech engine")

   /** Speech server address and port. */
   val speechServerUrl by cv("0.0.0.0:1235")
      .def(name = "Speech server > url", info = "Speech server address and port.")

   /** Words or phrases that will be removed from text representing the detected speech. Makes command matching more powerful. Case-insensitive. */
   val speechBlacklistWords by cList("a", "the", "please")
      .def(
         name = "Blacklisted words",
         info = "Words or phrases that will be removed from text representing the detected speech. Makes command matching more powerful. Case-insensitive."
      )

   /** [speechBlacklistWords] in performance-optimal format */
   private val speechBlacklistWords_ = mutableSetOf<String>().apply {
      speechBlacklistWords.onChangeAndNow { this setTo speechBlacklistWords.map { it.lowercase() } }
   }

   /** Engine used to generate voice. May require additional configuration */
   val llmEngine by cv(LlmEngine.NONE)
      .def(name = "Llm engine", info = "LLM engine for chat")

   /** Model for gpt4all. Must be in models-llm. */
   val llmGpt4AllModel by cv("none")
      .valuesUnsealed { dir.div("models-llm").children().map { it.name }.filter { it.endsWith("gguf") }.toList() + "none" }
      .def(name = "Llm engine > gpt4all > model", info = "Model for gpt4all. Must be in ${(dir / "models-llm").absolutePath}")

   /** Url of the OpenAI or OpenAI-compatible server */
   val llmOpenAiUrl by cv("http://localhost:1234/v1")
      .def(name = "Llm engine > openai > url", info = "Url of the OpenAI or OpenAI-compatible server")

   /** The user authorization of the OpenAI or OpenAI-compatible server */
   val llmOpenAiBearer by cv("ABC123xyz789").password()
      .def(name = "Llm engine > openai > bearer", info = "The user authorization of the OpenAI or OpenAI-compatible server")

   /** The llm model of the OpenAI or OpenAI-compatible server */
   val llmOpenAiModel by cv("").password()
      .def(name = "Llm engine > openai > model", info = "The llm model of the OpenAI or OpenAI-compatible server")

   /** System prompt telling llm to assume role, or exhibit behavior */
   val llmChatSysPrompt by cvn("You are helpful voice assistant. You are voiced by tts, be extremly short.").multiline(3)
      .def(name = "Llm chat > system prompt", info = "System prompt telling llm to assume role, or exhibit behavior")

   /** Maximum number of tokens in the reply. Further tokens will be cut off (by llm) */
   val llmChatMaxTokens by cvn(400).min(1)
      .def(name = "Llm chat > max tokens", info = "Maximum number of tokens in the reply. Further tokens will be cut off (by llm)")

   /** Output randomness. Scale 0-1, i.e. determinism-creativity. 0 will always pick the most likely next token.. */
   val llmChatTemp by cvn(0.5).between(0.0, 1.0)
      .def(name = "Llm chat > temperature", info = "The llm model of the OpenAI or OpenAI-compatible server")

   /** TODO */
   val llmChatTopP by cvn(0.95).between(0.1, 1.0)
      .def(name = "Llm chat > top P", info = "")

   /** TODO */
   val llmChatTopK by cvn(40).min(1)
      .def(name = "Llm chat > top K", info = "")

   private var isRunning = false

   override fun start() {
      // runtime-changeable properties
      // @formatter:off
      speechEngineCoquiVoice.chan().throttleToLast(2.seconds) subscribe { write("coqui-voice=$it") }
                    printRaw.chan().throttleToLast(2.seconds) subscribe { write("print-raw=$it") }
                       micOn.chan().throttleToLast(2.seconds) subscribe { write("mic-on=$it") }
                   micEnergy.chan().throttleToLast(2.seconds) subscribe { write("mic-energy=$it") }
              micEnergyDebug.chan().throttleToLast(2.seconds) subscribe { write("mic-energy-debug=$it") }
                    speechOn.chan().throttleToLast(2.seconds) subscribe { write("speech-on=$it") }
            llmChatSysPrompt.chan().throttleToLast(2.seconds) subscribe { write("llm-chat-sys-prompt=$it") }
            llmChatMaxTokens.chan().throttleToLast(2.seconds) subscribe { write("llm-chat-max-tokens=$it") }
                 llmChatTemp.chan().throttleToLast(2.seconds) subscribe { write("llm-chat-temp=$it") }
                 llmChatTopP.chan().throttleToLast(2.seconds) subscribe { write("llm-chat-topp=$it") }
                 llmChatTopK.chan().throttleToLast(2.seconds) subscribe { write("llm-chat-topk=$it") }

      startSpeechRecognition()

      // restart-requiring properties
      val processChangeVals = listOf<V<*>>(
         wakeUpWord, micName, whisperModel,
         speechEngine, speechEngineCharAiToken, speechEngineCoquiCudaDevice, speechEngineHttpUrl, speechServer, speechServerUrl,
         llmEngine, llmGpt4AllModel, llmOpenAiUrl, llmOpenAiBearer, llmOpenAiModel,
      )
      val processChange = processChangeVals.map { it.chan() }.reduce { a, b -> a + b }
      processChange.throttleToLast(2.seconds).subscribe { restart() } on onClose

      isRunning = true
      // @formatter:on
   }

   override fun stop() {
      isRunning = false
      stopSpeechRecognition()
      writing.closeAndWait()
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
      stopSpeechRecognition()
      startSpeechRecognition()
   }

   private fun handleInput(text: String) {
      if (text.startsWith("RAW: ")) Unit
      if (text.startsWith("USER: ")) handleSpeechRaw(text)
      if (text.startsWith("SYS: ")) handleSpeechRaw(text)
      if (text.startsWith("CHAT: ")) handleSpeechRaw(text)
      if (text.startsWith("COM: ")) handleSpeechRaw(text)
      if (text.startsWith("COM-DET: ")) handleSpeechRaw(text)
   }

   private fun handleSpeechRaw(text: String) {
      if (text.startsWith("COM: "))
         runFX { handleSpeech(text.substringAfter(": "), true) }
      if (text.startsWith("COM-DET: "))
         runFX { handleSpeech(text.substringAfter(": "), false) }
   }

   private fun handleSpeech(text: String, orDetectIntent: Boolean) {
      if (!isRunning) return
      speakingTextW.value = text
      var textSanitized = text.orEmpty().sanitize(speechBlacklistWords_)
      var result = handlers.firstNotNullOfOrNull { with(it) { SpeakContext(this, this@VoiceAssistant).action(textSanitized) } }
      if (result==null) {
         if (orDetectIntent) write("COM-DET: ${text.encodeBase64()}")
         else speak("Unrecognized command: $text")
      } else
         result.getAny().ifNotNull(::speak)
   }

   private val writing = ActorVt<Pair<Fut<Process>?, String>>("SpeechRecognition-writer") { (setup, it) ->
      setup?.blockAndGetOrThrow()?.outputStream?.apply { write("$it\n".toByteArray()); flush() }
   }

   fun write(text: String): Unit = writing(setup to text)

   @IsAction(name = "Speak text", info = "Identical to \"Narrate text\"")
   fun synthesize() = speak()

   @IsAction(name = "Narrate text", info = "Narrates the specified text using synthesized voice")
   fun speak() = action<String>("Narrate text", "Narrates the specified text using synthesized voice", IconMA.RECORD_VOICE_OVER, BLOCK) { speak(it) }.apply {
      constraintsN += listOf(Multiline, MultilineRows(10), RepeatableAction)
   }.invokeWithForm()

   fun speak(text: String) = write("SAY: ${text.encodeBase64()}")

   @IsAction(name = "Write chat", info = "Writes to voice assistant chat")
   fun chat() = action<String>("Write chat", "Writes to voice assistant chat", IconMA.CHAT, BLOCK) { chat(it) }.apply {
      constraintsN += listOf(Multiline, MultilineRows(10), RepeatableAction)
   }.invokeWithForm()

   fun chat(text: String) = write("CHAT: ${text.encodeBase64()}")

   fun raw(text: String) = write(text)

   enum class SpeechEngine(val code: String, override val nameUi: String, override val infoUi: String): NameUi, InfoUi {
      NONE("none", "None", "No voice"),
      SYSTEM("os", "System", "System voice. Fully offline"),
      CHARACTER_AI("character-ai", "Character.ai", "Voice using www.character.ai. Requires free account and access token"),
      COQUI("coqui", "Coqui", "Voice using huggingface.co/coqui/XTTS-v2. Fully offline"),
      HTTP("http", "Http server", "Voice using different instance of this application with speech server enabled"),
   }

   enum class LlmEngine(val code: String, override val nameUi: String, override val infoUi: String): NameUi, InfoUi {
      NONE("none", "None", "No chat"),
      GPT4ALL("gpt4all", "Gpt4All", "Gpt4All python bindings (requires downloading and specifying model)"),
      OPENAI("openai", "OpenAi", "OpenAI http client (requires access, but custom local server is also possible, e.g. LmStudio )"),
   }

   /** [SpeakHandler] action context. */
   data class SpeakContext(val handler: SpeakHandler, val plugin: VoiceAssistant) {
      /** [regex].matches(text) */
      fun matches(text: String): Boolean = handler.regex.matches(text)
   }

   /** Speech event handler. In ui shown as `"$name -> $commandUi"`. Action returns Try (with text to speak or null if none) or null if no match. */
   data class SpeakHandler(val name: String, val commandUi: String, val action: SpeakContext.(String) -> Try<String?, String?>?) {
      /** [commandUi] turned into regex */
      val regex by lazy { voiceCommandRegex(commandUi) }
   }

   companion object: PluginInfo, KLogging() {
      override val name = "Voice Assistant"
      override val description = "Provides speech recognition, synthesis, LLM chat and voice control capabilities.\nSee https://github.com/openai/whisper"
      override val isSupported = true
      override val isSingleton = true
      override val isEnabledByDefault = false

      val speechRecognitionWidgetFactory by lazy { WidgetFactory("VoiceAssistent", VoiceAssistentWidget::class, APP.location.widgets/"VoiceAssistent") }

      /** Ansi escape sequence pattern */
      private val ansi = Pattern.compile("\\x1B\\[[0-?]*[ -/]*[@-~]")

      /** @return this string without ansi escape sequences */
      private fun String.ansi() = ansi.matcher(this).replaceAll("")

      /** @return this string with newline prepended or empty string if blank */
      private fun String?.wrap() = if (isNullOrBlank()) "" else "\n$this"

      /** @return this string with unicode newline `\u2028` replaced to `\n` */
      private fun String.un() = replace("\u2028", "\n")

      /** @return consumes this input stream on virtual thread with the specified name by the block taking sequence of strings as written to the stream */
      private fun InputStream.consume(name: String, block: (Sequence<String>) -> Unit) =
         runOn(NEW(name)) { useStrings(1024*1024, block = block) }

      /** @return speech text adjusted to make it more versatile and more likely to match command */
      internal fun String.sanitize(blacklistWordsSet: Set<String>): String =
         trim().removeSuffix(".").lowercase().words().filterNot(blacklistWordsSet::contains).joinToString(" ")

   }

   class VoiceAssistentWidget(widget: Widget): SimpleController(widget) {

      init {
         val plugin = APP.plugins.plugin<VoiceAssistant>().asValue(onClose)
         var run = {}
         val mode = v("Chat")
         val chatSettings = v(false)

         root.prefSize = 500.emScaled x 500.emScaled
         root.consumeScrolling()
         root.lay += vBox(null, CENTER) {
            lay += hBox(null, CENTER) {
               lay += Icon(IconFA.COG).tooltip("Settings").apply {
                  disableProperty() syncFrom plugin.map { it==null }
                  onClickDo { APP.actions.app.openSettings(plugin.value?.configurableGroupPrefix) }
               }
               lay += CheckIcon().icons(IconMD.FILTER, IconMD.FILTER_REMOVE_OUTLINE).apply {
                  disableProperty() syncFrom plugin.map { it==null }
                  selected syncFrom plugin.flatMap { it!!.printRaw }.orElse(true)
                  selected attach { plugin.value?.printRaw?.value = it }
                  tooltip("Hide debug and raw output")
               }
               lay += Icon(IconFA.REFRESH).tooltip("Restart voice assistent")
                  .onClickDo { plugin.value?.restart() }
               lay += label("   ")
               lay += CheckIcon().icons(IconMD.TEXT_TO_SPEECH, IconMD.TEXT_TO_SPEECH_OFF).apply {
                  disableProperty() syncFrom plugin.map { it==null }
                  selected syncFrom plugin.flatMap { it!!.speechOn }.orElse(false)
                  selected attach { plugin.value?.speechOn?.value = it }
                  tooltip("Enable/disable voice")
               }
               lay += CheckIcon().icons(IconMA.MIC, IconMA.MIC_OFF).apply {
                  disableProperty() syncFrom plugin.map { it==null }
                  selected syncFrom plugin.flatMap { it!!.micOn }.orElse(false)
                  selected attach { plugin.value?.micOn?.value = it }
                  tooltip("Enable/disable microphone")
               }
               lay += label {
                  plugin.sync { text = if (it!=null) "Active" else "Inactive" }
               }
               lay += label("   ")
               lay += ValueToggleButtonGroup.ofObservableValue(mode, listOf("Raw", "Speak", "Chat")) {
                  tooltip = appTooltip(
                     when (it) {
                        "Raw" -> "Send the text to the Voice Assestant as if user wrote it in console"
                        "Speak" -> "Narrates the specified text using synthesized voice"
                        "Chat" -> "Send the text to the Voice Assestant as if user spoke it"
                        else -> fail { "Illegal value" }
                     }
                  )
               }.apply {
                  alignment = CENTER
               }
            }
            lay(ALWAYS) += hBox(5.emScaled, CENTER) {
               lay(ALWAYS) += vBox(5.emScaled, CENTER) {
                  lay(ALWAYS) += textArea {
                     id = "output"
                     isEditable = false
                     isFocusTraversable = false
                     isWrapText = true
                     prefColumnCount = 100

                     onEventDown(KEY_PRESSED, ENTER) { appendText("\n") }
                     plugin.syncNonNullWhile { it.onLocalInput attach ::appendTextSmart }
                  }
                  lay += stackPane {
                     lay(CENTER) += hBox(null, CENTER) {
                        lay(ALWAYS) += textArea("") {
                           id = "input"
                           isWrapText = true
                           isNewlineOnShiftEnter = true
                           prefColumnCount = 100
                           promptText = "${ENTER.nameUi} to send, ${SHIFT.nameUi} + ${ENTER.nameUi} for new line"
                           singLineProperty() sync {
                              styleclassToggle("text-area-singlelined", !it)
                              prefRowCount = if (it) 10 else 1
                           }

                           run = {
                              when (mode.value) {
                                 "Raw" -> { plugin.value?.raw(text) }
                                 "Speak" -> { plugin.value?.speak(text); clear() }
                                 "Chat" -> { plugin.value?.chat(text); clear() }
                              }
                           }
                           onEventDown(KEY_PRESSED, ENTER) { if (it.isShiftDown) insertNewline() else run() }
                        }
                        lay(NEVER) += CheckIcon(chatSettings).icons(IconFA.COG, IconFA.COG)
                        lay(NEVER) += Icon(IconFA.SEND).onClickDo { run() }
                     }
                  }
               }
               lay += stackPane {
                  chatSettings zip plugin zip2 mode sync { (showSettings, active, mode) ->
                     lay.clear()
                     lay += supplyIf(showSettings && active!=null && mode=="Chat") {
                        scrollPane {
                           isFitToWidth = true
                           isFitToHeight = false
                           prefSize = -1 x -1
                           vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
                           hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
                           minWidth = 250.emScaled
                           content = ConfigPane(
                              ListConfigurable.heterogeneous(
                                 plugin.value?.net {
                                    listOf(
                                       it::llmChatSysPrompt.getDelegateConfig(),
                                       it::llmChatMaxTokens.getDelegateConfig(),
                                       it::llmChatTemp.getDelegateConfig(),
                                       it::llmChatTopP.getDelegateConfig(),
                                       it::llmChatTopK.getDelegateConfig(),
                                    )
                                 }.orEmpty()
                              )
                           )
                        }
                     }
                  }
               }
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