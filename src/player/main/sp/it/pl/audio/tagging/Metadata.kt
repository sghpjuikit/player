package sp.it.pl.audio.tagging

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.Serializable
import java.net.URI
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.Year
import javafx.scene.paint.Color
import javafx.util.Duration
import kotlin.math.pow
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.KeyNotFoundException
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.id3.AbstractID3Tag
import org.jaudiotagger.tag.id3.AbstractID3v1Tag
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.ID3v24Frames
import org.jaudiotagger.tag.id3.framebody.FrameBodyPOPM
import org.jaudiotagger.tag.images.Artwork
import org.jaudiotagger.tag.mp4.Mp4FieldKey
import org.jaudiotagger.tag.mp4.Mp4Tag
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
import org.jaudiotagger.tag.wav.WavTag
import org.jetbrains.annotations.Blocking
import sp.it.pl.audio.SimpleSong
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.tagging.Chapter.Companion.chapter
import sp.it.pl.audio.tagging.Metadata.Companion.EMPTY
import sp.it.pl.audio.tagging.Metadata.Field
import sp.it.pl.main.APP
import sp.it.pl.main.isImage
import sp.it.pl.ui.objects.image.ArtworkCover
import sp.it.pl.ui.objects.image.Cover
import sp.it.pl.ui.objects.image.Cover.CoverSource
import sp.it.pl.ui.objects.image.EmptyCover
import sp.it.pl.ui.objects.image.FileCover
import sp.it.util.access.fieldvalue.ObjectFieldBase
import sp.it.util.access.fieldvalue.ObjectFieldRegistry
import sp.it.util.dev.failCase
import sp.it.util.dev.failIfFxThread
import sp.it.util.file.children
import sp.it.util.file.parentDirOrRoot
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Companion.ok
import sp.it.util.functional.andAlso
import sp.it.util.functional.asIs
import sp.it.util.functional.getOr
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.localDateTimeFromMillis
import sp.it.util.text.StringSeq
import sp.it.util.text.splitNoEmpty
import sp.it.util.text.toStringSeq
import sp.it.util.type.VType
import sp.it.util.type.isSubclassOf
import sp.it.util.type.raw
import sp.it.util.type.type
import sp.it.util.units.Bitrate
import sp.it.util.units.FileSize
import sp.it.util.units.FileSize.Companion.sizeInBytes
import sp.it.util.units.NofX
import sp.it.util.units.toHMSMs
import sp.it.util.units.uri

/**
 * Information about audio file, usually from audio file tag and header.
 *
 * The class is practically immutable and does not provide any setters, nor
 * allows updating of its state or any of its values.
 *
 * Metadata can be empty and [EMPTY] may be used instead of null.
 *
 * To access any field in a generic way, see [Field] and [getField].
 */
class Metadata: Song, Serializable {

   // file fields

   override val uri: URI get() = uri(id.replace(" ", "%20"))

   override lateinit var id: String

   /** File size in bytes or -1 if unknown */
   private var fileSizeInB: Long = -1

   // header fields

   /** Encoding type, e.g.: MPEG-1 Layer 3 */
   private var encodingType: String? = null

   /** Bitrate or 0 if unknown */
   private var bitrate: Int = 0

   /** Encoder or empty String if not available */
   private var encoder: String? = null

   /** Channels, e.g.: Stereo */
   private var channels: String? = null

   /** Sample rate, e.g.: 44100 */
   private var sampleRate: String? = null

   /** Length in milliseconds or 0 if unknown */
   private var lengthInMs = 0.0

   // tag fields

   /** Title or null if none */
   private var title: String? = null

   /** Album or null if none */
   private var album: String? = null

   /** First Artist or null if none */
   private var artist: String? = null

   /** Album artist or null if none */
   private var albumArtist: String? = null

   /** Composer or null if none */
   private var composer: String? = null

   /** Publisher or null if none */
   private var publisher: String? = null

   /** Track number or null if none */
   private var track: Int? = null

   /** Total number of tracks on album or null if none */
   private var tracksTotal: Int? = null

   /** Disc number or null if none */
   private var disc: Int? = null

   /** Total number of discs on album or null if none */
   private var discsTotal: Int? = null

   /** Genre or null if none */
   private var genre: String? = null

   /** Year as int or null if none */
   private var yearAsInt: Int? = null

   /** Raw rating or null if none */
   private var rating: Int? = null

   /** Maximal raw rating value */
   private var ratingMax: Int = 100

   /** Number of times this has been played or null if none */
   private var playcount: Int? = null

   /** Category or null if none */
   private var category: String? = null

   /** Comment or null if none */
   private var comment: String? = null

   /** Lyrics or null if none */
   private var lyrics: String? = null

   /** Mood or null if none */
   private var mood: String? = null

   /** Custom 1 or null if empty */
   private var custom1: String? = null

   /** Custom 2 or null if empty */
   private var custom2: String? = null

   /** Custom 3 or null if empty */
   private var custom3: String? = null

   /** Custom 4 or null if empty */
   private var custom4: String? = null

   /** Custom 5 or null if empty */
   private var custom5: String? = null

   /** Musicbrainz field MUSICBRAINZ_ARTISTID or null if empty */
   private var mbArtistId: String? = null

   /** Musicbrainz field MUSICBRAINZ_TRACK_ID or null if empty */
   private var mbTrackId: String? = null

   /** Musicbrainz field MUSICBRAINZ_DISC_ID or null if empty */
   private var mbDiscId: String? = null

   /** Musicbrainz field MUSICBRAINZ_RELEASEID or null if empty */
   private var mbReleaseId: String? = null

   /** Musicbrainz field MUSICBRAINZ_RELEASEARTISTID or null if empty */
   private var mbReleaseArtistId: String? = null

   // synthetic fields

   /** Color as string or null if none */
   private var color: String? = null

   /** Tags joined into a string or null if none */
   private var tags: String? = null

   /** Time this song was first played as string or null if none */
   private var playedFirst: String? = null

   /** Time this song was last played as string or null if none */
   private var playedLast: String? = null

   /** Time this song was added to library as string or null if none */
   private var libraryAdded: String? = null

   /** Creates metadata from a song, attempts to use as much data available, no i/o. */
   constructor(song: Song) {
      id = song.uri.toString()
      if (song is PlaylistSong) {
         artist = song.getArtist().takeIf { it.isNotBlank() }
         lengthInMs = song.time?.toMillis() ?: 0.0
         title = song.getTitle().takeIf { it.isNotBlank() }
      }
   }

   /** Creates metadata by reading the file. */
   internal constructor(file: AudioFile) {
      file.file.absoluteFile.loadFileFields()
      file.loadHeaderFields()
      file.tagOrCreateAndSetDefault.loadTagFieldAll()
   }

   private fun File.loadFileFields() {
      id = toURI().toString()
      fileSizeInB = sizeInBytes()
   }

   private fun AudioFile.loadHeaderFields() {
      val header = this.audioHeader
      bitrate = runTry { header.bitRateAsNumber.toInt() }.getOr(0)*(if (header.isVariableBitRate) -1 else 1) // TODO: header.bitRateAsNumber can throw NullPointerException for opus files
      lengthInMs = (1000*header.trackLength).toDouble()
      encodingType = header.format.orNull()   // format and encoding type are switched in jaudiotagger library
      channels = header.channels.orNull()
      sampleRate = header.sampleRate.orNull()
   }

   private fun Tag.loadTagFieldAll() {
      loadTagFieldsGeneral()
      when (this) {
         is AbstractID3Tag -> loadTagFields()
         is FlacTag -> vorbisCommentTag.loadTagFields()
         is VorbisCommentTag -> loadTagFields()
         is WavTag -> loadTagFields()
         is Mp4Tag -> loadTagFields()
      }
      this@Metadata.ratingMax = ratingMax
   }

   private fun Tag.loadTagFieldsGeneral() {
      encoder = loadAsString(this, FieldKey.ENCODER)
      title = loadAsString(this, FieldKey.TITLE)
      album = loadAsString(this, FieldKey.ALBUM)
      artist = loadAsString(this, FieldKey.ARTIST)
      albumArtist = loadAsString(this, FieldKey.ALBUM_ARTIST)
      composer = loadAsString(this, FieldKey.COMPOSER)

      // track
      val tr = loadAsString(this, FieldKey.TRACK)
      if (tr!=null) {
         if ('/' in tr) {
            // some apps use TRACK for "x/y" string format, we cover that
            track = tr.substringBefore('/').toIntOrNull()
            tracksTotal = tr.substringAfter('/').toIntOrNull()
         } else {
            track = tr.toIntOrNull()
            tracksTotal = loadAsInt(this, FieldKey.TRACK_TOTAL)
         }
      }

      // disc
      val dr = loadAsString(this, FieldKey.DISC_NO)
      if (dr!=null) {
         if ('/' in dr) {
            // some apps use DISC_NO for "x/y" string format, we cover that
            disc = dr.substringBefore('/').toIntOrNull()
            discsTotal = dr.substringAfter('/').toIntOrNull()
         } else {
            disc = dr.toIntOrNull()
            discsTotal = loadAsInt(this, FieldKey.DISC_TOTAL)
         }
      }

      playcount = loadAsInt(this, FieldKey.CUSTOM3)
      genre = loadAsString(this, FieldKey.GENRE)
      yearAsInt = loadAsInt(this, FieldKey.YEAR)
      category = loadAsString(this, FieldKey.GROUPING)
      comment = loadComment(this)
      lyrics = loadAsString(this, FieldKey.LYRICS)
      mood = loadAsString(this, FieldKey.MOOD)
      custom1 = loadAsString(this, FieldKey.CUSTOM1)
      custom2 = loadAsString(this, FieldKey.CUSTOM2)
      custom3 = loadAsString(this, FieldKey.CUSTOM3)
      custom4 = loadAsString(this, FieldKey.CUSTOM4)
      custom5 = loadAsString(this, FieldKey.CUSTOM5)
      mbArtistId = loadAsString(this, FieldKey.MUSICBRAINZ_ARTISTID)
      mbTrackId = loadAsString(this, FieldKey.MUSICBRAINZ_TRACK_ID)
      mbDiscId = loadAsString(this, FieldKey.MUSICBRAINZ_DISC_ID)
      mbReleaseId = loadAsString(this, FieldKey.MUSICBRAINZ_RELEASEID)
      mbReleaseArtistId = loadAsString(this, FieldKey.MUSICBRAINZ_RELEASEARTISTID)

      // Read synthetic fields
      if (!custom5.isNullOrBlank()) {
         for (tagField in custom5?.split(SEPARATOR_GROUP.toString().toRegex()).orEmpty()) {
            if (tagField.length<10) continue      // skip deformed to avoid exception
            val tagId = tagField.substring(0, 10)
            val tagValue = tagField.substring(10)
            when (tagId) {
               TAG_ID_PLAYED_FIRST -> playedFirst = tagValue
               TAG_ID_PLAYED_LAST -> playedLast = tagValue
               TAG_ID_LIB_ADDED -> libraryAdded = tagValue
               TAG_ID_COLOR -> color = tagValue
               TAG_ID_TAGS -> tags = tagValue
            }
         }
      }
   }

   // There are three types of fields in terms of  how they are stored/read:
   // tag agnostic - jaudiotagger handles the field transparently in a tag independent way
   // tag specific - we have to handle the field manually per tag type, using jaudiotagger
   // special - Custom fields just for this application. They are all aggregated in a single
   //           field, but that is an implementation detail (This works similar to a vorbis
   //           tag, where each field has a string id and there is no limit to order,
   //           multiplicity or content of the fields).

   // Following methods acquire data from tag type specific fields
   // RATING - Each tag handles rating differently, simply read it
   // PLAYCOUNT - We store playcount separately, but some tag types support it, so we check it
   //             and use it if we have no data on our own
   // PUBLISHER - Each tag handles it differently, simply read it
   // CATEGORY - Each tag handles it differently, simply read it

   private fun AbstractID3Tag.loadTagFields() {
      if (this is AbstractID3v1Tag) {
         // RATING + PLAYCOUNT +PUBLISHER + CATEGORY ----------------------------
         // id3 has no fields for this (note that when we write these fields we convert tag to
         // ID3v2, so this only happens with untagged songs, which we don't care about)
      } else if (this is AbstractID3v2Tag) {
         // RATING + PLAYCOUNT --------------------------------------------------
         // we use POPM field (rating + counter + mail/user)
         val frame1 = this.getFirstField(ID3v24Frames.FRAME_ID_POPULARIMETER)
         // if not present we get null and leave default values
         if (frame1!=null) {
            // we obtain body for the field
            val body1 = frame1.body as? FrameBodyPOPM
            // once again the body of the field might not be there
            if (body1!=null) {
               val rat = body1.rating //returns null if empty
               val cou = body1.counter //returns null if empty

               // Do not know why the values themselves are Long, but we only need int
               // both for rating and playcount.
               // all is good until the tag is actually damaged and the int can really
               // overflow during conversion, and we get ArithmeticException,
               // so we catch it and ignore the value
               if (rating==null) {
                  try {
                     rating = Math.toIntExact(rat)
                  } catch (ignored: ArithmeticException) {
                  }
               }

               try {
                  val pc = Math.toIntExact(cou)
                  if (playcount==null || pc>playcount!!) playcount = pc
               } catch (ignored: ArithmeticException) {
               }
            }
         }

         if (playcount==null)
            playcount = this.getFirst(ID3v24Frames.FRAME_ID_PLAY_COUNTER).orNull()?.toIntOrNull()

         // PUBLISHER -----------------------------------------------------------
         if (publisher==null)
            publisher = this.getFirst(ID3v24Frames.FRAME_ID_PUBLISHER).orNull()

         // CATEGORY ------------------------------------------------------------
         // general reading is good enough
      }
   }

   private fun WavTag.loadTagFields() {
      iD3Tag?.loadTagFields()
      // todo: implement fallback
      // i do not know how this works, so for now unimplemented
      // tag.getInfoTag();
      // RATING --------------------------------------------------------------

      // PLAYCOUNT -----------------------------------------------------------

      // PUBLISHER -----------------------------------------------------------

      // CATEGORY ------------------------------------------------------------
   }

   private fun Mp4Tag.loadTagFields() {
      // RATING --------------------------------------------------------------
      if (rating==null) {
         // id: 'rate'
         // all are equivalent:
         //      tag.getFirst(FieldKey.RATING)
         //      tag.getValue(FieldKey.RATING,0)
         //      tag.getItem("rate",0)
         val r = loadAsInt(this, FieldKey.RATING)
         rating = when {
            r==null -> null
            // sometimes we get unintelligible values for some reason (don't ask me),
            // Mp3Tagger app can recognize them somehow the values appear consistent so lets use them
            r==12592 -> 100
            r==14384 -> 80
            r==13872 -> 60
            r==13360 -> 40
            r==12848 -> 20
            r<0 || r>100 -> null
            else -> r
         }
      }

      // PLAYCOUNT -----------------------------------------------------------
      // no support

      // PUBLISHER -----------------------------------------------------------
      // id: '----:com.nullsoft.winamp:publisher'
      //      tag.getFirst(FieldKey.PRODUCER) // nope
      //      tag.getFirst(Mp4FieldKey.WINAMP_PUBLISHER) // works
      //      tag.getFirst(Mp4FieldKey.LABEL) // not same as WINAMP_PUBLISHER, but perhaps also valid
      //      tag.getFirst(FieldKey.KEY)
      if (publisher==null)
         publisher = this.getFirst(Mp4FieldKey.WINAMP_PUBLISHER).orNull()
      if (publisher==null)
         publisher = this.getFirst(Mp4FieldKey.MM_PUBLISHER).orNull()

      // CATEGORY ------------------------------------------------------------
      // general reading is good enough
   }

   private fun VorbisCommentTag.loadTagFields() {
      // RATING --------------------------------------------------------------
      if (rating==null) {
         this.getFirst("RATING").orNull()?.toIntOrNull()?.let {
            // some players use 0-5 value, so extends it to 100
            rating = if (it in 1..5) it*20 else it
         }
      }

      // PLAYCOUNT -----------------------------------------------------------
      // if we want to support playcount in vorbis specific field, we can
      // if (playcount==null)
      //      playcount = tag.getFirst("PLAYCOUNT")).orNull()?.toIntOrNull();

      // PUBLISHER -----------------------------------------------------------
      if (publisher==null)
         publisher = this.getFirst("PUBLISHER").orNull()

      // CATEGORY ------------------------------------------------------------
      if (category==null)
      // try to get category the winamp way
         category = this.getFirst("CATEGORY").orNull()
   }

   /**
    * @return true if this metadata is empty.
    * @see EMPTY
    */
   fun isEmpty(): Boolean = this===EMPTY

   /** @return comprehensive information about all string representable tag fields of this song */
   fun getInfo(): String = Field.all.asSequence()
      .filter { it.isTypeStringRepresentable() }
      .map { "${it.name()}: ${getField(it)}" }
      .joinToString("\n")

   override fun getPathAsString(): String = if (isEmpty()) "" else super.getPathAsString()

   override fun getFilename(): String = if (isEmpty()) "" else super.getFilename()

   override fun getFileSize() = FileSize.ofBytes(fileSizeInB)

   /** @return file size in bytes or [FileSize.VALUE_NA] if unknown */
   fun getFileSizeInB() = fileSizeInB

   /** Encoding type, e.g.: MPEG-1 Layer 3 */
   fun getEncodingType() = encodingType

   /** @return bitrate or null if unknown */
   fun getBitrate(): Bitrate = Bitrate.ofInt(bitrate)

   /** @return encoder or empty String if not available */
   fun getEncoder() = encoder

   override fun getFormat(): AudioFileFormat = AudioFileFormat.of(id)

   /** @return channels, e.g.: Stereo */
   fun getChannels() = channels

   /** @return sample rate, e.g.: 44100 */
   fun getSampleRate() = sampleRate

   /** @return the length or zero if unknown */
   fun getLength() = Duration(lengthInMs)

   /** @return length in milliseconds or 0 if unknown */
   fun getLengthInMs() = lengthInMs

   /** @return title or null if none */
   fun getTitle() = title

   /** @return title or empty string if none */
   fun getTitleOrEmpty() = title ?: ""

   /** @return album or null if none */
   fun getAlbum() = album

   /** @return album or empty string if none */
   fun getAlbumOrEmpty() = album ?: ""

   /** @return first artist or null if none */
   fun getArtist() = artist

   /** @return first artist or empty string if none */
   fun getArtistOrEmpty() = artist ?: ""

   /** @return album artist or null if none */
   fun getAlbumArtist() = albumArtist

   /** @return composer or null if none */
   fun getComposer() = composer

   /** @return publisher or null if none */
   fun getPublisher() = publisher

   /** @return track number or null if none */
   fun getTrack() = track

   /** @return total number of tracks on album or null if none */
   fun getTracksTotal() = tracksTotal

   /** @return track album information, e.g.: 1/1 */
   fun getTrackInfo() = NofX(track ?: -1, tracksTotal ?: -1)

   /** @return disc number or null if none */
   fun getDisc() = disc

   /** @return total number of discs on album or null if none */
   fun getDiscsTotal() = discsTotal

   /** @return disc album information, e.g.: 1/1 */
   fun getDiscInfo() = NofX(disc ?: -1, discsTotal ?: -1)

   /** @return genre or null if none */
   fun getGenre() = genre

   /** @return year or null if none */
   fun getYear(): Year? = yearAsInt?.let {
      try {
         Year.of(it)
      } catch (e: DateTimeException) {
         null
      }
   }

   /** @return year as int or null if none */
   fun getYearAsInt(): Int? = yearAsInt

   /** @return raw rating or null if none */
   fun getRating() = rating

   /** @return maximal raw rating value supported by the current tag */
   fun getRatingMax() = ratingMax

   /** @return rating in 0-1 value system or null if none */
   fun getRatingPercent(): Double? = rating?.let { it/ratingMax.toDouble() }

   /** @return rating in 0-1 value system or 0 if none */
   fun getRatingPercentOr0() = getRatingPercent() ?: 0.0

   /** @return the playcount or null if none */
   fun getPlaycount() = playcount

   /** @return the playcount or 0 if none */
   fun getPlaycountOr0(): Int = getPlaycount() ?: 0

   /** @return the playcount or -1 if none */
   fun getPlaycountOrMinus1(): Int = getPlaycount() ?: -1

   /** @return category or null if none */
   fun getCategory() = category

   /** @return comment or null if none */
   fun getComment() = comment

   /** @return lyrics or null if none */
   fun getLyrics() = lyrics

   /** @return mood or null if none */
   fun getMood() = mood

   /** @return custom 1 or null if empty */
   fun getCustom1() = custom1

   /** @return custom 2 or null if empty */
   fun getCustom2() = custom2

   /** @return custom 3 or null if empty */
   fun getCustom3() = custom3

   /** @return custom 4 or null if empty */
   fun getCustom4() = custom4

   /** @return custom 5 or null if empty */
   fun getCustom5() = custom5

   /** @return musicbrainz field MUSICBRAINZ_ARTISTID or null if empty */
   fun getMusicbrainzArtistId() = mbArtistId

   /** @return musicbrainz field MUSICBRAINZ_TRACK_ID or null if empty */
   fun getMusicbrainzTrackId() = mbTrackId

   /** @return musicbrainz field MUSICBRAINZ_DISC_ID or null if empty */
   fun getMusicbrainzDiscId() = mbDiscId

   /** @return musicbrainz field MUSICBRAINZ_RELEASEID or null if empty */
   fun getMusicbrainzReleaseId() = mbReleaseId

   /** @return musicbrainz field MUSICBRAINZ_RELEASEARTISTID or null if empty */
   fun getMusicbrainzReleaseArtistId() = mbReleaseArtistId

   /** @return cover using the respective source */
   @Blocking
   fun getCover(source: CoverSource): Cover {
      failIfFxThread()

      return when (source) {
         CoverSource.NONE -> EmptyCover
         CoverSource.TAG -> readCoverFromTag().orNull() ?: EmptyCover
         CoverSource.DIRECTORY -> readCoverFromDir() ?: EmptyCover
         CoverSource.ANY -> readCoverFromTag().orNull() ?: readCoverFromDir() ?: EmptyCover
      }
   }

   private fun readCoverFromTag(): Try<ArtworkCover?, Throwable> = readArtworkFromTag().map { if (it==null) null else ArtworkCover(it, it.info) }

   private fun readArtworkFromTag(): Try<Artwork?, Throwable> = ok(getFile())
      .andAlso { it?.readAudioFile() ?: ok() }
      .map { it?.tag?.firstArtwork }
      .ifError { logger.warn(it) { "Failed to read cover from tag of song=$uri" } }

   /** @return the cover image file on a file system or null if this song is not file based */
   private fun readCoverFromDir(): Cover? {
      return getFile()?.let { file ->
         val fs = file.parentDirOrRoot.children().toList()
         return sequenceOf(getFilename().takeIf { it.isNotBlank() }, title, album, "cover", "folder")
            .filterNotNull()
            .flatMap { filename -> fs.asSequence().filter { it.nameWithoutExtension.equals(filename, true) } }
            .find { it.isImage() }
            ?.let { FileCover(it) }
      }
   }

   /**
    * Returns chapters associated with this song. A [Chapter] represents
    * a time specific song comment. The result is ordered by natural order.
    *
    * Chapters are concatenated into string located in the Custom2 tag field.
    *
    * @return ordered list of chapters parsed from tag data
    */
   fun getChapters(): Chapters =
      custom2?.let {
         it.split(SEPARATOR_CHAPTER)
            .asSequence()
            .filter { it.isNotEmpty() }
            .mapNotNull { chapter(it) orNull { logger.error { it } } }
            .sorted()
            .toList()
            .let { Chapters(it) }
      } ?: Chapters()

   fun containsChapterAt(at: Duration): Boolean = getChapters().chapters.any { it.time==at }

   /** @return the color associated with this or null if none */
   fun getColor(): Color? = color?.let { APP.converter.general.ofS<Color?>(it).orNull() }

   /** Tags joined into a string using [SEPARATOR_UNIT] or null if none */
   fun getTags(): String? = tags

   /** Tags as sequence */
   fun getTagsAsSequence(): Sequence<String>? = tags?.splitNoEmpty(SEPARATOR_UNIT.toString())

   /** @return time this song was first played or null if none */
   fun getTimePlayedFirst(): LocalDateTime? = playedFirst?.toLongOrNull()?.localDateTimeFromMillis()

   /** @return time this song was last played or null if none */
   fun getTimePlayedLast(): LocalDateTime? = playedLast?.toLongOrNull()?.localDateTimeFromMillis()

   /** @return time this song was added to library or null if none */
   fun getTimeLibraryAdded(): LocalDateTime? = libraryAdded?.toLongOrNull()?.localDateTimeFromMillis()

   /** @return all available text about this song */
   fun getFulltext() = FIELDS_FULLTEXT.asSequence().map { it.getOf(this) }.filterNotNull().toStringSeq()

   /** @return index of the first same song as this in the active playlist or -1 if not on playlist */
   fun getPlaylistIndex(): Int? = PlaylistManager.use({ it.indexOfSame(this) + 1 }, null)

   /** @return index info of the first same song as this in the active playlist or null if not on playlist, e.g.: "15/30" */
   fun getPlaylistIndexInfo(): NofX = PlaylistManager.use({ NofX(it.indexOfSame(this) + 1, it.size) }, NofX(-1, -1))

   override fun toMeta() = this

   override fun toPlaylist() = PlaylistSong(uri, artist, title, lengthInMs)

   fun getMainField(): Field<*> = Field.TITLE

   fun <T> getField(field: Field<T>): T = field.getOf(this)

   fun <T> getFieldS(f: Field<T>, noVal: String): String {
      val o = getField(f)
      return if (f.getOf(EMPTY)==o) noVal else f.toS(o, noVal)
   }

   override fun toString() = "${Metadata::class} $uri"

   override fun equals(other: Any?) = this===other || other is Metadata && id==other.id

   override fun hashCode() = id.hashCode()

   /**
    * Compares by attributes in the exact order:
    *  * artist
    *  * album
    *  * disc number
    *  * track number
    *  * title
    */
   operator fun compareTo(m: Metadata): Int {
      var r = (artist ?: "") compareTo (m.artist ?: "")
      if (r!=0) return r
      r = (album ?: "") compareTo (m.album ?: "")
      if (r!=0) return r
      r = (disc ?: -1) compareTo (m.disc ?: -1)
      if (r!=0) return r
      r = (track ?: -1) compareTo (m.track ?: -1)
      if (r!=0) return r
      r = (title ?: "") compareTo (m.title ?: "")
      return r
   }

   companion object {
      private val logger = KotlinLogging.logger { }
      private const val serialVersionUID: Long = 1

      /** Delimiter between sections of data. In this case, between tags (concatenated to single string). */
      const val SEPARATOR_GROUP: Char = 29.toChar()
      /** Delimiter between records or rows. In this case, between values in a tag. */
      const val SEPARATOR_RECORD: Char = 30.toChar()
      /** Delimiter between fields of a record, or members of a row. In this case, between songs in a tag value. */
      const val SEPARATOR_UNIT: Char = 31.toChar()
      /** Delimiter between chapters */
      const val SEPARATOR_CHAPTER: Char = '|'

      // Custom tag ids. Ordinary string. Length 10 mandatory. Unique. Dev is free to use any
      // value - there is no predefined set of ids. Once set, never change!
      const val TAG_ID_PLAYED_LAST = "PLAYED_LST"
      const val TAG_ID_PLAYED_FIRST = "PLAYED_1ST"
      const val TAG_ID_LIB_ADDED = "LIB_ADDED_"
      const val TAG_ID_COLOR = "COLOR_____"
      const val TAG_ID_TAGS = "TAG_______"

      /**
       * EMPTY metadata. Substitute for null. Always use instead of null. Also
       * corrupted songs should transform into EMPTY metadata.
       *
       * All fields are at their default values.
       *
       * There are two ways to check whether Metadata object is [EMPTY]. Either use
       * reference operator `this == EMPTY` or call [isEmpty].
       *
       * Note: The reference operator works, because there is always only one
       * instance of EMPTY metadata.
       */
      @JvmField val EMPTY = Metadata(SimpleSong(uri("empty://empty")))

      private fun String?.orNull() = takeUnless { it.isNullOrBlank() }

      private fun Metadata.loadAsString(tag: Tag, f: FieldKey): String? =
         try {
            tag.getFirst(f)
               .also { if (it==null) logger.warn { "Jaudiotagger returned null for $f of $id" } }
               .takeIf { it.isNotBlank() }
         } catch (e: UnsupportedOperationException) {
            logger.warn { "Jaudiotagger failed to read $f of $id - field not supported" }
            null
         } catch (e: KeyNotFoundException) {
            logger.warn(e) { "Jaudiotagger failed to read $f of $id" }
            null
         }

      private fun Metadata.loadAsInt(tag: Tag, field: FieldKey): Int? = loadAsString(tag, field)?.toIntOrNull()

      private fun loadComment(tag: Tag): String? {
         // there is a bug where getField(Comment) returns CUSTOM1 field, this is workaround
         // this is how COMMENT field look like: Language="English"; Text="example";
         // this is how CUSTOM fields look like: Language="Media Monkey Format"; Description="Songs-DB_Custom5"; Text="example";
         if (!tag.hasField(FieldKey.COMMENT)) return null

         // get index of comment within all comment-type tags
         val fields = tag.getFields(FieldKey.COMMENT)
         val i = fields.indexOfFirst { "Description" !in it.toString() }
         return if (i>-1) tag.getValue(FieldKey.COMMENT, i) else null
      }

      @Suppress("UNCHECKED_CAST")
      private val FIELDS_FULLTEXT: List<Field<String?>> = Field.all.asSequence()
         .filter { it.type.raw.isSubclassOf<String?>() }
         .map { it as Field<String?> }
         .toList()
   }

   @Suppress("ClassName")
   open class Field<T>: ObjectFieldBase<Metadata, T> {

      constructor(type: VType<T>, extractor: (Metadata) -> T, toUi: (T?, String) -> String, name: String, description: String): super(type, extractor, name, description, toUi)

      override fun isTypeStringRepresentable(): Boolean = this !in FIELDS_NOT_STRING_REPRESENTABLE

      override fun isTypeFilterable(): Boolean = this!=COVER

      override fun searchSupported(): Boolean = super.searchSupported() || this==FULLTEXT

      override fun searchMatch(matcher: (String) -> Boolean): (Metadata) -> Boolean = when (this) {
         CHAPTERS -> { m -> CHAPTERS.getOf(m).seq.any(matcher) }
         FULLTEXT -> { m -> FULLTEXT.getOf(m).seq.any(matcher) }
         TAGS -> { m -> m.getTagsAsSequence().orEmpty().any(matcher) }
         else -> super.searchMatch(matcher)
      }

      fun isFieldEmpty(m: Metadata): Boolean = when(this) {
         COVER -> m.readArtworkFromTag().map { it==null }.getOr(true)
         else -> getOf(m)==getOf(EMPTY)
      }

      val typeGrouped: VType<*> get() = when (this) {
         TAGS -> type<String>()
         else -> type
      }

      fun groupBuildAccumulator(groupAccumulator: (Any?, Metadata) -> Unit): (Metadata) -> Unit = when (this) {
         FILESIZE -> { m -> groupAccumulator(GROUPS_FILESIZE[64 - java.lang.Long.numberOfLeadingZeros(m.fileSizeInB - 1)], m)  }
         RATING -> { m -> groupAccumulator(if (m.rating==null) -1.0 else GROUPS_RATING[(m.getRatingPercent()!!*100/5).toInt()], m) }
         TAGS -> { m -> m.getTagsAsSequence()?.forEach { tag -> groupAccumulator(tag, m) } ?: groupAccumulator(null, m) }
         else -> { m -> groupAccumulator(getOf(m), m) }
      }

      @Suppress("UNCHECKED_CAST")
      fun groupToS(o: Any?, substitute: String): String {
         if (o==null) return substitute
         return when (this) {
            TAGS -> o.asIs()
            else -> toS(o as T?, substitute)
         }
      }

      fun isAutoCompletable(): Boolean = this in FIELDS_AUTO_COMPLETABLE

      fun autocompleteGetOf(m: Metadata): Sequence<String> = when (this) {
         TAGS -> m.getTagsAsSequence().orEmpty()
         else -> sequenceOf(getOf(m)).filterIsInstance<String>().filter { it.isNotBlank() }
      }

      override fun cVisible(): Boolean = FIELDS_VISIBLE.contains(this)

      override fun cWidth(): Double = when(this) {
         PATH, TITLE, COMMENT, ALBUM -> 300.0
         ARTIST, ALBUM_ARTIST, COMPOSER, PUBLISHER, CATEGORY, FILENAME -> 150.0
         else -> 60.0
      }

      object PATH: Field<String>(type(), { it.getPathAsString() }, { o, or -> o ?: or }, "Path", "Song location")
      object FILENAME: Field<String>(type(), { it.getFilename() }, { o, or -> o ?: or }, "Filename", "Song file name without suffix")
      object FORMAT: Field<AudioFileFormat>(type(), { it.getFormat() }, { o, or -> o?.toString() ?: or }, "Format", "Song file type ")
      object FILE: Field<File?>(type(), { it.getFile() }, { o, or -> o?.toString() ?: or }, "File", "Song file")
      object FILESIZE: Field<FileSize>(type(), { it.getFileSize() }, { o, or -> o?.toString() ?: or }, "Filesize", "Song file size")
      object ENCODING: Field<String?>(type(), { it.encodingType }, { o, or -> o ?: or }, "Encoding", "Song encoding")
      object BITRATE: Field<Bitrate>(type(), { it.getBitrate() }, { o, or -> o?.toString() ?: or }, "Bitrate", "Number of kb per second of the song - quality aspect.")
      object ENCODER: Field<String?>(type(), { it.encoder }, { o, or -> o ?: or }, "Encoder", "Song encoder")
      object CHANNELS: Field<String?>(type(), { it.channels }, { o, or -> o ?: or }, "Channels", "Number of channels")
      object SAMPLE_RATE: Field<String?>(type(), { it.sampleRate }, { o, or -> o ?: or }, "Sample rate", "Sample frequency")
      object LENGTH: Field<Duration>(type(), { it.getLength() }, { o, or -> o?.toHMSMs(false) ?: or }, "Length", "Song length")
      object TITLE: Field<String?>(type(), { it.title }, { o, or -> o ?: or }, "Title", "Song title")
      object ALBUM: Field<String?>(type(), { it.album }, { o, or -> o ?: or }, "Album", "Song album")
      object ARTIST: Field<String?>(type(), { it.artist }, { o, or -> o ?: or }, "Artist", "Artist of the song")
      object ALBUM_ARTIST: Field<String?>(type(), { it.albumArtist }, { o, or -> o ?: or }, "Album artist", "Artist of the song album")
      object COMPOSER: Field<String?>(type(), { it.composer }, { o, or -> o ?: or }, "Composer", "Composer of the song")
      object PUBLISHER: Field<String?>(type(), { it.publisher }, { o, or -> o ?: or }, "Publisher", "Publisher of the album")
      object TRACK: Field<Int?>(type(), { it.track }, { o, or -> o?.toString() ?: or }, "Track", "Song number within album")
      object TRACKS_TOTAL: Field<Int?>(type(), { it.tracksTotal }, { o, or -> o?.toString() ?: or }, "Tracks total", "Number of songs in the album")
      object TRACK_INFO: Field<NofX>(type(), { it.getTrackInfo() }, { o, or -> o?.toString() ?: or }, "Track info", "Complete song number in format: track/track total")
      object DISC: Field<Int?>(type(), { it.disc }, { o, or -> o?.toString() ?: or }, "Disc", "Disc number within album")
      object DISCS_TOTAL: Field<Int?>(type(), { it.discsTotal }, { o, or -> o?.toString() ?: or }, "Discs total", "Number of discs in the album")
      object DISCS_INFO: Field<NofX>(type(), { it.getDiscInfo() }, { o, or -> o?.toString() ?: or }, "Discs info", "Complete disc number in format: disc/disc total")
      object GENRE: Field<String?>(type(), { it.genre }, { o, or -> o ?: or }, "Genre", "Genre of the song")
      object YEAR: Field<Year?>(type(), { it.getYear() }, { o, or -> o?.toString() ?: or }, "Year", "Year the album was published")
      object COVER: Field<ArtworkCover?>(type(), { it.readCoverFromTag().orNull() }, { o, or -> o?.toString() ?: or }, "Cover", "Cover of the song")
      object RATING: Field<Double?>(type(), { it.getRatingPercent() }, { o, or -> o?.let { "%.2f".format(APP.locale.value, it) } ?: or }, "Rating", "Song rating in 0-1 range")
      object RATING_RAW: Field<Int?>(type(), { it.getRating() }, { o, or -> o?.toString() ?: or }, "Rating (raw)", "Actual song rating value in tag. Maximal value depends on tag type")
      object RATING_RAW_MAX: Field<Int>(type(), { it.getRatingMax() }, { o, or -> o?.toString() ?: or }, "Rating max (raw)", "Maximal song rating value supported by current tag type")
      object PLAYCOUNT: Field<Int?>(type(), { it.getPlaycount() }, { o, or -> o?.toString() ?: or }, "Playcount", "Number of times the song was played.")
      object CATEGORY: Field<String?>(type(), { it.category }, { o, or -> o ?: or }, "Category", "Category of the song. Arbitrary")
      object COMMENT: Field<String?>(type(), { it.comment }, { o, or -> o ?: or }, "Comment", "User comment of the song. Arbitrary")
      object LYRICS: Field<String?>(type(), { it.lyrics }, { o, or -> o ?: or }, "Lyrics", "Lyrics for the song")
      object MOOD: Field<String?>(type(), { it.mood }, { o, or -> o ?: or }, "Mood", "Mood the song evokes")
      object COLOR: Field<Color?>(type(), { it.getColor() }, { o, or -> o?.toString() ?: or }, "Color", "Color the song evokes")
      object TAGS: Field<Set<String>?>(type(), { it.getTagsAsSequence()?.toSet() }, { o, or -> o?.toString() ?: or }, "Tags", "Tags associated with this song")
      object CHAPTERS: Field<Chapters>(type(), { it.getChapters() }, { o, or -> o?.toString() ?: or }, "Chapters", "Comments at specific time points of the song")
      object FULLTEXT: Field<StringSeq>(type(), { it.getFulltext() }, { o, or -> o?.toString() ?: or }, "Fulltext", "All possible fields merged into single text. Use for searching.")
      object CUSTOM1: Field<String?>(type(), { it.custom1 }, { o, or -> o ?: or }, "Custom1", "Custom field 1")
      object CUSTOM2: Field<String?>(type(), { it.custom2 }, { o, or -> o ?: or }, "Custom2", "Custom field 2. Reserved for field `Chapters`.")
      object CUSTOM3: Field<String?>(type(), { it.custom3 }, { o, or -> o ?: or }, "Custom3", "Custom field 3")
      object CUSTOM4: Field<String?>(type(), { it.custom4 }, { o, or -> o ?: or }, "Custom4", "Custom field 4")
      object CUSTOM5: Field<String?>(type(), { it.custom5 }, { o, or -> o ?: or }, "Custom5", "Custom field 5. Reserved for fields 'First played', 'Last played', 'Added to library'." )
      object MUSICBRAINZ_ARTIST_ID: Field<String?>(type(), { it.mbArtistId }, { o, or -> o ?: or }, "Musicbrainz artist_id", "Musicbrainz artist_id")
      object MUSICBRAINZ_DISC_ID: Field<String?>(type(), { it.mbDiscId }, { o, or -> o ?: or }, "Musicbrainz disc_id", "Musicbrainz disc_id")
      object MUSICBRAINZ_TRACK_ID: Field<String?>(type(), { it.mbTrackId }, { o, or -> o ?: or }, "Musicbrainz track_id", "Musicbrainz track_id")
      object MUSICBRAINZ_RELEASE_ID: Field<String?>(type(), { it.mbReleaseId }, { o, or -> o ?: or }, "Musicbrainz release_id", "Musicbrainz release_id")
      object MUSICBRAINZ_RELEASE_ARTIST_ID: Field<String?>(type(), { it.mbReleaseArtistId }, { o, or -> o ?: or }, "Musicbrainz release_artist_id", "Musicbrainz release_artist_id")
      object FIRST_PLAYED: Field<LocalDateTime?>(type(), { it.getTimePlayedFirst() }, { o, or -> o?.toString() ?: or }, "First played", "Marks time the song was played the first time.")
      object LAST_PLAYED: Field<LocalDateTime?>(type(), { it.getTimePlayedLast() }, { o, or -> o?.toString() ?: or }, "Last played", "Marks time the song was played the last time.")
      object ADDED_TO_LIBRARY: Field<LocalDateTime?>(type(), { it.getTimeLibraryAdded() }, { o, or -> o?.toString() ?: or }, "Added to library", "Marks time the song was added to the library.")

      companion object: ObjectFieldRegistry<Metadata, Field<*>>(Metadata::class) {

         init { registerDeclared() }

         private val FIELDS_AUTO_COMPLETABLE = setOf<Field<*>>(
            ENCODER, ALBUM, ALBUM_ARTIST, COMPOSER, PUBLISHER, GENRE, CATEGORY, MOOD, TAGS
         )

         private val FIELDS_VISIBLE = setOf<Field<*>>(
            TITLE, ALBUM, ARTIST, LENGTH, TRACK_INFO, DISCS_INFO, RATING, PLAYCOUNT
         )

         private val FIELDS_NOT_STRING_REPRESENTABLE = setOf<Field<*>>(
            COVER, // can not be converted to string
            CHAPTERS, // raw string form unsuitable for viewing
            FULLTEXT // purely for search purposes
         )

         private val GROUPS_FILESIZE = Array(65) {
            when (it) {
               0 -> FileSize(1)
               in 1..62 -> FileSize(2.0.pow(it.toDouble()).toLong())
               63 -> FileSize(Long.MAX_VALUE)
               64 -> FileSize(0)
               else -> failCase(it)
            }
         }

         private val GROUPS_RATING = (0..20).map { it*5/100.0 }.toDoubleArray()
      }

   }

}