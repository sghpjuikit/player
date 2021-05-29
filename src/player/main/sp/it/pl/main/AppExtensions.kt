package sp.it.pl.main

import javafx.scene.text.Font
import mu.KotlinLogging
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.read
import sp.it.pl.layout.widget.ComponentFactory
import sp.it.pl.layout.widget.isExperimental
import sp.it.util.async.IO
import sp.it.util.async.future.Fut
import sp.it.util.async.runIO
import sp.it.util.async.runNew
import sp.it.util.dev.fail
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.file.Util.isValidFile
import sp.it.util.file.div
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.system.runAsProgram
import sp.it.util.units.toEM
import java.io.File
import java.io.InputStream
import java.lang.ProcessBuilder.Redirect.PIPE
import kotlin.math.ceil
import sp.it.pl.core.CoreConverter
import sp.it.util.async.FX
import sp.it.util.async.launch
import sp.it.util.async.runFX

private val logger = KotlinLogging.logger {}

/** @return whether user can use this factory, exactly: APP.developerMode || ![ComponentFactory.isExperimental] */
fun ComponentFactory<*>.isUsableByUser() = APP.developerMode.value || !isExperimental()

/** Returns value of this number of scaled [sp.it.util.units.EM]s, ceil-ed to nearest integer, where scaled EM is current application font size */
val Number.emScaled: Double get() = fontScaled(APP.ui.font.value)

/** Returns value of this number of scaled [sp.it.util.units.EM]s, ceil-ed to nearest integer, where scaled EM is font size */
fun Number.fontScaled(font: Font?): Double = ceil(toDouble()*(font?.size?.toEM() ?: 1.0))

/**
 * Checks validity of a file to be a skin. True return file means the file
 * can be used as a skin (the validity of the skin itself is not included).
 * For files returning false this application will not allow skin change.
 * Valid skin file checks out the following:
 * - not null
 * - isValidFile()
 * - is located in Skins folder set for this application
 * - is .css
 * - is located in its own folder with the same name
 * example: /Skins/MySkin/MySkin.css
 *
 * @return true if parameter is valid skin file. False otherwise or if null.
 */
fun File.isValidSkinFile() = isValidFile(this) && isSkinFile()

fun File.isSkinFile(): Boolean {
   val name = nameWithoutExtension
   return path.endsWith(".css") && this==APP.location.skins/name/"$name.css"
}

fun File.isValidWidgetFile() = isValidFile(this) && isWidgetFile()

fun File.isWidgetFile(): Boolean {
   return path.endsWith(".fxml") && parentFile?.parentFile==APP.location.widgets
}

/** @return true iff this song is [Song.same] as the song that is currently playing */
fun Song.isPlaying(): Boolean = same(APP.audio.playingSong.value)

/** @return metadata representation of this song (content will be read if it hasn't been read before, and may be outdated) */
fun Song.toMetadata(action: (Metadata) -> Unit) {
   failIfNotFxThread()

   when {
      this is Metadata -> {
         action(this)
      }
      same(APP.audio.playingSong.value) -> {
         action(APP.audio.playingSong.value)
      }
      else -> {
         APP.db.songsById[id]
            .ifNotNull { action(it) }
            .ifNull {
               runIO {
                  read()
               } ui {
                  action(it)
               }
            }
      }
   }

}

/** @return true iff any songs contained in this group [Song.isPlaying] */
fun MetadataGroup.isPlaying(): Boolean = field.getOf(APP.audio.playingSong.value)==value

/** @return result of [sp.it.pl.core.CoreConverter.ui].[sp.it.util.parsing.ConverterToString.toS] */
fun <T> T?.toUi(): String = CoreConverter.ui.toS(this)

/** @return result of [sp.it.pl.core.CoreConverter.general].[sp.it.util.parsing.ConverterToString.toS] */
fun <T> T?.toS(): String = CoreConverter.general.toS(this)

/** Runs the specified block immediately or when application is [initialized](App.onStarted) on [runFX]. */
fun App.run1AppReady(block: suspend () -> Unit) {
   FX.launch {
      if (isInitialized.isOk) {
         block()
      } else {
         onStarted += { run1AppReady(block) }
      }
   }
}

/** Invokes [File.runAsProgram] and if error occurs logs and reports using [AppEventLog]. Returns process result (stdout) or error. */
fun File.runAsAppProgram(actionName: String, vararg arguments: String, then: (ProcessBuilder) -> Unit = {}): Fut<String> {
   fun String?.wrap() = if (isNullOrBlank()) "" else "\n$this"
   fun doOnError(e: Throwable?, text: String?) {
      logger.error(e) { "$actionName failed.\n${text.wrap()}" }
      AppEventLog.push("$actionName failed.", text.wrap())
   }

   return runAsProgram(*arguments) {
      it.redirectOutput(PIPE).redirectError(PIPE).apply(then)
   }.onError(IO) {
      doOnError(it, it.message)
   }.then(IO) { p ->
      var stdout = ""
      var stderr = ""
      val stdoutListener = runNew(StreamGobbler(p.inputStream) { stdout = it.wrap() })
      val stderrListener = runNew(StreamGobbler(p.errorStream) { stderr = it.wrap() })
      val success = p.waitFor()
      stdoutListener.block()
      stderrListener.block()
      if (success!=0) doOnError(null, stdout + stderr)
      if (success!=0) fail { "Process failed and returned $success" }
      stdout
   }
}

private class StreamGobbler(private val inputStream: InputStream, private val consumeInputLine: (String) -> Unit): Runnable {
   override fun run() {
      inputStream.bufferedReader().readText().apply(consumeInputLine)
   }
}