package sp.it.pl.audio.tagging

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import javafx.scene.image.Image
import mu.KotlinLogging
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.CannotReadVideoException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagException
import org.jaudiotagger.tag.id3.AbstractID3Tag
import org.jaudiotagger.tag.images.Artwork
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
import sp.it.pl.main.isAudio
import sp.it.util.functional.Try
import sp.it.util.functional.runTry
import sp.it.util.math.clip
import sp.it.util.ui.image.ImageLoadParamOfData
import sp.it.util.ui.image.loadImagePsd

private val logger = KotlinLogging.logger {}

/** @return maximal rating value */
val Tag.ratingMax: Int
   get() = when (this) {
      is AbstractID3Tag -> 255
      is VorbisCommentTag -> 100
      else -> 100
   }

/** @return minimal rating value */
val Tag.ratingMin: Int get() = 0

/** @return specified value clipped within minimal-maximal rating value range*/
fun Tag.clipRating(v: Double): Double = v.clip(ratingMin.toDouble(), ratingMax.toDouble())

/** @return audio file or error if fails */
fun File.readAudioFile(): Try<AudioFile?, Throwable> {
   val onError = { e: Throwable ->
      logger.error(e) { "Reading metadata failed for file $this" }
      Try.error(e)
   }
   return try {
      Try.ok(AudioFileIO.read(this))
   } catch (e: CannotReadVideoException) {
      if (!isAudio()) Try.ok()
      else onError(e)
   } catch (e: CannotReadException) {
      if (!isAudio()) Try.ok()
      else onError(e)
   } catch (e: IOException) {
      onError(e)
   } catch (e: TagException) {
      onError(e)
   } catch (e: ReadOnlyFileException) {
      onError(e)
   } catch (e: InvalidAudioFrameException) {
      onError(e)
   } catch (e: Throwable) {
      onError(e)
   }
}

/** @return new metadata taking the data from this audio file */
fun AudioFile.toMetadata() = Metadata(this)

/** @return cover image or null if none or error */
fun Artwork.imageOrNull(p: ImageLoadParamOfData): Try<Image?, Throwable> = runTry {
   loadImagePsd(ByteArrayInputStream(binaryData ?: return@runTry null), p, false)
}
/** @return cover information */
val Artwork.info: String
   get() = if (width<=0 || height <=0) "$description $mimeType" else "$description $mimeType ${width}x$height"