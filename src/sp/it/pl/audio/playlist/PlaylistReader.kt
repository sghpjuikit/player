package sp.it.pl.audio.playlist

import sp.it.pl.audio.Item
import sp.it.pl.audio.SimpleItem
import sp.it.pl.util.dev.fail
import sp.it.pl.util.file.div
import sp.it.pl.util.file.hasExtension
import sp.it.pl.util.file.parentDir
import sp.it.pl.util.functional.net
import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.Charset

@Suppress("UNUSED_VARIABLE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
fun readPlaylist(file: File): Playlist {

    val EXTM3U = "#EXTM3U"
    val EXTINF = "#EXTINF"
    val location = file.parentDir!!
    val encoding = when {
        file hasExtension "m3u" -> Charset.defaultCharset()
        file hasExtension "m3u8" -> Charsets.UTF_8
        else -> fail { "File=$file is not a supported playlist file" }
    }

    file.useLines(encoding) { lines ->
        var isFirstLine = true
        var isExtended = false
        val items = lines
                .onEach { if (isFirstLine && EXTM3U==it) isExtended=true }
                .onEach { isFirstLine = false }
                .filter { !it.startsWith("#") }
                .flatMap {
                    null
                        ?: it.toURIOrNull()
                                ?.net { SimpleItem(it) }?.net { sequenceOf(it) }
                        ?: File(it)
                                .net {
                                    if (it.isAbsolute) it
                                    else location/it.path
                                }
                                .net {
                                    if (it.isPlaylistFile()) readPlaylist(it).asSequence()
                                    else sequenceOf(SimpleItem(it))
                                }
                }
                .toList()
        return Playlist().apply { addItems(items) }
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

/** @return file denoting the resource of this uri or null if [IllegalArgumentException] is thrown */
private fun String.toURIOrNull() =
        try {
            URI(this)
        } catch (e: URISyntaxException) {
            null
        }