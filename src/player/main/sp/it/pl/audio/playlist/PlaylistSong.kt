package sp.it.pl.audio.playlist

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.media.Media
import javafx.util.Duration
import org.jaudiotagger.tag.FieldKey
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.AudioFileFormat
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.readAudioFile
import sp.it.pl.main.APP
import sp.it.util.access.fieldvalue.ObjectFieldBase
import sp.it.util.access.fieldvalue.ObjectFieldRegistry
import sp.it.util.async.runFX
import sp.it.util.dev.ThreadSafe
import sp.it.util.dev.failCase
import sp.it.util.dev.failIfFxThread
import sp.it.util.functional.orNull
import sp.it.util.identityHashCode
import sp.it.util.type.VType
import sp.it.util.type.type
import sp.it.util.units.toHMSMs
import java.net.URI
import sp.it.util.functional.net

/**
 * Song in playlist.
 *
 * Carries information:
 * * uri of the resource
 * * artist
 * * title
 * * length duration
 *
 * Cannot be changed, only updated. May be created updated, or ebe updated at a later time.
 */
class PlaylistSong: Song {

   val uriP: SimpleObjectProperty<URI>
   override val uri: URI get() = uriP.get()

   private var artist: String? = null
   private var title: String? = null

   val nameP: SimpleStringProperty
   val name: String get() = nameP.get()

   val timeP: SimpleObjectProperty<Duration>
   val time: Duration get() = timeP.get()

   /**
    * Returns true if the item was marked updated. Once item is updated it will stay in that state. Updated item
    * guarantees that all its values are valid, but does not guarantee that they are up-to-date. For manipulation
    * within the application there should be no need to update the item again. If the item changes, the change should
    * be handled by the application.
    *
    * If false, update() can be called.
    */
   @Volatile var isUpdated: Boolean = false
      private set
   /**
    * Returns true if this item was marked corrupt last time it was checked to be corrupted. This doesn't necessarily
    * reflect the real value, instead returns cached value to avoid i/o when performance is critical, e.g., in tables.
    *
    * If the validity of the check is prioritized, use [.isCorrupt].
    *
    * @return cached corrupted value
    */
   @Volatile var isCorruptCached = false
      internal set

   /** New not updated item */
   constructor(_uri: URI) {
      uriP = SimpleObjectProperty(_uri)
      nameP = SimpleStringProperty(getInitialName())
      timeP = SimpleObjectProperty(Duration(0.0))
      isUpdated = false
   }

   /** New updated item. */
   constructor(new_uri: URI, _artist: String?, _title: String?, _length: Double) {
      uriP = SimpleObjectProperty(new_uri)
      nameP = SimpleStringProperty()
      timeP = SimpleObjectProperty(Duration(_length))
      setATN(_artist, _title)
      isUpdated = true
   }

   /** @return the artist portion of the name. Empty string if item wasn't updated yet */
   fun getArtist() = artist ?: ""

   /** @return the title portion of the name. Empty string if item wasn't updated yet */
   fun getTitle() = title ?: ""

   /** @return the time in milliseconds or 0 if item wasn't updated yet */
   val timeMs: Double get() = timeP.get().toMillis()

   /**
    * Updates this item by reading the tag of the source file.
    * Involves I/O, so don't use on main thread. Safe to call from bgr thread.
    *
    * Calling this method on updated playlist item has no effect. E.g.:
    *  *  calling this method more than once
    *  *  calling this method on playlist item created from metadata
    *
    * note: `this.toMeta().toPlaylist()` effectively
    * prevents not updated songs from ever updating. Never use {@link AppActions#toMeta} where full
    * metadata object is required.
    */
   @ThreadSafe
   fun update() {
      if (isUpdated || isCorrupt()) return
      isUpdated = true

      // if library contains the item, use it & avoid I/O, improves performance 100-fold when song is in library
      val m = APP.db.songsById[id]
      if (m!=null) {
         runFX {
            update(m)
         }
         return
      }

      if (isFileBased()) {
         failIfFxThread()
         getFile()!!.readAudioFile().orNull()?.let { f ->
            val t = f.tag ?: null
            val h = f.audioHeader

            val length = (1000*h.trackLength).toDouble()
            val artist = t?.getFirst(FieldKey.ARTIST)
            val title = t?.getFirst(FieldKey.TITLE)

            runFX {
               setATN(artist, title)
               timeP.set(Duration(length))
            }
         }
      } else {
         try {
            val media = Media(uri.toString())
            setATN("", "")
            timeP.value = media.duration
         } catch (e: IllegalArgumentException) {
            isCorruptCached = true   // mark as corrupted on error
         } catch (e: NullPointerException) {
            isCorruptCached = true
         } catch (e: UnsupportedOperationException) {
            isCorruptCached = true
         }
      }
   }

   /** Updates this playlist item to data from specified metadata (involves no i/o). */
   fun update(m: Metadata) {
      uriP.set(m.uri)
      setATN(m.getArtist(), m.getTitle())
      timeP.set(m.getLength())
      isUpdated = true
   }

   private fun setATN(artist: String?, title: String?) {
      this.artist = artist
      this.title = title.takeUnless { it.isNullOrBlank() } ?: uri.path.substringAfterLast(".")
      this.nameP.set("${artist.orEmpty()} - ${title.orEmpty()}")
   }

   /** @return true if this item is corrupted */
   override fun isCorrupt(): Boolean {
      isCorruptCached = super.isCorrupt()
      return isCorruptCached
   }

   /** @return this */
   override fun toPlaylist() = this

   /** @return true iff this is the same object as the other, same as using === */
   override fun equals(other: Any?) = this===other

   override fun hashCode() = identityHashCode()

   override fun toString() = "$name\n$uri\n${time.toHMSMs()}"

   /** @return deep copy of this item */
   fun copy() = PlaylistSong(uri, artist, title, timeMs).also {
      it.isUpdated = isUpdated
      it.isCorruptCached = isCorruptCached
   }

   class Field<T>: ObjectFieldBase<PlaylistSong, T> {

      private constructor(type: VType<T>, name: String, description: String, extractor: (PlaylistSong) -> T): super(type, extractor, name, description)

      override fun toS(o: T?, substitute: String): String {
         return when (this) {
            NAME, TITLE, ARTIST -> if (""==o) substitute else o.toString()
            PATH, FORMAT -> o.toString()
            LENGTH -> (o as Duration).toHMSMs()
            else -> failCase(this)
         }
      }

      override fun cWidth(): Double = when (this) {
         NAME -> 400.0
         TITLE -> 250.0
         ARTIST -> 150.0
         LENGTH -> 60.0
         PATH -> 400.0
         FORMAT -> 60.0
         else -> 60.0
      }

      override fun cVisible(): Boolean = this===NAME || this===LENGTH

      @Suppress("RemoveExplicitTypeArguments")
      companion object: ObjectFieldRegistry<PlaylistSong, Field<*>>(PlaylistSong::class) {
         @JvmField val NAME = this + Field(type<String>(), "Name", "'Song artist' - 'Song title'") { it.name }
         @JvmField val TITLE = this + Field(type<String>(), "Title", "Song title") { it.title }
         @JvmField val ARTIST = this + Field(type<String>(), "Artist", "Song artist") { it.artist }
         @JvmField val LENGTH = this + Field(type<Duration>(), "Time", "Song length") { it.time }
         @JvmField val PATH = this + Field(type<String>(), "Path", "Song file path") { it.getPathAsString() }
         @JvmField val FORMAT = this + Field(type<AudioFileFormat>(), "Format", "Song file type") { it.getFormat() }
      }

   }

}

