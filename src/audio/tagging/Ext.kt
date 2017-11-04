package audio.tagging

import audio.Item
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.TagException
import util.Util.clip
import util.dev.logFile
import util.file.AudioFileFormat.mp3
import java.io.File
import java.io.IOException

private val logger = logFile("ItemUtil")

/** @return maximal value of the rating */
val Item.ratingMax: Int get() = if (mp3==getFormat()) 255 else 100

/** @return minimal value of the rating */
val Item.ratingMin: Double get() = 0.0

/** @return provided value clipped to min and max rating */
fun Item.clipRating(v: Double): Double = clip(ratingMin, v, ratingMax.toDouble())

// TODO: use Try
/** @return audio file or null if error */
fun File.readAudioFile(): AudioFile? =
    try {
        AudioFileIO.read(this)
    } catch (e: CannotReadException) {
        logger.error("Reading metadata failed for file {}", this, e)
        null
    } catch (e: IOException) {
        logger.error("Reading metadata failed for file {}", this, e)
        null
    } catch (e: TagException) {
        logger.error("Reading metadata failed for file {}", this, e)
        null
    } catch (e: ReadOnlyFileException) {
        logger.error("Reading metadata failed for file {}", this, e)
        null
    } catch (e: InvalidAudioFrameException) {
        logger.error("Reading metadata failed for file {}", this, e)
        null
    }