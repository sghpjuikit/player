package audio.tagging;

import audio.Item;
import java.io.File;
import java.io.IOException;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import static org.slf4j.LoggerFactory.getLogger;
import static util.Util.clip;
import static util.file.AudioFileFormat.mp3;

public abstract class MetaItem extends Item {

	/** @return maximal value of the rating. */
	public int getRatingMax() {
		return mp3==getFormat() ? 255 : 100;
	}

	/** @return minimal value of the rating. */
	public double getRatingMin() {
		return 0;
	}

	/** @return provided value clipped to min and max rating */
	double clipRating(double v) {
		return clip(getRatingMin(), v, getRatingMax());
	}

	// TODO: use Try
	/**
	 * Reads file, returns AudioFile object.
	 *
	 * @return audio file or null if error.
	 */
	public static AudioFile readAudioFile(File file) {
		try {
			return AudioFileIO.read(file);
		} catch (CannotReadException|IOException|TagException|ReadOnlyFileException|InvalidAudioFrameException e) {
			getLogger(MetaItem.class).error("Reading metadata failed for file {}", file, e);
			return null;
		}
	}
}