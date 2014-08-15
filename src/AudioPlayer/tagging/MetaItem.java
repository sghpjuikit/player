/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.tagging;

import AudioPlayer.playlist.Item;
import java.io.File;
import java.io.IOException;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import static utilities.AudioFileFormat.flac;
import static utilities.AudioFileFormat.mp3;
import static utilities.AudioFileFormat.ogg;
import utilities.Log;

/**
 *
 * @author Plutonium_
 */
public abstract class MetaItem extends Item {

    /** @return maximal value of the rating. */
    public double getRatingMax() {
        if (getFormat() == mp3)
            return 255;
        else if (getFormat() == flac || getFormat() == ogg)
            return 100;
        else
            return 100; // this case should never happen!
    }
    
    /**  @return minimal value of the rating. */
    public double getRatingMin() {
        return 0;
    }
    
    double clipRating(double val) {
        if (val < getRatingMin())
            return getRatingMin();
        else if (val > getRatingMax())
            return getRatingMax();
        else
            return val;
    }
    
//****************************** HELPER FUNCTIONS ****************************//
    
    /**
     * Reads file, returns AudioFile object.
     * @param file
     * @return Null if error.
     */
    static AudioFile readAudioFile(File file) {
        try {
            return AudioFileIO.read(file);
        } catch ( CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException ex) {
            Log.err("Reading metadata failed for file: " + file.toPath() + "."
                    + "Corrupt or inaccessible file or data.");
            return null;
        }
    }
}
