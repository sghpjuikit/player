package sp.it.pl.plugin.impl

import com.sun.jna.platform.win32.Kernel32
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import java.io.IOException
import java.io.InputStream
import java.lang.ProcessBuilder.Redirect.PIPE
import java.lang.StringBuilder
import java.util.regex.Pattern
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Line
import javax.sound.sampled.TargetDataLine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.invoke
import kotlinx.coroutines.withContext
import mu.KLogging
import sp.it.pl.core.InfoUi
import sp.it.pl.core.NameUi
import sp.it.pl.core.bodyAsJs
import sp.it.pl.core.bodyJs
import sp.it.pl.layout.WidgetFactory
import sp.it.pl.layout.WidgetUse.ANY
import sp.it.pl.main.APP
import sp.it.pl.main.Events.AppEvent.SystemSleepEvent
import sp.it.pl.main.IconMA
import sp.it.pl.main.isAudio
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.pl.ui.pane.ActionData.Threading.BLOCK
import sp.it.pl.ui.pane.action
import sp.it.util.access.V
import sp.it.util.access.readOnly
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.action.IsAction
import sp.it.util.async.NEW
import sp.it.util.async.actor.ActorVt
import sp.it.util.async.coroutine.FX
import sp.it.util.async.coroutine.VT
import sp.it.util.async.coroutine.invokeTry
import sp.it.util.async.coroutine.launch
import sp.it.util.async.future.Fut
import sp.it.util.async.runFX
import sp.it.util.async.runOn
import sp.it.util.collections.list.DestructuredList
import sp.it.util.collections.observableList
import sp.it.util.collections.setTo
import sp.it.util.conf.Constraint.Multiline
import sp.it.util.conf.Constraint.MultilineRows
import sp.it.util.conf.Constraint.RepeatableAction
import sp.it.util.conf.between
import sp.it.util.conf.butElement
import sp.it.util.conf.cList
import sp.it.util.conf.cr
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.min
import sp.it.util.conf.multiline
import sp.it.util.conf.noPersist
import sp.it.util.conf.nonBlank
import sp.it.util.conf.nonEmpty
import sp.it.util.conf.password
import sp.it.util.conf.readOnly
import sp.it.util.conf.uiConverter
import sp.it.util.conf.uiNoCustomUnsealedValue
import sp.it.util.conf.uiNoOrder
import sp.it.util.conf.values
import sp.it.util.conf.valuesUnsealed
import sp.it.util.dev.doNothing
import sp.it.util.dev.fail
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.file.json.JsNull
import sp.it.util.file.json.JsObject
import sp.it.util.file.json.JsString
import sp.it.util.file.json.JsValue
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Error
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.getAny
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.functional.toUnit
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.chan
import sp.it.util.reactive.on
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.reactive.plus
import sp.it.util.reactive.throttleToLast
import sp.it.util.system.EnvironmentContext
import sp.it.util.system.Os.WINDOWS
import sp.it.util.text.Char32
import sp.it.util.text.applyBackspace
import sp.it.util.text.camelToSpaceCase
import sp.it.util.text.chars32
import sp.it.util.text.concatApplyBackspace
import sp.it.util.text.encodeBase64
import sp.it.util.text.keys
import sp.it.util.text.lines
import sp.it.util.text.split2
import sp.it.util.text.splitTrimmed
import sp.it.util.text.toPrintableNonWhitespace
import sp.it.util.text.useStrings
import sp.it.util.text.words
import sp.it.util.type.atomic
import sp.it.util.units.seconds

/** Provides speech recognition and voice control capabilities. Uses whisper AI launched as python program. */
class VoiceAssistant: PluginBase() {
   private val onClose = Disposer()
   private val dir = APP.location / "speech-recognition-whisper"
   private var setup: Fut<Process>? = null
   private fun setup(): Fut<Process> {
      fun doOnError(eText: String, e: Throwable?, details: String?) = logger.error(e) { "$eText\n${details.wrap()}" }.toUnit()
      return runOn(NEW("SpeechRecognition-python-starter")) {
         val python = dir / "main.py"
         val commandRaw = listOf(
            "python", python.absolutePath,
            "wake-word=${wakeUpWord.value}",
            "mic-enabled=${micEnabled.value}",
            "mic-name=${micName.value ?: ""}",
            "mic-energy=${micEnergy.value}",
            "mic-energy-debug=${micEnergyDebug.value}",
            "mic-voice-detect=${micVoiceDetect.value}",
            "mic-voice-detect-prop=${micVoiceDetectProb.value}",
            "mic-voice-detect-debug=${micVoiceDetectProbDebug.value}",
            "parent-process=${ProcessHandle.current().pid()}",
            "speech-on=${ttsOn.value}",
            "speech-engine=${ttsEngine.value.code}",
            "coqui-voice=${ttsEngineCoquiVoice.value}",
            "coqui-cuda-device=${ttsEngineCoquiCudaDevice.value}",
            "speech-server=${ttsEngineHttpUrl.value}",
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
            "stt-engine=${sttEngine.value.code}",
            "stt-whisper-model=${sttWhisperModel.value}",
            "stt-whisper-device=${sttWhisperDevice.value}",
            "stt-nemo-model=${sttNemoModel.value}",
            "stt-nemo-device=${sttNemoDevice.value}",
            "stt-http-url=${sttHttpUrl.value}",
            "http-url=${httpUrl.value.net { it.substringAfterLast("/") }}",
            "use-python-commands=${usePythonCommands.value}",
         )
         val command = EnvironmentContext.runAsProgramArgsTransformer(commandRaw)
         val process = ProcessBuilder(command)
            .directory(dir)
            .redirectOutput(PIPE).redirectError(PIPE)
            .start()

         var stdout = ""
         var stderr = ""
         val stdoutListener = process.inputStream.consume("SpeechRecognition-stdout") {

            val p = object {
               var state = ""
               var str = StringBuilder("")
               fun String.onS(onS: (String, String?) -> Unit) = if (isNotEmpty()) onS(this, state) else Unit
               fun StringBuilder.determineState(): String {
                  return when {
                     startsWith("RAW: ") -> "RAW"
                     startsWith("USER: ") -> "USER"
                     startsWith("SYS: ") -> "SYS"
                     startsWith("COM: ") -> "COM"
                     startsWith("COM-DET: ") -> "COM-DET"
                     startsWith("ERR: ") -> "ERR"
                     else -> ""
                  }
               }
               fun process(t: String, onS: (String, String?) -> Unit, onE: (String, String) -> Unit) {
                  var s = t.replace("\r\n", "\n")
                  if ("\n" in s) {
                     s.split("\n").dropLast(1).forEach { processSingle(it.un(), onS, onE) }
                     str.clear()
                     str.append(s.substringAfterLast("\n").un())
                     s.substringAfterLast("\n").un().onS(onS)
                  } else {
                     val strOld = str.toString()
                     str.clear()
                     str.append(strOld + s.un())
                     state = str.determineState()
                     s.un().onS(onS)
                  }
               }

               fun processSingle(s: String, onS: (String, String?) -> Unit, onE: (String, String) -> Unit) {
                  str.append(s)
                  state = str.determineState()
                  if (state.isNotEmpty()) onE(str.toString().substringAfter(": "), state)
                  str.clear()
                  (s + "\n").onS(onS)
               }
            }

            stdout = it
               .map { it.ansi() }
               .filter { it.isNotEmpty() }
               .onEach { runFX {
                  p.process(
                     it,
                     { e, state ->
                        pythonOutStd.value = pythonOutStd.value.concatApplyBackspace(e)
                        if (state!=null && state!="") pythonOutEvent.value = pythonOutEvent.value.concatApplyBackspace(e)
                        if (state=="USER" || state=="SYS") pythonOutSpeak.value = pythonOutSpeak.value.concatApplyBackspace(e)
                        onLocalInput(e to state)
                     },
                     { e, state ->
                        handleInput(e, state)
                     },
                  )
               } }
               .lines()
               .map { it.applyBackspace() }
               .joinToString("")
         }
         val stderrListener = process.errorStream.consume("SpeechRecognition-stderr") {
            stderr = it
               .filter { it.isNotEmpty() }
               .map { it.ansi() }
               .map { it.applyBackspace() }
               .filter { it.isNotBlank() }
               .onEach { runFX {
                  pythonOutStd.value = pythonOutStd.value + it
                  onLocalInput(it to null)
               } }
               .joinToString("")
         }

         // run
         runOn(NEW("SpeechRecognition")) {
            val success = process.waitFor()
            stdoutListener.block()
            stderrListener.block()
            if (success!=0) doOnError("Python process failed and returned $success", null, stdout + stderr)
            process
         }.onError {
            doOnError("Starting python failed.", it, "")
         }

         process
      }.onError {
         doOnError("Starting python failed.", it, "")
      }
   }

   var commandWidgetNames = mapOf<String, String>()
      private set

   private val commandWidgetNamesRaw by cv("")
      .def(
         name = "Commands > widget names",
         info = "Alternative names for widgets for voice control, either for customization or easier recognition. " +
            "Comma separated names, widget per line, i.e.: `My widget = name1, name2, ..., nameN`"
      ).multiline(10) sync {
         commandWidgetNames = it.lines()
            .filter { it.isNotBlank() }
            .map { runTry { it.split2("=").net { (w, names) -> names.splitTrimmed(",").map { it.trim().lowercase() to w.trim().camelToSpaceCase() } } }.orNull() }
            .filterNotNull()
            .flatMap { it }
            .toMap()
      }

   /** Speech handlers called when user has spoken. Matched in order. */
   val handlers = observableList(
         *listOfNotNull(
            SpeakHandler(                            "Help", "help")                                         { if (matches(it)) Ok("List commands by saying, list commands") else null },
            SpeakHandler(                      "Do nothing", "ignore")                                       { if (matches(it)) Ok(null) else null },
            SpeakHandler(               "Restart Assistant", "restart assistant|yourself")                   { if (matches(it)) { speak("Ok"); restart(); Ok(null) } else null },
            SpeakHandler(                   "Help Commands", "list commands")                                { if (matches(it)) Ok(handlersHelpText()) else null },
            SpeakHandler(              "Start conversation", "start conversation")                           { if (matches(it)) { llmOn = true; Ok(null) } else null },
            SpeakHandler(            "Restart conversation", "restart conversation")                         { if (matches(it)) { llmOn = true; Ok(null) } else null },
            SpeakHandler(               "Stop conversation", "stop conversation")                            { if (matches(it)) { llmOn = false; Ok(null) } else null },
            SpeakHandler(                    "Current time", "what time is it")                              { voiceCommandCurrentTime(it) },
            SpeakHandler(                    "Current date", "what date is it")                              { voiceCommandCurrentDate(it) },
            SpeakHandler(                    "Current song", "what song|playback is active")                 { voiceCommandCurrentSong(it) },
            SpeakHandler(                 "Resume playback", "play|start|resume|continue music|playback")    { if (matches(it)) { APP.audio.resume(); Ok(null) } else null },
            SpeakHandler(                  "Pause playback", "stop|end|pause music|playback")                { if (matches(it)) { APP.audio.pause(); Ok(null) } else null },
            SpeakHandler(              "Play previous song", "play previous song")                           { if (matches(it)) { APP.audio.playlists.playPreviousItem(); Ok(null) } else null },
            SpeakHandler(                  "Play next song", "play next song")                               { if (matches(it)) { APP.audio.playlists.playNextItem(); Ok(null) } else null },
            SpeakHandler(       "Llm answer from clipboard", "generate|answer|write from? clipboard \$text") { voiceCommandGenerateClipboard(it) },
            SpeakHandler(                      "Llm answer", "generate|answer|write \$text")                 { voiceCommandGenerate(it) },
            SpeakHandler(            "Speak from clipboard", "speak|say from? clipboard")                    { voiceCommandSpeakClipboard(it) },
            SpeakHandler(              "Describe clipboard", "describe|define clipboard")                    { voiceCommandDescribeClipboard(it) },
            SpeakHandler(                   "Describe text", "describe|define \$text")                       { voiceCommandDescribeText(it) },
            SpeakHandler(                           "Speak", "speak|say \$text")                             { voiceCommandSpeakText(it) },
            SpeakHandler("Close window (${keys("ALT+F4")})", "close|hide window")                            { voiceCommandAltF4(it) },
            SpeakHandler(                     "Search text", "search for? \$text")                           { voiceSearch(it) },
            SpeakHandler(                       "Type text", "type \$text")                                  { voiceType(it) },
            SpeakHandler(             "Open widget by name", "open|show widget? \$widget_name widget?")      { voiceCommandOpenWidget(it) },
            SpeakHandler(                     "Shutdown OS", "shut down system|pc|computer|os")              { voiceCommandOsShutdown(it) }.takeIf { WINDOWS.isCurrent },
            SpeakHandler(                      "Restart OS", "restart system|pc|computer|os")                { voiceCommandOsRestart(it) }.takeIf { WINDOWS.isCurrent },
            SpeakHandler(                    "Hibernate OS", "hibernate system|pc|computer|os")              { voiceCommandOsHibernate(it) }.takeIf { WINDOWS.isCurrent },
            SpeakHandler(                        "Sleep OS", "sleep system|pc|computer|os")                  { voiceCommandOsSleep(it) }.takeIf { WINDOWS.isCurrent },
            SpeakHandler(                         "Lock OS", "lock system|pc|computer|os")                   { voiceCommandOsLock(it) }.takeIf { WINDOWS.isCurrent },
            SpeakHandler(                      "Log off OS", "log off system|pc|computer|os")                { voiceCommandOsLogOff(it) }.takeIf { WINDOWS.isCurrent },
            SpeakHandler(                    "Set reminder", "set reminder in|at \$time \$text")             { voiceCommandSetReminder(it) },
            SpeakHandler(                            "Wait", "wait \$time")                                  { voiceCommandWait(it) },
            SpeakHandler(                     "Count to...", "count from \$from to \$to")                    { voiceCommandCountTo(it) },
         ).toTypedArray()
      )

   /** [handlers] help text */
   private val handlersConf by cv("") {
         val t = v(it)
         handlers.onChangeAndNow { t.value = handlers.joinToString("\n") { "${it.name} -> ${it.commandUi}" } }
         t
      }
      .noPersist().readOnly().multiline(20)
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

   /** Whether microphone listening is allowed. */
   val micEnabled by cv(true)
      .def(name = "Microphone enabled", info = "Whether microphone listening is allowed. In general, this also prevents initial loading of speech-to-text AI model until enabled.")

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

   val micVoiceDetect by cv(false)
      .def(
         name = "Microphone > voice detect",
         info = "Microphone voice detection. If true, detects voice from verified voices and ignores others. Verified voices must be 16000Hz wav in `voices-verified` directory. Use `Microphone > voice detect treshold` to set up sensitivity."
      )

   val micVoiceDetectProb by cv(0.6).between(0.0, 1.0)
      .def(
         name = "Microphone > voice detect treshold",
         info = "Microphone voice detection treshold. Anything above this value is considered matched voice. Use `Microphone > voice detect treshold > debug` to determine optimal value."
      )

   val micVoiceDetectProbDebug by cv(false)
      .def(
         name = "Microphone > voice detect treshold > debug",
         info = "Optional bool whether microphone should be printing real-time `Microphone > voice detect treshold`. Use only to determine optimal `Microphone > voice detect treshold`."
      )

   /** Console output - all */
   val pythonOutStd = v<String>("")

   /** Console output - app events only */
   val pythonOutEvent = v<String>("")

   /** Console output - speaking only */
   val pythonOutSpeak = v<String>("")

   /** Opens console output */
   val pythonStdOutOpen by cr { APP.widgetManager.widgets.find(speechRecognitionWidgetFactory, ANY) }
      .def(name = "Output console", info = "Shows console output of the python process")

   /** Invoked for every voice assistant local process input token. */
   val onLocalInput = Handler1<Pair<String, String?>>()

   /** Words or phrases that will be removed from text representing the detected speech. Makes command matching more powerful. Case-insensitive. */
   val wakeUpWord by cv("system")
      .def(name = "Wake up word", info = "Optional wake words or phrases (separated by ',') that activate voice recognition. Case-insensitive.\nThe first wake word will be used as name for the system")

   /** Engine used to recognize speech. May require additional configuration */
   val sttEngine by cv(TtsEngine.WHISPER).uiNoOrder()
      .def(name = "Speech recognition", info = "Engine used to recognize speech. May require additional configuration")

   /** [TtsEngine.WHISPER] AI model used to transcribe voice to text */
   val sttWhisperModel by cv("base.en")
      .values { listOf("tiny.en", "tiny", "base.en", "base", "small.en", "small", "medium.en", "medium", "large", "large-v1", "large-v2", "large-v3") }
      .uiNoOrder()
      .def(name = "Speech recognition > Whisper model", info = "Whisper model for speech recognition.")

   /** [TtsEngine.WHISPER] torch device used to transcribe voice to text */
   val sttWhisperDevice by cv("")
      .def(name = "Speech recognition > Whisper device", info = "Whisper torch device for speech recognition. E.g. cpu, cuda:0, cuda:1. Default empty, which attempts to use cuda if available.")

   /** [TtsEngine.NEMO] AI model used to transcribe voice to text */
   val sttNemoModel by cv("nvidia/parakeet-ctc-1.1b")
      .values { listOf("nvidia/parakeet-tdt-1.1b", "nvidia/parakeet-ctc-1.1b", "nvidia/parakeet-ctc-0.6b") }
      .uiNoOrder()
      .def(name = "Speech recognition > Nemo model", info = "Nemo model for speech recognition.")

   /** [TtsEngine.NEMO] torch device used to transcribe voice to text */
   val sttNemoDevice by cv("")
      .def(name = "Speech recognition > Nemo device", info = "Nemo torch device for speech recognition. E.g. cpu, cuda:0, cuda:1. Default empty, which attempts to use cuda if available.")

   /** [TtsEngine.HTTP] torch device used to transcribe voice to text */
   val sttHttpUrl by cv("localhost:1235")
      .def(name = "Speech recognition > Http url", info = "Voice recognition server address and port.")

   /** Words or phrases that will be removed from text representing the detected speech. Makes command matching more powerful. Case-insensitive. */
   val sttBlacklistWords by cList("a", "the", "please")
      .def(
         name = "Blacklisted words",
         info = "Words or phrases that will be removed from text representing the detected speech. Makes command matching more powerful. Case-insensitive."
      )

   /** [sttBlacklistWords] in performance-optimal format */
   private val sttBlacklistWords_ = mutableSetOf<String>().apply {
      sttBlacklistWords.onChangeAndNow { this setTo sttBlacklistWords.map { it.lowercase() } }
   }

   /** Whether speech is allowed. */
   val ttsOn by cv(true)
      .def(name = "Speech enabled", info = "Whether speech is allowed. In general, this also prevents initial loading of speech AI model until enabled.")

   /** Engine used to generate voice. May require additional configuration */
   val ttsEngine by cv(SpeechEngine.SYSTEM).uiNoOrder()
      .def(name = "Speech engine", info = "Engine used to generate voice. May require additional configuration")

   /** Access token for character.ai account used when speech engine is Character.ai */
   val ttsEngineCoquiVoice by cv("Ann_Daniels.flac")
      .valuesUnsealed { (dir / "voices-coqui").children().filter { it.isAudio() }.map { it.name }.toList() }
      .def(
         name = "Speech engine > coqui > voice",
         info = "" +
            "Voice when using ${SpeechEngine.COQUI.nameUi} speech engine. " +
            "Sample file of the voice to be used. User can add more audio samples to ${(dir / "voices-coqui").absolutePath}. " +
            "Should be 3-10s long."
      )

   /** [SpeechEngine.COQUI] torch device used to transcribe voice to text */
   val ttsEngineCoquiCudaDevice by cv("")
      .def(
         name = "Speech engine > coqui > device",
         info = "Torch device for speech generation when using ${SpeechEngine.COQUI.nameUi} speech engine."
      )

   /** Speech server address and port to connect to. */
   val ttsEngineHttpUrl by cv("localhost:1236")
      .def(
         name = "Speech engine > http > url",
         info = "Speech server address and port to connect to when using ${SpeechEngine.HTTP.nameUi} speech engine."
      )

   /** Whether [llmEngine] conversation is active */
   var llmOn by atomic(false)
      private set

   /** Engine used to generate voice. May require additional configuration */
   val llmEngine by cv(LlmEngine.NONE).uiNoOrder()
      .def(name = "Llm engine", info = "LLM engine for chat")

   /** Model for gpt4all. Must be in models-gpt4all. */
   val llmGpt4AllModel by cv("none")
      .valuesUnsealed { dir.div("models-gpt4all").children().map { it.name }.filter { it.endsWith("gguf") }.toList() + "none" }
      .def(name = "Llm engine > gpt4all > model", info = "Model for gpt4all. Must be in ${(dir / "models-gpt4all").absolutePath}")

   /** Url of the OpenAI or OpenAI-compatible server */
   val llmOpenAiUrl by cv("http://localhost:1234/v1")
      .def(name = "Llm engine > openai > url", info = "Url of the OpenAI or OpenAI-compatible server")

   /** The user authorization of the OpenAI or OpenAI-compatible server */
   val llmOpenAiBearer by cv("ABC123xyz789").password()
      .def(name = "Llm engine > openai > bearer", info = "The user authorization of the OpenAI or OpenAI-compatible server. Server may ignore this.")

   /** The llm model of the OpenAI or OpenAI-compatible server */
   val llmOpenAiModel by cv("").password()
      .def(name = "Llm engine > openai > model", info = "The llm model of the OpenAI or OpenAI-compatible server. Server may ignore this.")

   /** System prompt telling llm to assume role, or exhibit behavior */
   val llmChatSysPrompt by cv("You are helpful voice assistant. You are voiced by tts, be extremly short.").multiline(5).nonBlank()
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

   /** Url of the http API of the AI executor */
   val httpUrl by cv("http://localhost:1236")
      .def(
         name = "Http url",
         info = "Url of the http API of the locally running AI executor"
      )

   /** Experimental flag to allow llm to repond with python commands. Much more powerful, but also somewhat unpredictable. Default false. */
   val usePythonCommands by cv(false)
      .def(
         name = "Use python commands",
         info = "Experimental flag to allow llm to repond with python commands. Much more powerful, but also somewhat unpredictable. Default false."
      )

   private var isRunning = false

   override fun start() {
      // runtime-changeable properties
      val p = 2.seconds
      // @formatter:off
               wakeUpWord.chan().throttleToLast(p) subscribe { write("wake-word=$it") }
               micEnabled.chan().throttleToLast(p) subscribe { write("mic-enabled=$it") }
                micEnergy.chan().throttleToLast(p) subscribe { write("mic-energy=$it") }
           micEnergyDebug.chan().throttleToLast(p) subscribe { write("mic-energy-debug=$it") }
       micVoiceDetectProb.chan().throttleToLast(p) subscribe { write("mic-voice-detect-prop=$it") }
  micVoiceDetectProbDebug.chan().throttleToLast(p) subscribe { write("mic-voice-detect-debug=$it") }
                    ttsOn.chan().throttleToLast(p) subscribe { write("speech-on=$it") }
      ttsEngineCoquiVoice.chan().throttleToLast(p) subscribe { write("coqui-voice=$it") }
         llmChatSysPrompt.chan().throttleToLast(p) subscribe { write("llm-chat-sys-prompt=$it") }
         llmChatMaxTokens.chan().throttleToLast(p) subscribe { write("llm-chat-max-tokens=$it") }
              llmChatTemp.chan().throttleToLast(p) subscribe { write("llm-chat-temp=$it") }
              llmChatTopP.chan().throttleToLast(p) subscribe { write("llm-chat-topp=$it") }
              llmChatTopK.chan().throttleToLast(p) subscribe { write("llm-chat-topk=$it") }
        usePythonCommands.chan().throttleToLast(p) subscribe { write("use-python-commands=$it") }
      // @formatter:on

      startSpeechRecognition()

      // restart-requiring properties
      val processChangeVals = listOf<V<*>>(
         micName, micVoiceDetect,
         sttEngine, sttWhisperModel, sttWhisperDevice, sttNemoModel, sttNemoDevice, sttHttpUrl,
         ttsEngine, ttsEngineCoquiCudaDevice, ttsEngineHttpUrl,
         llmEngine, llmGpt4AllModel, llmOpenAiUrl, llmOpenAiBearer, llmOpenAiModel,
         httpUrl
      )
      val processChange = processChangeVals.map { it.chan() }.reduce { a, b -> a + b }
      processChange.throttleToLast(2.seconds).subscribe { restart() } on onClose

      // turn off during hibernate
      installHibernationTermination()

      isRunning = true
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

   private fun installHibernationTermination() {
      // the python process !recover from hibernating properly and the AI is very heavy to be used in hibernate anyway
      // the closing must prevent hibernate until ai termination is complete, see
      // the startup is delayed so system is ready, which avoids starup issues
      onClose += APP.actionStream.onEventObject(SystemSleepEvent.Start) { stopSpeechRecognition() }
      onClose += APP.actionStream.onEventObject(SystemSleepEvent.Stop) { runFX(5.seconds) { startSpeechRecognition()} }
   }

   private val confirmers = mutableListOf<SpeakConfirmer>()

   private suspend fun confirm(text: String) {
      if (!isRunning) return
      val h = confirmers.removeLastOrNull()
      if (h != null && h.regex.matches(text)) h.action(text).getAny().ifNotNull(::speak)
   }

   private fun handleInput(text: String, state: String) {
      when (state) {
         "" -> Unit
         "ERR" -> Unit
         "RAW" -> launch(FX) { confirm(text) }
         "USER" -> launch(FX) { handleSpeech(text, user = true) }
         "SYS" -> Unit
         "COM" -> launch(FX) { handleSpeech(text, command = true, orDetectIntent = true) }
         "COM-DET" -> launch(FX) { handleSpeech(text, command = true, orDetectIntent = false) }
      }
   }

   private suspend fun handleSpeech(text: String, user: Boolean = false, command: Boolean = false, orDetectIntent: Boolean = false) {
      if (!isRunning) return
      if (user) speakingTextW.value = text
      if (!command) return

      var textSanitized = text.removePrefix("COM ").removeSuffix(" COM").replace("_", " ").sanitize(sttBlacklistWords_)
      var result = handlers.firstNotNullOfOrNull { SpeakContext(it, this@VoiceAssistant)(textSanitized) }
      if (result==null) {
         if (!orDetectIntent) speak("Unrecognized command: $textSanitized")
         else {
            intent("", textSanitized).ifError {
               logger.error(it) { "Failed to understand command $textSanitized" }
               speak("Recognized command failed with error: ${it.message ?: ""}")
            }.ifOk {
               handleSpeech(it, command = true, orDetectIntent = false)
            }
         }
      } else
         result.getAny().ifNotNull(::speak)
   }

   private val writing = ActorVt<Pair<Fut<Process>?, String>>("SpeechRecognition-writer") { (setup, it) ->
      try {
         setup?.blockAndGetOrThrow()?.outputStream?.apply { write("$it\n".toByteArray()); flush() }
      } catch (e: IOException) {
         if (e.message=="The pipe is being closed") doNothing("Stream may be closed mid write")
         else throw e
      }
   }

   suspend fun intent(functions: String?, userPrompt: String) =
      VT.invokeTry {
         val url = httpUrl.value.net { "$it/intent"}
         APP.http.client.post(url) { bodyJs("functions" to (functions ?: ""), "userPrompt" to userPrompt) }.bodyAsText()
      }

   fun write(text: String): Unit = writing(setup to text)
   
   fun writeCom(command: String): Unit = writing(setup to "COM: ${command.encodeBase64()}")

   fun writeComPyt(command: String): Unit = writing(setup to "COM-PYT: ${command.encodeBase64()}")

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

   @Throws(Throwable::class)
   suspend fun speakEvent(eventToReactTo: LlmString, fallback: String): Unit =
      VT {
         APP.http.client.post("${httpUrl.value}/tts-event") { bodyJs("event_to_react_to" to eventToReactTo, "fallback" to fallback) }
      }

   @Throws(Throwable::class)
   suspend fun state(actorType: String, eventType: String): JsValue =
      VT {
         APP.http.client.get("${httpUrl.value}/actor-events?actor=${actorType}&type=${eventType}").bodyAsJs()
      }

   enum class TtsEngine(val code: String, override val nameUi: String, override val infoUi: String): NameUi, InfoUi {
      NONE("none", "None", "No speech recognition"),
      WHISPER("whisper", "Whisper", "OpenAI Whisper speech recognition. Fully offline."),
      NEMO("nemo", "Nemo", "Nvidia Nemo ASR. Fully offline."),
      HTTP("http", "Http", "Recognition using different instance of this application with http enabled."),
   }

   enum class SpeechEngine(val code: String, override val nameUi: String, override val infoUi: String): NameUi, InfoUi {
      NONE("none", "None", "No voice"),
      SYSTEM("os", "System", "System voice. Fully offline"),
      COQUI("coqui", "Coqui", "Voice using huggingface.co/coqui/XTTS-v2 model. Fully offline"),
      TACOTRON2("tacotron2", "Tacotron2", "Voice using pytorch.org/hub/nvidia_deeplearningexamples_tacotron2 model. Fully offline"),
      SPEECHBRAIN("speechbrain", "Speechbrain", "Voice using speechbrain/tts-tacotron2-ljspeech + speechbrain/tts-hifigan-ljspeech model. Fully offline"),
      FASTPITCH("fastpitch", "Fastpitch", "Voice using Nvidia's fastpitch. Fully offline"),
      HTTP("http", "Http server", "Voice using different instance of this application with speech server enabled"),
   }

   enum class LlmEngine(val code: String, override val nameUi: String, override val infoUi: String): NameUi, InfoUi {
      NONE("none", "None", "No chat"),
      GPT4ALL("gpt4all", "Gpt4All", "Gpt4All python bindings (requires downloading and specifying model)"),
      OPENAI("openai", "OpenAi", "OpenAI http client (requires access, but custom local server is also possible, e.g. LmStudio)"),
   }

   /** [SpeakHandler] action context. */
   data class SpeakContext(val handler: SpeakHandler, val plugin: VoiceAssistant, val intent: Boolean = true) {

      /** @return result of handler action, i.e., `handler.regex.matches(text)` */
      suspend operator fun invoke(text: String): Try<String?, String?>? = invoke(text, handler.action)

      /** @return result of action, i.e., `handler.regex.matches(text)` */
      suspend operator fun invoke(text: String, action: suspend SpeakContext.(String) -> Try<String?, String?>?): Try<String?, String?>? = action(this, text)

      /** @return copy with [intent], i.e., `copy(intent = false)` */
      fun withIntent(): SpeakContext = copy(intent = false)

      /** @return whether handler matches specified text (with non-empty parameter values), i.e., `handler.regex.matches(text)` */
      fun matches(text: String): Boolean = handler.regex.matches(text) && args(text).none { it.isBlank() }

      /** @return parameter values or fails if not [matches] */
      fun args(text: String) = DestructuredList(handler.regex.matchEntire(text)!!.groupValues.drop(1).map { it.replace("_", " ") })

      /**
       * @param confirmText text to be spoken to user to request feedback
       * @param commandUi matcher
       * @param action action that runs on FX thread, takes intent detection result an returns voice feedback
       * @return `Ok(null)` ([confirmText] is always spoken)
       */
      public suspend fun confirming(confirmText: String, commandUi: String, action: suspend (String) -> Try<String?, String?>): Ok<Nothing?> =
         FX {
            plugin.confirmers += SpeakConfirmer(commandUi, action)
            plugin.speak(confirmText)
            Ok(null)
         }

      /**
       * @param action action that runs on FX thread, takes intent detection result, returns Try (with text to speak or null if none) or null if no match.
       * @return `Ok(null)`
       */
      public suspend fun intent(text: String, functions: String, userPrompt: String, action: suspend SpeakContext.(String) -> Try<String?, String?>?): Try<String?, String?>? =
         VT {
            runTry {
               val url = plugin.httpUrl.value.net { "$it/intent"}
               val command = APP.http.client.post(url) { bodyJs(JsObject(mapOf("functions" to JsString(functions), "userPrompt" to JsString(userPrompt)))) }.bodyAsText()
               FX {
                  withIntent()(command, action)
               }
            }.getOrSupply {
               logger.error(it) { "Failed to understand command $text" }
               Error("Failed to understand command $text")
            }
         }
   }

   /**
    * Speech event handler. In ui shown as `"$name -> $commandUi"`.
    * @param action action that runs on FX thread, takes command, returns Try (with text to speak or null if none) or null if no match. */
   data class SpeakHandler(val name: String, val commandUi: String, val action: suspend SpeakContext.(String) -> Try<String?, String?>?) {
      /** [commandUi] turned into regex */
      val regex by lazy { voiceCommandRegex(commandUi) }
   }

   /**
    * Speech event confirmer. In ui shown as `"$name -> $commandUi"`
    * @param action action that runs on FX thread, takes original command, returns Try (with text to speak or null if none). */
   private data class SpeakConfirmer(val commandUi: String, val action: suspend (String) -> Try<String?, String?>) {
      /** [commandUi] turned into regex */
      val regex = voiceCommandRegex(commandUi)
   }

   companion object: PluginInfo, KLogging() {
      override val name = "Voice Assistant"
      override val description = "Provides speech recognition, synthesis, LLM chat and voice control capabilities using various AI models."
      override val isSupported = true
      override val isSingleton = true
      override val isEnabledByDefault = false

      val speechRecognitionWidgetFactory by lazy { WidgetFactory("VoiceAssistant", VoiceAssistantWidget::class, APP.location.widgets/"VoiceAssistant") }

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
      internal fun String.sanitize(blacklistWordsSet: Set<String>): String {
         var s = " " + trim().lowercase() + " "
         blacklistWordsSet.forEach { it -> s = s.replace(" " + it + " ", "") }
         s = s.replace("  ", " ").trim()
         return s
      }
   }

}

@JvmInline
value class LlmString(val value: String)