package sp.it.pl.audio.playlist

import mu.KotlinLogging
import sp.it.pl.audio.Item
import sp.it.pl.audio.SimpleItem
import sp.it.pl.util.dev.fail
import sp.it.pl.util.dev.throwIfFxThread
import sp.it.pl.util.file.div
import sp.it.pl.util.file.hasExtension
import sp.it.pl.util.file.parentDir
import sp.it.pl.util.functional.net
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.Charset

private const val EXTM3U = "#EXTM3U"
private const val EXTINF = "#EXTINF"
private val logger = KotlinLogging.logger { }

@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
fun readPlaylist(file: File): List<Item> {
    throwIfFxThread()

    val location = file.parentDir!!
    val encoding = when {
        file hasExtension "m3u" -> Charset.defaultCharset()
        file hasExtension "m3u8" -> Charsets.UTF_8
        else -> fail { "File=$file is not a supported playlist file" }
    }

    file.useLines(encoding) { lines ->
        var isFirstLine = true
        var isExtended = false
        return lines
                .onEach { if (isFirstLine && EXTM3U==it) isExtended = true }
                .onEach { isFirstLine = false }
                .filter { !it.startsWith("#") && !it.isEmpty() }
                .flatMap {
                    null
                        ?: it.toURIOrNull()?.net { sequenceOf(SimpleItem(it)) }
                        ?: File(it).absoluteTo(location)
                                ?.net {
                                    if (it.isPlaylistFile()) readPlaylist(it).asSequence()
                                    else sequenceOf(SimpleItem(it))
                                }
                                ?: sequenceOf()
                }
                .toList()
    }
}

fun writePlaylist(playlist: List<Item>, name: String, location: File) {
    val file = location/"$name.m3u8"
    file.bufferedWriter(Charsets.UTF_8).use { writer ->
        playlist.forEach {
            writer.appendln(it.uri.toString())
        }
    }
}

fun File.isPlaylistFile() = hasExtension("m3u", "m3u8")

private val workingDirPath = File("").absolutePath

private fun File.absoluteTo(location: File): File? {
    return if (isAbsolute) {
        this
    } else {
        try {
            File(absolutePath.replace(workingDirPath, location.path)).canonicalFile
        } catch (e: IOException) {
            logger.error(e) { "Failed to resolve relative path=$this to location=$location" }
            null
        }
    }
}

private fun String.toURIOrNull() =
        if (startsWith("file:") || startsWith("http:") || startsWith("https:")) {
            try {
                URI(this)
            } catch (e: URISyntaxException) {
                null
            }
        } else {
            null
        }