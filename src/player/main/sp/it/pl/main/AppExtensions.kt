package sp.it.pl.main

import mu.KotlinLogging
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.layout.widget.ComponentFactory
import sp.it.pl.layout.widget.isExperimental
import sp.it.util.async.NEW
import sp.it.util.async.runNew
import sp.it.util.file.Util.isValidFile
import sp.it.util.file.div
import sp.it.util.file.nameWithoutExtensionOrRoot
import sp.it.util.system.runAsProgram
import sp.it.util.ui.EM
import java.io.File
import java.io.InputStream
import java.lang.ProcessBuilder.Redirect.PIPE
import kotlin.math.ceil

private val logger = KotlinLogging.logger {}

/** @return whether user can use this factory, exactly: APP.developerMode || ![ComponentFactory.isExperimental] */
fun ComponentFactory<*>.isUsableByUser() = APP.developerMode.value || !isExperimental()

/** @return value scaled by current application font size (in [EM] units) and ceil-ed to nearest integer */
fun Number.scaleEM() = ceil(toDouble()*APP.ui.font.value.size.EM)

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
fun File.isValidSkinFile(): Boolean {
   val name = nameWithoutExtensionOrRoot
   val skinFile = APP.location.skins/name/"$name.css"
   return isValidFile(this) && path.endsWith(".css") && this==skinFile
}

fun File.isValidWidgetFile(): Boolean {
   return isValidFile(this) && path.endsWith(".fxml") && parentFile?.parentFile==APP.location.widgets
}

/** @return true iff this song is [Song.same] as the song that is currently playing */
fun Song.isPlaying(): Boolean = same(APP.audio.playingSong.value)

/** @return true iff any songs contained in this group [Song.isPlaying] */
fun MetadataGroup.isPlaying(): Boolean = field.getOf(APP.audio.playingSong.value)==value // TODO: move out

/** Runs the specified block immediately or when application is [initialized](App.onStarted). */
fun App.run1AppReady(block: () -> Unit) {
   if (isInitialized.isOk) {
      block()
   } else {
      onStarted += { run1AppReady(block) }
   }
}

/** Invokes [File.runAsProgram] and if error occurs logs and reports using [AppErrors]. */
fun File.runAsAppProgram(actionName: String, vararg arguments: String, then: (ProcessBuilder) -> Unit = {}) {
   fun String?.wrap() = if (isNullOrBlank()) "" else "\n$this"
   fun doOnError(e: Throwable?, text: String?) {
      logger.error(e) { "$actionName failed.${text.wrap()}" }
      AppErrors.push("$actionName failed.${text.wrap()}")
   }

   runAsProgram(*arguments) {
      it.redirectOutput(PIPE).redirectError(PIPE).apply(then)
   }.onError(NEW) {
      doOnError(it, it.message)
   }.onOk(NEW) {
      it.ifError {
         doOnError(it, it.message)
      }
      it.ifOk { p ->
         var stdout = ""
         var stderr = ""
         runNew(StreamGobbler(p.inputStream) { stdout = it.wrap() })
         runNew(StreamGobbler(p.errorStream) { stderr = it.wrap() })
         val success = p.waitFor()
         if (success!=0)
            doOnError(null, stdout + stderr)  // TODO: handle with timer (report optionally both p.start failure and p.exit failure)
      }
   }
}

private class StreamGobbler(private val inputStream: InputStream, private val consumeInputLine: (String) -> Unit): Runnable {
   override fun run() {
      inputStream.bufferedReader().readText().apply(consumeInputLine)
   }
}