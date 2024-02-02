package sp.it.pl.plugin.impl

import java.time.LocalDate
import java.time.LocalTime
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode
import javafx.scene.robot.Robot
import sp.it.pl.layout.ComponentLoaderStrategy
import sp.it.pl.main.APP
import sp.it.pl.plugin.impl.VoiceAssistant.SpeakContext
import sp.it.pl.plugin.impl.VoiceAssistant.SpeakHandler
import sp.it.pl.voice.toVoiceS
import sp.it.util.dev.printIt
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.net
import sp.it.util.system.Os
import sp.it.util.system.Windows
import sp.it.util.text.camelToSpaceCase
import sp.it.util.text.encodeBase64
import sp.it.util.text.equalsNc

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
      if (f!=null) Ok("Ok") else Try.Error("No widget $fNameRaw available.")
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

fun voiceCommandRegex(commandUi: String) = Regex(
   commandUi.net { it ->
      val parts = it.split(" ")
      val opts = (parts.mapIndexed { i, p -> i to p.endsWith("?") } + (-1 to true) + (parts.size to true)).toMap()
      fun opt(i: Int) = opts[i]!!
      fun String.ss(i: Int) = this
      fun String.rr() = replace("(", "").replace(")", "").replace("?", "")
      parts
         // resolve params
         .map {
            if (it.startsWith("$")) ".*"
            else it
         }
         .mapIndexed { i, p ->
            when {
               p.contains("|") && p.endsWith("?") -> p.rr().net { "(${it.split("|").joinToString("|") { it.ss(i) }})?" }
               p.endsWith("?") -> p.rr().net { "(${it.ss(i)})?" }
               p.contains("|") -> p.rr().net { "($it)".ss(i) }
               else -> p.rr().ss(i)
            }
         }
         .joinToString(" *").replace(" * *", " *").trim()
   }
)