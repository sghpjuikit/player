package sp.it.pl.plugin.impl

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode
import javafx.scene.robot.Robot
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.layout.ComponentLoaderStrategy
import sp.it.pl.main.APP
import sp.it.pl.main.ScheduledNote
import sp.it.pl.plugin.impl.VoiceAssistant.SpeakContext
import sp.it.pl.voice.toVoiceS
import sp.it.util.async.runVT
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Error
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.flatten
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.system.Os
import sp.it.util.system.Windows
import sp.it.util.text.camelToSpaceCase
import sp.it.util.text.equalsNc
import sp.it.util.text.words
import sp.it.util.ui.pressReleaseShortcut
import sp.it.util.ui.pressReleaseText
import sp.it.util.units.uuid

typealias ComMatch = Try<String?, String?>?

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
            ?.net { it.getTitle()?.net { "It is $it " }.orEmpty() + (it.getArtist() ?: it.getAlbumArtist())?.net { " by $it" }.orEmpty() }
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
      Robot().apply {
         pressReleaseShortcut(KeyCode.CONTROL, KeyCode.F)
         delay(100)
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
   if (matches(text))
      if (!Os.WINDOWS.isCurrent) Error("Unsupported on this platform")
      else confirming("Do you really wish to shut down computer?", "yes") { Windows.shutdown().map { null }.mapError { it.localizedMessage } }
   else null

suspend fun SpeakContext.voiceCommandOsRestart(text: String): ComMatch =
   if (matches(text))
      if (!Os.WINDOWS.isCurrent) Error("Unsupported on this platform")
      else confirming("Do you really wish to restart computer?", "yes") { Windows.restart().map { null }.mapError { it.localizedMessage } }
   else null

suspend fun SpeakContext.voiceCommandOsSleep(text: String): ComMatch =
   if (matches(text))
      if (!Os.WINDOWS.isCurrent) Error("Unsupported on this platform")
      else confirming("Do you really wish to sleep computer?", "yes") { Windows.sleep().map { null }.mapError { it.localizedMessage } }
   else null

suspend fun SpeakContext.voiceCommandOsHibernate(text: String): ComMatch =
   if (matches(text))
      if (!Os.WINDOWS.isCurrent) Error("Unsupported on this platform")
      else confirming("Do you really wish to hibernate computer?", "yes") { Windows.hibernate().map { null }.mapError { it.localizedMessage } }
   else null

suspend fun SpeakContext.voiceCommandOsLogOff(text: String): ComMatch =
   if (matches(text))
      if (!Os.WINDOWS.isCurrent) Error("Unsupported on this platform")
      else confirming("Do you really wish to logOff computer?", "yes") { Windows.logOff().map { null }.mapError { it.localizedMessage } }
   else null

fun SpeakContext.voiceCommandOsLock(text: String): ComMatch =
   if (matches(text))
      if (!Os.WINDOWS.isCurrent) Error("Unsupported on this platform")
      else Windows.lock().map { null }.mapError { it.localizedMessage }
   else null

fun SpeakContext.voiceCommandSetReminder(text: String): ComMatch =
   if (matches(text)) {
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
               var nTime = it.removePrefix("in ").substringBefore(" ").let { runTry { Instant.parse(it) }.orNull() }
               if (nTime==null) Error("Invalid time format")
               else Ok(ScheduledNote(uuid(), nTime, nText))
            }
            else -> Error("Invalid time format")
         }.map {
            APP.scheduler.jobs += it
            Ok("Reminder scheduled")
         }.flatten()
      }
   } else {
      null
   }

fun SpeakContext.voiceCommandCountTo(text: String): ComMatch =
   if (matches(text)) {
      val pattern = Regex("""count from (.*) to (.*)""")
      val (from, to) = pattern.find(text)!!.destructured.net { (a,b) -> (a.toIntOrNull() ?: 1) to (b.toIntOrNull() ?: 10) }
      runVT {
         repeat(max(from, to) - min(from, to) + 1) {
            APP.plugins.get<VoiceAssistant>()?.speak("${(from + it)}...")
         }
      }
      Ok("Ok")
   } else {
      null
   }

fun voiceCommandRegex(commandUi: String) = Regex(
   commandUi.net { it ->
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