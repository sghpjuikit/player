package sp.it.pl.audio

import java.io.File
import java.net.URI
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.tagging.AudioFileFormat
import sp.it.pl.audio.tagging.Metadata
import sp.it.util.file.parentDirOrRoot
import sp.it.util.file.toFileOrNull
import sp.it.util.functional.net
import sp.it.util.units.FileSize

/** Representation of audio resource based on [URI]. */
abstract class Song {

   /** @return URI of the audio resource this song represents */
   abstract val uri: URI

   /** @return internal id of this song */
   open val id get() = uri.toString()

   /** @return true iff this song represents a file on a local system, false indicates external resource (e.g. http) */
   fun isFileBased(): Boolean = "file"==uri.scheme

   /**
    * @return absolute file this song represents or null if it is not based on file
    */
   open fun getFile(): File? = if (isFileBased()) uri.toFileOrNull()!!.absoluteFile else null

   /** @return human-readable path of the resource this song represents */
   open fun getPathAsString(): String {
      val path = uri.path
      return if (path.isNullOrBlank()) "" else uri.path.substring(1)
   }

   /**
    * Parent directory of the resource. Only for file based songs.
    *
    * @return parent directory of the song in the file system or null if it is not based on file
    */
   fun getLocation(): File? = getFile()?.parentDirOrRoot

   /** @return human-readable location of the resource this song represents */
   fun getLocationAsString(): String = getLocation()?.net { it.path } ?: getPathAsString()

   /** @return filename without suffix or empty string if none */
   open fun getFilename(): String = getFilenameFull().substringBeforeLast(".")

   /** @return filename with suffix or empty string if none */
   fun getFilenameFull(): String {
      val p = uri.path
      if (p==null || p.isEmpty()) return ""
      val i = p.lastIndexOf('/')
      return if (i==-1 || p.length<2) p else p.substring(i + 1)
   }

   /** @return the suffix of the resource of this song or empty string if none, e.g.: mp3 */
   fun getSuffix() = uri.path?.substringAfterLast('.', "") ?: ""

   /**
    * @return file format of this song as recognized by the application. It can differ from simple suffix string. This
    * is recommended way to obtain type of file as it utilizes application's built-in mechanism.
    */
   open fun getFormat(): AudioFileFormat = AudioFileFormat.of(uri)

   /**
    * Returns filesize of the file resource of this song. The filesize will remain unknown if unable to determine.
    *
    * @return the filesize of this song
    */
   open fun getFileSize(): FileSize = getFile()?.let { FileSize(it) } ?: FileSize(0)

   /**
    * Returns initial name. Name derived purely from URI of the song.
    *
    * Name can denote an song such as PlaylistSong.
    *
    * Use as an initialization value when only URI is known about the song and
    * more user-friendly information is desired than the raw uri.
    *
    * Default implementation is equivalent to [getFilename]
    *
    * @return initial name of the song.
    */
   fun getInitialName(): String = getFilename()

   /**
    * Checks whether the song can be played. Only non corrupted songs can be played.
    *
    * Song is labeled corrupt iff it fulfills at any of the conditions for file based songs:
    * * file does not exist
    * * file is not a file (is a directory)
    * * file is not readable
    *
    * @return playability/validity of the song
    */
   open fun isCorrupt(): Boolean = getFile()?.net { !it.isFile || !it.exists() || !it.canRead() } ?: false

   /** @return true iff the URIs of the songs are equal */
   fun same(i: Song?): Boolean = i!=null && i.uri==uri

   /** @return true iff the provided uri equals to that of this song */
   fun same(r: URI?): Boolean = r!=null && r==uri

   /** @return metadata representation of this song (without reading it explicitly, so content may be missing) */
   open fun toMeta() = Metadata(this)

   /** @return playlist song representation of this song */
   open fun toPlaylist() = PlaylistSong(uri)

   /** @return simple song representation this song */
   open fun toSimple() = SimpleSong(uri)

   companion object

}