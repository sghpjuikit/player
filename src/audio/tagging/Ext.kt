package audio.tagging

import audio.Item
import mu.KotlinLogging
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.TagException
import util.Util.clip
import util.file.AudioFileFormat.mp3
import java.io.File
import java.io.IOException

private val logger = KotlinLogging.logger {}

/** @return maximal value of the rating */
val Item.ratingMax: Int get() = if (mp3==getFormat()) 255 else 100  // TODO: make a Tag.extensionMethod

/** @return minimal value of the rating */
val Item.ratingMin: Double get() = 0.0  // TODO: make a Tag.extensionMethod

/** @return provided value clipped to min and max rating */
fun Item.clipRating(v: Double): Double = clip(ratingMin, v, ratingMax.toDouble())  // TODO: make a Tag.extensionMethod

// TODO: use Try
/** @return audio file or null if error */
fun File.readAudioFile(): AudioFile? =
    try {
        AudioFileIO.read(this)
    } catch (e: CannotReadException) {
        logger.error(e) { "Reading metadata failed for file $this" }
        null
    } catch (e: IOException) {
        logger.error(e) { "Reading metadata failed for file $this" }
        null
    } catch (e: TagException) {
        logger.error(e) { "Reading metadata failed for file $this" }
        null
    } catch (e: ReadOnlyFileException) {
        logger.error(e) { "Reading metadata failed for file $this" }
        null
    } catch (e: InvalidAudioFrameException) {
        logger.error(e) { "Reading metadata failed for file $this" }
        null
    }