package sp.it.pl.plugin.impl

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.ProcessBuilder.Redirect.PIPE
import javafx.beans.value.ObservableValue
import kotlinx.coroutines.invoke
import mu.KLogging
import sp.it.pl.audio.audioInputDeviceNames
import sp.it.pl.audio.audioOutputDeviceNames
import sp.it.pl.core.InfoUi
import sp.it.pl.core.NameUi
import sp.it.pl.core.bodyAsJs
import sp.it.pl.core.bodyJs
import sp.it.pl.core.orMessage
import sp.it.pl.layout.WidgetFactory
import sp.it.pl.layout.WidgetUse.ANY
import sp.it.pl.main.APP
import sp.it.pl.main.Bool
import sp.it.pl.main.Events.AppEvent.SystemSleepEvent
import sp.it.pl.main.IconMA
import sp.it.pl.main.isAudio
import sp.it.pl.main.runCommandWithOutput
import sp.it.pl.main.toUi
import sp.it.pl.main.withAppProgress
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.pl.ui.pane.ActionData.Threading.BLOCK
import sp.it.pl.ui.pane.action
import sp.it.util.access.V
import sp.it.util.access.readOnly
import sp.it.util.access.v
import sp.it.util.access.valueTry
import sp.it.util.access.vn
import sp.it.util.action.IsAction
import sp.it.util.async.NEW
import sp.it.util.async.actor.ActorVt
import sp.it.util.async.coroutine.FX
import sp.it.util.async.coroutine.VT
import sp.it.util.async.coroutine.invokeTry
import sp.it.util.async.coroutine.launch
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.future.Fut
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.async.future.awaitFx
import sp.it.util.async.future.awaitFxOrBlock
import sp.it.util.async.runFX
import sp.it.util.async.runOn
import sp.it.util.collections.list.DestructuredList
import sp.it.util.collections.observableList
import sp.it.util.collections.setTo
import sp.it.util.conf.ConfV
import sp.it.util.conf.ConfVRO
import sp.it.util.conf.Configurable
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.Constraint.Multiline
import sp.it.util.conf.Constraint.MultilineRows
import sp.it.util.conf.Constraint.RepeatableAction
import sp.it.util.conf.EditMode
import sp.it.util.conf.ListConfigurable
import sp.it.util.conf.between
import sp.it.util.conf.cList
import sp.it.util.conf.cNest
import sp.it.util.conf.cr
import sp.it.util.conf.cv
import sp.it.util.conf.cvNest
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.getDelegateConfig
import sp.it.util.conf.min
import sp.it.util.conf.multiline
import sp.it.util.conf.noPersist
import sp.it.util.conf.noUi
import sp.it.util.conf.nonBlank
import sp.it.util.conf.nonEmpty
import sp.it.util.conf.nonNull
import sp.it.util.conf.password
import sp.it.util.conf.readOnly
import sp.it.util.conf.uiConverter
import sp.it.util.conf.uiGeneral
import sp.it.util.conf.uiNoCustomUnsealedValue
import sp.it.util.conf.uiNoOrder
import sp.it.util.conf.uiPaginated
import sp.it.util.conf.values
import sp.it.util.conf.valuesUnsealed
import sp.it.util.dev.doNothing
import sp.it.util.dev.markUsed
import sp.it.util.dev.printIt
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.file.hasExtension
import sp.it.util.file.json.JsBool
import sp.it.util.file.json.JsNumber
import sp.it.util.file.json.JsObject
import sp.it.util.file.json.JsString
import sp.it.util.file.json.JsValue
import sp.it.util.file.json.toCompactS
import sp.it.util.file.readTextTry
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
import sp.it.util.reactive.attach
import sp.it.util.reactive.chan
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.reactive.onItemAdded
import sp.it.util.reactive.plus
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.throttleToLast
import sp.it.util.system.EnvironmentContext
import sp.it.util.text.appendSent
import sp.it.util.text.applyBackspace
import sp.it.util.text.camelToSpaceCase
import sp.it.util.text.concatApplyBackspace
import sp.it.util.text.encodeBase64
import sp.it.util.text.lines
import sp.it.util.text.split2
import sp.it.util.text.splitNoEmpty
import sp.it.util.text.splitTrimmed
import sp.it.util.text.useStrings
import sp.it.util.units.seconds
import sp.it.util.units.uuid

/** Provides speech recognition and voice control capabilities. Uses whisper AI launched as python program. */
class VoiceAssistant: PluginBase() {
   private val onClose = Disposer()
   private var setup: Fut<Process>? = null
   private val isProgressWritable = v(false)

   private fun setup(runLlmServerCommand: String?): Fut<Process> {

      fun doOnError(eText: String, e: Throwable?, details: String?) = logger.error(e) { "$eText\n${details.wrap()}" }.toUnit()
      return runOn(NEW("SpeechRecognition-python-starter")) {

         llmOpenAiServerStart(runLlmServerCommand)

         val python = dir / "main.py"
         fun audioIns(): String =
            if (mics.isEmpty()) ""
            else JsObject(
               mics.map {
                  it.name.value.orEmpty() to JsObject(
                     "location" to JsString(it.location.value),
                     "energy" to JsNumber(it.energy.value),
                     "verbose" to JsBool(it.verbose.value),
                  )
               }
            ).toCompactS()
         fun audioOuts(): String =
            if (audioOuts.isEmpty()) ""
            else JsObject(
               audioOuts.associate { it.name.value.orEmpty() to JsString(it.location.value) }
            ).toCompactS()

         val commandRaw = listOf(
            "python", python.absolutePath,
            "wake-word=${wakeUpWord.value}",
            "main-speaker=${mainSpeaker.value}",
            "main-location=${mainLocation.value}",
            "mics=${audioIns().replace("\"", "\\\"")}",
            "mic-enabled=${micEnabled.value}",
            "mic-voice-detect=${micVoiceDetect.value}",
            "mic-voice-detect-device=${micVoiceDetectDevice.value}",
            "mic-voice-detect-prop=${micVoiceDetectProb.value}",
            "mic-voice-detect-debug=${micVoiceDetectProbDebug.value}",
            "audio-out=${audioOuts().replace("\"", "\\\"")}",
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
            "llm-chat-sys-prompt=${llmChatSysPrompt.value.replace('\n', ' ')}",
            "llm-chat-max-tokens=${llmChatMaxTokens.value}",
            "llm-chat-temp=${llmChatTemp.value}",
            "llm-chat-topp=${llmChatTopP.value}",
            "llm-chat-topk=${llmChatTopK.value}",
            "stt-engine=${sttEngine.value.code}",
            "stt-whisper-model=${sttWhisperModel.value}",
            "stt-whisper-device=${sttWhisperDevice.value}",
            "stt-fasterwhisper-model=${sttFasterWhisperModel.value}",
            "stt-fasterwhisper-device=${sttFasterWhisperDevice.value}",
            "stt-whispers2t-model=${sttWhisperSt2Model.value}",
            "stt-whispers2t-device=${sttWhisperSt2Device.value}",
            "stt-nemo-model=${sttNemoModel.value}",
            "stt-nemo-device=${sttNemoDevice.value}",
            "stt-http-url=${sttHttpUrl.value}",
            "http-url=${httpUrl.value.net { it.substringAfterLast("/") }}",
            "use-python-commands=${false}",
         )
         val command = EnvironmentContext.runAsProgramArgsTransformer(commandRaw)
         val process = ProcessBuilder(command)
            .directory(dir)
            .redirectOutput(PIPE).redirectError(PIPE)
            .start()

         var stdout = ""
         var stderr = ""
         val stdoutListener = process.inputStream.consume("SpeechRecognition-stdout") {
            val reader = VoiceAssistentCliReader(isProgressWritable)
            // capture stdout
            stdout = it
               .map { it.noAnsiProgress().ansi() }
               .filter { it.isNotEmpty() }
               .onEach { runFX {
                  reader.process(
                     it,
                     { e, state ->
                        pythonOutStd.value = pythonOutStd.value.concatApplyBackspace(e)
                        if (state!=null && state!="") pythonOutEvent.value = pythonOutEvent.value.concatApplyBackspace(e)
                        if (state=="USER" || state=="SYS") pythonOutSpeak.value = pythonOutSpeak.value.concatApplyBackspace(e)
                        onLocalInputReducer.push(e to state)
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
            // capture stderr
            stderr = it
               .filter { it.isNotEmpty() }
               .map { it.ansi() }
               .map { it.applyBackspace() }
               .filter { it.isNotBlank() }
               .onEach { runFX {
                  pythonOutStd.value = pythonOutStd.value + it
                  onLocalInputReducer.push(it to null)
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

   /** Whether voice assistent is busy with computation computing. */
   val isProgress = isProgressWritable.readOnly()

   /**
    * Alternative names for widgets for voice control, either for customization or easier recognition.
    * Comma separated names, widget per line, i.e.: `My widget = name1, name2, ..., nameN`
    */
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

   /** Allow llm to repond with python commands. Much more powerful assistant, but also somewhat unpredictable. Default false. */
   val usePythonCommands by cv(false)
      .def(
         name = "Use python commands",
         info = "Allow llm to repond with python commands. Much more powerful assistant, but also somewhat unpredictable. Default false."
      )

   /** Speech handlers called when user has spoken. Matched in order. */
   val handlers = observableList(*voiceCommands().toTypedArray())

   /** [handlers] help text */
   private val handlersConf by cv("") {
         val t = v(it)
         handlers.onChangeAndNow {
            t.value = handlers.joinToString("\n") {
               "${it.name} -> ${it.commandUi}" + (if (it is SpeakHandlerGroup) "\n"+it.commands.joinToString("\n") { "   ${it.name} -> ${it.commandUi}" } else "")
            }
         }
         t
      }
      .noPersist().readOnly().multiline(20)
      .def(
         name = "Commands",
         info = "Shows active voice speech recognition commands.\n Matched in order.\n '?' means optional word. '|' means alternative word."
      )

   /** [handlers] help text */
   internal fun handlersHelpText(): String =
      handlers
         .mapIndexed { i, h -> "\n$i. ${h.name}, activate by saying ${h.commandUi}" }
         .joinToString("", prefix = "Here is list of currently active commands:")

   /** Last spoken text - writable */
   private val speakingTextW = vn<String>(null)

   /** Last spoken text */
   val speakingText = speakingTextW.readOnly()

   /** Speaker sent to assistant as context when none other available. Speaker is usually determined from name of the matched voice from verified voices. */
   val mainSpeaker by cv<String>(mainSpeakerInitial)
      .valuesUnsealed { obtainSpeakers() }
      .uiNoOrder()
      .def(name = "Main speaker", info = "Speaker sent to assistant as context when none other available. Speaker is usually determined from name of the matched voice from verified voices.")

   /** Location sent to assistant as context when none other available. Location is usually determined from microphone location. */
   val mainLocation by cv<String>(mainLocationInitial)
      .def(name = "Main location", info = "Location sent to assistant as context when none other available. Location is usually determined from microphone location.")

   /** Preferred song order for certain song operations, such as adding songs to playlist */
   val mics by cList<AudioIn>({ AudioIn() }, { it }, AudioIn()).uiPaginated(false).def(
      name = "Microphones",
      info = "Audio input devices configuration"
   )

   class AudioIn: ConfigurableBase<Any?>() {
      /** Microphone to be used. Null causes automatic microphone selection */
      val name by cvn<String>(null)
         .valuesUnsealed { audioInputDeviceNames() }
         .uiNoCustomUnsealedValue()
         .def(name = "Name", info = "Microphone to be used. Null causes automatic microphone selection.")
      /** Location sent to assistant as context. */
      val location by cv<String>(mainLocationInitial)
         .def(name = "Location", info = "Location sent to assistant as context.")
      /** Volume above this number is considered speech. Set so ambient energy level is below. */
      val energy by cv<Int>(120)
         .between(0, 32767).uiGeneral()
         .def(name = "Energy treshold", info = "Volume above this number is considered speech. Set so ambient energy level is below.")
      /** Current volume. Useful to tune energy treshold */
      val energyCurrent by cv<Try<Short, Throwable>>(Ok(0.toShort()))
         .uiConverter { it.map { it.toUi() }.orMessage().getAny() }
         .noPersist().def(editable = EditMode.APP)
         .def(name = "Energy current", info = "Current volume. Between 0..32767 Useful to tune energy treshold.")
      /** Microphone verbose event logging. */
      val verbose by cv(false)
         .def(name = "Verbose", info = "Verbose event logging.")
   }

   /** Whether microphone listening is allowed. */
   val micEnabled by cv(true)
      .def(name = "Microphone enabled", info = "Whether microphone listening is allowed. In general, this also prevents initial loading of speech-to-text AI model until enabled.")

   private val micChanges = v(uuid()).apply {
      mics.onChange { value = uuid() }
      mics.onItemAdded {
         it.name attach { value = uuid() }
         it.location attach { value = uuid() }
         it.energy attach { value = uuid() }
         it.verbose attach { value = uuid() }
      }
   }

   val micVoiceDetect by cv(false).noUi().def(
      name = "Enabled",
      info = "Microphone voice detection. If true, detects voice from verified voices and ignores others. Verified voices must be 16000Hz wav in `voices-verified` directory. Use `Microphone > voice detect treshold` to set up sensitivity."
   )

   val micVoiceDetectDevice by cv("cpu").noUi().def(
      name = "Device",
      info = "Microphone voice detection torch device, e.g., cpu, cuda:0, cuda:1. Default 'cpu'."
   )

   val micVoiceDetectProb by cv(0.6).between(0.0, 1.0).noUi().def(
      name = "Treshold",
      info = "Microphone voice detection treshold. Anything above this value is considered matched voice. Use `Microphone > voice detect treshold > debug` to determine optimal value."
   )

   val micVoiceDetectProbDebug by cv(false).noUi().def(
      name = "Debug",
      info = "Optional bool whether microphone should be printing real-time `Microphone > voice detect treshold`. Use only to determine optimal `Microphone > voice detect treshold`."
   )

   internal val micVoiceDetectDetails by cNest(
      ListConfigurable.heterogeneous(
         ::micVoiceDetect.getDelegateConfig(),
         ::micVoiceDetectDevice.getDelegateConfig(),
         ::micVoiceDetectProb.getDelegateConfig(),
         ::micVoiceDetectProbDebug.getDelegateConfig()
      )
   ).noPersist().def(
      name = "Microphone > speaker detection"
   )

   /** Preferred song order for certain song operations, such as adding songs to playlist */
   val audioOuts by cList<AudioOut>({ AudioOut() }, { it }, AudioOut()).uiPaginated(false).def(
      name = "Audio output devices",
      info = "Audio output devices configuration"
   )

   private val audioOutChanges = v(uuid()).apply {
      audioOuts.onChange { value = uuid() }
      audioOuts.onItemAdded {
         it.name attach { value = uuid() }
         it.location attach { value = uuid() }
      }
   }

   class AudioOut: ConfigurableBase<Any?>() {
      /** Speaker to be used. Null causes automatic speaker selection */
      val name by cvn<String>(null)
         .valuesUnsealed { audioOutputDeviceNames() }
         .uiNoCustomUnsealedValue()
         .def(name = "Name", info = "Speaker to be used. Null causes automatic speaker selection.")
      /** Location of the speaker. */
      val location by cv<String>(mainLocationInitial)
         .def(name = "Location", info = "Location of the speaker.")
   }

   /** Console output - all */
   val pythonOutStd = v<String>("")

   /** Console output - app events only */
   val pythonOutEvent = v<String>("")

   /** Console output - speaking only */
   val pythonOutSpeak = v<String>("")

   /** Opens console output */
   val pythonStdOutOpen by cr { APP.widgetManager.widgets.find(voiceAssistantWidgetFactory, ANY) }
      .def(name = "Output console", info = "Shows console output of the python process")

   private val onLocalInputReducer = EventReducer.toEvery<Pair<String, String?>>(30.0, { a, b ->
      // agregate subsequent events of same state
      if (a.second==b.second) (a.first.concatApplyBackspace(b.first)) to a.second
      // else emit last and starts new agregation
      else { onLocalInput(a); b }
   }) { onLocalInput(it) }

   /** Invoked for every voice assistant local process input token. */
   val onLocalInput = Handler1<Pair<String, String?>>()
   
   /** Words or phrases that will be removed from text representing the detected speech. Makes command matching more powerful. Case-insensitive. */
   val wakeUpWord by cv("system").nonBlank()
      .def(name = "Wake up word", info = "Optional wake words or phrases (separated by ',') that activate voice recognition. Case-insensitive.\nThe first wake word will be used as name for the system")

   /** [wakeUpWord]'s first, i.e., primary word. */
   val wakeUpWordPrimary: String get() =
      wakeUpWord.value.splitNoEmpty(",").firstOrNull() ?: "system"

   /** Engine used to recognize speech. May require additional configuration */
   val sttEngine by cv(SttEngine.WHISPER).uiNoOrder()
      .def(name = "Speech recognition", info = "Engine used to recognize speech. May require additional configuration")

   /** [SttEngine.WHISPER] AI model used to transcribe voice to text */
   val sttWhisperModel by cv("base.en")
      .values { listOf("tiny.en", "tiny", "base.en", "base", "small.en", "small", "medium.en", "medium", "large", "large-v1", "large-v2", "large-v3", "turbo") }
      .uiNoOrder()
      .noUi()
      .def(name = "Model", info = "Whisper model for speech recognition.")

   /** [SttEngine.WHISPER] torch device used to transcribe voice to text */
   val sttWhisperDevice by cv("")
      .noUi()
      .def(name = "Device", info = "Whisper torch device for speech recognition. E.g. cpu, cuda:0, cuda:1. Default empty, which falls back to 'cpu'.")

   /** [SttEngine.FASTER_WHISPER] AI model used to transcribe voice to text */
   val sttFasterWhisperModel by cv("small.en")
      .values { listOf("tiny.en", "tiny", "base.en", "base", "small.en", "small", "medium.en", "medium", "large", "large-v1", "large-v2", "large-v3", "distil-small.en", "distil-medium.en", "distil-large-v2", "distil-large-v3") }
      .uiNoOrder()
      .noUi()
      .def(name = "Model", info = "Whisper model for speech recognition. Distill models are faster versions of original models.")

   /** [SttEngine.FASTER_WHISPER] torch device used to transcribe voice to text */
   val sttFasterWhisperDevice by cv("")
      .noUi()
      .def(name = "Device", info = "Whisper torch device for speech recognition. E.g. cpu, cuda:0, cuda:1. Default empty, which falls back to 'cpu'.")

   /** [SttEngine.WHISPER_S2T] AI model used to transcribe voice to text */
   val sttWhisperSt2Model by cv("small.en")
      .values { listOf("tiny.en", "tiny", "base.en", "base", "small.en", "small", "medium.en", "medium", "large", "large-v1", "large-v2", "large-v3") }
      .uiNoOrder()
      .noUi()
      .def(name = "Model", info = "Whisper model for speech recognition.")

   /** [SttEngine.WHISPER_S2T] torch device used to transcribe voice to text */
   val sttWhisperSt2Device by cv("")
      .noUi()
      .def(name = "Device", info = "Whisper torch device for speech recognition. E.g. cpu, cuda:0, cuda:1. Default empty, which falls back to 'cpu'.")

   /** [SttEngine.NEMO] AI model used to transcribe voice to text */
   val sttNemoModel by cv("nvidia/parakeet-ctc-1.1b")
      .values { listOf("nvidia/parakeet-tdt-1.1b", "nvidia/parakeet-tdt_ctc-110m", "nvidia/parakeet-ctc-1.1b", "nvidia/parakeet-ctc-0.6b") }
      .uiNoOrder()
      .noUi()
      .def(name = "Model", info = "Nemo model for speech recognition.")

   /** [SttEngine.NEMO] torch device used to transcribe voice to text */
   val sttNemoDevice by cv("")
      .noUi()
      .def(name = "Device", info = "Nemo torch device for speech recognition. E.g. cpu, cuda:0, cuda:1. Default empty, which attempts to use cuda if available.")

   /** [SttEngine.HTTP] torch device used to transcribe voice to text */
   val sttHttpUrl by cv("localhost:1235")
      .noUi()
      .def(name = "Url", info = "Voice recognition server address and port.")

   /** Settings for currently speech recognition engine */
   internal val sttEngineDetails by cvNest(sttEngine) {
      when (it) {
         SttEngine.NONE, null -> ListConfigurable.heterogeneous<Any?>()
         SttEngine.WHISPER -> ListConfigurable.heterogeneous(::sttWhisperModel.getDelegateConfig(), ::sttWhisperDevice.getDelegateConfig())
         SttEngine.FASTER_WHISPER -> ListConfigurable.heterogeneous(::sttFasterWhisperModel.getDelegateConfig(),::sttFasterWhisperDevice.getDelegateConfig())
         SttEngine.WHISPER_S2T -> ListConfigurable.heterogeneous(::sttWhisperSt2Model.getDelegateConfig(), ::sttWhisperSt2Device.getDelegateConfig())
         SttEngine.NEMO -> ListConfigurable.heterogeneous(::sttNemoModel.getDelegateConfig(), ::sttNemoDevice.getDelegateConfig())
         SttEngine.HTTP -> ListConfigurable.heterogeneous(::sttHttpUrl.getDelegateConfig())
      }
   }.noPersist()
      .def(name = "Speech recognition >", info = "Settings for currently speech recognition engine")

   /** Words or phrases that will be removed from text representing the detected speech. Makes command matching more powerful. Case-insensitive. */
   val sttBlacklistWords by cList("a", "the", "please")
      .noUi()
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
   val ttsEngine by cv(TtsEngine.SYSTEM).uiNoOrder()
      .def(name = "Speech engine", info = "Engine used to generate voice. May require additional configuration")

   /** [TtsEngine.COQUI] voice. Sample file of the voice to be used. */
   val ttsEngineCoquiVoice by cv("Ann_Daniels.flac")
      .valuesUnsealed { (dir / "voices-coqui").children().filter { it.isAudio() }.map { it.name }.toList() }
      .noUi()
      .def(
         name = "Voice",
         info = "" +
            "Voice when using ${TtsEngine.COQUI.nameUi} speech engine. " +
            "Sample file of the voice to be used. User can add more audio samples to ${(dir / "voices-coqui").absolutePath}. " +
            "Should be 3-10s long."
      )

   /** [TtsEngine.COQUI] torch device used to transcribe voice to text */
   val ttsEngineCoquiCudaDevice by cv("")
      .noUi()
      .def(
         name = "Device",
         info = "Torch device for speech generation when using ${TtsEngine.COQUI.nameUi} speech engine."
      )

   /** Speech server address and port to connect to. */
   val ttsEngineHttpUrl by cv("localhost:1236")
      .noUi()
      .def(
         name = "Url",
         info = "Speech server address and port to connect to when using ${TtsEngine.HTTP.nameUi} speech engine."
      )

   /** Settings for currently active speech engine */
   internal val ttsEngineDetails by cvNest(ttsEngine) {
      when (it) {
         TtsEngine.COQUI -> ListConfigurable.heterogeneous(::ttsEngineCoquiVoice.getDelegateConfig(), ::ttsEngineCoquiCudaDevice.getDelegateConfig())
         TtsEngine.HTTP -> ListConfigurable.heterogeneous(::ttsEngineHttpUrl.getDelegateConfig())
         null, TtsEngine.NONE, TtsEngine.SYSTEM, TtsEngine.TACOTRON2, TtsEngine.SPEECHBRAIN, TtsEngine.FASTPITCH -> ListConfigurable.heterogeneous<Any?>()
      }
   }.noPersist()
      .def(name = "Speech engine >", info = "Settings for currently active speech engine")

   /** Whether [llmEngine] conversation is active */
   val llmOn = v(false)

   /** Engine used to generate voice. May require additional configuration */
   val llmEngine by cv(LlmEngine.NONE).uiNoOrder()
      .def(name = "Llm engine", info = "LLM engine for chat")

   /** Model for gpt4all. Must be in models-gpt4all. */
   val llmGpt4AllModel by cv("none")
      .valuesUnsealed { dir.div("models-gpt4all").children().map { it.name }.filter { it.endsWith("gguf") }.toList() + "none" }
      .noUi()
      .def(name = "Model", info = "Model for gpt4all. Must be in ${(dir / "models-gpt4all").absolutePath}")

   /** Url of the OpenAI or OpenAI-compatible server */
   val llmOpenAiUrl by cv("http://localhost:1234/v1")
      .noUi()
      .def(name = "Url", info = "Url of the OpenAI-compatible server")

   /** The user authorization of the OpenAI or OpenAI-compatible server */
   val llmOpenAiBearer by cv("ABC123xyz789").password()
      .noUi()
      .def(name = "Bearer", info = "The user authorization of the OpenAI-compatible server. Server may ignore this.")

   /** The llm model of the OpenAI or OpenAI-compatible server */
   val llmOpenAiModel by cv("")
      .noUi()
      .def(name = "Model", info = "The llm model of the OpenAI-compatible server. Server may ignore this.")

   /** Cli command to start llm server. Use if you want to automatize starting local AI server. Invoked on plugin start or waking from hibernation. */
   val llmOpenAiServerStartCommand by cvn<String>(null).nonEmpty()
      .noUi()
      .def(name = "Llm server start command. Prefix with ! to disable.", info = buildString {
         appendSent("Cli command to start llm server. Use if you want to automatize starting local AI server.")
         appendSent("Invoked on plugin start or waking from hibernation. Can refer to '${::llmOpenAiModel.getDelegateConfig().nameUi}' using '\$model'.")
         appendSent("Command must be idempotent - have no effect if ran multiple times.")
         appendSent("Example for `LmStudio` on `Windows`: `cmd /c lms ps | findstr \$model > nul || lms load --gpu max --yes --exact --quiet \$model`.")
      })

   /** Cli command to stop llm server. Use if you want to automatize stopping local AI server. Invoked on plugin stop or hibernation. */
   val llmOpenAiServerStopCommand by cvn<String>(null).nonEmpty()
      .noUi()
      .def(name = "Llm server stop command. Prefix with ! to disable.", info = buildString {
         appendSent("Cli command to start llm server. Use if you want to automatize stopping local AI server. Invoked on plugin stop or hibernation.")
         appendSent("Can refer to '${::llmOpenAiModel.getDelegateConfig().nameUi}' using '\$model'")
         appendSent("Command must be idempotent - have no effect if ran multiple times.")
         appendSent("Example for `LmStudio` on `Windows`: `cmd /c lms ps | findstr \$model > nul && lms unload --yes --quiet --no-launch \$model`.")
      })

   private fun llmOpenAiServerStartCommandCompute(on: Bool) =
      llmOpenAiServerStartCommand.value.takeIf { on && llmEngine.value==LlmEngine.OPENAI }?.replace("\$model", llmOpenAiModel.value)

   private fun llmOpenAiServerStopCommandCompute(on: Bool) =
      llmOpenAiServerStopCommand.value.takeIf { on && llmEngine.value==LlmEngine.OPENAI }?.replace("\$model", llmOpenAiModel.value)

   private fun llmOpenAiServerStart(command: String?) =
      runTry {
         val c = command
         if (c!=null && !c.startsWith("!"))
            runCommandWithOutput(c).withAppProgress("Start LLM server").awaitFxOrBlock()
      }

   private fun llmOpenAiServerStop(on: Bool) =
      runTry {
         val c = llmOpenAiServerStopCommandCompute(on)
         if (c!=null && !c.startsWith("!"))
            runCommandWithOutput(c).withAppProgress("Stop LLM server").awaitFxOrBlock()
      }

   /** Settings for currently active llm engine */
   internal val llmEngineDetails by cvNest(llmEngine) {
      when (it) {
         LlmEngine.OPENAI -> ListConfigurable.heterogeneous(
            ::llmOpenAiUrl.getDelegateConfig(), ::llmOpenAiBearer.getDelegateConfig(),
            ::llmOpenAiModel.getDelegateConfig(),
            ::llmOpenAiServerStartCommand.getDelegateConfig(), ::llmOpenAiServerStopCommand.getDelegateConfig()
         )
         LlmEngine.GPT4ALL -> ListConfigurable.heterogeneous(::llmGpt4AllModel.getDelegateConfig())
         LlmEngine.NONE, null -> ListConfigurable.heterogeneous<Any?>()
      }
   }.noPersist()
      .def(name = "Llm engine >", info = "Settings for currently active llm engine")

   /** System prompt telling llm to assume role, or exhibit behavior */
   val llmChatSysPrompt by cv("You are helpful voice assistant. You are voiced by tts, be extremly short.").multiline(10).nonBlank()
      .noPersist()
      .def(name = "Llm chat > system prompt", info = "System prompt telling llm to assume role, or exhibit behavior", editable = EditMode.APP)

   /** System prompt telling llm to assume role, or exhibit behavior */
   val llmChatSysPromptFile by cv(dirPersonas / "System.txt").valuesUnsealed { dirPersonas.children { it hasExtension "txt" }.toList() }
      .uiConverter { it.nameWithoutExtension }
      .def(name = "Llm chat > system prompt file", info = "System prompt telling llm to assume role, or exhibit behavior")
      .sync { llmChatSysPrompt valueTry it.readTextTry() }

   /** Maximum number of tokens in the reply. Further tokens will be cut off (by llm) */
   val llmChatMaxTokens by cvn(400).min(1)
      .def(name = "Llm chat > max tokens", info = "Maximum number of tokens in the reply. Further tokens will be cut off (by llm)")

   /** Output randomness. Scale 0-1, i.e. determinism-creativity. 0 will always pick the most likely next token.. */
   val llmChatTemp by cvn(0.5).between(0.0, 1.0)
      .def(name = "Llm chat > temperature", info = "The llm model of the OpenAI or OpenAI-compatible server")

   /** top P */
   val llmChatTopP by cvn(0.95).between(0.1, 1.0)
      .def(name = "Llm chat > top P", info = "")

   /** top K */
   val llmChatTopK by cvn(40).min(1)
      .def(name = "Llm chat > top K", info = "")

   /** Url of the http API of the AI executor */
   val httpUrl by cv("http://localhost:1236")
      .def(
         name = "Http url",
         info = "Url of the http API of the locally running AI executor"
      )

   private var isRunning = false

   override fun start() {
      // runtime-changeable properties
      val p2 = 2.seconds
      val p5 = 5.seconds
      // @formatter:off
               wakeUpWord.chan().throttleToLast(p2) subscribe { write("wake-word=$it") }
              mainSpeaker.chan().throttleToLast(p2) subscribe { write("main-speaker=$it") }
             mainLocation.chan().throttleToLast(p2) subscribe { write("main-location=$it") }
               micEnabled.chan().throttleToLast(p2) subscribe { write("mic-enabled=$it") }
       micVoiceDetectProb.chan().throttleToLast(p2) subscribe { write("mic-voice-detect-prop=$it") }
  micVoiceDetectProbDebug.chan().throttleToLast(p2) subscribe { write("mic-voice-detect-debug=$it") }
                    ttsOn.chan().throttleToLast(p2) subscribe { write("speech-on=$it") }
      ttsEngineCoquiVoice.chan().throttleToLast(p2) subscribe { write("coqui-voice=$it") }
         llmChatSysPrompt.chan().throttleToLast(p5) subscribe { write("llm-chat-sys-prompt=${it.replace('\n', ' ').replace('\r', ' ')}") }
         llmChatMaxTokens.chan().throttleToLast(p2) subscribe { write("llm-chat-max-tokens=$it") }
              llmChatTemp.chan().throttleToLast(p2) subscribe { write("llm-chat-temp=$it") }
              llmChatTopP.chan().throttleToLast(p2) subscribe { write("llm-chat-topp=$it") }
              llmChatTopK.chan().throttleToLast(p2) subscribe { write("llm-chat-topk=$it") }
      // @formatter:on

      startSpeechRecognition(true)

      // restart-requiring properties
      val processChangeVals = listOf<V<*>>(
         micChanges, micVoiceDetect, micVoiceDetectDevice, audioOutChanges,
         sttEngine, sttWhisperModel, sttWhisperDevice, sttWhisperSt2Model, sttWhisperSt2Device, sttFasterWhisperModel, sttFasterWhisperDevice, sttNemoModel, sttNemoDevice, sttHttpUrl,
         ttsEngine, ttsEngineCoquiCudaDevice, ttsEngineHttpUrl,
         llmEngine, llmGpt4AllModel, llmOpenAiUrl, llmOpenAiBearer, llmOpenAiModel,
         httpUrl
      )
      val processChange = processChangeVals.map { it.chan() }.reduce { a, b -> a + b }
      processChange.throttleToLast(2.seconds).subscribe { restart() } on onClose

      // turn off during hibernate
      // due to:
      // - 1 the python process may not recover from sleep properly
      // - 2 AI models slow down sleep and wear hw considerably
      // the closing must prevent hibernate until ai termination is complete
      // the startup is delayed so system is ready, which avoids starup issues
      onClose += APP.actionStream.onEventObject(SystemSleepEvent.Pre) { stopSpeechRecognition(true); fut().thenWait(5.seconds).awaitFx() }
      onClose += APP.actionStream.onEventObject(SystemSleepEvent.Stop) { runFX(5.seconds) { startSpeechRecognition(true) } }

      isRunning = true
   }

   override fun stop() {
      isRunning = false
      stopSpeechRecognition(true)
      writing.closeAndWait()
      onClose()
   }

   private fun startSpeechRecognition(runLlmServerCommand: Bool) {
      stopSpeechRecognition(false)
      setup = setup(llmOpenAiServerStartCommandCompute(runLlmServerCommand))
   }

   private fun stopSpeechRecognition(runLlmServerCommand: Bool) {
      write("EXIT")
      setup = null
      llmOpenAiServerStop(runLlmServerCommand)
   }

   @IsAction(name = "Restart Voice Assistant", info = "Restarts Voice Assistant python program")
   fun restart() {
      stopSpeechRecognition(false)
      startSpeechRecognition(false)
   }

   private val confirmers = mutableListOf<SpeakConfirmer>()

   private suspend fun confirm(text: String): Unit? {
      if (!isRunning) return null
      val h = confirmers.removeLastOrNull()
      return if (h == null) null
      else h.action(h.regex.matches(text), text)?.getAny().ifNotNull(::speak)?.toUnit()
   }

   private fun handleInput(text: String, state: String) {
      when (state) {
         "" -> Unit
         "RAW" -> Unit
         "ERR" -> Unit
         "USER-RAW" -> launch(FX) { confirm(text) }
         "USER" -> launch(FX) { confirm(text) ?: handleSpeech(text, user = true) }
         "SYS" -> Unit
         "COM" -> launch(FX) { handleSpeech(text, command = true, orDetectIntent = true) }
      }
   }

   private suspend fun handleSpeech(text: String, user: Boolean = false, command: Boolean = false, orDetectIntent: Boolean = false) {
      if (!isRunning) return
      var speaker = text.substringBefore(":")
      var location = text.substringAfter(":").substringBefore(":")
      var textContent = text.substringAfter(":").substringAfter(":")
      var textSanitized = textContent.replace("_", " ").sanitize(sttBlacklistWords_)
      if (user) speakingTextW.value = textSanitized
      if (!command) return
      var handlersViable = handlers.asSequence().filter { it.type==SpeakHandler.Type.KOTLN || !usePythonCommands.value }
      var (c, result) = handlersViable.firstNotNullOfOrNull { SpeakContext(it, this@VoiceAssistant, speaker, location)(textSanitized)?.let { r -> it to r } } ?: (null to null)
      logger.info { "Speech ${if (orDetectIntent) "event" else "intent"} handled by command `${c?.name}`, request=`${speaker}:${textSanitized}`" }
      if (result==null) {
         if (!orDetectIntent)
            if (usePythonCommands.value) writeComPytInt(speaker, location, textContent)
            else speak("Unrecognized command: $textSanitized")
         else {
            if (usePythonCommands.value)
               writeComPytInt(speaker, location, textContent)
            else
               intent(voiceCommandsPrompt(), textSanitized).ifError {
                  logger.error(it) { "Failed to understand command $textSanitized" }
                  speak("Recognized command failed with error: ${it.message ?: ""}")
               }.ifOk {
                  handleSpeech(speaker + ":" + it, command = true, orDetectIntent = false)
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

   suspend fun intent(functions: String, userPrompt: String) =
      VT.invokeTry {
         val url = httpUrl.value.net { "$it/intent"}
         APP.http.client.post(url) { bodyJs("functions" to functions, "userPrompt" to userPrompt) }.bodyAsText()
            .replace("_", " ").sanitize(sttBlacklistWords_)
      }

   fun write(text: String): Unit = writing(setup to text)
   
   fun writeCom(command: String): Unit = write("COM: ${command.encodeBase64()}")

   fun writeComPyt(speaker: String, location: String?, command: String): Unit = write("COM-PYT: ${speaker}:${location.orEmpty()}:${command.encodeBase64()}")

   fun writeComPytInt(speaker: String, location: String?, command: String): Unit = write("COM-PYT-INT: ${speaker}:${location.orEmpty()}:${command.encodeBase64()}")

   fun writeChat(speaker: String, text: String) = write("CHAT: ${speaker}:${null.orEmpty()}:${text.encodeBase64()}")

   @IsAction(name = "Speak text", info = "Identical to \"Narrate text\"")
   fun synthesize() = speak()

   @IsAction(name = "Narrate text", info = "Narrates the specified text using synthesized voice")
   fun speak() = action<String>("Narrate text", "Narrates the specified text using synthesized voice", IconMA.RECORD_VOICE_OVER, BLOCK) { speak(it) }.apply {
      constraintsN += listOf(Multiline, MultilineRows(10), RepeatableAction)
   }.invokeWithForm()

   fun speak(text: String) = write("SAY: ${text.encodeBase64()}")

   @IsAction(name = "Write chat", info = "Writes to voice assistant chat")
   fun chat() = action<String>("Write chat", "Writes to voice assistant chat", IconMA.CHAT, BLOCK) { writeChat("User", it) }.apply {
      constraintsN += listOf(Multiline, MultilineRows(10), RepeatableAction)
   }.invokeWithForm()

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

   @Throws(Throwable::class)
   suspend fun events(): JsValue =
      VT {
         APP.http.client.get("${httpUrl.value}/actor-events-all").bodyAsJs()
      }

   enum class SttEngine(val code: String, override val nameUi: String, override val infoUi: String): NameUi, InfoUi {
      NONE("none", "None", "No speech recognition"),
      WHISPER("whisper", "Whisper", "OpenAI Whisper speech recognition. Fully offline."),
      FASTER_WHISPER("faster-whisper", "Faster Whisper", "OpenAI Whisper speech recognition. Fully offline. Optimized for speed (int8 inferrence + distill models)"),
      WHISPER_S2T("whispers2t", "WhisperS2T", "OpenAI Whisper speech recognition. Fully offline. Optimized for speed."),
      NEMO("nemo", "Nemo", "Nvidia Nemo ASR. Fully offline."),
      HTTP("http", "Http", "Recognition using different instance of this application with http enabled."),
   }

   enum class TtsEngine(val code: String, override val nameUi: String, override val infoUi: String): NameUi, InfoUi {
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
   data class SpeakContext(val handler: SpeakHandler, val plugin: VoiceAssistant, val speaker: String, val location: String, val intent: Boolean = true) {

      /** @return result of handler action, i.e., `handler.regex.matches(text)` */
      suspend operator fun invoke(text: String): Try<String?, String?>? = invoke(text, handler.action)

      /** @return result of action, i.e., `handler.regex.matches(text)` */
      suspend operator fun invoke(text: String, action: suspend SpeakContext.(String) -> Try<String?, String?>?): Try<String?, String?>? = action(this, text)

      /** @return copy with [intent], i.e., `copy(intent = false)` */
      fun withIntent(): SpeakContext = copy(intent = false)

      fun withCommand(commandUi: String): SpeakContext = copy(handler = SpeakHandler(handler.type, handler.name, commandUi, { null }), intent = true)

      fun withCommand(handler: SpeakHandler): SpeakContext = copy(handler = handler, intent = true)

      /** @return whether handler matches specified text (with non-empty parameter values), i.e., `handler.regex.matches(text)` */
      fun matches(text: String): Boolean = handler.regex.matches(text) && args(text).none { it.isBlank() }

      /** @return parameter values or fails if not [matches] */
      fun args(text: String) = DestructuredList(handler.regex.matchEntire(text)!!.groupValues.drop(1).map { it.replace("_", " ") })

      /**
       * Confirms question with user and invokes the block if matcher matches user's answer. Includes optional UI warning.
       * @param confirmText text to be spoken to user to request feedback
       * @param commandUi matcher
       * @param action action that runs on FX thread, takes intent detection result an returns voice feedback
       * @return `Ok(null)` ([confirmText] is always spoken)
       */
      public suspend fun confirming(confirmText: String, commandUi: String, ui: Boolean = false, action: suspend (String) -> Try<String?, String?>): Ok<Nothing?> =
         FX {
            var n = null as Notification?
            val c = SpeakConfirmer(commandUi) { matches, text ->
               n?.hide()
               matches.markUsed("Use AI as matcher")
               intent(text, "User is asked a yes or no question. Detect if he answers positively and respond with `yes` or `no`.", text) {
                  if (it=="yes") action(text)
                  else null
               }
            }
            n = if (!ui) null else APP.plugins.get<Notifier>()?.showWarningNotification("", confirmText) { plugin.confirmers -= c }
            plugin.confirmers += c
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
               val command = APP.http.client.post(url) { bodyJs("functions" to functions, "userPrompt" to userPrompt) }.bodyAsText()
                  .replace("_", " ").sanitize(this@SpeakContext.plugin.sttBlacklistWords_)
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
   open class SpeakHandler(val type: Type, val name: String, val commandUi: String, val action: suspend SpeakContext.(String) -> Try<String?, String?>?) {
      /** [commandUi] turned into regex */
      val regex by lazy { voiceCommandRegex(commandUi) }

      enum class Type { PYTHN, KOTLN, DEFER, ALIAS }
   }

   open class SpeakHandlerGroup(
      type: Type, name: String, commandUi: String, val commands: List<SpeakHandler>, action: suspend SpeakContext.(String) -> Try<String?, String?>?
   ): SpeakHandler(type, name, commandUi, action)

   /**
    * Speech event confirmer. In ui shown as `"$name -> $commandUi"`
    * @param action action that runs on FX thread, takes original command, returns Try (with text to speak or null if none). */
   private data class SpeakConfirmer(val commandUi: String, val action: suspend (Boolean, String) -> Try<String?, String?>?) {
      /** [commandUi] turned into regex */
      val regex = voiceCommandRegex(commandUi)
   }

   companion object: PluginInfo, KLogging() {
      override val name = "Voice Assistant"
      override val description = "Provides speech recognition, synthesis, LLM chat and voice control capabilities using various AI models."
      override val isSupported = true
      override val isSingleton = true
      override val isEnabledByDefault = false

      val dir by lazy { APP.location / "speech-recognition-whisper" }
      val dirPersonas by lazy { dir / "personas" }

      val mainLocationInitial = "PC"

      val mainSpeakerInitial = "User"

      fun obtainSpeakers(): List<String> = listOf(mainSpeakerInitial) + (dir / "voices-verified").children().filter { it hasExtension "wav" }.map { it.nameWithoutExtension }

      fun obtainPersonas(): Sequence<File> = dirPersonas.children().filter { it hasExtension "txt" }

      val voiceAssistantWidgetFactory by lazy { WidgetFactory("VoiceAssistant", VoiceAssistantWidget::class, APP.location.widgets/"VoiceAssistant") }

      val voiceAssistantPersonasWidgetFactory by lazy { WidgetFactory("Voice Assistant persona editor", VoiceAssistantPersona::class, APP.location.widgets/"Voice-Assistant-Persona-Editor") }

      /** @return this string with newline prepended or empty string if blank */
      private fun String?.wrap() = if (isNullOrBlank()) "" else "\n$this"

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