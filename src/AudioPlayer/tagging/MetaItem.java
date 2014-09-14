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
import utilities.Log;
import static utilities.Parser.File.AudioFileFormat.mp3;

/**
 *
 * @author Plutonium_
 */
public abstract class MetaItem extends Item {

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
        return v<getRatingMin() ? getRatingMin() : v>getRatingMax() ? getRatingMax() : v; 
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
