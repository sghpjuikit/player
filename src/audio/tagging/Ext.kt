package audio.tagging

import mu.KotlinLogging
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagException
import org.jaudiotagger.tag.id3.AbstractID3Tag
import org.jaudiotagger.tag.images.Artwork
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
import util.Util.clip
import util.functional.Try
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException

private val logger = KotlinLogging.logger {}

/** @return maximal rating value */
val Tag.ratingMax: Int get() = when (this) {
        is AbstractID3Tag -> 255
        is VorbisCommentTag -> 100
        else -> 100
    }

/** @return minimal rating value */
val Tag.ratingMin: Int get() = 0

/** @return specified value clipped within minimal-maximal rating value range*/
fun Tag.clipRating(v: Double): Double = clip(ratingMin.toDouble(), v, ratingMax.toDouble())

/** @return audio file or error if fails */
fun File.readAudioFile(): Try<AudioFile, Void> =
        try {
            Try.ok(AudioFileIO.read(this))
        } catch (e: CannotReadException) {
            logger.error(e) { "Reading metadata failed for file $this" }
            Try.error<AudioFile>()
        } catch (e: IOException) {
            logger.error(e) { "Reading metadata failed for file $this" }
            Try.error<AudioFile>()
        } catch (e: TagException) {
            logger.error(e) { "Reading metadata failed for file $this" }
            Try.error<AudioFile>()
        } catch (e: ReadOnlyFileException) {
            logger.error(e) { "Reading metadata failed for file $this" }
            Try.error<AudioFile>()
        } catch (e: InvalidAudioFrameException) {
            logger.error(e) { "Reading metadata failed for file $this" }
            Try.error<AudioFile>()
        }

/** @return new metadata taking the data from this audio file */
fun AudioFile.toMetadata() = Metadata(this)

@Suppress("DEPRECATION")
/** @return cover image or null if none or error */
val Artwork.imageOrNull: BufferedImage? get() = try {
    // TODO: avoid
    // jaudiotagger bug, Artwork.getImage() can throw NullPointerException sometimes
    // at java.io.ByteArrayInputStream.<init>(ByteArrayInputStream.java:106) ~[na:na]
    // at org.jaudiotagger.tag.images.StandardArtwork.getImage(StandardArtwork.java:95) ~[jaudiotagger-2.2.6-SNAPSHOT.jar:na]
    image as? BufferedImage
} catch (e: NullPointerException) {
    null
}

/** @return cover information */
val Artwork.info: String? get() = "$description $mimeType ${width}x$height"