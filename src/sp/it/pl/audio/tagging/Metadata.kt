package sp.it.pl.audio.tagging

import javafx.scene.paint.Color
import javafx.util.Duration
import mu.KLogging
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
import sp.it.pl.audio.Item
import sp.it.pl.audio.playlist.PlaylistItem
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.tagging.Metadata.Field
import sp.it.pl.audio.tagging.chapter.Chapter
import sp.it.pl.audio.tagging.chapter.Chapters
import sp.it.pl.gui.objects.image.cover.Cover
import sp.it.pl.gui.objects.image.cover.Cover.CoverSource
import sp.it.pl.gui.objects.image.cover.FileCover
import sp.it.pl.gui.objects.image.cover.ImageCover
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.SwitchException
import sp.it.pl.util.access.fieldvalue.ObjectFieldBase
import sp.it.pl.util.dev.throwIfFxThread
import sp.it.pl.util.file.AudioFileFormat
import sp.it.pl.util.file.ImageFileFormat
import sp.it.pl.util.file.Util.EMPTY_URI
import sp.it.pl.util.file.listChildren
import sp.it.pl.util.file.nameWithoutExtensionOrRoot
import sp.it.pl.util.functional.orNull
import sp.it.pl.util.functional.seqOf
import sp.it.pl.util.localDateTimeFromMillis
import sp.it.pl.util.text.Strings
import sp.it.pl.util.text.toStrings
import sp.it.pl.util.units.Bitrate
import sp.it.pl.util.units.Dur
import sp.it.pl.util.units.FileSize
import sp.it.pl.util.units.FileSize.Companion.sizeInB
import sp.it.pl.util.units.NofX
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.net.URI
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.Year
import java.util.HashSet
import java.util.Objects
import kotlin.collections.HashMap
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.streams.toList

/**
 * Information about audio file.
 * Provided by reading tag and audio file header (mostly).
 * Everything that is possible to know about the audio file is accessible through
 * this class. This class also provides some non-tag, application specific
 * or external information associated with this song, like cover files.
 *
 * The class is practically immutable and does not provide any setters, nor
 * allows updating of its state or any of its values.
 *
 * Metadata can be empty and should be used instead of null. See [.EMPTY]
 *
 * The getters of this class return mostly string. For empty fields the output
 * is "" (empty string) and for non-string getters it varies, but it is never
 * null. See documentation
 * for the specific field. The rule is however, that the check for empty value
 * should be necessary only in rare cases.
 *
 * Every field returns string, primitive, or Object with toString method that
 * returns the best possible string representation of the field's value, including
 * its empty value.
 *
 * To access any field in a general way, see [Field]
 */
@Suppress("unused")
class Metadata: Item, Serializable {

    // file fields

    override val uri: URI get() = URI.create(id.replace(" ", "%20"))

    override var id = EMPTY_URI.toString()

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

    /** Raw rating or -1 if empty */
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

    // synthetic fields

    /** Color as string or null if none */
    private var color: String? = null

    /** Tags joined into a string or null if none */
    private var tags: String? = null

    /** Time this item was first played as string or null if none */
    private var playedFirst: String? = null

    /** Time this item was last played as string or null if none */
    private var playedLast: String? = null

    /** Time this item was added to library as string or null if none */
    private var libraryAdded: String? = null

    /* --------------------- AS OBJECT ---------------------------------------------------------------------------------- */

    /* Creates empty metadata */
    private constructor() {
        id = EMPTY_URI.toString()
    }

    /** Creates metadata from an item, attempts to use as much data available, no i/o. */
    constructor(item: Item) {
        id = item.uri.toString()
        if (item is PlaylistItem) {
            artist = item.getArtist()
            lengthInMs = item.time.toMillis()
            title = item.getTitle()
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
        fileSizeInB = sizeInB()
    }
    
    private fun AudioFile.loadHeaderFields() {
        val header = this.audioHeader
        header.bitRate
        bitrate = header.bitRateAsNumber.toInt()*(if (header.isVariableBitRate) -1 else 1)
        lengthInMs = (1000*header.trackLength).toDouble()
        encodingType = header.format.orNull()   // format and encoding type are switched in jaudiotagger library...
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
        val i = tr!!.indexOf('/')
        if (i!=-1) {
            // some apps use TRACK for "x/y" string format, we cover that
            track = tr.substring(0, i).toIntOrNull()
            tracksTotal = tr.substring(i+1, tr.length).toIntOrNull()
        } else {
            track = tr.toIntOrNull()
            tracksTotal = loadAsInt(this, FieldKey.TRACK_TOTAL)
        }

        // disc
        val dr = loadAsString(this, FieldKey.DISC_NO)
        val j = dr!!.indexOf('/')
        if (j!=-1) {
            // some apps use DISC_NO for "x/y" string format, we cover that
            disc = dr.substring(0, j).toIntOrNull()
            discsTotal = dr.substring(j+1, dr.length).toIntOrNull()
        } else {
            disc = dr.toIntOrNull()
            discsTotal = loadAsInt(this, FieldKey.DISC_TOTAL)
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

        // Read synthetic fields
        if (!custom5!!.isEmpty()) {
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

                    // i do not know why the values themselves are Long, but we only need int
                    // both for rating and playcount.
                    // all is good until the tag is actually damaged and the int can really
                    // overflow during conversion and we get ArithmeticException
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
            // todo: also check ID3v24Frames.FRAME_ID_PLAY_COUNTER

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

    /** @return comprehensive information about all string representable tag fields of this */
    fun getInfo(): String = Field.FIELDS.asSequence()
            .filter { it.isTypeStringRepresentable() }
            .map { "${it.name()}: ${getField(it)}" }
            .joinToString("\n")
    
    override fun getPathAsString(): String = if (isEmpty()) "" else super.getPathAsString()

    override fun getFilename(): String = if (isEmpty()) "" else super.getFilename()

    override fun getFileSize() = FileSize(fileSizeInB)
    
    /** @return file size in bytes or -1 if unknown */
    fun getFileSizeInB() = fileSizeInB

    /** Encoding type, e.g.: MPEG-1 Layer 3 */
    fun getEncodingType() = encodingType

    /** @return bitrate or null if unknown */
    fun getBitrate(): Bitrate? = Bitrate(bitrate)

    /** @return encoder or empty String if not available */
    fun getEncoder() = encoder

    override fun getFormat(): AudioFileFormat = AudioFileFormat.of(id)

    /** @return channels, e.g.: Stereo */
    fun getChannels() = channels

    /** @return sample rate, e.g.: 44100 */
    fun getSampleRate() = sampleRate

    /** @return the length or zero if unknown */
    fun getLength(): Dur = Dur(lengthInMs)

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

    /** Rating in 0-1 value system or null if none */
    fun getRatingPercent(): Double? = rating?.let { it/ratingMax.toDouble() }

    /** Rating in 0-1 value system or 0 if none */
    fun getRatingPercentOr0() = getRatingPercent() ?: 0.0

    /** @return the current rating value in 0-max value system or null if none */
    fun getRatingToStars(max: Int): Double? = getRatingPercent()?.let { it*max }

    /** @return the current rating value in 0-max value system or 0 if none */
    fun getRatingToStarsOr0(max: Int) = getRatingToStars(max) ?: 0.0

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

    /** @return cover using the respective source */
    fun getCover(source: CoverSource): Cover {
        throwIfFxThread()
        return when (source) {
            Cover.CoverSource.TAG -> getReadCoverOfTag() ?: Cover.EMPTY
            Cover.CoverSource.DIRECTORY -> readCoverOfDir() ?: Cover.EMPTY
            Cover.CoverSource.ANY -> seqOf(CoverSource.TAG, CoverSource.DIRECTORY)
                    .mapNotNull { getCover(it) }
                    .firstOrNull { !it.isEmpty }
                    ?: Cover.EMPTY
        }
    }

    private fun getReadCoverOfTag(): Cover? = try {
        readArt()?.let { ImageCover(it.imageOrNull, it.info ?: "") }
    } catch (e: IOException) {
        null
    }

    private fun readArt(): Artwork? {
        val af = if (isFileBased()) getFile().readAudioFile().orNull() else null
        return af?.tag?.firstArtwork
    }

    /** @return the cover image file on a file system or null if this item is not file based */
    private fun readCoverOfDir(): Cover? {
        if (!isFileBased()) return null

        val fs = getLocation().listChildren().toList()
        return seqOf(getFilename().takeIf { it.isNotBlank() }, title, album, "cover", "folder")
                .filterNotNull()
                .flatMap { filename -> fs.asSequence().filter { it.nameWithoutExtensionOrRoot.equals(filename, true) } }
                .find { ImageFileFormat.isSupported(it) }
                ?.let { FileCover(it, "") }
    }

    /**
     * Returns chapters associated with this item. A [Chapter] represents
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
                    .filter { !it.isEmpty() }
                    .mapNotNull {
                        try {
                            Chapter(it)
                        } catch (e: IllegalArgumentException) {
                            logger().error(e) { "String '$it' is not a valid chapter, chapters=$custom2, uri=$uri" }
                            null
                        }
                    }
                    .sorted()
                    .toList()
                    .let { Chapters(it) }
        } ?: Chapters()

    fun containsChapterAt(at: Duration): Boolean = getChapters().chapters.any { it.time==at }

    /** @return the color associated with this or null if none */
    fun getColor(): Color? = color?.let { APP.converter.general.ofS<Color>(it).getOr(null) }

    /** Tags joined into a string or null if none */
    fun getTags(): String? = tags

    /** @return time this item was first played or null if none */
    fun getTimePlayedFirst(): LocalDateTime? = playedFirst?.toLongOrNull()?.localDateTimeFromMillis()

    /** @return time this item was last played or null if none */
    fun getTimePlayedLast(): LocalDateTime? = playedLast?.toLongOrNull()?.localDateTimeFromMillis()

    /** @return time this item was added to library or null if none */
    fun getTimeLibraryAdded(): LocalDateTime? = libraryAdded?.toLongOrNull()?.localDateTimeFromMillis()

    /** @return all available text about this item */
    fun getFulltext() = FIELDS_FULLTEXT.asSequence().map { it.getOf(this) }.filterNotNull().toStrings()

    /** @return index of the first same item as this in the active playlist or -1 if not on playlist */
    fun getPlaylistIndex(): Int? = PlaylistManager.use({ it.indexOfSame(this)+1 }, null)

    /** @return index info of the first same item as this in the active playlist or null if not on playlist, e.g.: "15/30" */
    fun getPlaylistIndexInfo(): NofX = PlaylistManager.use({ NofX(it.indexOfSame(this)+1, it.size) }, NofX(-1, -1))

    override fun toMeta() = this

    override fun toPlaylist() = PlaylistItem(uri, artist, title, lengthInMs)

/* --------------------- AS FIELDED TYPE ---------------------------------------------------------------------------- */

    fun getMainField(): Field<*> = Field.TITLE

    private fun <T: Any> getField(field: Field<T>): T? = field.getOf(this)

    fun <T: Any> getFieldS(f: Field<T>): String {
        val o = getField(f)
        return if (o==null) "<none>" else f.toS(o, "<none>")
    }

    fun <T: Any> getFieldS(f: Field<T>, no_val: String): String {
        val o = getField(f)
        return if (o==null) no_val else f.toS(o, no_val)
    }

    override fun toString() = "${Metadata::class} $uri"

    override fun equals(other: Any?) = this===other || other is Metadata && id==other.id

    override fun hashCode(): Int = 79*7+Objects.hashCode(this.id)

    /**
     * Compares with other metadata by attributes in the exact order:
     *  * artist
     *  * album
     *  * disc number
     *  * track number
     *  * title
     */
    operator fun compareTo(m: Metadata): Int {
        var r = (artist ?: "").compareTo(m.artist ?: "")
        if (r!=0) return r
        r = (album ?: "").compareTo(m.album ?: "")
        if (r!=0) return r
        r = Integer.compare(disc ?: -1, m.disc ?: -1)
        if (r!=0) return r
        r = Integer.compare(track ?: -1, m.track ?: -1)
        if (r!=0) return r
        r = (title ?: "").compareTo(m.title ?: "")
        return r
    }

    companion object: KLogging() {

        /** Delimiter between sections of data. In this case, between tags (concatenated to single string). */
        @JvmField val SEPARATOR_GROUP: Char = 29.toChar()
        /** Delimiter between records or rows. In this case, between values in a tag. */
        @JvmField val SEPARATOR_RECORD: Char = 30.toChar()
        /** Delimiter between fields of a record, or members of a row. In this case, between items in a tag value. */
        @JvmField val SEPARATOR_UNIT: Char = 31.toChar()
        /** Delimiter between chapters */
        @JvmField val SEPARATOR_CHAPTER: Char = '|'

        // Custom tag ids. Ordinary string. Length 10 mandatory. Unique. Dev is free to use any
        // value - there is no predefined set of ids. Once set, never change!
        @JvmField val TAG_ID_PLAYED_LAST = "PLAYED_LST"
        @JvmField val TAG_ID_PLAYED_FIRST = "PLAYED_1ST"
        @JvmField val TAG_ID_LIB_ADDED = "LIB_ADDED_"
        @JvmField val TAG_ID_COLOR = "COLOR_____"
        @JvmField val TAG_ID_TAGS = "TAG_______"

        /**
         * EMPTY metadata. Substitute for null. Always use instead of null. Also
         * corrupted items should transform into EMPTY metadata.
         *
         * All fields are at their default values.
         *
         * There are two ways to check whether Metadata object is EMPTY. Either use
         * reference operator this == Metadata.EMPTY or call [.isEmpty].
         *
         * Note: The reference operator works, because there is always only one
         * instance of EMPTY metadata.
         */
        @JvmField val EMPTY = Metadata()

        @JvmStatic fun metadataID(u: URI): String = u.toString()

        private fun String?.orNull() = takeUnless { it.isNullOrBlank() }

        private fun Metadata.loadAsString(tag: Tag, f: FieldKey): String? =
                try {
                    val s = tag.getFirst(f) // can throw UnsupportedOperationException
                    if (s==null) {
                        logger().warn { "Jaudiotagger returned null for $f of $id" }
                    }
                    s
                } catch (e: UnsupportedOperationException) {
                    logger().warn { "Jaudiotagger failed to read $f of $id" }
                    ""
                } catch (e: KeyNotFoundException) {
                    logger().warn(e) { "Jaudiotagger failed to read $f of $id" }
                    ""
                }

        private fun Metadata.loadAsInt(tag: Tag, field: FieldKey): Int? = loadAsString(tag, field)?.toIntOrNull()

        // use this to get comment, not getField(COMMENT); because it is bugged
        private fun loadComment(tag: Tag): String {
            // there is a bug where getField(Comment) returns CUSTOM1 field, this is workaround
            // this is how COMMENT field look like:
            //      Language="English"; Text="example";
            // this is how CUSTOM fields look like:
            //      Language="Media Monkey Format"; Description="Songs-DB_Custom5"; Text="example";
            if (!tag.hasField(FieldKey.COMMENT)) return ""

            // get index of comment within all comment-type tags
            val fields = tag.getFields(FieldKey.COMMENT)
            val i = fields.indexOfFirst { !it.toString().contains("Description") }
            return if (i>-1) tag.getValue(FieldKey.COMMENT, i) else ""
        }

        @Suppress("UNCHECKED_CAST")
        private val FIELDS_FULLTEXT: List<Field<String>> = Field.FIELDS.asSequence()
                .filter { String::class.java==it.type }
                .map { it as Field<String> }
                .toList()
    }

    class Field<T: Any>: ObjectFieldBase<Metadata, T> {

        internal constructor(type: KClass<T>, extractor: (Metadata) -> T?, name: String, description: String): super(type, extractor, name, description) {
            FIELDS_IMPL += this
            FIELD_NAMES_IMPL += name
            FIELDS_BY_NAME[name] = this
        }

        fun isAutoCompletable(): Boolean = isTypeStringRepresentable() && !NOT_AUTO_COMPLETABLE.contains(this)

        override fun isTypeStringRepresentable(): Boolean = !NOT_STRING_REPRESENTABLE.contains(this)

        override fun isTypeFilterable(): Boolean = this!=COVER

        override fun searchSupported(): Boolean = super.searchSupported() || this==FULLTEXT

        override fun searchMatch(matcher: (String) -> Boolean): (Metadata) -> Boolean =
            when (this) {
                CHAPTERS -> { m -> getOf(m)?.strings?.any(matcher) ?: false }
                FULLTEXT -> { m -> getOf(m)?.strings?.any(matcher) ?: false }
                else -> super.searchMatch(matcher)
            }

        fun getGroupedOf(): (Metadata) -> Any? = when(this) {
            // Note that groups must include the 'empty' group for when the value is empty
            FILESIZE -> { m -> GROUPS_FILESIZE[64-java.lang.Long.numberOfLeadingZeros(m.fileSizeInB-1)] }
            RATING -> { m -> if (m.rating==null) -1.0 else GROUPS_RATING[(m.getRatingPercent()!!*100/5).toInt()] }
            else -> { m -> getOf(m) }
        }

        fun isFieldEmpty(m: Metadata): Boolean = getOf(m)==getOf(Metadata.EMPTY)

        override fun isTypeNumberNoNegative(): Boolean = true

        override fun toS(o: T?, substitute: String): String {
            if (o==null || ""==o) return substitute
            if (this===RATING_RAW)
                return o.toString()
            if (this===RATING) return String.format("%.2f", o as Double)
            return if (this===DISC || this===DISCS_TOTAL || this===TRACK || this===TRACKS_TOTAL || this===PLAYCOUNT)
                if (getOf(Metadata.EMPTY)==o) substitute
                else o.toString() else o.toString()
        }

        override fun cVisible(): Boolean = VISIBLE.contains(this)

        override fun cWidth(): Double = (if (this===PATH || this===TITLE) 160 else 60).toDouble()

        companion object {

            private val FIELDS_IMPL: MutableSet<Field<*>> = HashSet()
            private val FIELDS_BY_NAME = HashMap<String, Field<*>>()
            private val FIELD_NAMES_IMPL: MutableSet<String> = HashSet()
            @JvmField val FIELDS: Set<Field<*>> = FIELDS_IMPL
            @JvmField val FIELD_NAMES: Set<String> = FIELD_NAMES_IMPL

            @JvmField val PATH = Field(String::class, { it.getPathAsString() }, "Path", "Song location")
            @JvmField val FILENAME = Field(String::class, { it.getFilename() }, "Filename", "Song file name without suffix")
            @JvmField val FORMAT = Field(AudioFileFormat::class, { it.getFormat() }, "Format", "Song file type ")
            @JvmField val FILESIZE = Field(FileSize::class, { it.getFileSize() }, "Filesize", "Song file size")
            @JvmField val ENCODING = Field(String::class, { it.encodingType }, "Encoding", "Song encoding")
            @JvmField val BITRATE = Field(Bitrate::class, { it.getBitrate() }, "Bitrate", "Number of kb per second of the song - quality aspect.")
            @JvmField val ENCODER = Field(String::class, { it.encoder }, "Encoder", "Song encoder")
            @JvmField val CHANNELS = Field(String::class, { it.channels }, "Channels", "Number of channels")
            @JvmField val SAMPLE_RATE = Field(String::class, { it.sampleRate }, "Sample_rate", "Sample frequency")
            @JvmField val LENGTH = Field(Dur::class, { it.getLength() }, "Length", "Song length")
            @JvmField val TITLE = Field(String::class, { it.title }, "Title", "Song title")
            @JvmField val ALBUM = Field(String::class, { it.album }, "Album", "Song album")
            @JvmField val ARTIST = Field(String::class, { it.artist }, "Artist", "Artist of the song")
            @JvmField val ALBUM_ARTIST = Field(String::class, { it.albumArtist }, "Album_artist", "Artist of the song album")
            @JvmField val COMPOSER = Field(String::class, { it.composer }, "Composer", "Composer of the song")
            @JvmField val PUBLISHER = Field(String::class, { it.publisher }, "Publisher", "Publisher of the album")
            @JvmField val TRACK = Field(Int::class, { it.track }, "Track", "Song number within album")
            @JvmField val TRACKS_TOTAL = Field(Int::class, { it.tracksTotal }, "Tracks_total", "Number of songs in the album")
            @JvmField val TRACK_INFO = Field(NofX::class, { it.getTrackInfo() }, "Track_info", "Complete song number in format: track/track total")
            @JvmField val DISC = Field(Int::class, { it.disc }, "Disc", "Disc number within album")
            @JvmField val DISCS_TOTAL = Field(Int::class, { it.discsTotal }, "Discs_total", "Number of discs in the album")
            @JvmField val DISCS_INFO = Field(NofX::class, { it.getDiscInfo() }, "Discs_info", "Complete disc number in format: disc/disc total")
            @JvmField val GENRE = Field(String::class, { it.genre }, "Genre", "Genre of the song")
            @JvmField val YEAR = Field(Year::class, { it.getYear() }, "Year", "Year the album was published")
            @JvmField val COVER = Field(Cover::class, { it.getReadCoverOfTag() }, "Cover", "Cover of the song")
            @JvmField val RATING = Field(Double::class, { it.getRatingPercent() }, "Rating", "Song rating in 0-1 range")
            @JvmField val RATING_RAW = Field(Int::class, { it.rating }, "Rating_raw", "Song rating tag value. Depends on tag type")
            @JvmField val PLAYCOUNT = Field(Int::class, { it.getPlaycount() }, "Playcount", "Number of times the song was played.")
            @JvmField val CATEGORY = Field(String::class, { it.category }, "Category", "Category of the song. Arbitrary")
            @JvmField val COMMENT = Field(String::class, { it.comment }, "Comment", "User comment of the song. Arbitrary")
            @JvmField val LYRICS = Field(String::class, { it.lyrics }, "Lyrics", "Lyrics for the song")
            @JvmField val MOOD = Field(String::class, { it.mood }, "Mood", "Mood the song evokes")
            @JvmField val COLOR = Field(Color::class, { it.getColor() }, "Color", "Color the song evokes")
            @JvmField val TAGS = Field(String::class, { it.tags }, "Tags", "Tags associated with this song")
            @JvmField val CHAPTERS = Field(Chapters::class, { it.getChapters() }, "Chapters", "Comments at specific time points of the song")
            @JvmField val FULLTEXT = Field(Strings::class, { it.getFulltext() }, "Fulltext", "All possible fields merged into single text. Use for searching.")
            @JvmField val CUSTOM1 = Field(String::class, { it.custom1 }, "Custom1", "Custom field 1. Reserved for chapters.")
            @JvmField val CUSTOM2 = Field(String::class, { it.custom2 }, "Custom2", "Custom field 2. Reserved for color.")
            @JvmField val CUSTOM3 = Field(String::class, { it.custom3 }, "Custom3", "Custom field 3. Reserved for playback.")
            @JvmField val CUSTOM4 = Field(String::class, { it.custom4 }, "Custom4", "Custom field 4")
            @JvmField val CUSTOM5 = Field(String::class, { it.custom5 }, "Custom5", "Custom field 5")
            @JvmField val FIRST_PLAYED = Field(LocalDateTime::class, { it.getTimePlayedFirst() }, "First_played", "Marks time the song was played the first time.")
            @JvmField val LAST_PLAYED = Field(LocalDateTime::class, { it.getTimePlayedLast() }, "Last_played", "Marks time the song was played the last time.")
            @JvmField val ADDED_TO_LIBRARY = Field(LocalDateTime::class, { it.getTimeLibraryAdded() }, "Added_to_library", "Marks time the song was added to the library.")

            private val NOT_AUTO_COMPLETABLE = setOf<Field<*>>(
                    TITLE, RATING_RAW,
                    COMMENT, LYRICS, COLOR, PLAYCOUNT, PATH, FILENAME, FILESIZE, ENCODING,
                    LENGTH, TRACK, TRACKS_TOTAL, TRACK_INFO, DISC, DISCS_TOTAL, DISCS_INFO,
                    COVER, RATING, CHAPTERS
            )
            private val VISIBLE = setOf<Field<*>>(
                    TITLE, ALBUM, ARTIST, LENGTH, TRACK_INFO, DISCS_INFO, RATING, PLAYCOUNT
            )
            private val NOT_STRING_REPRESENTABLE = setOf<Field<*>>(
                    COVER, // can not be converted to string
                    CHAPTERS, // raw string form unsuitable for viewing
                    FULLTEXT // purely for search purposes
            )
            private val GROUPS_FILESIZE = Array(65) {
                when(it) {
                    0 -> FileSize(1)
                    in 1..62 -> FileSize(Math.pow(2.0, it.toDouble()).toLong())
                    63 -> FileSize(java.lang.Long.MAX_VALUE)
                    64 -> FileSize(0)
                    else -> throw SwitchException(it)
                }
            }
            private val GROUPS_RATING = (0..20).map { it*5/100.0 }.toDoubleArray()

            @JvmStatic fun valueOf(s: String): Field<*> = FIELDS_BY_NAME[s] ?: throw SwitchException(s)
        }

    }

}