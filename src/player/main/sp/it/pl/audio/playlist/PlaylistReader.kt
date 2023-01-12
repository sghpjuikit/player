package sp.it.pl.audio.playlist

import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8
import mu.KotlinLogging
import sp.it.pl.audio.SimpleSong
import sp.it.pl.audio.Song
import sp.it.pl.main.FileExtensions.m3u
import sp.it.pl.main.FileExtensions.m3u8
import org.jetbrains.annotations.Blocking
import sp.it.util.dev.fail
import sp.it.util.dev.failIfFxThread
import sp.it.util.file.div
import sp.it.util.file.hasExtension
import sp.it.util.functional.net
import sp.it.util.functional.runTry

private const val EXTM3U = "#EXTM3U"
private const val EXTINF = "#EXTINF"
private val logger = KotlinLogging.logger { }

@Blocking
fun readM3uPlaylist(file: File): List<Song> {
   failIfFxThread()

   val location = file.parentFile ?: fail { "File=$file is not a playlist file" }
   val encoding = when {
      file hasExtension m3u -> Charset.defaultCharset()
      file hasExtension m3u8 -> UTF_8
      else -> fail { "File=$file is not a supported playlist file" }
   }

   return file.useLines(encoding) { lines ->
      lines
         .filter { l -> !l.startsWith("#") && l.isNotEmpty() }
         .flatMap { l ->
            null
               ?: l.toAbsoluteURIOrNull()?.net { sequenceOf(SimpleSong(it)) }
               ?: File(l).absoluteTo(location)?.net {
                  if (it.isM3uPlaylist()) readM3uPlaylist(it).asSequence()
                  else sequenceOf(SimpleSong(it))
               }
               ?: sequenceOf()
         }
         .toList()
   }
}

@Blocking
fun writeM3uPlaylist(playlist: List<Song>, name: String, dir: File) {
   failIfFxThread()

   val file = dir/"$name.$m3u8"
   runTry {
      file.bufferedWriter(UTF_8).use { w ->
         playlist.forEach {
            w.appendLine(it.uri.toString())
         }
      }
   }.ifError {
      logger.error(it) { "Failed to write playlist file=$file" }
   }
}

fun File.isM3uPlaylist() = hasExtension(m3u, m3u8)

private fun File.absoluteTo(dir: File): File? =
   if (isAbsolute) {
      this
   } else {
      try {
         File(dir, this.path).canonicalFile
      } catch (e: IOException) {
         logger.error(e) { "Failed to resolve relative path=$this to location=$dir" }
         null
      }
   }

fun String.toAbsoluteURIOrNull() =
   try {
      URI(this).takeIf { it.isAbsolute }
   } catch (e: URISyntaxException) {
      null
   }