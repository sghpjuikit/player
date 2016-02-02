/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.tagging;

import java.io.File;
import java.io.IOException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import AudioPlayer.Item;

import static org.slf4j.LoggerFactory.getLogger;
import static util.file.AudioFileFormat.mp3;
import static util.Util.clip;

/**
 *
 * @author Plutonium_
 */
public abstract class MetaItem<CI extends Item> extends Item<CI> {

    /** @return maximal value of the rating. */
    public int getRatingMax() {
        return mp3 == getFormat() ? 255 : 100;
    }

    /**  @return minimal value of the rating. */
    public double getRatingMin() {
        return 0;
    }

    /** @return provided value clipped to min and max rating */
    double clipRating(double v) {
        return clip(getRatingMin(),v,getRatingMax());
    }

//****************************** HELPER FUNCTIONS ****************************//

    /**
     * Reads file, returns AudioFile object.
     * @param file
     * @return Null if error.
     */
    public static AudioFile readAudioFile(File file) {
        try {
            return AudioFileIO.read(file);
        } catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
            getLogger(MetaItem.class).error("Reading metadata failed for file {}", file,e );
            return null;
        }
    }
}
