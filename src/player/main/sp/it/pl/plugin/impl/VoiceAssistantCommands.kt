package sp.it.pl.plugin.impl

import java.time.Duration
import java.time.Instant
import java.time.Period
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode
import javafx.scene.robot.Robot
import kotlin.math.max
import kotlin.math.min
import sp.it.pl.layout.ComponentLoaderStrategy
import sp.it.pl.main.APP
import sp.it.pl.main.ScheduledNote
import sp.it.pl.plugin.impl.VoiceAssistant.SpeakContext
import sp.it.util.async.runVT
import sp.it.util.async.sleep
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Error
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.flatten
import sp.it.util.functional.net
import sp.it.util.system.Os
import sp.it.util.system.Windows
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.text.camelToSpaceCase
import sp.it.util.text.encodeBase64
import sp.it.util.text.equalsNc
import sp.it.util.text.words
import sp.it.util.units.uuid

typealias ComMatch = Try<String?, String?>?

fun SpeakContext.voiceCommandGenerate(text: String): ComMatch =
   if (matches(text)) {
      plugin.write("PASTE: " + ("Generate " + text).encodeBase64())
      Ok(null)
   } else
      null

fun SpeakContext.voiceCommandGenerateClipboard(text: String): ComMatch =
   if (matches(text)) {
      plugin.write("PASTE: " + ("Generate " + text + "\nClipboard:\n```" + (Clipboard.getSystemClipboard().string ?: "" + "```")).encodeBase64())
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
      Robot().apply {
         keyPress(KeyCode.ALT)
         keyPress(KeyCode.F4)
         keyRelease(KeyCode.F4)
         keyRelease(KeyCode.ALT)
      }
      Ok(null)
   } else
      null

fun SpeakContext.voiceCommandOpenWidget(text: String): ComMatch =
   if (text.startsWith("open")) {
      val fNameRaw = text.removePrefix("open").trimStart().removePrefix("widget").removeSuffix("widget").trim().camelToSpaceCase()
      val fName = plugin.commandWidgetNames.get(fNameRaw) ?: fNameRaw
      val f = APP.widgetManager.factories.getComponentFactories().find { it.name.camelToSpaceCase() equalsNc fName }
      if (f!=null) ComponentLoaderStrategy.DOCK.loader(f)
      if (f!=null) Ok("Ok") else Error("No widget $fNameRaw available.")
   } else {
      null
   }

fun SpeakContext.voiceCommandOsShutdown(text: String): ComMatch =
   if (matches(text) && Os.WINDOWS.isCurrent) Windows.shutdown().map { null }.mapError { it.localizedMessage } else null

fun SpeakContext.voiceCommandOsRestart(text: String): ComMatch =
   if (matches(text) && Os.WINDOWS.isCurrent) Windows.restart().map { null }.mapError { it.localizedMessage } else null

fun SpeakContext.voiceCommandOsSleep(text: String): ComMatch =
   if (matches(text) && Os.WINDOWS.isCurrent) Windows.sleep().map { null }.mapError { it.localizedMessage } else null

fun SpeakContext.voiceCommandOsHibernate(text: String): ComMatch =
   if (matches(text) && Os.WINDOWS.isCurrent) Windows.hibernate().map { null }.mapError { it.localizedMessage } else null

fun SpeakContext.voiceCommandOsLock(text: String): ComMatch =
   if (matches(text) && Os.WINDOWS.isCurrent) Windows.lock().map { null }.mapError { it.localizedMessage } else null

fun SpeakContext.voiceCommandOsLogOff(text: String): ComMatch =
   if (matches(text) && Os.WINDOWS.isCurrent) Windows.logOff().map { null }.mapError { it.localizedMessage } else null

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
         repeat(max(from, to) - min(from, to)) {
            sleep(1000)
            APP.plugins.get<VoiceAssistant>()?.speak("${(from + it)}")
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
         .map {
            if (it.startsWith("$")) ".*" else it
         }
         .map { p ->
            when {
               p.contains("|") && p.endsWith("?") -> p.rr().net { "(${it.split("|").joinToString("|") { it }})?" }
               p.endsWith("?") -> p.rr().net { "(${it})?" }
               p.contains("|") -> p.rr().net { "($it)" }
               else -> p.rr()
            }
         }
         .joinToString(" *").replace(" * *", " *").trim()
   }
)