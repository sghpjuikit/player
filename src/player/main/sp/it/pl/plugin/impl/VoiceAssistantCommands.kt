package sp.it.pl.plugin.impl

import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import javafx.geometry.Pos
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode
import javafx.scene.robot.Robot
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.invoke
import kotlinx.coroutines.suspendCancellableCoroutine
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.layout.ComponentLoaderStrategy
import sp.it.pl.main.APP
import sp.it.pl.main.ScheduledNote
import sp.it.pl.plugin.impl.VoiceAssistant.SpeakContext
import sp.it.pl.plugin.impl.VoiceAssistant.SpeakHandler
import sp.it.pl.plugin.impl.VoiceAssistant.SpeakHandler.Type.ALIAS
import sp.it.pl.plugin.impl.VoiceAssistant.SpeakHandler.Type.DEFER
import sp.it.pl.plugin.impl.VoiceAssistant.SpeakHandler.Type.KOTLN
import sp.it.pl.plugin.impl.VoiceAssistant.SpeakHandler.Type.PYTHN
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.pl.voice.toVoiceS
import sp.it.util.async.coroutine.VT
import sp.it.util.async.coroutine.delay
import sp.it.util.async.coroutine.delayTill
import sp.it.util.async.coroutine.launch
import sp.it.util.async.runFX
import sp.it.util.file.div
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Error
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.flatten
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.system.Os
import sp.it.util.system.Os.WINDOWS
import sp.it.util.system.Windows
import sp.it.util.text.camelToSpaceCase
import sp.it.util.text.equalsNc
import sp.it.util.text.keys
import sp.it.util.text.words
import sp.it.util.ui.pressReleaseShortcut
import sp.it.util.ui.pressReleaseText
import sp.it.util.units.millis
import sp.it.util.units.seconds
import sp.it.util.units.uuid

typealias ComMatch = Try<String?, String?>?

internal fun VoiceAssistant.voiceCommands(): List<SpeakHandler> {
   val sh = ::SpeakHandler
   return listOfNotNull(
      sh(PYTHN,        "Repeat last speech", "repeat last speech")                                                     { null },
      sh(PYTHN,                  "Greeting", "greeting \$user_greeting")                                               { null },
      sh(KOTLN,                "Show emote", "show emote \$text")                                                      { voiceCommandShowEmote(it) },
      sh(KOTLN,              "Show warning", "show warning \$text")                                                    { voiceCommandShowWarn(it) },
      sh(KOTLN,                      "Help", "help")                                                                   { if (matches(it)) Ok("List commands by saying, list commands") else null },
      sh(KOTLN,                "Do nothing", "ignore")                                                                 { if (matches(it)) Ok(null) else null },
      sh(KOTLN,         "Restart Assistant", "restart assistant|yourself")                                             { if (matches(it)) { speak("Ok"); restart(); Ok(null) } else null },
      sh(KOTLN,             "Help Commands", "list commands")                                                          { if (matches(it)) Ok(handlersHelpText()) else null },
      sh(KOTLN,        "Start conversation", "start conversation")                                                     { if (matches(it)) { llmOn = true; Ok(null) } else null },
      sh(KOTLN,      "Restart conversation", "restart conversation")                                                   { if (matches(it)) { llmOn = true; Ok(null) } else null },
      sh(KOTLN,         "Stop conversation", "stop conversation")                                                      { if (matches(it)) { llmOn = false; Ok(null) } else null },
      sh(KOTLN,              "Current time", "what time is it")                                                        { voiceCommandCurrentTime(it) },
      sh(KOTLN,              "Current date", "what date is it")                                                        { voiceCommandCurrentDate(it) },
      sh(KOTLN,              "Current song", "what song|playback is active")                                           { voiceCommandCurrentSong(it) },
      sh(KOTLN,           "Resume playback", "play|start|resume|continue music|playback")                              { voiceCommandPlaybackResume(it) },
      sh(KOTLN,            "Pause playback", "stop|end|pause music|playback")                                          { voiceCommandPlaybackPause(it) },
      sh(KOTLN,        "Play previous song", "play previous song")                                                     { voiceCommandPlaybackPlayPreviousSong(it) },
      sh(KOTLN,            "Play next song", "play next song")                                                         { voiceCommandPlaybackPlayNextSong(it) },
      sh(KOTLN,           "Play first song", "play first song")                                                        { voiceCommandPlaybackPlayFirstSong(it) },
      sh(KOTLN,            "Play last song", "play last song")                                                         { voiceCommandPlaybackPlayLastSong(it) },
      sh(KOTLN,               "Play volume", "play volume \$number_zero_one")                                          { voiceCommandPlaybackVolume(it) },
      sh(KOTLN,           "Adjust playback", "playback \$text")                                                        { voiceCommandPlayback(it) },
      sh(KOTLN, "Llm answer from clipboard", "generate|answer|write from? clipboard \$text")                           { voiceCommandGenerateClipboard(it) },
      sh(KOTLN,                "Llm answer", "generate|answer|write \$text")                                           { voiceCommandGenerate(it) },
      sh(DEFER,                     "Speak", "speak|say \$text")                                                       { voiceCommandSpeakText(it) },
      sh(DEFER,      "Speak from clipboard", "speak|say from? clipboard")                                              { voiceCommandSpeakClipboard(it) },
      sh(DEFER,        "Describe clipboard", "describe|define clipboard // describe the term or content in clipboard") { voiceCommandDescribeClipboard(it) },
      sh(DEFER,             "Describe text", "describe|define \$text // describe the text")                            { voiceCommandDescribeText(it) },
      sh(KOTLN,              "Close window", "close|hide window")                                                      { voiceCommandAltF4(it) },
      sh(KOTLN,               "Search text", "search for? \$text")                                                     { voiceSearch(it) },
      sh(KOTLN,                 "Type text", "type \$text")                                                            { voiceType(it) },
      sh(KOTLN,       "Open widget by name", "open|show widget? \$widget_name widget?")                                { voiceCommandOpenWidget(it) },
      sh(KOTLN,               "Shutdown OS", "shut down system|pc|computer|os")                                        { voiceCommandOsShutdown(it) }.takeIf { WINDOWS.isCurrent },
      sh(KOTLN,                "Restart OS", "restart system|pc|computer|os")                                          { voiceCommandOsRestart(it) }.takeIf { WINDOWS.isCurrent },
      sh(KOTLN,              "Hibernate OS", "hibernate system|pc|computer|os")                                        { voiceCommandOsHibernate(it) }.takeIf { WINDOWS.isCurrent },
      sh(KOTLN,                  "Sleep OS", "sleep system|pc|computer|os")                                            { voiceCommandOsSleep(it) }.takeIf { WINDOWS.isCurrent },
      sh(KOTLN,                   "Lock OS", "lock system|pc|computer|os")                                             { voiceCommandOsLock(it) }.takeIf { WINDOWS.isCurrent },
      sh(KOTLN,                "Log off OS", "log off system|pc|computer|os")                                          { voiceCommandOsLogOff(it) }.takeIf { WINDOWS.isCurrent },
      sh(KOTLN,              "Set reminder", "set reminder in|at \$time \$text")                                       { voiceCommandSetReminder(it) },
      sh(DEFER,                      "Wait", "wait \$time // units: s")                                                { voiceCommandWait(it) },
      sh(DEFER,               "Count to...", "count from \$from to \$to")                                              { voiceCommandCountTo(it) }.takeUnless { usePythonCommands.value },
      sh(ALIAS,              "Open weather", "open weather")                                                           { null },
   )
}

internal fun VoiceAssistant.voiceCommandsPrompt(): String =
   (
      // commands without matchers, this lets python always handles them
      "" +
      // normal commands
      """
      * 'list commands'
      * 'restart assistant|yourself'
      * 'start|restart|stop conversation'
      * 'shut_down|restart|hibernate|sleep|lock|log_off system|pc|computer|os'
      * 'list available voices'
      * 'change voice Δvoice' // resolve to one from {', '.join(voices)}
      * 'open Δwidget_name'  // various tasks can be acomplished using appropriate widget
      * 'play music'
      * 'stop music'
      * 'play previous|next|first|last song'
      * 'play volume Δvalue'  // sets playback volume, values are double between <0-1>
      * 'play volume +|-'  // increases/decreases playback volume as many times as many +|- is used (use <1-10>)
      * 'what song|playback is active'
      * 'what time is it'
      * 'what date is it'
      * 'generate from? clipboard'
      * 'lights on|off' // turns all lights on/off
      * 'lights group Δgroup_name on|off?'  // room is usually a group
      * 'list light groups'
      * 'light bulb Δbulb_name on|off?'
      * 'list light bulbs'
      * 'lights scene Δscene_name'  // sets light scene, scene is usually a mood, user must say 'scene' word
      * 'list light scenes'
      """ +
      // aliases
      handlers.filter { it.type==ALIAS }.map { "* '" + it.commandUi + "'" }.joinToString("") +
      // commands that steal python-code precendence (and must be disabled)
      "" +
      // fallback
      """
      * 'unidentified' // no other command is highly probable
      """
   ).replace('Δ', '$').lineSequence().filter { it.isNotBlank() }.joinToString("\n")


suspend fun SpeakContext.voiceCommandShowEmote(text: String): ComMatch =
   if (matches(text)) {
      if (text != "show emote none")
         APP.plugins.use<Notifier> {
            val n = Thumbnail(400.0, 400.0).apply {
               pane.isMouseTransparent = true
               loadFile(VoiceAssistant.dir / text.substringAfter("show emote ").replace(" ", "_"))
            }
            val not = it.showNotification("Emote", n.pane, true, Pos.TOP_RIGHT)
            delay(1.seconds)
            n.animationPlay()
            n::animationOnDone.delayTill()
            delay(1.seconds)
            not.hide()
         }
      Ok(null)
   } else
      null

suspend fun SpeakContext.voiceCommandPlayback(text: String): ComMatch =
   if (matches(text)) {
      val p = """
         * 'play|start|resume|continue music' // stops or ends or pauses music playback
         * 'stop|end|pause music' // stops or ends or pauses music playback
         * 'play previous|next|first|last song' // changes playback to previous|next|first|last song
         * 'play volume Δvalue'  // sets playback volume, values are double between <0-1>
         * 'play volume +|-'  // increases/decreases playback volume as many times as many +|- is used (use <1-10>), e.g. 'play volume ++++'
      """.trimIndent().replace('Δ', '$')
      intent(text, p, text.substringAfter("playback ")) {
         null
            ?: with(withCommand("play|start|resume|continue|unpause music|playback")) { voiceCommandPlaybackResume(it) }
            ?: with(withCommand("stop|end|pause music|playback"                    )) { voiceCommandPlaybackPause(it) }
            ?: with(withCommand("play previous song"                               )) { voiceCommandPlaybackPlayPreviousSong(it) }
            ?: with(withCommand("play next song"                                   )) { voiceCommandPlaybackPlayNextSong(it) }
            ?: with(withCommand("play first song"                                  )) { voiceCommandPlaybackPlayFirstSong(it) }
            ?: with(withCommand("play last song"                                   )) { voiceCommandPlaybackPlayLastSong(it) }
            ?: with(withCommand("play volume \$number_zero_one"                    )) { voiceCommandPlaybackVolume(it) }
            ?: Error("Unknown playback command '${it}'")
      }
   } else
      null

fun SpeakContext.voiceCommandPlaybackResume(text: String) =
   if (matches(text)) { APP.audio.resume(); Ok(null) }
   else null

fun SpeakContext.voiceCommandPlaybackPause(text: String) =
   if (matches(text)) { APP.audio.pause(); Ok(null) }
   else null

fun SpeakContext.voiceCommandPlaybackPlayPreviousSong(text: String) =
   if (matches(text)) { APP.audio.playlists.playPreviousItem(); Ok(null) }
   else null

fun SpeakContext.voiceCommandPlaybackPlayNextSong(text: String) =
   if (matches(text)) { APP.audio.playlists.playNextItem(); Ok(null) }
   else null

fun SpeakContext.voiceCommandPlaybackPlayFirstSong(text: String) =
   if (matches(text)) { APP.audio.playlists.playFirstItem(); Ok(null) }
   else null

fun SpeakContext.voiceCommandPlaybackPlayLastSong(text: String) =
   if (matches(text)) { APP.audio.playlists.playLastItem(); Ok(null) }
   else null

fun SpeakContext.voiceCommandPlaybackVolume(text: String): ComMatch =
   if (matches(text)) {
      runTry {
         val (v) = args(text)
              if ('-' in v) repeat(v.count { it=='-' }) { APP.audio.volumeDec() }
         else if ('+' in v) repeat(v.count { it=='+' }) { APP.audio.volumeInc() }
         else APP.audio.volume.value = v.toDouble()
      }.map { null }.mapError {
         "Error " + it.localizedMessage
      }
   } else
      null

fun SpeakContext.voiceCommandShowWarn(text: String): ComMatch =
   if (matches(text)) {
      APP.plugins.get<Notifier>()?.showWarningNotification("", text.substringAfter("show warning ")) {}
      Ok(null)
   } else
      null

fun SpeakContext.voiceCommandCurrentTime(text: String): ComMatch =
   if (matches(text)) Ok(LocalTime.now().net { "Right now it is ${it.toVoiceS()}" })
   else null

fun SpeakContext.voiceCommandCurrentDate(text: String): ComMatch =
   if (matches(text)) Ok(LocalDate.now().net { "Today is ${it.toVoiceS()}" })
   else null

fun SpeakContext.voiceCommandCurrentSong(text: String): ComMatch =
   if (matches(text))
      Ok(
         APP.audio.playingSong.value.takeIf { it!=Metadata.EMPTY }
            ?.net { it.getTitle()?.net { "It is $it" }.orEmpty() + (it.getArtist() ?: it.getAlbumArtist())?.net { " by $it" }.orEmpty() }
            ?.net { it.takeIf { it.isNotBlank() } ?: "There is not enough song tag metadata to tell" }
            ?: "There is no active playback"
      )
   else null

fun SpeakContext.voiceCommandGenerate(text: String): ComMatch =
   if (matches(text)) {
      plugin.writeCom("generate " + text)
      Ok(null)
   } else
      null

fun SpeakContext.voiceCommandGenerateClipboard(text: String): ComMatch =
   if (matches(text)) {
      plugin.writeCom("generate " + text + "\nClipboard:\n```" + (Clipboard.getSystemClipboard().string ?: "" + "```"))
      Ok(null)
   } else
      null

fun SpeakContext.voiceCommandDescribeClipboard(text: String): ComMatch =
   if (matches(text)) {
      plugin.writeCom("do-describe " + Clipboard.getSystemClipboard().string.orEmpty())
      Ok(null)
   } else
      null

fun SpeakContext.voiceCommandDescribeText(text: String): ComMatch =
   if (matches(text)) {
      plugin.writeCom("do-describe $text")
      Ok(null)
   } else
      null

fun SpeakContext.voiceCommandSpeakClipboard(text: String): ComMatch =
   if (matches(text)) Ok(Clipboard.getSystemClipboard().string ?: "")
   else null

fun SpeakContext.voiceCommandSpeakText(text: String): ComMatch =
   if (matches(text)) Ok(text.removePrefix("speak").removePrefix("say").trim())
   else null

fun SpeakContext.voiceCommandAltF4(text: String): ComMatch =
   if (matches(text)) {
      Robot().pressReleaseShortcut(KeyCode.ALT, KeyCode.F4)
      Ok(null)
   } else
      null

suspend fun SpeakContext.voiceSearch(text: String): ComMatch =
   if (matches(text)) {
      delay(100.millis) // UI may not be ready
      Robot().apply {
         pressReleaseShortcut(KeyCode.CONTROL, KeyCode.F)
         delay(100.millis)
         fun String.isSpelled() = words().all { it.length==1 }
         fun String.despell() = replace(" ", "")
         val t = args(text)[0].net { if (it.isSpelled()) it.despell() else it }
         pressReleaseText(t)
      }
      Ok(null)
   } else
      null

suspend fun SpeakContext.voiceType(text: String): ComMatch =
   if (matches(text)) {
      delay(100.millis) // UI may not be ready
      Robot().apply {
         fun String.isSpelled() = words().all { it.length==1 }
         fun String.despell() = replace(" ", "")
         val t = args(text)[0].net { if (it.isSpelled()) it.despell() else it }
         pressReleaseText(t)
      }
      Ok(null)
   } else
      null

fun SpeakContext.availableWidgets() =
   (plugin.commandWidgetNames.keys + APP.widgetManager.factories.getComponentFactories().map { it.name })
      .map { "- ${it.lowercase()}" }
      .joinToString("\n")

suspend fun SpeakContext.voiceCommandOpenWidget(text: String): ComMatch =
   if (text.startsWith("open")) {
      val fNameRaw = text.removePrefix("open").trimStart().removePrefix("widget").removeSuffix("widget").trim().camelToSpaceCase()
      val fName = plugin.commandWidgetNames.get(fNameRaw) ?: fNameRaw
      val f = APP.widgetManager.factories.getComponentFactories().find { it.name.camelToSpaceCase() equalsNc fName }
      if (f!=null) ComponentLoaderStrategy.DOCK.loader(f)
      if (f!=null) Ok("Ok")
      else if (!intent) Error("No widget $fNameRaw available.")
      else intent(text, "${availableWidgets()}\n- unidentified // no recognized function", fNameRaw) { this("open widget $it") }
   } else {
      if (!intent) Error("No such widget available.")
      else null
   }

suspend fun SpeakContext.voiceCommandOsShutdown(text: String): ComMatch =
   if (!matches(text)) null
   else if (!Os.WINDOWS.isCurrent) Error("Unsupported on this platform")
   else confirming("Do you really wish to shut down computer?", "yes", ui = true) { Windows.shutdown().map { null }.mapError { it.localizedMessage } }

suspend fun SpeakContext.voiceCommandOsRestart(text: String): ComMatch =
   if (!matches(text)) null
   else if (!Os.WINDOWS.isCurrent) Error("Unsupported on this platform")
   else confirming("Do you really wish to restart computer?", "yes", ui = true) { Windows.restart().map { null }.mapError { it.localizedMessage } }

suspend fun SpeakContext.voiceCommandOsSleep(text: String): ComMatch =
   if (!matches(text)) null
   else if (!Os.WINDOWS.isCurrent) Error("Unsupported on this platform")
   else confirming("Do you really wish to sleep computer?", "yes", ui = true) { Windows.sleep().map { null }.mapError { it.localizedMessage } }

suspend fun SpeakContext.voiceCommandOsHibernate(text: String): ComMatch =
   if (!matches(text)) null
   else if (!Os.WINDOWS.isCurrent) Error("Unsupported on this platform")
   else confirming("Do you really wish to hibernate computer?", "yes", ui = true) { Windows.hibernate().map { null }.mapError { it.localizedMessage } }

suspend fun SpeakContext.voiceCommandOsLogOff(text: String): ComMatch =
   if (!matches(text)) null
   else if (!Os.WINDOWS.isCurrent) Error("Unsupported on this platform")
   else confirming("Do you really wish to logOff computer?", "yes", ui = true) { Windows.logOff().map { null }.mapError { it.localizedMessage } }

fun SpeakContext.voiceCommandOsLock(text: String): ComMatch =
   if (!matches(text)) null
   else if (!Os.WINDOWS.isCurrent) Error("Unsupported on this platform")
   else Windows.lock().map { null }.mapError { it.localizedMessage }

fun SpeakContext.voiceCommandSetReminder(text: String): ComMatch =
   if (handler.regex.matches(text)) {
   // if (matches(text)) {  // this command is not matched properly with this function due to non-greedy groups
      text.removePrefix("set reminder ").net {
         when {
            it.startsWith("in ") -> {
               val t1 = it.removePrefix("in ")
               val t2 = t1.words().firstOrNull() ?: ""
               val t3 = t1.substringAfter(" ")
               val isUnitSpaced = Regex("s|sec|m|min|h|hour|d|day|w|week|mon|y|year") !in t2
               var nText = if (isUnitSpaced) t3.substringAfter(" ") else t3
               var nUnit = if (isUnitSpaced) t3.substringBefore(" ") else t2
               val nUnitAmount = t2.dropLastWhile { !Character.isDigit(it) }.toLongOrNull()
               var nTime = when {
                  nUnitAmount==null -> null
                  nUnit.contains("year") || nUnit.endsWith("y") -> Period.ofYears(nUnitAmount.toInt())
                  nUnit.contains("mon")                         -> Period.ofMonths(nUnitAmount.toInt())
                  nUnit.contains("week") || nUnit.endsWith("w") -> Period.ofWeeks(nUnitAmount.toInt())
                  nUnit.contains("day")  || nUnit.endsWith("d") -> Duration.ofDays(nUnitAmount)
                  nUnit.contains("hour") || nUnit.endsWith("h") -> Duration.ofHours(nUnitAmount)
                  nUnit.contains("min")  || nUnit.endsWith("m") -> Duration.ofMinutes(nUnitAmount)
                  nUnit.contains("sec")  || nUnit.endsWith("s") -> Duration.ofSeconds(nUnitAmount)
                  else -> null
               }
               if (nTime==null) Error("Invalid time format")
               else Ok(ScheduledNote(uuid(), Instant.now().plus(nTime), nText))
            }
            it.startsWith("at ") -> {
               var nText = it.removePrefix("at ").substringAfter(" ")
               var nTime = it.removePrefix("at ").substringBefore(" ").uppercase().let { runTry { Instant.parse(it) }.orNull() }
               if (nTime==null) Error("Invalid time format")
               else Ok(ScheduledNote(uuid(), nTime, nText))
            }
            else -> Error("Invalid time format")
         }.map {
            APP.scheduler schedule it
            Ok("Reminder scheduled")
         }.flatten()
      }
   } else {
      null
   }

suspend fun SpeakContext.voiceCommandWait(text: String): ComMatch =
   if (matches(text)) {
      val nTime = text.removePrefix("wait ").removeSuffix("s").trim().toDoubleOrNull()
      if (nTime!=null) delay(nTime.seconds)
      if (nTime!=null) Ok(null)
      else if (!intent) Error("Invalid time format.")
      else intent(text, "- wait-\$time_period_in_seconds", text) { this(it) }
   } else {
      if (!intent) Error("Invalid time format.")
      else null
   }

suspend fun SpeakContext.voiceCommandCountTo(text: String): ComMatch =
   if (matches(text)) {
      val pattern = Regex("""count from (.*) to (.*)""")
      val (from, to) = pattern.find(text)!!.destructured.net { (a,b) -> (a.toIntOrNull() ?: 1) to (b.toIntOrNull() ?: 10) }
      VT {
         APP.plugins.get<VoiceAssistant>()?.writeComPyt(
            "User",
            """
            for i in range($from, $to):
               wait(0.5)
               speak(str(i))
            """.trimIndent()
         )
      }
      Ok(null)
   } else {
      null
   }

fun List<SpeakHandler>.toPromptHint() =
   joinToString("") { voiceCommandToPromptHint(it.commandUi) }

fun voiceCommandToPromptHint(commandUi: String) =
   "\n* '${voiceCommandNoOptionalParts(voiceCommandWithoutComment(commandUi))}' ${voiceCommandCommentOnly(commandUi)}"

fun voiceCommandWithoutComment(commandUi: String) =
   commandUi.substringBefore("//").trim()

fun voiceCommandCommentOnly(commandUi: String) =
   if ("//" in commandUi) "//" + commandUi.substringAfter("//") else ""

fun voiceCommandNoOptionalParts(commandUi: String) =
   commandUi.replace(" *", " ").splitToSequence(" ").mapNotNull { if (it.endsWith("?") && "|" !in it && "$" !in it) null else it }.joinToString(" ")

fun voiceCommandRegex(commandUi: String) =
   Regex(
      commandUi
         // remove comment hint
         .substringBefore("//").trim()
         // to regex
         .net { it ->
            fun String.rr() = replace("(", "").replace(")", "").replace("?", "")
            it.split(" ")
               .map { p ->
                  when {
                     p.startsWith("$") -> "(.+?)"
                     p.contains("|") && p.endsWith("?") -> p.rr().net { "(?:${it.split("|").joinToString("|") { it }})?" }
                     p.endsWith("?") -> p.rr().net { "(?:${it})?" }
                     p.contains("|") -> p.rr().net { "(?:$it)" }
                     else -> p.rr()
                  }
               }
               .joinToString(" *").replace(" * *", " *").trim()
         }
   )