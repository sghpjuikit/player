package sp.it.pl.audio.playlist

import mu.KotlinLogging
import sp.it.pl.audio.SimpleSong
import sp.it.pl.audio.Song
import sp.it.util.dev.Blocks
import sp.it.util.dev.fail
import sp.it.util.dev.failIfFxThread
import sp.it.util.file.div
import sp.it.util.file.hasExtension
import sp.it.util.file.parentDir
import sp.it.util.functional.net
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.Charset

private const val EXTM3U = "#EXTM3U"
private const val EXTINF = "#EXTINF"
private val logger = KotlinLogging.logger { }

@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@Blocks
fun readPlaylist(file: File): List<Song> {
    failIfFxThread()

    val location = file.parentDir ?: fail { "File=$file is not a playlist file" }
    val encoding = when {
        file hasExtension "m3u" -> Charset.defaultCharset()
        file hasExtension "m3u8" -> Charsets.UTF_8
        else -> fail { "File=$file is not a supported playlist file" }
    }

    file.useLines(encoding) { lines ->
        return lines
            .filter { !it.startsWith("#") && !it.isEmpty() }
            .flatMap {
                null
                    ?: it.toURIOrNull()?.net { sequenceOf(SimpleSong(it)) }
                    ?: File(it).absoluteTo(location)
                            ?.net {
                                if (it.isPlaylistFile()) readPlaylist(it).asSequence()
                                else sequenceOf(SimpleSong(it))
                            }
                            ?: sequenceOf()
            }
            .toList()
    }
}

@Blocks
fun writePlaylist(playlist: List<Song>, name: String, dir: File) {
    failIfFxThread()

    val file = dir/"$name.m3u8"
    file.bufferedWriter(Charsets.UTF_8).use { writer ->
        playlist.forEach {
            writer.appendln(it.uri.toString())
        }
    }
}

fun File.isPlaylistFile() = hasExtension("m3u", "m3u8")

private fun File.absoluteTo(dir: File): File? {
    return if (isAbsolute) {
        this
    } else {
        try {
            File(dir, this.path).canonicalFile
        } catch (e: IOException) {
            logger.error(e) { "Failed to resolve relative path=$this to location=$dir" }
            null
        }
    }
}

private fun String.toURIOrNull() =
    try {
        URI(this).takeIf { it.isAbsolute }
    } catch (e: URISyntaxException) {
        null
    }