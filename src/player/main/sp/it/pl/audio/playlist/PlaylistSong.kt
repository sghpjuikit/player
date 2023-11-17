package sp.it.pl.audio.playlist

import io.ktor.client.request.head
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.media.Media
import javafx.util.Duration
import org.jaudiotagger.tag.FieldKey
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.readAudioFile
import sp.it.pl.main.APP
import sp.it.util.access.fieldvalue.ObjectFieldBase
import sp.it.util.access.fieldvalue.ObjectFieldRegistry
import sp.it.util.async.runFX
import sp.it.util.dev.ThreadSafe
import sp.it.util.dev.failIfFxThread
import sp.it.util.functional.orNull
import sp.it.util.identityHashCode
import sp.it.util.type.VType
import sp.it.util.type.type
import sp.it.util.units.toHMSMs
import java.net.URI
import javafx.collections.MapChangeListener
import javafx.collections.ObservableMap
import kotlinx.coroutines.runBlocking
import sp.it.pl.audio.tagging.AudioFileFormat
import sp.it.pl.core.CoreConverter
import sp.it.pl.main.AppHttp.Companion.isFromSpitPlayer
import sp.it.pl.main.isAudio
import sp.it.pl.main.isVideo
import sp.it.pl.main.toUi
import sp.it.util.async.coroutine.IO
import sp.it.util.async.coroutine.VT
import sp.it.util.async.runLater
import sp.it.util.dev.failIf
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.functional.asIf
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.reactive.onItemSync
import sp.it.util.units.durationOfHMSMs
import sp.it.util.units.millis

/**
 * Song in playlist.
 *
 * Carries information:
 * * uri of the resource
 * * artist
 * * title
 * * length duration
 *
 * Cannot be changed, only [update]. May be created updated, or be updated at a later time.
 */
class PlaylistSong: Song {

   private var uriP: URI? = null
   override val uri: URI get() = uriP!!

   private var artist: String? = null
   private var title: String? = null

   val nameP: SimpleStringProperty
   val name: String get() = nameP.get()

   /** The time property. Contains null if song wasn't [update] yet */
   val timeP: SimpleObjectProperty<Duration?>
   /** The time or null if song wasn't [update] yet */
   val time: Duration? get() = timeP.get()
   /** The time in milliseconds or null if song wasn't update yet */
   val timeMs: Double? get() = timeP.get()?.toMillis()

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
      private set

   /** New not updated item */
   constructor(_uri: URI) {
      uriP = _uri
      nameP = SimpleStringProperty(getInitialName())
      timeP = SimpleObjectProperty(null)
      isUpdated = false
   }

   /** New updated item. */
   constructor(new_uri: URI, _artist: String?, _title: String?, _length: Double?) {
      uriP = new_uri
      nameP = SimpleStringProperty()
      timeP = SimpleObjectProperty()
      setATN(_artist, _title, _length?.net { Duration(it) })
      isUpdated = true
   }

   /** @return the artist portion of the name. Empty string if song wasn't [update] yet */
   fun getArtist() = artist ?: ""

   /** @return the title portion of the name. Empty string if song wasn't [update] yet */
   fun getTitle() = title ?: ""

   /**
    * Updates this song by reading the tag of the source file.
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

      // if library contains the song, use it & avoid I/O, improves performance 100-fold when song is in library
      val m = APP.db.songsById[id]
      if (m!=null) {
         runFX {
            update(m)
         }
         return
      }

      if (isFileBased()) {
         failIfFxThread()
         val f = getFile()!!
         when {
            f.isAudio() -> {
               f.readAudioFile().orNull()?.let { af ->
                  val t = af.tag ?: null
                  val h = af.audioHeader
                  val length = (1000*h.trackLength).toDouble()
                  val artist = t?.getFirst(FieldKey.ARTIST)
                  val title = t?.getFirst(FieldKey.TITLE)
                  setATN(artist, title, Duration(length))
               }
            }
            f.isVideo() ->
               setATN(null, null, null)
         }
      } else if (isHttpBased()) {
         runBlocking(VT) {
            val r = APP.http.client.head(uri.toString()) { }
            val isSpitPlayer = r.isFromSpitPlayer()
            if (isSpitPlayer) {
               setATN(
                  r.headers["Spit-Song-${Metadata.Field.ARTIST.name()}"]?.let { CoreConverter.general.ofS(Metadata.Field.ARTIST.type, it).orNull() },
                  r.headers["Spit-Song-${Metadata.Field.TITLE.name()}"]?.let { CoreConverter.general.ofS(Metadata.Field.TITLE.type, it).orNull() },
                  r.headers["Spit-Song-${Metadata.Field.LENGTH.name()}"]?.let { CoreConverter.general.ofS(Metadata.Field.LENGTH.type, it).orNull() },
               )
            } else {
               runFX {
                  runTry {
                     fun Duration.known() = takeIf { it!=Duration.UNKNOWN }
                     val media = Media(uri.toString())
                     setATN(
                        media.metadata["artist"]?.asIf<String>(),
                        media.metadata["title"]?.asIf<String>(),
                        media.metadata["duration"]?.asIf<Duration>()?.known() ?: media.duration?.known(),
                     )
                     media.onError = Runnable {
                        isCorruptCached = true
                     }
                     media.metadata.addListener(
                        object: MapChangeListener<String, Any?> {
                           override fun onChanged(change: MapChangeListener.Change<out String, out Any?>) {
                              media.metadata.removeListener(this)
                              runLater {
                                 setATN(
                                    media.metadata["artist"]?.asIf<String>(),
                                    media.metadata["title"]?.asIf<String>(),
                                    media.metadata["duration"]?.asIf<Duration>()?.known() ?: media.duration?.known(),
                                 )
                              }
                           }
                        }
                     )
                  }.ifError {
                     isCorruptCached = true
                  }
               }
            }
         }
      } else
         setATN(null, null, null)
   }

   /** Updates this song to data from specified metadata */
   fun update(m: Metadata) {
      failIf(uriP!=m.uri) { "Update of $uriP failed, because Metadata uri is ${m.uri}" }
      val f = getFile()
      if (f==null || f.isAudio()) {
         setATN(m.getArtist(), m.getTitle(), m.getLength())
         isUpdated = true
      }
   }

   /** Updates this song time to specified data if the time is still null */
   fun updateTime(time: Duration) {
      failIfNotFxThread()
      if (timeP.value==null) timeP.value = time
   }

   private fun setATN(artist: String?, title: String?, duration: Duration?) {
      runFX {
         this.artist = artist
         this.title = title.takeUnless { it.isNullOrBlank() } ?: uri.path.substringAfterLast("/").substringBeforeLast(".")
         this.nameP.value = listOfNotNull(this.artist, this.title).joinToString(" - ")
         this.timeP.value = duration ?: this.timeP.value
      }
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

   override fun toString() = listOfNotNull(name, uri, time?.toHMSMs()).joinToString("\n")

   /** @return deep copy of this item */
   fun copy() = PlaylistSong(uri, artist, title, timeMs).also {
      it.isUpdated = isUpdated
      it.isCorruptCached = isCorruptCached
   }

   sealed class Field<T>: ObjectFieldBase<PlaylistSong, T> {

      private constructor(name: String, description: String, type: VType<T>, extractor: (PlaylistSong) -> T, toUi: (T?, String) -> String): super(type, extractor, name, description, toUi)

      override fun cWidth(): Double = when (this) { NAME -> 400.0; TITLE -> 250.0; ARTIST -> 150.0; LENGTH -> 60.0; PATH -> 400.0; FORMAT -> 60.0 }

      override fun cVisible(): Boolean = this===NAME || this===LENGTH

      object NAME: Field<String>("Name", "'Song artist' - 'Song title'", type(), { it.name }, { o, or -> if (o=="" || o==null) or else o.toUi() })
      object TITLE: Field<String?>("Title", "Song title", type(), { it.title }, { o, or -> if (o=="" || o==null) or else o.toUi() })
      object ARTIST: Field<String?>("Artist", "Song artist", type(), { it.artist }, { o, or -> if (o=="" || o==null) or else o.toUi() })
      object LENGTH: Field<Duration?>("Time", "Song length", type(), { it.time }, { o, or -> o?.toHMSMs(false) ?: or })
      object PATH: Field<String>("Path", "Song file path", type(), { it.getPathAsString() }, { o, or -> o?.toUi() ?: or })
      object FORMAT: Field<AudioFileFormat>("Format", "Song file type", type(), { it.getFormat() }, { o, or -> o?.toUi() ?: or })

      companion object: ObjectFieldRegistry<PlaylistSong, Field<*>>(PlaylistSong::class) {
         init { registerDeclared() }
      }

   }

}

